// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MirrorNodeHttpClientTest {

    private static final String BASE_URL = "https://mirror.example/api/v1";
    private static final MirrorNodeRestPath PATH = MirrorNodeRestPath.of("/accounts/0.0.5/tokens");

    private final FakeTransport transport = new FakeTransport();

    private MirrorNodeHttpClient clientWith(MirrorNodeHttpRetryPolicy policy) {
        return clientWith(policy, Collections.emptyMap(), () -> false);
    }

    private MirrorNodeHttpClient clientWith(
            MirrorNodeHttpRetryPolicy policy, Map<String, String> headers, java.util.function.BooleanSupplier closed) {
        return MirrorNodeHttpClient.create(BASE_URL, transport, policy, headers, closed);
    }

    private static MirrorNodeHttpRetryPolicy fastPolicy() {
        return MirrorNodeHttpRetryPolicy.defaults()
                .withInitialBackoff(Duration.ofMillis(1))
                .withMaxBackoff(Duration.ofMillis(2));
    }

    private static MirrorNodeHttpClient.CallDeadline deadline(Duration total) {
        return MirrorNodeHttpClient.CallDeadline.startingNow(total);
    }

    private static HttpResponse response(int status, String body) {
        return HttpResponse.create(status, body.getBytes(StandardCharsets.UTF_8), Map.of());
    }

    private static HttpResponse response(int status, String body, Map<String, List<String>> headers) {
        return HttpResponse.create(status, body.getBytes(StandardCharsets.UTF_8), headers);
    }

    @Test
    @DisplayName("A 503 followed by a 200 retries once and succeeds")
    void retriesARetryableStatus() {
        transport.enqueue(response(503, "busy"));
        transport.enqueue(response(200, "ok"));

        var result = clientWith(fastPolicy()).get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10)));

        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(transport.requests).hasSize(2);
    }

    @Test
    @DisplayName("A 400 is returned to the caller rather than retried")
    void doesNotRetryATerminalStatus() {
        transport.enqueue(response(400, "{\"_status\":{\"messages\":[{\"message\":\"bad\"}]}}"));

        var result = clientWith(fastPolicy()).get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10)));

        assertThat(result.getStatusCode()).isEqualTo(400);
        assertThat(new String(result.getBody(), StandardCharsets.UTF_8)).contains("bad");
        assertThat(transport.requests).hasSize(1);
    }

    @Test
    void retries408() {
        transport.enqueue(response(408, ""));
        transport.enqueue(response(200, "ok"));

        assertThat(clientWith(fastPolicy())
                        .get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10)))
                        .getStatusCode())
                .isEqualTo(200);
        assertThat(transport.requests).hasSize(2);
    }

    @Test
    @DisplayName("501 is a server that will answer identically next time, so it is not retried")
    void doesNotRetry501() {
        transport.enqueue(response(501, ""));

        assertThat(clientWith(fastPolicy())
                        .get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10)))
                        .getStatusCode())
                .isEqualTo(501);
        assertThat(transport.requests).hasSize(1);
    }

    @Test
    @DisplayName("A retryable status on every attempt raises retries-exhausted-error with the last response")
    void reportsExhaustedRetries() {
        var policy = fastPolicy().withMaxAttempts(3);
        for (var i = 0; i < 3; i++) {
            transport.enqueue(response(503, "still busy"));
        }

        var failure = catchThrowableOfType(MirrorNodeHttpException.class, () -> clientWith(policy)
                .get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10))));

        assertThat(failure.getKind()).isEqualTo(HttpTransportErrorKind.RETRIES_EXHAUSTED_ERROR);
        assertThat(failure.getLastResponse()).isNotNull();
        assertThat(failure.getLastResponse().getStatusCode()).isEqualTo(503);
        assertThat(transport.requests).hasSize(3);
    }

    @Test
    @DisplayName("A terminal transport failure consumes exactly one attempt")
    void doesNotRetryATerminalTransportFailure() {
        transport.enqueue(new HttpTransportException(
                HttpTransportErrorKind.UNKNOWN_HOST_ERROR, "the host name does not resolve"));

        assertThatThrownBy(
                        () -> clientWith(fastPolicy()).get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10))))
                .isInstanceOf(HttpTransportException.class)
                .extracting(e -> ((HttpTransportException) e).getKind())
                .isEqualTo(HttpTransportErrorKind.UNKNOWN_HOST_ERROR);

        assertThat(transport.requests).hasSize(1);
    }

    @Test
    @DisplayName("A failure outside the seven identifiers is treated as non-retryable")
    void doesNotRetryAnUnrecognisedFailure() {
        transport.enqueue(new IllegalArgumentException("a hand-written adapter threw something else"));

        assertThatThrownBy(
                        () -> clientWith(fastPolicy()).get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(transport.requests).hasSize(1);
    }

    @Test
    @DisplayName("A retryable transport failure is retried")
    void retriesARetryableTransportFailure() {
        transport.enqueue(
                new HttpTransportException(HttpTransportErrorKind.CONNECTION_ERROR, "the connection was refused"));
        transport.enqueue(response(200, "ok"));

        assertThat(clientWith(fastPolicy())
                        .get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10)))
                        .getStatusCode())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Retry-After wins over the computed backoff")
    void honoursRetryAfterInSeconds() {
        transport.enqueue(response(429, "", Map.of("Retry-After", List.of("1"))));
        transport.enqueue(response(200, "ok"));

        var start = System.nanoTime();
        var result = clientWith(fastPolicy()).get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(30)));
        var elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(900));
    }

    @Test
    @DisplayName("A Retry-After longer than the remaining deadline fails immediately rather than waiting")
    void retryAfterBeyondTheDeadlineFailsFast() {
        transport.enqueue(response(429, "", Map.of("retry-after", List.of("3600"))));

        var start = System.nanoTime();
        var failure = catchThrowableOfType(MirrorNodeHttpException.class, () -> clientWith(fastPolicy())
                .get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(5))));
        var elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(failure.getKind()).isEqualTo(HttpTransportErrorKind.DEADLINE_EXCEEDED_ERROR);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
        assertThat(transport.requests).hasSize(1);
    }

    @Test
    @DisplayName("Running out of total deadline is distinguishable from exhausting the attempt budget")
    void reportsDeadlineExceeded() {
        var policy = MirrorNodeHttpRetryPolicy.defaults()
                .withMaxAttempts(10)
                .withInitialBackoff(Duration.ofMillis(200))
                .withMaxBackoff(Duration.ofMillis(200));

        for (var i = 0; i < 10; i++) {
            transport.enqueue(response(503, ""));
        }

        var failure = catchThrowableOfType(MirrorNodeHttpException.class, () -> clientWith(policy)
                .get(PATH, Cancellation.none(), deadline(Duration.ofMillis(300))));

        assertThat(failure.getKind()).isEqualTo(HttpTransportErrorKind.DEADLINE_EXCEEDED_ERROR);
        assertThat(transport.requests).hasSizeLessThan(10);
    }

    @Test
    @DisplayName("A cancellation during a backoff sleep is not discovered after it")
    void cancellationInterruptsBackoff() {
        var policy = MirrorNodeHttpRetryPolicy.defaults()
                .withInitialBackoff(Duration.ofSeconds(5))
                .withMaxBackoff(Duration.ofSeconds(5));

        var source = CancellationSource.create();
        transport.enqueue(response(503, ""));
        transport.onRequest(source::cancel);

        var start = System.nanoTime();
        assertThatThrownBy(
                        () -> clientWith(policy).get(PATH, source.getCancellation(), deadline(Duration.ofSeconds(30))))
                .isInstanceOf(HttpTransportException.class)
                .extracting(e -> ((HttpTransportException) e).getKind())
                .isEqualTo(HttpTransportErrorKind.CANCELLED_ERROR);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    @DisplayName("Client.close() during a call stops the adapter, even with an injected transport")
    void clientCloseStopsTheAdapter() {
        var closed = new AtomicBoolean(false);
        transport.enqueue(response(503, ""));
        transport.onRequest(() -> closed.set(true));

        assertThatThrownBy(() -> clientWith(fastPolicy(), Collections.emptyMap(), closed::get)
                        .get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(30))))
                .isInstanceOf(HttpTransportException.class)
                .extracting(e -> ((HttpTransportException) e).getKind())
                .isEqualTo(HttpTransportErrorKind.CLIENT_CLOSED_ERROR);
    }

    @Test
    @DisplayName("A retried POST replays its body byte for byte")
    void replaysThePostBody() {
        var body = "{\"data\":\"0xabc\"}".getBytes(StandardCharsets.UTF_8);
        transport.enqueue(response(503, ""));
        transport.enqueue(response(200, "ok"));

        clientWith(fastPolicy())
                .post(PATH, "application/json", body, Cancellation.none(), deadline(Duration.ofSeconds(10)));

        assertThat(transport.requests).hasSize(2);
        assertThat(transport.requests.get(0).getBody()).isEqualTo(body);
        assertThat(transport.requests.get(1).getBody()).isEqualTo(body);
        assertThat(transport.requests.get(1).getContentType()).isEqualTo("application/json");
    }

    @Test
    @DisplayName("The SDK identity header wins, and caller headers ride along")
    void sendsTheIdentityHeaderAndCallerHeaders() {
        transport.enqueue(response(200, "ok"));

        clientWith(fastPolicy(), Map.of("authorization", "Bearer test"), () -> false)
                .get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10)));

        var headers = transport.requests.get(0).getHeaders();
        assertThat(headers).containsEntry(SdkUserAgent.HEADER_NAME, SdkUserAgent.value());
        assertThat(headers).containsEntry("authorization", "Bearer test");
        assertThat(headers).doesNotContainKey("user-agent");
    }

    @Test
    @DisplayName("The endpoint's content type outranks anything the caller configured")
    void endpointContentTypeWins() {
        transport.enqueue(response(200, "ok"));

        clientWith(fastPolicy(), Map.of("content-type", "text/plain"), () -> false)
                .post(
                        PATH,
                        "application/protobuf",
                        new byte[] {1},
                        Cancellation.none(),
                        deadline(Duration.ofSeconds(10)));

        assertThat(transport.requests.get(0).getHeaders()).containsEntry("content-type", "application/protobuf");
    }

    @Test
    @DisplayName("Each attempt is bounded by whichever of the two timeouts is tighter")
    void perAttemptDeadlineIsTheTighterBound() {
        transport.enqueue(response(200, "ok"));

        clientWith(fastPolicy().withPerAttemptTimeout(Duration.ofSeconds(30)))
                .get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(2)));

        assertThat(transport.requests.get(0).getDeadline()).isLessThanOrEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void theUrlIsTheBaseUrlConcatenatedWithThePath() {
        transport.enqueue(response(200, "ok"));

        clientWith(fastPolicy()).get(PATH, Cancellation.none(), deadline(Duration.ofSeconds(10)));

        assertThat(transport.requests.get(0).getUrl()).isEqualTo("https://mirror.example/api/v1/accounts/0.0.5/tokens");
    }

    private static final class FakeTransport implements HttpTransport {
        private final Queue<Object> outcomes = new ArrayDeque<>();
        private final List<Runnable> onRequest = new ArrayList<>();
        final List<HttpRequest> requests = new ArrayList<>();

        void enqueue(HttpResponse response) {
            outcomes.add(response);
        }

        void enqueue(Throwable failure) {
            outcomes.add(failure);
        }

        /**
         * Runs once, when the next request is made -- the hook the "something happened mid-call" tests need.
         */
        void onRequest(Runnable action) {
            onRequest.add(action);
        }

        @Override
        public CompletionStage<HttpResponse> roundTrip(HttpRequest request, Cancellation cancellation) {
            requests.add(request);

            if (!onRequest.isEmpty()) {
                onRequest.remove(0).run();
            }

            var outcome = outcomes.poll();

            if (outcome == null) {
                return CompletableFuture.failedFuture(new AssertionError("no outcome was queued for this request"));
            }

            return outcome instanceof Throwable failure
                    ? CompletableFuture.failedFuture(failure)
                    : CompletableFuture.completedFuture((HttpResponse) outcome);
        }

        @Override
        public void close(Duration closeTimeout) {
            // Owned by the test, not by the adapter.
        }
    }
}
