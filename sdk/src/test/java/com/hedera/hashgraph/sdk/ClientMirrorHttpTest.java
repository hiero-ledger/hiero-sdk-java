// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientMirrorHttpTest {

    private static final TokenId TOKEN_ID = TokenId.fromString("0.0.1135");
    private static final AccountId ACCOUNT_ID = AccountId.fromString("0.0.12345");

    private final List<HttpServer> servers = new ArrayList<>();

    @AfterEach
    void tearDown() {
        servers.forEach(server -> server.stop(0));
    }

    private Client newClient() throws Exception {
        var client = Client.forNetwork(Collections.emptyMap());
        client.setRequestTimeout(Duration.ofSeconds(10));
        return client;
    }

    private static MirrorNodeTokenBalance query(Client client) throws Exception {
        return new MirrorNodeTokenBalanceQuery()
                .setAccountId(ACCOUNT_ID)
                .setTokenId(TOKEN_ID)
                .execute(client);
    }

    private int startServer(ConcurrentLinkedQueue<Integer> hits) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        var port = server.getAddress().getPort();
        servers.add(server);

        server.createContext("/api/v1/accounts", exchange -> {
            hits.add(port);
            var body = "{\"tokens\":[],\"links\":{\"next\":null}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(body);
            }
        });

        server.start();
        return port;
    }

    @Test
    @DisplayName("Three calls against three mirror nodes visit each one in turn")
    void picksTheBaseUrlByRoundRobin() throws Exception {
        var hits = new ConcurrentLinkedQueue<Integer>();
        var ports = List.of(startServer(hits), startServer(hits), startServer(hits));

        try (var client = newClient()) {
            client.setMirrorNetwork(
                    ports.stream().map(port -> "localhost:" + port).toList());

            for (var i = 0; i < 6; i++) {
                query(client);
            }
        }

        assertThat(hits).hasSize(6);
        assertThat(hits.stream().distinct().toList())
                .as("every configured mirror node should have been used")
                .containsExactlyInAnyOrderElementsOf(ports);
        assertThat(hits.stream().limit(3).distinct().toList())
                .as("the first three calls should each pick a different node")
                .hasSize(3);
    }

    @Test
    @DisplayName("Two query executions on one client share one transport")
    void sharesOneTransport() throws Exception {
        var transport = new CountingTransport();

        try (var client = newClient()) {
            client.setMirrorNetwork(Collections.singletonList("localhost:5600"));
            client.setMirrorNodeHttpConfig(client.getMirrorNodeHttpConfig().withTransport(transport));

            query(client);
            query(client);
        }

        assertThat(transport.calls).hasValue(2);
    }

    @Test
    @DisplayName("A client that never used mirror REST closes without raising")
    void closingAnUnusedClientIsANoOp() throws Exception {
        var client = newClient();
        client.close();
    }

    @Test
    @DisplayName("Closing does not close a transport the application injected")
    void doesNotCloseAnInjectedTransport() throws Exception {
        var transport = new CountingTransport();

        var client = newClient();
        client.setMirrorNetwork(Collections.singletonList("localhost:5600"));
        client.setMirrorNodeHttpConfig(client.getMirrorNodeHttpConfig().withTransport(transport));

        query(client);
        client.close();

        assertThat(transport.closed).isFalse();
    }

    @Test
    @DisplayName("Setting the config replaces rather than merges, and the getter reports what was supplied")
    void configSetterReplaces() throws Exception {
        try (var client = newClient()) {
            assertThat(client.getMirrorNodeHttpConfig().getTransport()).isNull();
            assertThat(client.getMirrorNodeHttpConfig().getRetryPolicy().getMaxAttempts())
                    .isEqualTo(5);

            client.setMirrorNodeHttpConfig(MirrorNodeHttpConfig.defaults()
                    .withRetryPolicy(MirrorNodeHttpRetryPolicy.defaults().withMaxAttempts(2)));

            assertThat(client.getMirrorNodeHttpConfig().getRetryPolicy().getMaxAttempts())
                    .isEqualTo(2);

            // set(get()) is a true no-op rather than a statement of ownership.
            client.setMirrorNodeHttpConfig(client.getMirrorNodeHttpConfig());
            assertThat(client.getMirrorNodeHttpConfig().getTransport()).isNull();
        }
    }

    @Test
    @DisplayName("A query setter overrides only the field it names: attempts change, the rest do not")
    void querySetterOverridesOneFieldOnly() throws Exception {
        var transport = new CountingTransport();
        transport.status = 503;

        try (var client = newClient()) {
            client.setMirrorNetwork(Collections.singletonList("localhost:5600"));
            client.setMirrorNodeHttpConfig(client.getMirrorNodeHttpConfig()
                    .withTransport(transport)
                    .withRetryPolicy(MirrorNodeHttpRetryPolicy.defaults()
                            .withMaxAttempts(9)
                            .withPerAttemptTimeout(Duration.ofSeconds(3))
                            .withInitialBackoff(Duration.ofMillis(1))
                            .withMaxBackoff(Duration.ofMillis(2))));

            assertThatThrownBy(() -> new MirrorNodeTokenBalanceQuery()
                            .setAccountId(ACCOUNT_ID)
                            .setTokenId(TOKEN_ID)
                            .setMaxAttempts(2)
                            .execute(client))
                    .hasMessageContaining(HttpTransportErrorKind.RETRIES_EXHAUSTED_ERROR.getId());
        }

        // The query's setMaxAttempts(2) wins over the client's 9...
        assertThat(transport.calls).hasValue(2);
        // ...and every other field still comes from the client's policy.
        assertThat(transport.lastDeadline).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    @DisplayName("A client with no mirror network configured says so rather than failing obscurely")
    void rejectsAnEmptyMirrorNetwork() throws Exception {
        try (var client = newClient()) {
            assertThatThrownBy(() -> query(client))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no mirror network");
        }
    }

    /**
     * Records what reaches the seam: how many attempts were made, and the per-attempt deadline the
     * adapter derived, which is the only part of the policy a transport can observe.
     */
    private static final class CountingTransport implements HttpTransport {
        final AtomicInteger calls = new AtomicInteger();
        volatile int status = 200;

        @Nullable
        volatile Duration lastDeadline = null;

        volatile boolean closed = false;

        @Override
        public CompletionStage<HttpResponse> roundTrip(HttpRequest request, Cancellation cancellation) {
            calls.incrementAndGet();
            lastDeadline = request.getDeadline();
            return CompletableFuture.completedFuture(
                    HttpResponse.create(status, "{\"tokens\":[]}".getBytes(StandardCharsets.UTF_8), Map.of()));
        }

        @Override
        public void close(Duration closeTimeout) {
            closed = true;
        }
    }
}
