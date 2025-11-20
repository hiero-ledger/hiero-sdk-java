// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.mirror.NetworkServiceGrpc;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
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
    private FeeEstimateServiceStub feeEstimateServiceStub;
    private Server server;
    private FeeEstimateQuery query;

    @BeforeEach
    void setUp() throws Exception {
        client = Client.forNetwork(Collections.emptyMap());
        client.setRequestTimeout(Duration.ofSeconds(10));
        // FIX: Use unique in-process server name for each test run
        String serverName = "test-" + System.nanoTime();

        client.setMirrorNetwork(Collections.singletonList("in-process:" + serverName));

        feeEstimateServiceStub = new FeeEstimateServiceStub();
        server = InProcessServerBuilder.forName(serverName) // FIX: unique name here
                .addService(feeEstimateServiceStub)
                .directExecutor()
                .build()
                .start();

        query = new FeeEstimateQuery();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Verify the stub received and processed all requests
        feeEstimateServiceStub.verify();

        // FIX: ensure proper cleanup between tests
        if (server != null) {
            server.shutdownNow(); // FIX: force shutdown to avoid lingering registrations
            server.awaitTermination(1, TimeUnit.SECONDS);
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    @DisplayName(
            "Given a FeeEstimateQuery is executed when the Mirror service is unavailable, when the query is executed, then it retries according to the existing query retry policy for UNAVAILABLE errors")
    void retriesOnUnavailableErrors() {
        query.setTransaction(DUMMY_TRANSACTION).setMaxAttempts(3).setMaxBackoff(Duration.ofMillis(500));

        var expectedRequest = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery.newBuilder()
                .setModeValue(FeeEstimateMode.STATE.code)
                .setTransaction(DUMMY_TRANSACTION)
                .build();

        feeEstimateServiceStub.enqueueError(
                expectedRequest, Status.UNAVAILABLE.withDescription("transient").asRuntimeException());
        feeEstimateServiceStub.enqueue(expectedRequest, newSuccessResponse(FeeEstimateMode.STATE, 2, 6, 8));

        var response = query.execute(client);

        assertThat(response.getMode()).isEqualTo(FeeEstimateMode.STATE);
        assertThat(response.getTotal()).isEqualTo(26);
        assertThat(feeEstimateServiceStub.requestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName(
            "Given a FeeEstimateQuery times out, when the query is executed, then it retries according to the existing query retry policy for DEADLINE_EXCEEDED errors")
    void retriesOnDeadlineExceededErrors() {
        query.setTransaction(DUMMY_TRANSACTION).setMaxAttempts(3).setMaxBackoff(Duration.ofMillis(500));

        var expectedRequest = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery.newBuilder()
                .setModeValue(FeeEstimateMode.STATE.code)
                .setTransaction(DUMMY_TRANSACTION)
                .build();

        feeEstimateServiceStub.enqueueError(
                expectedRequest,
                Status.DEADLINE_EXCEEDED.withDescription("timeout").asRuntimeException());
        feeEstimateServiceStub.enqueue(expectedRequest, newSuccessResponse(FeeEstimateMode.STATE, 4, 8, 20));

        var response = query.execute(client);

        assertThat(response.getMode()).isEqualTo(FeeEstimateMode.STATE);
        assertThat(response.getTotal()).isEqualTo(60);
        assertThat(feeEstimateServiceStub.requestCount()).isEqualTo(2);
    }

    private static com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse newSuccessResponse(
            FeeEstimateMode mode, int networkMultiplier, long nodeBase, long serviceBase) {
        long networkSubtotal = nodeBase * networkMultiplier;
        long total = networkSubtotal + nodeBase + serviceBase;
        return com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse.newBuilder()
                .setModeValue(mode.code)
                .setNetwork(com.hedera.hashgraph.sdk.proto.mirror.NetworkFee.newBuilder()
                        .setMultiplier(networkMultiplier)
                        .setSubtotal(networkSubtotal)
                        .build())
                .setNode(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate.newBuilder()
                        .setBase(nodeBase)
                        .build())
                .setService(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate.newBuilder()
                        .setBase(serviceBase)
                        .build())
                .setTotal(total)
                .build();
    }

    private static class FeeEstimateServiceStub extends NetworkServiceGrpc.NetworkServiceImplBase {
        private final Queue<com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery> expectedRequests =
                new ArrayDeque<>();
        private final Queue<Object> responses = new ArrayDeque<>();
        private int observedRequests = 0;

        void enqueue(
                com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery request,
                com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse response) {
            expectedRequests.add(request);
            responses.add(response);
        }

        void enqueueError(
                com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery request, StatusRuntimeException error) {
            expectedRequests.add(request);
            responses.add(error);
        }

        int requestCount() {
            return observedRequests;
        }

        void verify() {
            assertThat(expectedRequests).isEmpty();
            assertThat(responses).isEmpty();
        }

        @Override
        public void getFeeEstimate(
                com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery request,
                StreamObserver<com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse> responseObserver) {
            observedRequests++;
            var expected = expectedRequests.poll();
            assertThat(expected)
                    .as("expected request to be queued before invoking getFeeEstimate")
                    .isNotNull();
            assertThat(request).isEqualTo(expected);

            var response = responses.poll();
            assertThat(response)
                    .as("response or error should be queued before invoking getFeeEstimate")
                    .isNotNull();

            if (response instanceof StatusRuntimeException error) {
                responseObserver.onError(error);
                return;
            }

            responseObserver.onNext((com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse) response);
            responseObserver.onCompleted();
        }
    }
}
