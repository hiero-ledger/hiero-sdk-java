// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RegisteredNodeAddressBookQueryMockTest {
    private static final PublicKey MOCK_ADMIN_KEY = PrivateKey.generateECDSA().getPublicKey();

    private Client client;
    private RegisteredNodeAddressBookQuery query;
    private RegisteredNodeAddressBookQueryMockTest.StubMirrorRestServer stub;

    @BeforeEach
    void setup() throws Exception {
        stub = new StubMirrorRestServer();
        stub.start();

        client = Client.forNetwork(Collections.emptyMap());
        client.setRequestTimeout(Duration.ofSeconds(10));
        client.setMirrorNetwork(Collections.singletonList("localhost:" + stub.getPort()));
        client.setMirrorNodeHttpConfig(client.getMirrorNodeHttpConfig()
                .withRetryPolicy(MirrorNodeHttpRetryPolicy.defaults()
                        .withInitialBackoff(Duration.ofMillis(1))
                        .withMaxBackoff(Duration.ofMillis(2))));

        query = new RegisteredNodeAddressBookQuery();
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
    @DisplayName("Given RegisterAddressBookQuery can parse single response page")
    void parseSingleRegisteredPage() throws Exception {
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(node(1, "node-1")), null)));

        var addressBook = query.execute(client);
        assertThat(addressBook.registeredNodes).hasSize(1);

        var node = addressBook.registeredNodes.getFirst();
        assertThat(node.adminKey).isEqualTo(MOCK_ADMIN_KEY);
        assertThat(node.description).isEqualTo("node-1");
        assertThat(node.registeredNodeId).isEqualTo(1);

        var endpoints = node.serviceEndpoints;
        assertThat(endpoints).hasSize(1);
        assertThat(endpoints.getFirst()).isInstanceOf(BlockNodeServiceEndpoint.class);
        assertThat(endpoints.getFirst().domainName).isEqualTo("test.block.com");
        assertThat(endpoints.getFirst().port).isEqualTo(443);
    }

    @Test
    @DisplayName("Given RegisteredAddressBookQuery without a nodeId or limit, use default page size")
    void queryRegisteredAddressBookWithoutNodeId() throws Exception {
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(node(1, "node-1")), null)));

        var addressBook = query.execute(client);
        assertThat(addressBook.registeredNodes).hasSize(1);
        assertThat(stub.getLastQueryParams()).isEqualTo("limit=25");
        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given RegisteredAddressBookQuery with nodeId, adds the registerednode.id query params")
    void queryRegisteredAddressBookWithNodeId() throws Exception {
        query.setRegisteredNodeId(1);
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(node(1, "node-1")), null)));

        var addressBook = query.execute(client);
        assertThat(addressBook.registeredNodes).hasSize(1);
        assertThat(stub.getLastQueryParams()).isEqualTo("limit=25&registerednode.id=1");
        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given RegisteredAddressBookQuery with a custom limit, it uses that as page size")
    void queryRegisteredAddressBookWithLimit() throws Exception {
        query.setLimit(1);
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(node(1, "node-1")), null)));

        var addressBook = query.execute(client);
        assertThat(addressBook.registeredNodes).hasSize(1);
        assertThat(stub.getLastQueryParams()).isEqualTo("limit=1");
        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given RegisteredAddressBookQuery with non-positive limit falls back to default limit")
    void nonPositiveLimitFallsBackToDefault() throws Exception {
        query.setLimit(-3);
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(node(1, "node-1")), null)));

        var addressBook = query.execute(client);
        assertThat(addressBook.registeredNodes).hasSize(1);
        assertThat(stub.getLastQueryParams()).isEqualTo("limit=25");
        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given RegisterAddressBookQuery can parse multiple response page")
    void parseMultipleRegisteredPage() throws Exception {
        stub.enqueue(new StubResponse(
                200,
                registeredNodePage(
                        List.of(node(1, "node-1"), node(2, "node-2")),
                        "/api/v1/network/registered-nodes?limit=2&registerednode.id=gt:2")));
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(node(3, "node-3")), null)));

        var addressBook = query.execute(client);
        assertThat(addressBook.registeredNodes).hasSize(3);

        for (int i = 0; i < addressBook.registeredNodes.size(); i++) {
            var node = addressBook.registeredNodes.get(i);
            assertThat(node.adminKey).isEqualTo(MOCK_ADMIN_KEY);
            assertThat(node.description).isEqualTo("node-" + (i + 1));
            assertThat(node.registeredNodeId).isEqualTo(i + 1);

            var endpoints = node.serviceEndpoints;
            assertThat(endpoints).hasSize(1);
            assertThat(endpoints.getFirst()).isInstanceOf(BlockNodeServiceEndpoint.class);
            assertThat(endpoints.getFirst().domainName).isEqualTo("test.block.com");
            assertThat(endpoints.getFirst().port).isEqualTo(443);
        }
    }

    @Test
    @DisplayName("Fetch RegisterAddressBook With two pages")
    void queryRegisteredAddressBookWithMultiplePage() throws Exception {
        query.setLimit(2);
        stub.enqueue(new StubResponse(
                200,
                registeredNodePage(
                        List.of(node(1, "node-1"), node(2, "node-2")),
                        "/api/v1/network/registered-nodes?limit=2&registerednode.id=gt:2")));
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(node(3, "node-3")), null)));

        var addressBook = query.execute(client);
        assertThat(addressBook.registeredNodes).hasSize(3);
        assertThat(stub.getLastQueryParams()).isEqualTo("limit=2&registerednode.id=gt:2");
        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Test an empty registered_nodes array gives an empty address book")
    void emptyNodesArray() throws Exception {
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(), null)));
        var addressBook = query.execute(client);
        assertThat(addressBook.registeredNodes).isEmpty();
        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("A 503 followed by a 200 is retried and succeeds")
    void retriesOnUnavailable() throws Exception {
        stub.enqueue(new StubResponse(503, "unavailable"));
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(node(1, "node-1")), null)));

        var addressBook = query.execute(client);

        assertThat(addressBook.registeredNodes).hasSize(1);
        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("A 202 followed by 503 followed by a 200 is retried and succeeds")
    void retriesOnUnavailableInBetween() throws Exception {
        query.setLimit(1);

        stub.enqueue(new StubResponse(
                200,
                registeredNodePage(
                        List.of(node(1, "node-1")),
                        "/api/v1/network/registered-nodes?limit=1&registerednode.id=gt:1")));
        stub.enqueue(new StubResponse(503, "unavailable"));
        stub.enqueue(new StubResponse(200, registeredNodePage(List.of(node(2, "node-2")), null)));

        var addressBook = query.execute(client);

        assertThat(addressBook.registeredNodes).hasSize(2);
        assertThat(stub.requestCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("When every attempt returns 503, it stops after maxAttempts and fails")
    void failsAfterMaxAttempts() {
        query.setMaxAttempts(2);
        stub.enqueue(new StubResponse(503, "unavailable"));
        stub.enqueue(new StubResponse(503, "unavailable"));

        assertThatThrownBy(() -> query.execute(client)).isInstanceOf(ExecutionException.class);

        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("HTTP 400 is not retried and surfaces as IllegalStateException")
    void doesNotRetryOn400() {
        stub.enqueue(new StubResponse(400, "bad request"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("400");

        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Malformed JSON surfaces as IllegalStateException")
    void malformedJson() {
        stub.enqueue(new StubResponse(200, "{ not json"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed JSON");

        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("A JSON array instead of an object is treated as malformed")
    void jsonArrayInsteadOfObject() {
        stub.enqueue(new StubResponse(200, "[]"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed JSON");
    }

    @Test
    @DisplayName("A client that has made no mirror REST call has built no transport")
    void buildsTheTransportLazily() {
        assertThat(client.getMirrorNodeHttpConfig().getTransport()).isNull();
    }

    private static String node(int nodeId, String description) {
        description = description == null ? "" : description;
        return """
         {
           "admin_key": {
             "_type": "ProtobufEncoded",
             "key": "%s"
           },
           "created_timestamp": "1586567700.453054001",
           "description": "%s",
           "registered_node_id": %d,
           "service_endpoints": [
             {
               "block_node": {
                  "endpoint_apis": ["STATUS"]
               },
               "domain_name": "test.block.com",
               "general_service": null,
               "ip_address": null,
               "mirror_node": null,
               "port": 443,
               "requires_tls": false,
               "rpc_relay": null,
               "type": "BLOCK_NODE"
             }
            ],
           "timestamp": {
             "from": "187654.000123457",
             "to": null
           }
         }
       """.formatted(MOCK_ADMIN_KEY.toStringDER(), description, nodeId);
    }

    private static String registeredNodePage(List<String> registeredNodeResponse, String nextLink) {
        return """
          {
            "registered_nodes": [%s],
            "links": {
              "next": %s
            }
          }
        """.formatted(
                        String.join(",", registeredNodeResponse), nextLink != null ? "\"" + nextLink + "\"" : "null");
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
        private final Queue<RegisteredNodeAddressBookQueryMockTest.StubResponse> responses = new ArrayDeque<>();
        private int observedRequests = 0;
        private String lastQueryParams;
        private HttpServer server;
        private int port;

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress().getPort();
            server.createContext("/api/v1/network/registered-nodes", exchange -> {
                observedRequests++;
                lastQueryParams = exchange.getRequestURI().getQuery();
                var response = responses.poll();
                assertThat(response)
                        .as("response should be queued before invoking the registered address book query")
                        .isNotNull();

                assertThat(exchange.getRequestMethod()).isEqualTo("GET");

                byte[] bodyBytes = response.body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(response.status, bodyBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bodyBytes);
                }
            });
            server.start();
        }

        void enqueue(RegisteredNodeAddressBookQueryMockTest.StubResponse response) {
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
