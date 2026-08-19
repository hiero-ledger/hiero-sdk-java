// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MirrorNodeAccountBalanceQueryMockTest {

    private static final String EMPTY_BALANCES_RESPONSE =
            "{\"timestamp\":\"1.0\",\"balances\":[],\"links\":{\"next\":null}}";

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
    @DisplayName("Given a non-existent account, the empty balances array fails with INVALID_ACCOUNT_ID")
    void emptyBalancesArrayThrowsInvalidAccountId() {
        query.setAccountId(AccountId.fromString("0.0.999999999"));

        stub.enqueue(new StubResponse(200, EMPTY_BALANCES_RESPONSE));

        assertThatExceptionOfType(PrecheckStatusException.class)
                .isThrownBy(() -> query.execute(client))
                .satisfies(e -> {
                    assertThat(e.status).isEqualTo(Status.INVALID_ACCOUNT_ID);
                    assertThat(e.transactionId).isNull();
                })
                .withMessageContaining("INVALID_ACCOUNT_ID");

        // A 200 is a final answer: the empty array must not be retried as if it were transient.
        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("An account that exists with no hbars still reports zero rather than failing")
    void zeroBalanceAccountIsNotTreatedAsMissing() throws Exception {
        query.setAccountId(AccountId.fromString("0.0.12345"));

        stub.enqueue(new StubResponse(200, newBalanceResponse("0.0.12345", 0L)));

        var balance = query.execute(client);

        assertThat(balance.hbars).isEqualTo(Hbar.ZERO);
    }

    @Test
    @DisplayName("On the async path, a non-existent account completes the future exceptionally")
    void asyncNotFoundCompletesFutureExceptionally() {
        query.setAccountId(AccountId.fromString("0.0.999999999"));

        stub.enqueue(new StubResponse(200, EMPTY_BALANCES_RESPONSE));

        var future = query.executeAsync(client);

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(PrecheckStatusException.class);
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(PrecheckStatusException.class);
    }

    @Test
    @DisplayName("A 200 body with no balances array is reported as malformed, not as a missing account")
    void missingBalancesKeyIsReportedAsMalformed() {
        query.setAccountId(AccountId.fromString("0.0.12345"));

        stub.enqueue(new StubResponse(200, "{\"timestamp\":\"1.0\"}"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("balances");
    }

    @Test
    @DisplayName("A balances entry with no balance field is reported as malformed, not as zero")
    void balancesEntryWithoutBalanceFieldIsReportedAsMalformed() {
        query.setAccountId(AccountId.fromString("0.0.12345"));

        stub.enqueue(new StubResponse(200, "{\"balances\":[{\"account\":\"0.0.12345\"}]}"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("`balance` field");
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
    @DisplayName("A public key alias is sent as the bare base32 alias the mirror node expects")
    void sendsAlias() throws Exception {
        var aliasKey = PrivateKey.generateED25519().getPublicKey();
        query.setAccountId(aliasKey.toAccountId(0, 0));

        stub.enqueue(new StubResponse(200, newBalanceResponse("0.0.12345", 42L)));

        var balance = query.execute(client);

        assertThat(balance.hbars).isEqualTo(Hbar.fromTinybars(42L));
        var expectedAlias = BaseEncoding.base32()
                .omitPadding()
                .encode(aliasKey.toProtobufKey().toByteArray());
        assertThat(stub.getLastQueryParams()).isEqualTo("account.id=" + expectedAlias);
        assertThat(expectedAlias).doesNotContain(".").doesNotContain("=");
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
    @DisplayName("Given a 200 response with a malformed body, the failure names the parse, not an HTTP error")
    void reportsMalformedBody() {
        query.setAccountId(AccountId.fromString("0.0.12345")).setMaxAttempts(3);

        stub.enqueue(new StubResponse(200, "not json"));

        assertThatThrownBy(() -> query.execute(client))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("malformed JSON");

        assertThat(stub.requestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given autoValidateChecksums, a bad checksum fails before making a network call")
    void validatesChecksumWhenEnabled() {
        client.setLedgerId(LedgerId.TESTNET);
        client.setAutoValidateChecksums(true);

        query.setAccountId(AccountId.fromString("0.0.12345-aaaaa"));

        assertThatThrownBy(() -> query.execute(client)).isInstanceOf(IllegalArgumentException.class);

        assertThat(stub.requestCount()).isZero();
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
