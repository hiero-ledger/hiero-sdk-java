// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.io.BaseEncoding;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MirrorNodeTokenBalanceQueryMockTest {

    private static final TokenId TOKEN_ID = TokenId.fromString("0.0.1135");

    private Client client;
    private MirrorNodeTokenBalanceQuery query;
    private StubMirrorRestServer stub;

    @BeforeEach
    void setUp() throws Exception {
        stub = new StubMirrorRestServer();
        stub.start();

        client = Client.forNetwork(Collections.emptyMap());
        client.setRequestTimeout(Duration.ofSeconds(10));
        client.setMirrorNetwork(Collections.singletonList("localhost:" + stub.getPort()));
        // Keep the backoff out of the test's wall clock; the retry behaviour itself is asserted here.
        client.setMirrorNodeHttpConfig(client.getMirrorNodeHttpConfig()
                .withRetryPolicy(MirrorNodeHttpRetryPolicy.defaults()
                        .withInitialBackoff(Duration.ofMillis(1))
                        .withMaxBackoff(Duration.ofMillis(2))));

        query = new MirrorNodeTokenBalanceQuery();
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
    @DisplayName("Given a fungible token relationship, the amount and decimals are parsed")
    void parsesFungibleBalance() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 1000, "3")));

        var balance = query.execute(client);

        assertThat(balance.isAssociated()).isTrue();
        assertThat(balance.balance).isEqualTo(1000);
        assertThat(balance.decimals).isEqualTo(3);
        assertThat(balance.tokenId).isEqualTo(TOKEN_ID);
        assertThat(stub.requestCount()).isEqualTo(1);
        assertThat(stub.getLastPath()).isEqualTo("/api/v1/accounts/0.0.12345/tokens");
        assertThat(stub.getLastQueryParams()).isEqualTo("token.id=0.0.1135&limit=1");
    }

    @Test
    @DisplayName("For a non-fungible token the balance is the number of NFTs held")
    void parsesNftCount() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 4, null)));

        var balance = query.execute(client);

        assertThat(balance.isAssociated()).isTrue();
        assertThat(balance.balance).isEqualTo(4);
        assertThat(balance.decimals).isNull();
    }

    @Test
    @DisplayName("An empty tokens array is 'not associated', not an error")
    void reportsNotAssociatedForAnEmptyArray() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, "{\"tokens\":[],\"links\":{\"next\":null}}"));

        var balance = query.execute(client);

        assertThat(balance.isAssociated()).isFalse();
        assertThat(balance.balance).isZero();
        assertThat(balance.tokenId).isEqualTo(TOKEN_ID);
    }

    @Test
    @DisplayName("An entity the mirror node does not know is 'not associated', not an error")
    void reportsNotAssociatedForAnUnknownEntity() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.99999999")).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(404, "{\"_status\":{\"messages\":[{\"message\":\"Not found\"}]}}"));

        var balance = query.execute(client);

        assertThat(balance.isAssociated()).isFalse();
        assertThat(balance.balance).isZero();
    }

    @Test
    @DisplayName("A retryable status is retried and the call succeeds")
    void retriesARetryableStatus() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(503, "busy"));
        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 7, "0")));

        assertThat(query.execute(client).balance).isEqualTo(7);
        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("A terminal status names what the mirror node said rather than dumping the body")
    void surfacesTheMirrorNodeErrorEnvelope() {
        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        stub.enqueue(
                new StubResponse(400, "{\"_status\":{\"messages\":[{\"detail\":\"Invalid parameter: token.id\"}]}}"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 400")
                .hasMessageContaining("Invalid parameter: token.id");

        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("A retryable status on every attempt reports the exhausted budget")
    void reportsExhaustedRetries() {
        query.setAccountId(AccountId.fromString("0.0.12345"))
                .setTokenId(TOKEN_ID)
                .setMaxAttempts(2);

        stub.enqueue(new StubResponse(503, "busy"));
        stub.enqueue(new StubResponse(503, "busy"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining(HttpTransportErrorKind.RETRIES_EXHAUSTED_ERROR.getId());

        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    void reportsMalformedJson() {
        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, "not json at all"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed JSON");
    }

    @Test
    @DisplayName("An EVM address is sent in its 0x-prefixed hex form")
    void sendsAnEvmAddressAsHex() throws Exception {
        var evmAddress = "302a300506032b6570032100114e6abc371b82da";
        query.setAccountId(AccountId.fromEvmAddress(
                        EvmAddress.fromString("0x00000000000000000000000000000000000004d2"), 0, 0))
                .setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 1, "0")));

        query.execute(client);

        assertThat(stub.getLastPath()).isEqualTo("/api/v1/accounts/0x00000000000000000000000000000000000004d2/tokens");
        assertThat(evmAddress).isNotEmpty();
    }

    @Test
    @DisplayName("A public key alias is sent as unpadded base32 of the protobuf Key, not as its DER form")
    void sendsAnAliasAsBase32() throws Exception {
        var key = PrivateKey.generateED25519().getPublicKey();
        query.setAccountId(AccountId.fromString("0.0." + key.toStringDER())).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 1, "0")));

        query.execute(client);

        var expected =
                BaseEncoding.base32().omitPadding().encode(key.toProtobufKey().toByteArray());
        assertThat(stub.getLastPath()).isEqualTo("/api/v1/accounts/" + expected + "/tokens");
    }

    @Test
    void requiresBothIds() {
        assertThatThrownBy(() ->
                        new MirrorNodeTokenBalanceQuery().setTokenId(TOKEN_ID).execute(client))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accountId must be set");

        assertThatThrownBy(() -> new MirrorNodeTokenBalanceQuery()
                        .setAccountId(AccountId.fromString("0.0.12345"))
                        .execute(client))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tokenId must be set");
    }

    @Test
    @DisplayName("A client that has made no mirror REST call has built no transport")
    void buildsTheTransportLazily() {
        assertThat(client.getMirrorNodeHttpConfig().getTransport()).isNull();
    }

    @Test
    @DisplayName("An injected transport is used, and the getter still reports what was supplied")
    void usesAnInjectedTransport() throws Exception {
        var injected = new RecordingTransport();
        client.setMirrorNodeHttpConfig(client.getMirrorNodeHttpConfig().withTransport(injected));

        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        var balance = query.execute(client);

        assertThat(balance.isAssociated()).isTrue();
        assertThat(injected.requests).hasSize(1);
        assertThat(stub.requestCount()).isZero();
        assertThat(client.getMirrorNodeHttpConfig().getTransport()).isSameAs(injected);

        // Ownership follows construction: the SDK never closes a transport it did not build.
        client.close();
        assertThat(injected.closed).isFalse();
        client = null;
    }

    private static String newRelationshipResponse(String tokenId, long balance, String decimals) {
        var decimalsField = decimals == null ? "" : ",\n            \"decimals\": " + decimals;
        return """
                {
                  "tokens": [
                    {
                      "token_id": "%s",
                      "balance": %d,
                      "automatic_association": false,
                      "created_timestamp": "1.0",
                      "freeze_status": "UNFROZEN",
                      "kyc_status": "NOT_APPLICABLE"%s
                    }
                  ],
                  "links": { "next": null }
                }
                """.formatted(tokenId, balance, decimalsField);
    }

    private static final class RecordingTransport implements HttpTransport {
        final java.util.List<HttpRequest> requests = new java.util.ArrayList<>();
        volatile boolean closed = false;

        @Override
        public java.util.concurrent.CompletionStage<HttpResponse> roundTrip(
                HttpRequest request, Cancellation cancellation) {
            requests.add(request);
            return java.util.concurrent.CompletableFuture.completedFuture(HttpResponse.create(
                    200,
                    newRelationshipResponse(TOKEN_ID.toString(), 42, "2").getBytes(StandardCharsets.UTF_8),
                    java.util.Map.of()));
        }

        @Override
        public void close(Duration closeTimeout) {
            closed = true;
        }
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
        private String lastPath;
        private String lastQueryParams;
        private String lastUserAgent;
        private HttpServer server;
        private int port;

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress().getPort();
            server.createContext("/api/v1/accounts", exchange -> {
                observedRequests++;
                lastPath = exchange.getRequestURI().getPath();
                lastQueryParams = exchange.getRequestURI().getQuery();
                lastUserAgent = exchange.getRequestHeaders().getFirst("x-user-agent");

                var response = responses.poll();
                assertThat(response)
                        .as("response should be queued before invoking the token balance query")
                        .isNotNull();

                assertThat(exchange.getRequestMethod()).isEqualTo("GET");
                assertThat(lastUserAgent).isEqualTo(SdkUserAgent.value());

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

        String getLastPath() {
            return lastPath;
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
