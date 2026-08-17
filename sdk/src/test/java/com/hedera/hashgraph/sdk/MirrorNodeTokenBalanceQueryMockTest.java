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
    @DisplayName("Given a non-fungible token relationship, balance is the number of NFTs held")
    void parsesNftCount() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        // NON_FUNGIBLE_UNIQUE relationships report a null decimals and a count in balance.
        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 2, "null")));

        var balance = query.execute(client);

        assertThat(balance.isAssociated()).isTrue();
        assertThat(balance.balance).isEqualTo(2);
        assertThat(balance.decimals).isNull();
    }

    @Test
    @DisplayName("Given an entity with no relationship to the token, isAssociated is false and balance is zero")
    void emptyTokensArrayMeansNotAssociated() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, "{\"tokens\":[],\"links\":{\"next\":null}}"));

        var balance = query.execute(client);

        assertThat(balance.isAssociated()).isFalse();
        assertThat(balance.balance).isZero();
        assertThat(balance.decimals).isNull();
        // The requested token id is carried through even though the response held no relationship.
        assertThat(balance.tokenId).isEqualTo(TOKEN_ID);
    }

    @Test
    @DisplayName("Given an associated token with a zero balance, isAssociated stays true")
    void associatedWithZeroBalanceIsDistinctFromNotAssociated() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345")).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 0, "3")));

        var balance = query.execute(client);

        assertThat(balance.isAssociated()).isTrue();
        assertThat(balance.balance).isZero();
    }

    @Test
    @DisplayName("Given an EVM address, it is sent in the path in 0x-prefixed form")
    void sendsEvmAddressInPath() throws Exception {
        var evmAddress = new AccountId(0, 0, 12345678).toEvmAddress();
        query.setAccountId(AccountId.fromEvmAddress("0x" + evmAddress)).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 5, "0")));

        var balance = query.execute(client);

        assertThat(balance.balance).isEqualTo(5);
        assertThat(stub.getLastPath()).isEqualTo("/api/v1/accounts/0x" + evmAddress + "/tokens");
    }

    @Test
    @DisplayName("Given a public key alias, it is sent in the path as shard.realm.alias")
    void sendsAliasInPath() throws Exception {
        var aliasAccountId = PrivateKey.generateED25519().getPublicKey().toAccountId(0, 0);
        query.setAccountId(aliasAccountId).setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 7, "1")));

        var balance = query.execute(client);

        assertThat(balance.balance).isEqualTo(7);
        assertThat(stub.getLastPath()).isEqualTo("/api/v1/accounts/" + aliasAccountId + "/tokens");
    }

    @Test
    @DisplayName("Given a contract id converted to an account id, the path uses the contract's number")
    void sendsContractDerivedIdInPath() throws Exception {
        var contractId = ContractId.fromString("0.0.98765");
        query.setAccountId(new AccountId(contractId.shard, contractId.realm, contractId.num))
                .setTokenId(TOKEN_ID);

        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 10, "0")));

        var balance = query.execute(client);

        assertThat(balance.balance).isEqualTo(10);
        assertThat(stub.getLastPath()).isEqualTo("/api/v1/accounts/0.0.98765/tokens");
    }

    @Test
    @DisplayName("Given the mirror node is unavailable, the query retries on HTTP 503")
    void retriesOnUnavailable() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345"))
                .setTokenId(TOKEN_ID)
                .setMaxAttempts(3)
                .setMaxBackoff(Duration.ofMillis(500));

        stub.enqueue(new StubResponse(503, "transient error"));
        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 42, "2")));

        var balance = query.execute(client);

        assertThat(balance.balance).isEqualTo(42);
        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given the mirror node gateway times out, the query retries on HTTP 504")
    void retriesOnGatewayTimeout() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345"))
                .setTokenId(TOKEN_ID)
                .setMaxAttempts(3)
                .setMaxBackoff(Duration.ofMillis(500));

        stub.enqueue(new StubResponse(504, "gateway timeout"));
        stub.enqueue(new StubResponse(200, newRelationshipResponse(TOKEN_ID.toString(), 9, "2")));

        var balance = query.execute(client);

        assertThat(balance.balance).isEqualTo(9);
        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given an invalid parameter, the mirror node returns HTTP 400 and the query does not retry")
    void doesNotRetryOn400() {
        query.setAccountId(AccountId.fromString("0.0.12345"))
                .setTokenId(TOKEN_ID)
                .setMaxAttempts(3);

        stub.enqueue(new StubResponse(400, "bad request"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("400");

        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given an entity the mirror node does not know, HTTP 404 reports not associated rather than failing")
    void missingEntityReportsNotAssociated() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.999999999"))
                .setTokenId(TOKEN_ID)
                .setMaxAttempts(3);

        stub.enqueue(new StubResponse(404, "{\"_status\":{\"messages\":[{\"message\":\"Not found\"}]}}"));

        var balance = query.execute(client);

        assertThat(balance.isAssociated()).isFalse();
        assertThat(balance.balance).isZero();
        assertThat(balance.decimals).isNull();
        assertThat(balance.tokenId).isEqualTo(TOKEN_ID);
        // 404 is an answer, not a transient failure, so it must not consume retry attempts.
        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given no account id is set, the query fails before making a network call")
    void failsBeforeNetworkCallWhenAccountIdMissing() {
        query.setTokenId(TOKEN_ID);

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accountId must be set");

        assertThat(stub.requestCount()).isZero();
    }

    @Test
    @DisplayName("Given no token id is set, the query fails before making a network call")
    void failsBeforeNetworkCallWhenTokenIdMissing() {
        query.setAccountId(AccountId.fromString("0.0.12345"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tokenId must be set");

        assertThat(stub.requestCount()).isZero();
    }

    private static String newRelationshipResponse(String tokenId, long balance, String decimals) {
        return """
                {
                  "tokens": [
                    {
                      "automatic_association": true,
                      "balance": %d,
                      "created_timestamp": "1234567890.000000001",
                      "decimals": %s,
                      "freeze_status": "UNFROZEN",
                      "kyc_status": "NOT_APPLICABLE",
                      "token_id": "%s"
                    }
                  ],
                  "links": { "next": null }
                }
                """.formatted(balance, decimals, tokenId);
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
        private HttpServer server;
        private int port;

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress().getPort();
            // The account id is part of the path, so the whole /accounts subtree is served here.
            server.createContext("/api/v1/accounts", exchange -> {
                observedRequests++;
                lastPath = exchange.getRequestURI().getPath();
                lastQueryParams = exchange.getRequestURI().getQuery();
                var response = responses.poll();
                assertThat(response)
                        .as("response should be queued before invoking the token balance query")
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
