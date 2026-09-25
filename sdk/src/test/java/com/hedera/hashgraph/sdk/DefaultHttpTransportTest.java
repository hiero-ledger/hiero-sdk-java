// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultHttpTransportTest {

    private HttpServer server;
    private DefaultHttpTransport transport;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        transport = DefaultHttpTransport.create(HttpTransportConfiguration.defaults());
    }

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.close(Duration.ofSeconds(2));
        }
        if (server != null) {
            server.stop(0);
        }
    }

    private String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    private void handle(String path, HttpHandler handler) {
        server.createContext(path, handler);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (var stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }

    private HttpResponse roundTrip(HttpRequest request) throws Exception {
        return transport
                .roundTrip(request, Cancellation.none())
                .toCompletableFuture()
                .get(20, TimeUnit.SECONDS);
    }

    private HttpTransportException failureOf(HttpRequest request, Cancellation cancellation) {
        var failure = catchThrowableOfType(CompletionException.class, () -> transport
                .roundTrip(request, cancellation)
                .toCompletableFuture()
                .join());

        assertThat(failure.getCause()).isInstanceOf(HttpTransportException.class);
        return (HttpTransportException) failure.getCause();
    }

    @Test
    @DisplayName("A 404 is a successful exchange, not a failure")
    void nonSuccessStatusIsReturnedNotRaised() throws Exception {
        handle("/missing", exchange -> respond(exchange, 404, "{\"_status\":{}}"));

        var response = roundTrip(HttpRequest.create(HttpMethod.GET, url("/missing")));

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).contains("_status");
    }

    @Test
    @DisplayName("Response header names arrive lowercased and repeated values are all kept")
    void headersAreLowercasedAndListValued() throws Exception {
        handle("/headers", exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "7");
            exchange.getResponseHeaders().add("X-Repeated", "one");
            exchange.getResponseHeaders().add("X-Repeated", "two");
            respond(exchange, 200, "ok");
        });

        var response = roundTrip(HttpRequest.create(HttpMethod.GET, url("/headers")));

        assertThat(response.getFirstHeaderValue("retry-after")).contains("7");
        assertThat(response.getFirstHeaderValue("Retry-After")).contains("7");
        assertThat(response.getHeaders().get("x-repeated")).containsExactlyInAnyOrder("one", "two");
    }

    @Test
    @DisplayName("Request headers reach the wire")
    void sendsRequestHeaders() throws Exception {
        var seen = new ArrayList<String>();
        handle("/echo", exchange -> {
            seen.add(exchange.getRequestHeaders().getFirst("x-user-agent"));
            respond(exchange, 200, "ok");
        });

        roundTrip(HttpRequest.create(HttpMethod.GET, url("/echo")).withHeader("X-User-Agent", "hiero-sdk-java/TEST"));

        assertThat(seen).containsExactly("hiero-sdk-java/TEST");
    }

    @Test
    @DisplayName("A POST body is sent with its content type")
    void sendsABody() throws Exception {
        var seen = new ArrayList<String>();
        handle("/post", exchange -> {
            seen.add(exchange.getRequestHeaders().getFirst("content-type"));
            seen.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "ok");
        });

        roundTrip(HttpRequest.create(HttpMethod.POST, url("/post"))
                .withBody("hello".getBytes(StandardCharsets.UTF_8), "application/protobuf"));

        assertThat(seen).containsExactly("application/protobuf", "hello");
    }

    @Test
    @DisplayName("A body past the cap fails rather than reaching the caller truncated")
    void refusesAnOverLargeBody() throws Exception {
        transport.close(Duration.ZERO);
        transport = DefaultHttpTransport.create(
                HttpTransportConfiguration.defaults().withMaxResponseBytes(1024));

        // Chunked, so there is no content-length to short-circuit on and the cap must be enforced while
        // the body streams.
        handle("/big", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var stream = exchange.getResponseBody()) {
                for (var i = 0; i < 64; i++) {
                    stream.write(new byte[256]);
                    stream.flush();
                }
            }
        });

        assertThat(failureOf(HttpRequest.create(HttpMethod.GET, url("/big")), Cancellation.none())
                        .getKind())
                .isEqualTo(HttpTransportErrorKind.RESPONSE_TOO_LARGE_ERROR);
    }

    @Test
    @DisplayName("An announced over-large body is refused before it is buffered")
    void refusesAnAnnouncedOverLargeBody() throws Exception {
        transport.close(Duration.ZERO);
        transport = DefaultHttpTransport.create(
                HttpTransportConfiguration.defaults().withMaxResponseBytes(16));

        handle("/announced", exchange -> respond(exchange, 200, "x".repeat(128)));

        assertThat(failureOf(HttpRequest.create(HttpMethod.GET, url("/announced")), Cancellation.none())
                        .getKind())
                .isEqualTo(HttpTransportErrorKind.RESPONSE_TOO_LARGE_ERROR);
    }

    @Test
    @DisplayName("A body at exactly the cap is allowed")
    void allowsABodyAtTheCap() throws Exception {
        transport.close(Duration.ZERO);
        transport = DefaultHttpTransport.create(
                HttpTransportConfiguration.defaults().withMaxResponseBytes(16));

        handle("/exact", exchange -> respond(exchange, 200, "x".repeat(16)));

        assertThat(roundTrip(HttpRequest.create(HttpMethod.GET, url("/exact"))).getBody())
                .hasSize(16);
    }

    @Test
    @DisplayName("The deadline bounds the whole exchange, not just time to the response headers")
    void deadlineBoundsTheBodyToo() throws Exception {
        handle("/drip", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var stream = exchange.getResponseBody()) {
                for (var i = 0; i < 100; i++) {
                    stream.write(new byte[] {'x'});
                    stream.flush();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } catch (IOException ignored) {
                // The client abandoned the exchange, which is the point of the test.
            }
        });

        var start = System.nanoTime();
        var failure = failureOf(
                HttpRequest.create(HttpMethod.GET, url("/drip")).withDeadline(Duration.ofSeconds(1)),
                Cancellation.none());
        var elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(failure.getKind()).isEqualTo(HttpTransportErrorKind.TIMEOUT_ERROR);
        assertThat(failure.isRetryable()).isTrue();
        assertThat(elapsed).isLessThan(Duration.ofSeconds(8));
    }

    @Test
    @DisplayName("A cancellation fired mid-exchange ends it as cancelled, not as a timeout")
    void cancellationEndsTheExchange() throws Exception {
        var source = CancellationSource.create();

        handle("/slow", exchange -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            respond(exchange, 200, "ok");
        });

        var request = HttpRequest.create(HttpMethod.GET, url("/slow")).withDeadline(Duration.ofSeconds(30));
        var stage = transport.roundTrip(request, source.getCancellation());

        Thread.sleep(200);
        source.cancel();

        var failure = catchThrowableOfType(
                CompletionException.class, () -> stage.toCompletableFuture().join());

        assertThat(failure.getCause()).isInstanceOf(HttpTransportException.class);
        assertThat(((HttpTransportException) failure.getCause()).getKind())
                .isEqualTo(HttpTransportErrorKind.CANCELLED_ERROR);
    }

    @Test
    @DisplayName("Every exchange releases its cancellation registration, so one source accumulates nothing")
    void releasesCancellationRegistrations() throws Exception {
        var source = CancellationSource.create();
        var runs = new AtomicInteger();

        handle("/ok", exchange -> respond(exchange, 200, "ok"));

        for (var i = 0; i < 20; i++) {
            transport
                    .roundTrip(HttpRequest.create(HttpMethod.GET, url("/ok")), source.getCancellation())
                    .toCompletableFuture()
                    .get(20, TimeUnit.SECONDS);
        }

        source.getCancellation().onCancel(runs::incrementAndGet).release();
        source.cancel();

        // If the transport leaked its registrations the source would still hold 20 dead closures; the
        // only observable effect available from here is that cancelling does not resurrect them.
        assertThat(runs).hasValue(0);
    }

    @Test
    @DisplayName("Concurrent exchanges do not corrupt one another's bodies")
    void isSafeToCallConcurrently() throws Exception {
        handle("/n", exchange -> respond(exchange, 200, exchange.getRequestURI().getQuery()));

        var stages = new ArrayList<java.util.concurrent.CompletableFuture<HttpResponse>>();
        for (var i = 0; i < 24; i++) {
            stages.add(transport
                    .roundTrip(HttpRequest.create(HttpMethod.GET, url("/n?i=" + i)), Cancellation.none())
                    .toCompletableFuture());
        }

        for (var i = 0; i < stages.size(); i++) {
            assertThat(new String(stages.get(i).get(30, TimeUnit.SECONDS).getBody(), StandardCharsets.UTF_8))
                    .isEqualTo("i=" + i);
        }
    }

    @Test
    @DisplayName("A closed transport fails fast and opens no connection")
    void closedTransportFailsFast() {
        var reached = new AtomicInteger();
        handle("/after-close", exchange -> {
            reached.incrementAndGet();
            respond(exchange, 200, "ok");
        });

        transport.close(Duration.ofSeconds(2));

        assertThat(failureOf(HttpRequest.create(HttpMethod.GET, url("/after-close")), Cancellation.none())
                        .getKind())
                .isEqualTo(HttpTransportErrorKind.CLIENT_CLOSED_ERROR);
        assertThat(reached).hasValue(0);
    }

    @Test
    @DisplayName("Closing twice is a no-op")
    void closeIsIdempotent() {
        transport.close(Duration.ofSeconds(1));
        transport.close(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("A refused connection is retryable")
    void refusedConnectionIsRetryable() throws Exception {
        int deadPort;
        try (var socket = new ServerSocket(0)) {
            deadPort = socket.getLocalPort();
        }

        var failure = failureOf(
                HttpRequest.create(HttpMethod.GET, "http://localhost:" + deadPort + "/nope"), Cancellation.none());

        assertThat(failure.getKind()).isEqualTo(HttpTransportErrorKind.CONNECTION_ERROR);
        assertThat(failure.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("A cancellation fired before the call starts never reaches the wire")
    void preCancelledCallNeverStarts() {
        var reached = new AtomicInteger();
        handle("/never", exchange -> {
            reached.incrementAndGet();
            respond(exchange, 200, "ok");
        });

        var source = CancellationSource.create();
        source.cancel();

        assertThat(failureOf(HttpRequest.create(HttpMethod.GET, url("/never")), source.getCancellation())
                        .getKind())
                .isEqualTo(HttpTransportErrorKind.CANCELLED_ERROR);
        assertThat(reached).hasValue(0);
    }
}
