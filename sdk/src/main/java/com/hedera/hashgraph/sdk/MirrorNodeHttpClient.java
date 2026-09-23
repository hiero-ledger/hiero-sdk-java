// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;

/**
 * The mirror node REST adapter: the layer that owns base-URL resolution, retry, backoff and the timeout
 * bounds, sitting between a query and an {@link HttpTransport}.
 *
 * <p>Not public API. Exporting it would make "call any mirror node REST endpoint" a supported feature
 * forever; the decision is one-way by design, since a later release can export it and no release can
 * unexport it.
 *
 * <p>It owns nothing. One transport can back several instances, which is what lets a single connection
 * pool serve several mirror nodes.
 *
 * <p>This class blocks the calling thread. Queries call it from the {@link Client}'s executor, matching
 * how every other mirror node REST query in this SDK is executed.
 */
final class MirrorNodeHttpClient {
    /**
     * Clamp on the exponent, so {@code initialBackoff * 2^n} cannot overflow on a large attempt budget.
     */
    private static final int MAX_BACKOFF_SHIFT = 30;

    private static final long SLEEP_SLICE_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

    private final String baseUrl;
    private final HttpTransport transport;
    private final MirrorNodeHttpRetryPolicy retryPolicy;
    private final Map<String, String> requestHeaders;
    private final BooleanSupplier clientClosed;

    private MirrorNodeHttpClient(
            String baseUrl,
            HttpTransport transport,
            MirrorNodeHttpRetryPolicy retryPolicy,
            Map<String, String> requestHeaders,
            BooleanSupplier clientClosed) {
        this.baseUrl = baseUrl;
        this.transport = transport;
        this.retryPolicy = retryPolicy;
        this.requestHeaders = requestHeaders;
        this.clientClosed = clientClosed;
    }

    /**
     * Build an adapter bound to one mirror node for the whole of one call.
     *
     * @param baseUrl the mirror node REST base URL, pinned for every attempt and every page of the call
     * @param transport the transport to exchange through; never closed by this class
     * @param retryPolicy the resolved budget, whose total deadline must already be positive
     * @param requestHeaders the caller headers from {@link MirrorNodeHttpConfig}
     * @param clientClosed whether the owning client has shut down
     * @return the new adapter
     */
    static MirrorNodeHttpClient create(
            String baseUrl,
            HttpTransport transport,
            MirrorNodeHttpRetryPolicy retryPolicy,
            Map<String, String> requestHeaders,
            BooleanSupplier clientClosed) {
        return new MirrorNodeHttpClient(
                Objects.requireNonNull(baseUrl, "baseUrl must not be null"),
                Objects.requireNonNull(transport, "transport must not be null"),
                Objects.requireNonNull(retryPolicy, "retryPolicy must not be null"),
                Objects.requireNonNull(requestHeaders, "requestHeaders must not be null"),
                Objects.requireNonNull(clientClosed, "clientClosed must not be null"));
    }

    /**
     * Resolve the one bound that spans a whole call.
     *
     * <p>A total deadline of zero on the policy means "inherit the client's request timeout" rather than
     * "no bound": merged SDK policy already defines {@code requestTimeout} as the overall execution
     * duration of a query including retries, backoff and node rotation, and a mirror node REST query is a
     * query. An explicit per-call timeout argument overrides both.
     *
     * @param policy the resolved retry policy
     * @param clientRequestTimeout the client's request timeout
     * @param explicitTimeout the timeout the caller passed to {@code execute}, or null
     * @return the budget for the whole call
     */
    static Duration resolveTotalDeadline(
            MirrorNodeHttpRetryPolicy policy, Duration clientRequestTimeout, @Nullable Duration explicitTimeout) {
        if (explicitTimeout != null) {
            return explicitTimeout;
        }

        return policy.getTotalDeadline().isZero() ? clientRequestTimeout : policy.getTotalDeadline();
    }

    /**
     * Extract the mirror node this adapter is pinned to.
     *
     * @return the base URL
     */
    String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Extract the resolved retry budget.
     *
     * @return the policy
     */
    MirrorNodeHttpRetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    /**
     * Perform a {@code GET}, retrying per the policy.
     *
     * @param path the path below the base URL
     * @param cancellation the caller's cancellation, passed through unwrapped to every attempt
     * @param deadline the call's remaining budget tracker
     * @return the response on any status the policy does not retry, including 4xx
     */
    HttpResponse get(MirrorNodeRestPath path, Cancellation cancellation, CallDeadline deadline) {
        return execute(HttpRequest.create(HttpMethod.GET, path.resolveAgainst(baseUrl)), cancellation, deadline);
    }

    /**
     * Perform a {@code POST}, retrying per the policy.
     *
     * <p>A {@code POST} is retried like a {@code GET}: every mirror node REST endpoint in scope is
     * read-only, so the method is not a reason to suppress a retry. The body is replayed byte for byte
     * on each attempt.
     *
     * @param path the path below the base URL
     * @param contentType the body's media type
     * @param body the request body
     * @param cancellation the caller's cancellation, passed through unwrapped to every attempt
     * @param deadline the call's remaining budget tracker
     * @return the response on any status the policy does not retry, including 4xx
     */
    HttpResponse post(
            MirrorNodeRestPath path,
            String contentType,
            byte[] body,
            Cancellation cancellation,
            CallDeadline deadline) {
        return execute(
                HttpRequest.create(HttpMethod.POST, path.resolveAgainst(baseUrl))
                        .withBody(body, contentType),
                cancellation,
                deadline);
    }

    private HttpResponse execute(HttpRequest baseRequest, Cancellation cancellation, CallDeadline deadline) {
        var request = withHeaders(baseRequest);

        HttpResponse lastResponse = null;
        HttpTransportException lastFailure = null;

        for (var attempt = 1; attempt <= retryPolicy.getMaxAttempts(); attempt++) {
            requireStillRunning(cancellation);

            var remaining = deadline.remainingNanos();
            if (remaining <= 0) {
                throw deadlineExceeded(lastResponse);
            }

            lastResponse = null;
            lastFailure = null;

            try {
                lastResponse = await(
                        transport.roundTrip(request.withDeadline(attemptDeadline(remaining)), cancellation), remaining);

                if (!retryPolicy.isRetryable(lastResponse.getStatusCode())) {
                    return lastResponse;
                }
            } catch (HttpTransportException e) {
                // A terminal failure consumes exactly one attempt and must not burn the budget.
                if (!e.isRetryable()) {
                    throw e;
                }
                lastFailure = e;
            }

            if (attempt == retryPolicy.getMaxAttempts()) {
                break;
            }

            sleep(backoffFor(attempt - 1, lastResponse, deadline), cancellation, deadline);
        }

        if (lastFailure != null) {
            throw lastFailure;
        }

        throw new MirrorNodeHttpException(
                HttpTransportErrorKind.RETRIES_EXHAUSTED_ERROR,
                "the mirror node answered with a retryable status on every one of " + retryPolicy.getMaxAttempts()
                        + " attempts",
                lastResponse);
    }

    private HttpRequest withHeaders(HttpRequest request) {
        var withCallerHeaders = request.withHeaders(requestHeaders);

        var contentType = request.getContentType();
        if (contentType != null) {
            // The endpoint's own content type outranks anything the caller configured.
            withCallerHeaders = withCallerHeaders.withHeader("content-type", contentType);
        }

        // The identity header is owned by the SDK and wins over everything.
        return withCallerHeaders.withHeader(SdkUserAgent.HEADER_NAME, SdkUserAgent.value());
    }

    private Duration attemptDeadline(long remainingNanos) {
        var perAttempt = retryPolicy.getPerAttemptTimeout();

        // Zero means "no per-attempt cap", so the remaining total is the only bound.
        if (perAttempt.isZero()) {
            return Duration.ofNanos(remainingNanos);
        }

        return Duration.ofNanos(Math.min(perAttempt.toNanos(), remainingNanos));
    }

    /**
     * How long to wait before the next attempt: {@code Retry-After} if the node sent one, otherwise
     * exponential backoff with full jitter.
     */
    private Duration backoffFor(int retryIndex, @Nullable HttpResponse response, CallDeadline deadline) {
        var retryAfter = response == null ? null : parseRetryAfter(response);

        if (retryAfter != null) {
            // Retry-After always wins, bounded by the remaining total deadline and never by maxBackoff:
            // maxBackoff caps a guess about how long to wait, while Retry-After is the node stating the
            // fact the guess approximates.
            if (retryAfter.toNanos() > deadline.remainingNanos()) {
                throw deadlineExceeded(response);
            }
            return retryAfter;
        }

        var shift = Math.min(retryIndex, MAX_BACKOFF_SHIFT);
        var capNanos = Math.min(
                retryPolicy.getMaxBackoff().toNanos(),
                retryPolicy.getInitialBackoff().toNanos() << shift);

        if (capNanos <= 0) {
            return Duration.ZERO;
        }

        // Full jitter rather than a fixed curve: every SDK retrying a throttled mirror node on the same
        // curve re-converges on it.
        return Duration.ofNanos(ThreadLocalRandom.current().nextLong(capNanos));
    }

    @Nullable
    private static Duration parseRetryAfter(HttpResponse response) {
        var raw = response.getFirstHeaderValue("retry-after").orElse(null);
        if (raw == null) {
            return null;
        }

        var trimmed = raw.trim();

        try {
            return Duration.ofSeconds(Long.parseLong(trimmed));
        } catch (NumberFormatException ignored) {
            // Not delta-seconds, so it must be the HTTP-date form.
        }

        try {
            var when = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME);
            var delta = Duration.between(ZonedDateTime.now(when.getZone()), when);
            // A date already in the past means "now".
            return delta.isNegative() ? Duration.ZERO : delta;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private HttpResponse await(CompletionStage<HttpResponse> stage, long remainingNanos) {
        var future = stage.toCompletableFuture();

        try {
            return future.get(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof HttpTransportException transportException) {
                throw transportException;
            }
            // A transport failure the adapter does not recognise is terminal. Non-retryable is the safe
            // direction: a wrongly terminal error surfaces after one attempt, a wrongly retryable one
            // costs the whole budget and hides the cause.
            throw cause instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new IllegalStateException("the HTTP transport failed", cause);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new HttpTransportException(
                    HttpTransportErrorKind.TIMEOUT_ERROR, "the transport did not honour its deadline", e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new HttpTransportException(HttpTransportErrorKind.CANCELLED_ERROR, "the call was interrupted", e);
        }
    }

    /**
     * Sleep, but wake for a cancellation, for the total deadline, and for {@code Client.close()} — a
     * bound discovered after the sleep is not a bound.
     */
    private void sleep(Duration duration, Cancellation cancellation, CallDeadline deadline) {
        var endNanos = System.nanoTime() + duration.toNanos();

        while (true) {
            requireStillRunning(cancellation);

            var now = System.nanoTime();
            if (now - endNanos >= 0) {
                return;
            }

            if (deadline.remainingNanos() <= 0) {
                throw deadlineExceeded(null);
            }

            var slice = Math.min(endNanos - now, SLEEP_SLICE_NANOS);

            try {
                TimeUnit.NANOSECONDS.sleep(slice);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new HttpTransportException(
                        HttpTransportErrorKind.CANCELLED_ERROR, "the call was interrupted while backing off", e);
            }
        }
    }

    private void requireStillRunning(Cancellation cancellation) {
        // An injected transport is never closed by the SDK, so without this check it would go on serving
        // retries for a client that has already shut down.
        if (clientClosed.getAsBoolean()) {
            throw new HttpTransportException(
                    HttpTransportErrorKind.CLIENT_CLOSED_ERROR, "the client was closed during the call");
        }

        if (cancellation.isCancelled()) {
            throw new HttpTransportException(HttpTransportErrorKind.CANCELLED_ERROR, "the call was cancelled");
        }
    }

    private static MirrorNodeHttpException deadlineExceeded(@Nullable HttpResponse lastResponse) {
        return new MirrorNodeHttpException(
                HttpTransportErrorKind.DEADLINE_EXCEEDED_ERROR, "the call exceeded its total deadline", lastResponse);
    }

    /**
     * The one budget that spans a whole call, every page and every backoff included.
     *
     * <p>A call is one query execution; an attempt is one exchange. The distinction is load-bearing: a
     * paginated read is one call and many attempts, and a {@code k}-page walk under a per-exchange
     * reading would get {@code k} times the budget while claiming to be bounded.
     */
    static final class CallDeadline {
        private final long endNanos;

        private CallDeadline(long endNanos) {
            this.endNanos = endNanos;
        }

        /**
         * Start the clock.
         *
         * @param total the whole budget for the call; must be positive
         * @return the new tracker
         */
        static CallDeadline startingNow(Duration total) {
            return new CallDeadline(System.nanoTime() + total.toNanos());
        }

        /**
         * How much of the budget is left.
         *
         * @return the remaining nanoseconds, which may be zero or negative
         */
        long remainingNanos() {
            return endNanos - System.nanoTime();
        }
    }
}
