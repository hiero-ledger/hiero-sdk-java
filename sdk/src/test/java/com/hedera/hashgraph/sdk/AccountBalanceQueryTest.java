// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.hedera.hashgraph.sdk.proto.QueryHeader;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the deprecation of {@link AccountBalanceQuery}: the consensus node
 * {@code CryptoService/cryptoGetBalance} endpoint is being retired, so the query warns on construction
 * and fails on every execution path without reaching a node.
 */
public class AccountBalanceQueryTest {

    private static final String EXPECTED_MESSAGE_FRAGMENT = "AccountBalanceQuery is no longer supported";

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerializeWithAccountId() {
        var builder = com.hedera.hashgraph.sdk.proto.Query.newBuilder();
        new AccountBalanceQuery()
                .setAccountId(AccountId.fromString("0.0.5005"))
                .onMakeRequest(builder, QueryHeader.newBuilder().build());
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void shouldSerializeWithContractId() {
        var builder = com.hedera.hashgraph.sdk.proto.Query.newBuilder();
        new AccountBalanceQuery()
                .setContractId(ContractId.fromString("0.0.5005"))
                .onMakeRequest(builder, QueryHeader.newBuilder().build());
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void constructorLogsDeprecationWarning() {
        var originalErr = System.err;
        var captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            new AccountBalanceQuery();
        } finally {
            System.setErr(originalErr);
        }

        assertThat(captured.toString(StandardCharsets.UTF_8)).contains(AccountBalanceQuery.DEPRECATION_MESSAGE);
    }

    @Test
    @DisplayName("execute throws, naming the replacement")
    void executeThrows() throws Exception {
        try (var mocker = recordingMocker()) {
            assertThatThrownBy(() -> new AccountBalanceQuery()
                            .setAccountId(new AccountId(0, 0, 10))
                            .execute(mocker.mocker.client))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining(EXPECTED_MESSAGE_FRAGMENT)
                    .hasMessageContaining("MirrorNodeAccountBalanceQuery");
        }
    }

    @Test
    @DisplayName("execute with an explicit timeout throws the same way")
    void executeWithTimeoutThrows() throws Exception {
        try (var mocker = recordingMocker()) {
            assertThatThrownBy(() -> new AccountBalanceQuery()
                            .setAccountId(new AccountId(0, 0, 10))
                            .execute(mocker.mocker.client, Duration.ofSeconds(5)))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining(EXPECTED_MESSAGE_FRAGMENT);
        }
    }

    @Test
    @DisplayName("executeAsync completes exceptionally, wrapped exactly once")
    void executeAsyncFails() throws Exception {
        try (var mocker = recordingMocker()) {
            var future = new AccountBalanceQuery()
                    .setAccountId(new AccountId(0, 0, 10))
                    .executeAsync(mocker.mocker.client);

            var joinFailure = catchThrowableOfType(CompletionException.class, future::join);
            assertThat(joinFailure.getCause())
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining(EXPECTED_MESSAGE_FRAGMENT);

            var getFailure = catchThrowableOfType(ExecutionException.class, future::get);
            assertThat(getFailure.getCause()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    @DisplayName("the callback overloads report the failure through onFailure rather than throwing")
    void executeAsyncCallbackReportsFailure() throws Exception {
        try (var mocker = recordingMocker()) {
            var error = new AtomicReference<Throwable>();

            new AccountBalanceQuery()
                    .setAccountId(new AccountId(0, 0, 10))
                    .executeAsync(mocker.mocker.client, balance -> {}, error::set);

            assertThat(error.get()).isNotNull();
            assertThat(rootCauseOf(error.get()))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining(EXPECTED_MESSAGE_FRAGMENT);
        }
    }

    @Test
    @DisplayName("getCost throws too, since it targets the same retiring endpoint")
    void getCostThrows() throws Exception {
        try (var mocker = recordingMocker()) {
            var query = new AccountBalanceQuery().setAccountId(new AccountId(0, 0, 10));

            assertThatThrownBy(() -> query.getCost(mocker.mocker.client))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining(EXPECTED_MESSAGE_FRAGMENT);

            var failure = catchThrowableOfType(CompletionException.class, () -> query.getCostAsync(mocker.mocker.client)
                    .join());
            assertThat(failure.getCause()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    @DisplayName("no request reaches any consensus node")
    void makesNoNetworkCall() throws Exception {
        try (var mocker = recordingMocker()) {
            var query = new AccountBalanceQuery().setAccountId(new AccountId(0, 0, 10));

            assertThatThrownBy(() -> query.execute(mocker.mocker.client))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> query.execute(mocker.mocker.client, Duration.ofSeconds(5)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> query.getCost(mocker.mocker.client))
                    .isInstanceOf(UnsupportedOperationException.class);

            catchThrowableOfType(CompletionException.class, () -> query.executeAsync(mocker.mocker.client)
                    .join());
            catchThrowableOfType(CompletionException.class, () -> query.getCostAsync(mocker.mocker.client)
                    .join());

            assertThat(mocker.requests)
                    .as("the deprecated query must not reach a node on any execution path")
                    .isEmpty();
        }
    }

    private static Throwable rootCauseOf(Throwable throwable) {
        var current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * A single-node mock that records every request it is asked to serve. Nothing is queued to answer
     * with: reaching the node at all is the failure this is looking for.
     */
    private static RecordingMocker recordingMocker() {
        var requests = new ArrayList<com.hedera.hashgraph.sdk.proto.Query>();

        List<Object> node = List.of((Function<Object, Object>) request -> {
            requests.add((com.hedera.hashgraph.sdk.proto.Query) request);
            throw new AssertionError("the deprecated AccountBalanceQuery reached a node: " + request);
        });

        return new RecordingMocker(Mocker.withResponses(Collections.singletonList(node)), requests);
    }

    private record RecordingMocker(Mocker mocker, List<com.hedera.hashgraph.sdk.proto.Query> requests)
            implements AutoCloseable {
        @Override
        public void close() throws Exception {
            mocker.close();
        }
    }
}
