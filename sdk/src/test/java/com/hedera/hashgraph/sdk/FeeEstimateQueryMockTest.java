// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeeEstimateQueryMockTest {

    private static final com.hedera.hashgraph.sdk.proto.Transaction DUMMY_TRANSACTION =
            com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                    .setSignedTransactionBytes(ByteString.copyFromUtf8("dummy"))
                    .build();

    private Client client;
    private HttpServer server;
    private FeeEstimateQuery query;
    private StubMirrorRestServer stub;

    @BeforeEach
    void setUp() throws Exception {
        stub = new StubMirrorRestServer();
        stub.start();

        client = Client.forNetwork(Collections.emptyMap());
        client.setRequestTimeout(Duration.ofSeconds(10));
        client.setMirrorNetwork(Collections.singletonList("localhost:" + stub.getPort()));

        query = new FeeEstimateQuery();
    }

    @AfterEach
    void tearDown() throws Exception {
        stub.verify();
        stub.stop();
        if (client != null) {
            client.close();
        }
    }

    @Test
    @DisplayName(
            "Given a FeeEstimateQuery is executed when the Mirror service is unavailable, when the query is executed, then it retries according to the existing query retry policy for HTTP 503 errors")
    void retriesOnUnavailableErrors() throws IOException, InterruptedException {
        query.setTransaction(DUMMY_TRANSACTION).setMaxAttempts(3).setMaxBackoff(Duration.ofMillis(500));

        stub.enqueue(new StubResponse(503, "transient error"));
        stub.enqueue(new StubResponse(200, newSuccessResponse(FeeEstimateMode.STATE, 2, 6, 8)));

        var response = query.execute(client);

        assertThat(response.getTotal()).isEqualTo(26);
        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName(
            "Given a FeeEstimateQuery times out, when the query is executed, then it retries according to the existing query retry policy for HTTP timeouts")
    void retriesOnDeadlineExceededErrors() throws IOException, InterruptedException {
        query.setTransaction(DUMMY_TRANSACTION).setMaxAttempts(3).setMaxBackoff(Duration.ofMillis(500));

        stub.enqueue(new StubResponse(504, "gateway timeout"));
        stub.enqueue(new StubResponse(200, newSuccessResponse(FeeEstimateMode.STATE, 4, 8, 20)));

        var response = query.execute(client);

        assertThat(response.getTotal()).isEqualTo(60);
        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given a FeeEstimateQuery succeeds on first attempt, it returns the parsed fee response")
    void succeedsOnFirstAttempt() throws IOException, InterruptedException {
        query.setTransaction(DUMMY_TRANSACTION).setMode(FeeEstimateMode.INTRINSIC);

        stub.enqueue(new StubResponse(200, newSuccessResponse(FeeEstimateMode.INTRINSIC, 3, 10, 20)));

        var response = query.execute(client);

        assertThat(response.getTotal()).isEqualTo(3 * 10 + 10 + 20);
        assertThat(response.getHighVolumeMultiplier()).isEqualTo(1);
        assertThat(stub.requestCount()).isEqualTo(1);
        assertThat(stub.getLastQueryParams()).doesNotContain("high_volume_throttle");
    }

    @Test
    @DisplayName("Given a FeeEstimateQuery without explicit mode, it defaults to INTRINSIC")
    void defaultsToIntrinsic() throws IOException, InterruptedException {
        query.setTransaction(DUMMY_TRANSACTION);

        stub.enqueue(new StubResponse(200, newSuccessResponse(FeeEstimateMode.INTRINSIC, 3, 10, 20)));

        var response = query.execute(client);

        assertThat(stub.getLastQueryParams()).contains("mode=INTRINSIC");
    }

    @Test
    @DisplayName("Given a FeeEstimateQuery with high volume throttle, it sends the parameter in the URL")
    void sendsHighVolumeThrottle() throws IOException, InterruptedException {
        query.setTransaction(DUMMY_TRANSACTION).setHighVolumeThrottle(5000);

        stub.enqueue(new StubResponse(200, newSuccessResponse(FeeEstimateMode.INTRINSIC, 3, 10, 20)));

        var response = query.execute(client);

        assertThat(stub.getLastQueryParams()).contains("high_volume_throttle=5000");
        assertThat(response.getHighVolumeMultiplier()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Given a FeeEstimateQuery receives HTTP 400, it does not retry")
    void doesNotRetryOn400() {
        query.setTransaction(DUMMY_TRANSACTION).setMaxAttempts(3);

        stub.enqueue(new StubResponse(400, "bad request"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("400");

        assertThat(stub.requestCount()).isEqualTo(1);
    }

    private static String newSuccessResponse(
            FeeEstimateMode mode, int networkMultiplier, long nodeBase, long serviceBase) {
        long networkSubtotal = nodeBase * networkMultiplier;
        long total = networkSubtotal + nodeBase + serviceBase;
        return """
                {
                  "mode": "%s",
                  "network": {"multiplier": %d, "subtotal": %d},
                  "node": {"base": %d, "extras": []},
                  "service": {"base": %d, "extras": []},
                  "high_volume_multiplier": 1,
                  "total": %d
                }
                """.formatted(mode, networkMultiplier, networkSubtotal, nodeBase, serviceBase, total);
    }

    private static final class StubResponse {
        final int status;
        final String body;

        StubResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private static final class StubMirrorRestServer {
        private final Queue<StubResponse> responses = new ArrayDeque<>();
        private int observedRequests = 0;
        private String lastQueryParams;
        private HttpServer server;
        private int port;

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress().getPort();
            server.createContext("/api/v1/network/fees", exchange -> {
                observedRequests++;
                lastQueryParams = exchange.getRequestURI().getQuery();
                var response = responses.poll();
                assertThat(response)
                        .as("response should be queued before invoking network fee estimation")
                        .isNotNull();

                // Validate request structure similar to JS implementation
                assertThat(exchange.getRequestHeaders().getFirst("Content-Type"))
                        .isEqualTo("application/protobuf");
                var queryParams = exchange.getRequestURI().getQuery();
                assertThat(queryParams).contains("mode=");

                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                assertThat(requestBody.length).isGreaterThan(0);

                byte[] bodyBytes = response.body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(response.status, bodyBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bodyBytes);
                }
            });
            server.start();
        }

        void enqueue(StubResponse response) {
            responses.add(response);
        }

        void stop() {
            if (server != null) {
                server.stop(0);
            }
        }

        int requestCount() {
            return observedRequests;
        }

        int getPort() {
            return port;
        }

        String getLastQueryParams() {
            return lastQueryParams;
        }

        void verify() {
            assertThat(responses)
                    .as("all queued responses should have been served")
                    .isEmpty();
        }
    }
}
