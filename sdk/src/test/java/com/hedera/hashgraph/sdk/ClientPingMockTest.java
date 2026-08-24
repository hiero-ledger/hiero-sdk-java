// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.proto.CryptoGetInfoResponse;
import com.hedera.hashgraph.sdk.proto.Query;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import com.hedera.hashgraph.sdk.proto.ResponseType;
import io.grpc.Status;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the node liveness probe behind {@link Client#ping(AccountId)} and {@link Client#pingAll()}: a
 * {@code CryptoService/getAccountInfo} request for account {@code 0.0.2} sent with
 * {@code ResponseType = COST_ANSWER}, which replaced the old {@code AccountBalanceQuery} probe.
 */
class ClientPingMockTest {

    /**
     * The treasury account.
     */
    private static final com.hedera.hashgraph.sdk.proto.AccountID EXPECTED_PROBE_ACCOUNT_ID =
            new AccountId(0, 0, 2).toProtobuf();

    /**
     * The response a healthy node returns to a COST_ANSWER query: a precheck of OK plus the fee.
     */
    private static Response costAnswer(long costTinybars) {
        return Response.newBuilder()
                .setCryptoGetInfo(CryptoGetInfoResponse.newBuilder()
                        .setHeader(ResponseHeader.newBuilder()
                                .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                                .setResponseType(ResponseType.COST_ANSWER)
                                .setCost(costTinybars)))
                .build();
    }

    private static void assertIsCostAnswerAccountInfoProbe(Query request) {
        assertThat(request.hasCryptoGetInfo())
                .withFailMessage("probe must be sent to CryptoService/getAccountInfo, was: %s", request)
                .isTrue();
        assertThat(request.hasCryptogetAccountBalance())
                .withFailMessage("probe must not use the deprecated AccountBalanceQuery")
                .isFalse();
        assertThat(request.getCryptoGetInfo().getAccountID()).isEqualTo(EXPECTED_PROBE_ACCOUNT_ID);
        assertThat(request.getCryptoGetInfo().getHeader().getResponseType()).isEqualTo(ResponseType.COST_ANSWER);
    }

    /**
     * A single-node response list that records the request it received and answers with a cost.
     */
    private static List<Object> recordingCostAnswer(List<Query> sink) {
        Function<Query, Response> handler = request -> {
            synchronized (sink) {
                sink.add(request);
            }
            return costAnswer(25);
        };
        return List.of(handler);
    }

    @Test
    @DisplayName("ping sends CryptoService/getAccountInfo for account 0.0.2 with ResponseType = COST_ANSWER")
    void pingSendsCostAnswerGetAccountInfoForTreasuryAccount() throws Exception {
        var captured = new ArrayList<Query>();

        try (var mocker = Mocker.withResponses(List.of(recordingCostAnswer(captured)))) {
            mocker.client.ping(new AccountId(0, 0, 3));
        }

        assertThat(captured).hasSize(1);
        assertIsCostAnswerAccountInfoProbe(captured.get(0));
    }

    @Test
    @DisplayName("pingAsync sends CryptoService/getAccountInfo for account 0.0.2 with ResponseType = COST_ANSWER")
    void pingAsyncSendsCostAnswerGetAccountInfoForTreasuryAccount() throws Exception {
        var captured = new ArrayList<Query>();

        try (var mocker = Mocker.withResponses(List.of(recordingCostAnswer(captured)))) {
            mocker.client.pingAsync(new AccountId(0, 0, 3)).get();
        }

        assertThat(captured).hasSize(1);
        assertIsCostAnswerAccountInfoProbe(captured.get(0));
    }

    @Test
    @DisplayName("a cost response is treated as success")
    void costResponseIsTreatedAsSuccess() throws Exception {
        try (var mocker = Mocker.withResponses(List.of(List.of(costAnswer(25))))) {
            assertThatCode(() -> mocker.client.ping(new AccountId(0, 0, 3))).doesNotThrowAnyException();
        }

        try (var mocker = Mocker.withResponses(List.of(List.of(costAnswer(25))))) {
            assertThatCode(() -> mocker.client.pingAsync(new AccountId(0, 0, 3)).get())
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("a cost response of zero is still a success — the returned fee is irrelevant to liveness")
    void zeroCostResponseIsTreatedAsSuccess() throws Exception {
        try (var mocker = Mocker.withResponses(List.of(List.of(costAnswer(0))))) {
            assertThatCode(() -> mocker.client.ping(new AccountId(0, 0, 3))).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("the probe requires no operator, so ping works on an operator-less client")
    void pingWorksWithoutAnOperator() throws Exception {
        var captured = new ArrayList<Query>();

        try (var mocker = Mocker.withResponsesAndNoOperator(List.of(recordingCostAnswer(captured)))) {
            assertThat(mocker.client.getOperatorAccountId()).isNull();
            assertThatCode(() -> mocker.client.ping(new AccountId(0, 0, 3))).doesNotThrowAnyException();
        }

        assertThat(captured).hasSize(1);
        assertIsCostAnswerAccountInfoProbe(captured.get(0));
    }

    @Test
    @DisplayName("pingAll probes every node with a COST_ANSWER getAccountInfo for 0.0.2")
    void pingAllProbesEveryNode() throws Exception {
        var node3 = new ArrayList<Query>();
        var node4 = new ArrayList<Query>();
        var node5 = new ArrayList<Query>();

        try (var mocker = Mocker.withResponses(
                List.of(recordingCostAnswer(node3), recordingCostAnswer(node4), recordingCostAnswer(node5)))) {
            assertThat(mocker.client.getNetwork()).hasSize(3);
            mocker.client.pingAll();
        }

        for (var captured : List.of(node3, node4, node5)) {
            assertThat(captured).hasSize(1);
            assertIsCostAnswerAccountInfoProbe(captured.get(0));
        }
    }

    @Test
    @DisplayName("pingAllAsync probes every node with a COST_ANSWER getAccountInfo for 0.0.2")
    void pingAllAsyncProbesEveryNode() throws Exception {
        var node3 = new ArrayList<Query>();
        var node4 = new ArrayList<Query>();
        var node5 = new ArrayList<Query>();

        try (var mocker = Mocker.withResponses(
                List.of(recordingCostAnswer(node3), recordingCostAnswer(node4), recordingCostAnswer(node5)))) {
            mocker.client.pingAllAsync().get();
        }

        for (var captured : List.of(node3, node4, node5)) {
            assertThat(captured).hasSize(1);
            assertIsCostAnswerAccountInfoProbe(captured.get(0));
        }
    }

    @Test
    @DisplayName("a successful ping decreases the node's backoff and leaves it healthy")
    void successfulPingDecreasesNodeBackoff() throws Exception {
        try (var mocker = Mocker.withResponses(List.of(List.of(costAnswer(25))))) {
            var node = mocker.client.network.nodes.get(0);
            node.minBackoff = Duration.ofMillis(250);
            node.currentBackoff = Duration.ofSeconds(4);

            mocker.client.ping(new AccountId(0, 0, 3));

            assertThat(node.currentBackoff).isEqualTo(Duration.ofSeconds(2));
            assertThat(node.badGrpcStatusCount).isZero();
            assertThat(node.isHealthy()).isTrue();
        }
    }

    @Test
    @DisplayName("a failed ping increases the node's backoff and marks it unhealthy")
    void failedPingIncreasesNodeBackoff() throws Exception {
        var unavailable = Status.UNAVAILABLE.asRuntimeException();

        try (var mocker = Mocker.withResponses(List.of(List.of(unavailable)))) {
            mocker.client.setMaxAttempts(1);

            var node = mocker.client.network.nodes.get(0);
            node.currentBackoff = Duration.ofSeconds(30);

            assertThatExceptionOfType(MaxAttemptsExceededException.class)
                    .isThrownBy(() -> mocker.client.ping(new AccountId(0, 0, 3)));

            assertThat(node.badGrpcStatusCount).isEqualTo(1);
            assertThat(node.isHealthy()).isFalse();
        }
    }

    @Test
    @DisplayName("a precheck failure on the probe surfaces as a PrecheckStatusException")
    void precheckFailureOnProbeThrows() throws Exception {
        var invalidAccount = Response.newBuilder()
                .setCryptoGetInfo(CryptoGetInfoResponse.newBuilder()
                        .setHeader(ResponseHeader.newBuilder()
                                .setNodeTransactionPrecheckCode(ResponseCodeEnum.INVALID_ACCOUNT_ID)
                                .setResponseType(ResponseType.COST_ANSWER)))
                .build();

        try (var mocker = Mocker.withResponses(Collections.singletonList(List.of(invalidAccount)))) {
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> mocker.client.ping(new AccountId(0, 0, 3)))
                    .withMessageContaining("INVALID_ACCOUNT_ID");
        }
    }
}
