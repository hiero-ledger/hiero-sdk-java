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

class MirrorNodeAccountBalanceQueryMockTest {

    private Client client;
    private MirrorNodeAccountBalanceQuery query;
    private StubMirrorRestServer stub;

    @BeforeEach
    void setUp() throws Exception {
        stub = new StubMirrorRestServer();
        stub.start();

        client = Client.forNetwork(Collections.emptyMap());
        client.setRequestTimeout(Duration.ofSeconds(10));
        client.setMirrorNetwork(Collections.singletonList("localhost:" + stub.getPort()));

        query = new MirrorNodeAccountBalanceQuery();
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
    @DisplayName("Given an account with a non-zero balance, the tinybar balance is parsed into hbars")
    void parsesBalance() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345"));

        stub.enqueue(new StubResponse(200, newBalanceResponse("0.0.12345", 123456789L)));

        var balance = query.execute(client);

        assertThat(balance.hbars).isEqualTo(Hbar.fromTinybars(123456789L));
        assertThat(stub.requestCount()).isEqualTo(1);
        assertThat(stub.getLastQueryParams()).isEqualTo("account.id=0.0.12345");
    }

    @Test
    @DisplayName("Given a non-existent account, the mirror node returns an empty array and the balance is zero")
    void emptyBalancesArrayYieldsZero() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.999999999"));

        stub.enqueue(new StubResponse(200, "{\"timestamp\":\"1.0\",\"balances\":[],\"links\":{\"next\":null}}"));

        var balance = query.execute(client);

        assertThat(balance.hbars).isEqualTo(Hbar.ZERO);
    }

    @Test
    @DisplayName("Given an EVM address, it is sent to the mirror node in 0x-prefixed form")
    void sendsEvmAddress() throws Exception {
        var evmAddress = new AccountId(0, 0, 12345678).toEvmAddress();
        query.setAccountId(AccountId.fromEvmAddress("0x" + evmAddress));

        stub.enqueue(new StubResponse(200, newBalanceResponse("0.0.12345", 500L)));

        var balance = query.execute(client);

        assertThat(balance.hbars).isEqualTo(Hbar.fromTinybars(500L));
        assertThat(stub.getLastQueryParams()).isEqualTo("account.id=0x" + evmAddress);
    }

    @Test
    @DisplayName("A public key alias is rendered as shard.realm.alias, though the endpoint rejects that form")
    void rendersAlias() throws Exception {
        var aliasAccountId = PrivateKey.generateED25519().getPublicKey().toAccountId(0, 0);
        query.setAccountId(aliasAccountId);

        stub.enqueue(new StubResponse(200, newBalanceResponse("0.0.12345", 42L)));

        var balance = query.execute(client);

        assertThat(balance.hbars).isEqualTo(Hbar.fromTinybars(42L));
        // URL rendering only. A real mirror node answers HTTP 400 for an alias in the account.id query
        // parameter — see MirrorNodeAccountBalanceQueryIntegrationTest.aliasAddressedAccountIsRejected.
        assertThat(stub.getLastQueryParams()).isEqualTo("account.id=" + aliasAccountId);
    }

    @Test
    @DisplayName("Given the mirror node is unavailable, the query retries on HTTP 503")
    void retriesOnUnavailable() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345")).setMaxAttempts(3).setMaxBackoff(Duration.ofMillis(500));

        stub.enqueue(new StubResponse(503, "transient error"));
        stub.enqueue(new StubResponse(200, newBalanceResponse("0.0.12345", 7L)));

        var balance = query.execute(client);

        assertThat(balance.hbars).isEqualTo(Hbar.fromTinybars(7L));
        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given the mirror node gateway times out, the query retries on HTTP 504")
    void retriesOnGatewayTimeout() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345")).setMaxAttempts(3).setMaxBackoff(Duration.ofMillis(500));

        stub.enqueue(new StubResponse(504, "gateway timeout"));
        stub.enqueue(new StubResponse(200, newBalanceResponse("0.0.12345", 9L)));

        var balance = query.execute(client);

        assertThat(balance.hbars).isEqualTo(Hbar.fromTinybars(9L));
        assertThat(stub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given an invalid ID format, the mirror node returns HTTP 400 and the query does not retry")
    void doesNotRetryOn400() {
        query.setAccountId(AccountId.fromString("0.0.12345")).setMaxAttempts(3);

        stub.enqueue(new StubResponse(400, "bad request"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("400");

        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given no account id is set, the query fails before making a network call")
    void failsBeforeNetworkCallWhenAccountIdMissing() {
        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accountId must be set");

        assertThat(stub.requestCount()).isZero();
    }

    private static String newBalanceResponse(String account, long tinybars) {
        return """
                {
                  "timestamp": "1234567890.000000000",
                  "balances": [
                    {
                      "account": "%s",
                      "balance": %d
                    }
                  ],
                  "links": { "next": null }
                }
                """.formatted(account, tinybars);
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
            server.createContext("/api/v1/balances", exchange -> {
                observedRequests++;
                lastQueryParams = exchange.getRequestURI().getQuery();
                var response = responses.poll();
                assertThat(response)
                        .as("response should be queued before invoking the balance query")
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
