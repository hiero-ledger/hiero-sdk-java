// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class MirrorNodeContractQueryMockTest {
    private static final ContractId CONTRACT_ID = new ContractId(0, 0, 1234);
    private static final AccountId SENDER_ID = new AccountId(0, 0, 5005);
    private static final String MOCK_CONTRACT_EVM = "0x1111111111111111111111111111111111111111";
    private static final String MOCK_SENDER_EVM = "0x2222222222222222222222222222222222222222";
    private static final ByteString MOCK_CALL_DATA = ByteString.copyFrom(new byte[] {1, 2, 3});

    private Client client;
    private MirrorNodeContractQueryMockTest.StubMirrorRestServer stub;

    @BeforeEach
    void setUp() throws Exception {
        stub = new MirrorNodeContractQueryMockTest.StubMirrorRestServer();
        stub.start();

        client = Client.forNetwork(Collections.emptyMap());
        client.setRequestTimeout(Duration.ofSeconds(10));
        client.setMirrorNetwork(Collections.singletonList("localhost:" + stub.getPort()));
        client.setMirrorNodeHttpConfig(client.getMirrorNodeHttpConfig()
                .withRetryPolicy(MirrorNodeHttpRetryPolicy.defaults()
                        .withInitialBackoff(Duration.ofMillis(1))
                        .withMaxBackoff(Duration.ofMillis(2))));
    }

    @AfterEach
    void tearDown() throws Exception {
        stub.verify();
        stub.stop();
        if (client != null) {
            client.close();
        }
    }

    // MirrorNodeContractCallQuery

    @Test
    @DisplayName("Contract call returns the result string")
    void callReturnsResult() throws Exception {
        stub.enqueue(new StubResponse(200, "{\"result\": \"0x1234\"}"));

        var result = new MirrorNodeContractCallQuery()
                .setContractId(CONTRACT_ID)
                .setFunctionParameters(MOCK_CALL_DATA)
                .execute(client);
        assertThat(result).isEqualTo("0x1234");
        assertThat(stub.requestCount()).isEqualTo(1);

        var body = stub.getLastBody();
        assertThat(body.get("data").getAsString()).isEqualTo("010203");
        assertThat(body.get("to").getAsString()).isEqualTo(CONTRACT_ID.toEvmAddress());
        assertThat(body.get("estimate").getAsBoolean()).isFalse();
        assertThat(body.has("from")).isFalse();
        assertThat(body.has("gas")).isFalse();
        assertThat(body.has("gasPrice")).isFalse();
        assertThat(body.has("value")).isFalse();
        assertThat(body.has("block")).isFalse();
    }

    @Test
    @DisplayName("Contract call includes every optional field that was set")
    void callIncludesOptionalFields() throws Exception {
        var query = new MirrorNodeContractCallQuery()
                .setContractId(CONTRACT_ID)
                .setFunctionParameters(MOCK_CALL_DATA)
                .setSender(SENDER_ID)
                .setGasLimit(100_000)
                .setGasPrice(7)
                .setValue(42)
                .setBlockNumber(99L);
        stub.enqueue(new StubResponse(200, "{\"result\": \"0x1234\"}"));

        var result = query.execute(client);
        assertThat(result).isEqualTo("0x1234");
        assertThat(stub.requestCount()).isEqualTo(1);

        var body = stub.getLastBody();
        assertThat(body.get("data").getAsString()).isEqualTo("010203");
        assertThat(body.get("to").getAsString()).isEqualTo(CONTRACT_ID.toEvmAddress());
        assertThat(body.get("estimate").getAsBoolean()).isFalse();
        assertThat(body.get("from").getAsString()).isEqualTo(SENDER_ID.toEvmAddress());
        assertThat(body.get("gas").getAsLong()).isEqualTo(100_000);
        assertThat(body.get("gasPrice").getAsLong()).isEqualTo(7);
        assertThat(body.get("value").getAsLong()).isEqualTo(42);
        assertThat(body.get("block").getAsLong()).isEqualTo(99);
    }

    // MirrorNodeContractEstimateGasQuery

    @Test
    @DisplayName("Gas estimate query parses the hex gas value and sets estimate=true")
    void estimateParsesHexGas() throws Exception {
        stub.enqueue(new StubResponse(200, "{\"result\": \"0x5208\"}"));

        var gas = new MirrorNodeContractEstimateGasQuery()
                .setContractId(CONTRACT_ID)
                .setFunctionParameters(MOCK_CALL_DATA)
                .execute(client);
        assertThat(gas).isEqualTo(21000L);

        var body = stub.getLastBody();
        assertThat(body.get("estimate").getAsBoolean()).isTrue();
        assertThat(body.get("to").getAsString()).isEqualTo(CONTRACT_ID.toEvmAddress());
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
        private final Queue<MirrorNodeContractQueryMockTest.StubResponse> responses = new ArrayDeque<>();
        private int observedRequests = 0;
        private String lastQueryParams;
        private JsonObject lastBody;
        private HttpServer server;
        private int port;

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress().getPort();
            server.createContext("/api/v1/contracts/call", exchange -> {
                observedRequests++;
                lastQueryParams = exchange.getRequestURI().getQuery();

                var response = responses.poll();
                assertThat(response)
                        .as("response should be queued before invoking mirror-node contract query")
                        .isNotNull();

                assertThat(exchange.getRequestHeaders().getFirst("Content-Type"))
                        .isEqualTo("application/json");

                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                assertThat(requestBody.length).isGreaterThan(0);

                lastBody = JsonParser.parseString(new String(requestBody, StandardCharsets.UTF_8))
                        .getAsJsonObject();

                byte[] bodyBytes = response.body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(response.status, bodyBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bodyBytes);
                }
            });
            server.start();
        }

        void enqueue(MirrorNodeContractQueryMockTest.StubResponse response) {
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

        JsonObject getLastBody() {
            return lastBody;
        }

        void verify() {
            assertThat(responses)
                    .as("all queued responses should have been served")
                    .isEmpty();
        }
    }
}
