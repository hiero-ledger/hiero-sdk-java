// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.io.ByteArrayOutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import javax.net.ssl.SSLException;

/**
 * The SDK-supplied {@link HttpTransport}, used when no transport is injected.
 *
 * <p>This is the <b>only</b> class in the SDK's new transport layer that names {@code java.net.http},
 * and it always names it fully qualified — the SDK's own {@link HttpRequest} and {@link HttpResponse}
 * live in this package and would otherwise be ambiguous to a reader. Please do not "tidy" the qualified
 * names into imports. Keeping every {@code java.net.http} reference inside this one class is also what
 * lets an Android application inject an OkHttp-backed transport and never load this class, on a
 * platform where {@code java.net.http} does not exist at any API level.
 */
public final class DefaultHttpTransport implements HttpTransport {
    /**
     * Headers {@code java.net.http.HttpRequest.Builder.header} refuses outright. They are dropped
     * rather than allowed to raise, because the SPI contract is that {@code roundTrip} returns a failed
     * stage and never throws.
     */
    private static final Set<String> RESTRICTED_HEADERS =
            Set.of("connection", "content-length", "date", "expect", "from", "host", "upgrade", "via", "warning");

    private final java.net.http.HttpClient httpClient;
    private final HttpTransportConfiguration configuration;
    private final ScheduledExecutorService deadlineScheduler;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private DefaultHttpTransport(java.net.http.HttpClient httpClient, HttpTransportConfiguration configuration) {
        this.httpClient = httpClient;
        this.configuration = configuration;
        this.deadlineScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "hiero-sdk-http-deadline");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Build a transport with a <b>private</b> connection pool, so pool limits and TLS settings belong to
     * the SDK and are not affected by anything else in the process.
     *
     * <p>The pool is private but not isolated from the environment: like the JDK's own default client it
     * honours the {@code http.proxyHost} family of system properties.
     *
     * @param configuration how to build it
     * @return the new transport
     */
    public static DefaultHttpTransport create(HttpTransportConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration must not be null");

        var builder = java.net.http.HttpClient.newBuilder()
                .followRedirects(
                        configuration.getMaxRedirects() == 0
                                ? java.net.http.HttpClient.Redirect.NEVER
                                : java.net.http.HttpClient.Redirect.NORMAL);

        // Zero means "keep the platform stack's own bound", which is expressed by not calling the
        // builder method at all.
        if (!configuration.getConnectTimeout().isZero()) {
            builder.connectTimeout(configuration.getConnectTimeout());
        }

        return new DefaultHttpTransport(builder.build(), configuration);
    }

    @Override
    public CompletionStage<HttpResponse> roundTrip(HttpRequest request, Cancellation cancellation) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(cancellation, "cancellation must not be null");

        if (closed.get()) {
            return CompletableFuture.failedStage(new HttpTransportException(
                    HttpTransportErrorKind.CLIENT_CLOSED_ERROR, "the HTTP transport has been closed"));
        }

        if (cancellation.isCancelled()) {
            return CompletableFuture.failedStage(new HttpTransportException(
                    HttpTransportErrorKind.CANCELLED_ERROR, "the call was cancelled before it started"));
        }

        java.net.http.HttpRequest jdkRequest;
        try {
            jdkRequest = toJdkRequest(request);
        } catch (RuntimeException e) {
            // A request the JDK will not express is a caller error, not a transport failure. It reaches
            // the adapter unrecognised and is therefore terminal, which is correct.
            return CompletableFuture.failedStage(e);
        }

        // A deadline and a caller cancellation both abort the exchange through the same cancel(true),
        // so the resulting CancellationException cannot say which fired. Whichever gets here first
        // records itself.
        var cause = new AtomicReference<HttpTransportErrorKind>();
        var result = new CompletableFuture<HttpResponse>();

        var inflight = httpClient.sendAsync(jdkRequest, boundedBodyHandler(configuration.getMaxResponseBytes()));

        var registration = cancellation.onCancel(() -> {
            cause.compareAndSet(null, HttpTransportErrorKind.CANCELLED_ERROR);
            inflight.cancel(true);
        });

        var deadline = request.getDeadline();
        ScheduledFuture<?> timer = null;
        if (deadline != null && !deadline.isZero()) {
            timer = deadlineScheduler.schedule(
                    () -> {
                        cause.compareAndSet(null, HttpTransportErrorKind.TIMEOUT_ERROR);
                        inflight.cancel(true);
                    },
                    Math.max(1L, deadline.toNanos()),
                    TimeUnit.NANOSECONDS);
        }
        var scheduledTimer = timer;

        inflight.whenComplete((response, throwable) -> {
            if (scheduledTimer != null) {
                scheduledTimer.cancel(false);
            }
            registration.release();

            if (throwable != null) {
                result.completeExceptionally(toTransportException(throwable, cause.get()));
            } else {
                result.complete(fromJdkResponse(response));
            }
        });

        return result;
    }

    @Override
    public void close(Duration closeTimeout) {
        Objects.requireNonNull(closeTimeout, "closeTimeout must not be null");

        if (!closed.compareAndSet(false, true)) {
            return;
        }

        try {
            httpClient.shutdown();
            if (!httpClient.awaitTermination(closeTimeout)) {
                httpClient.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            httpClient.shutdownNow();
        } finally {
            deadlineScheduler.shutdownNow();
        }
    }

    private java.net.http.HttpRequest toJdkRequest(HttpRequest request) {
        var builder = java.net.http.HttpRequest.newBuilder().uri(URI.create(request.getUrl()));

        // Precedence, later winning: transport defaults, then the request's own headers.
        var merged = new LinkedHashMap<String, String>(configuration.getDefaultHeaders());
        merged.putAll(request.getHeaders());

        var contentType = request.getContentType();
        if (contentType != null) {
            merged.put("content-type", contentType);
        }

        for (var entry : merged.entrySet()) {
            if (!RESTRICTED_HEADERS.contains(entry.getKey())) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        var body = request.getBody();
        builder.method(
                request.getMethod().name(),
                body == null
                        ? java.net.http.HttpRequest.BodyPublishers.noBody()
                        : java.net.http.HttpRequest.BodyPublishers.ofByteArray(body));

        // A cheap fast-fail only. It bounds time to response headers, not the whole exchange, which is
        // why the deadline is also enforced by cancelling the exchange outright.
        var deadline = request.getDeadline();
        if (deadline != null && !deadline.isZero()) {
            builder.timeout(deadline);
        }

        return builder.build();
    }

    private static HttpResponse fromJdkResponse(java.net.http.HttpResponse<byte[]> response) {
        var headers = new HashMap<String, List<String>>();
        for (var entry : response.headers().map().entrySet()) {
            headers.merge(entry.getKey().toLowerCase(Locale.ROOT), List.copyOf(entry.getValue()), (a, b) -> {
                var combined = new ArrayList<>(a);
                combined.addAll(b);
                return List.copyOf(combined);
            });
        }

        return HttpResponse.create(response.statusCode(), response.body(), headers);
    }

    private HttpTransportException toTransportException(Throwable throwable, @Nullable HttpTransportErrorKind cause) {
        var unwrapped = unwrap(throwable);

        for (var link = unwrapped; link != null; link = link.getCause()) {
            if (link instanceof HttpTransportException transportException) {
                return transportException;
            }
        }

        if (unwrapped instanceof java.util.concurrent.CancellationException) {
            var kind = cause != null ? cause : HttpTransportErrorKind.CANCELLED_ERROR;
            return new HttpTransportException(
                    kind,
                    kind == HttpTransportErrorKind.TIMEOUT_ERROR
                            ? "the request deadline elapsed"
                            : "the call was cancelled",
                    unwrapped);
        }

        if (unwrapped instanceof java.net.http.HttpTimeoutException) {
            // HttpConnectTimeoutException is a subtype, so both land here.
            return new HttpTransportException(
                    HttpTransportErrorKind.TIMEOUT_ERROR, "the request deadline elapsed", unwrapped);
        }

        for (var link = unwrapped; link != null; link = link.getCause()) {
            if (link instanceof SSLException || link instanceof CertificateException) {
                return new HttpTransportException(
                        HttpTransportErrorKind.TLS_ERROR,
                        "TLS handshake or certificate verification failed",
                        unwrapped);
            }
        }

        // The JDK frequently reports a name-resolution failure as a ConnectException wrapping an
        // UnknownHostException, so the whole chain has to be walked before falling through.
        for (var link = unwrapped; link != null; link = link.getCause()) {
            if (link instanceof UnknownHostException) {
                return new HttpTransportException(
                        HttpTransportErrorKind.UNKNOWN_HOST_ERROR, "the host name does not resolve", unwrapped);
            }
        }

        if (closed.get() && unwrapped instanceof IllegalStateException) {
            return new HttpTransportException(
                    HttpTransportErrorKind.CLIENT_CLOSED_ERROR, "the HTTP transport has been closed", unwrapped);
        }

        var message = unwrapped instanceof ConnectException ? "the connection was refused" : "the request failed";
        return new HttpTransportException(HttpTransportErrorKind.CONNECTION_ERROR, message, unwrapped);
    }

    private static Throwable unwrap(Throwable throwable) {
        var current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * A body handler that refuses a body larger than the cap rather than truncating it.
     *
     * <p>Truncating silently would hand the bytes to a JSON parser as malformed data, producing an error
     * that names the wrong cause. It also pre-checks {@code content-length}, so an over-large body a
     * well-behaved server announces is never buffered at all.
     */
    private static java.net.http.HttpResponse.BodyHandler<byte[]> boundedBodyHandler(long maxResponseBytes) {
        return responseInfo -> {
            var contentLength = responseInfo.headers().firstValueAsLong("content-length");
            var announcedTooLarge = contentLength.isPresent() && contentLength.getAsLong() > maxResponseBytes;
            return new BoundedBodySubscriber(maxResponseBytes, announcedTooLarge);
        };
    }

    private static final class BoundedBodySubscriber implements java.net.http.HttpResponse.BodySubscriber<byte[]> {
        private final long maxResponseBytes;
        private final boolean rejectImmediately;
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private long total = 0L;
        private boolean done = false;

        @Nullable
        private Flow.Subscription subscription;

        BoundedBodySubscriber(long maxResponseBytes, boolean rejectImmediately) {
            this.maxResponseBytes = maxResponseBytes;
            this.rejectImmediately = rejectImmediately;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;

            if (rejectImmediately) {
                reject();
                return;
            }

            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            if (done) {
                return;
            }

            for (ByteBuffer item : items) {
                total += item.remaining();

                // Strictly greater: the cap itself is allowed, one byte past it is not.
                if (total > maxResponseBytes) {
                    reject();
                    return;
                }

                var chunk = new byte[item.remaining()];
                item.get(chunk);
                buffer.writeBytes(chunk);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (!done) {
                done = true;
                body.completeExceptionally(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                body.complete(buffer.toByteArray());
            }
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        private void reject() {
            done = true;
            if (subscription != null) {
                subscription.cancel();
            }
            body.completeExceptionally(new HttpTransportException(
                    HttpTransportErrorKind.RESPONSE_TOO_LARGE_ERROR,
                    "the response body exceeded " + maxResponseBytes + " bytes"));
        }
    }
}
