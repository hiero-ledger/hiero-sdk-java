// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import org.hiero.sdk.Executable.GrpcRequest;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseCodeEnum;
import org.hiero.sdk.proto.ResponseHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

class ExecutableTest {
    Client client;
    Network network;
    Node node3, node4, node5;
    List<AccountId> nodeAccountIds;

    @BeforeEach
    void setup() {
        client = Client.forMainnet();
        network = Mockito.mock(Network.class);
        client.network = network;
        client.setLogger(new Logger(LogLevel.WARN));

        node3 = Mockito.mock(Node.class);
        node4 = Mockito.mock(Node.class);
        node5 = Mockito.mock(Node.class);

        when(node3.getAccountId()).thenReturn(new AccountId(3));
        when(node4.getAccountId()).thenReturn(new AccountId(4));
        when(node5.getAccountId()).thenReturn(new AccountId(5));
        when(network.getNodeProxies(new AccountId(3))).thenReturn(List.of(node3));
        when(network.getNodeProxies(new AccountId(4))).thenReturn(List.of(node4));
        when(network.getNodeProxies(new AccountId(5))).thenReturn(List.of(node5));

        nodeAccountIds = Arrays.asList(new AccountId(3), new AccountId(4), new AccountId(5));
    }

    @Test
    void firstNodeHealthy() {
        when(node3.isHealthy()).thenReturn(true);

        var tx = new DummyTransaction();
        tx.setNodeAccountIds(nodeAccountIds);
        tx.setNodesFromNodeAccountIds(client);
        tx.setMinBackoff(Duration.ofMillis(10));
        tx.setMaxBackoff(Duration.ofMillis(1000));

        var node = tx.getNodeForExecute(1);
        assertThat(node).isEqualTo(node3);
    }

    @Test
    void calloptionsShouldRespectGrpcDeadline() {
        when(node3.isHealthy()).thenReturn(true);

        var tx = new DummyTransaction();
        tx.setNodeAccountIds(nodeAccountIds);
        tx.setNodesFromNodeAccountIds(client);
        tx.setMinBackoff(Duration.ofMillis(10));
        tx.setMaxBackoff(Duration.ofMillis(1000));
        tx.setGrpcDeadline(Duration.ofSeconds(10));

        var grpcRequest = tx.getGrpcRequest(1);

        var timeRemaining = grpcRequest.getCallOptions().getDeadline().timeRemaining(TimeUnit.MILLISECONDS);
        assertThat(timeRemaining).isLessThan(10000);
        assertThat(timeRemaining).isGreaterThan(9000);
    }

    @Test
    void executableShouldUseGrpcDeadline() throws InterruptedException, PrecheckStatusException, TimeoutException {
        when(node3.isHealthy()).thenReturn(true);

        var tx = new DummyTransaction();
        tx.setNodeAccountIds(nodeAccountIds);
        tx.setNodesFromNodeAccountIds(client);
        tx.setMinBackoff(Duration.ofMillis(10));
        tx.setMaxBackoff(Duration.ofMillis(1000));
        tx.setMaxAttempts(10);

        var timeout = Duration.ofSeconds(5);
        var currentTimeRemaining = new AtomicLong(timeout.toMillis());
        final long minimumRetryDelayMs = 100;
        final long defaultDeadlineMs = timeout.toMillis() - (minimumRetryDelayMs * (tx.getMaxAttempts() / 2));

        // later on when the transaction is executed its grpc deadline should not be modified...
        tx.setGrpcDeadline(Duration.ofMillis(defaultDeadlineMs));

        tx.blockingUnaryCall = (grpcRequest) -> {
            var grpc = (GrpcRequest) grpcRequest;

            var grpcTimeRemaining = grpc.getCallOptions().getDeadline().timeRemaining(TimeUnit.MILLISECONDS);

            // the actual grpc deadline should be no larger than the smaller of the two values -
            // the default transaction level grpc deadline and the remaining timeout
            assertThat(grpcTimeRemaining).isLessThanOrEqualTo(defaultDeadlineMs);
            assertThat(grpcTimeRemaining).isLessThanOrEqualTo(currentTimeRemaining.get());

            assertThat(grpcTimeRemaining).isGreaterThan(0);

            // transaction's grpc deadline should keep its original value
            assertThat(tx.grpcDeadline().toMillis()).isEqualTo(defaultDeadlineMs);

            currentTimeRemaining.set(currentTimeRemaining.get() - minimumRetryDelayMs);

            if (currentTimeRemaining.get() > 0) {
                try {
                    Thread.sleep(minimumRetryDelayMs);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // Status.UNAVAILABLE tells the Executable to retry the request
                throw new StatusRuntimeException(io.grpc.Status.UNAVAILABLE);
            }

            throw new StatusRuntimeException(io.grpc.Status.ABORTED);
        };

        assertThatExceptionOfType(MaxAttemptsExceededException.class).isThrownBy(() -> {
            tx.execute(client, timeout);
        });
    }

    @Test
    void multipleNodesUnhealthy() {
        when(node3.isHealthy()).thenReturn(false);
        when(node4.isHealthy()).thenReturn(true);

        when(node3.getRemainingTimeForBackoff()).thenReturn(1000L);

        var tx = new DummyTransaction();
        tx.setNodeAccountIds(nodeAccountIds);
        tx.setNodesFromNodeAccountIds(client);
        tx.setMinBackoff(Duration.ofMillis(10));
        tx.setMaxBackoff(Duration.ofMillis(1000));

        var node = tx.getNodeForExecute(1);
        assertThat(node).isEqualTo(node4);
    }

    @Test
    void allNodesUnhealthy() {
        when(node3.isHealthy()).thenReturn(false);
        when(node4.isHealthy()).thenReturn(false);
        when(node5.isHealthy()).thenReturn(false);

        when(node3.getRemainingTimeForBackoff()).thenReturn(4000L);
        when(node4.getRemainingTimeForBackoff()).thenReturn(3000L);
        when(node5.getRemainingTimeForBackoff()).thenReturn(5000L);

        var tx = new DummyTransaction();
        tx.setNodeAccountIds(nodeAccountIds);
        tx.setNodesFromNodeAccountIds(client);
        tx.setMinBackoff(Duration.ofMillis(10));
        tx.setMaxBackoff(Duration.ofMillis(1000));
        tx.nodeAccountIds.setIndex(1);

        var node = tx.getNodeForExecute(1);
        assertThat(node).isEqualTo(node4);
    }

    @Test
    void multipleRequestsWithSingleHealthyNode() {
        when(node3.isHealthy()).thenReturn(true);
        when(node4.isHealthy()).thenReturn(false);
        when(node5.isHealthy()).thenReturn(false);

        when(node4.getRemainingTimeForBackoff()).thenReturn(4000L);
        when(node5.getRemainingTimeForBackoff()).thenReturn(3000L);

        var tx = new DummyTransaction();
        tx.setNodeAccountIds(nodeAccountIds);
        tx.setNodesFromNodeAccountIds(client);
        tx.setMinBackoff(Duration.ofMillis(10));
        tx.setMaxBackoff(Duration.ofMillis(1000));

        var node = tx.getNodeForExecute(1);
        assertThat(node).isEqualTo(node3);
        tx.nodeAccountIds.advance();
        tx.nodes.advance();

        node = tx.getNodeForExecute(2);
        assertThat(node).isEqualTo(node3);
        verify(node4).getRemainingTimeForBackoff();
        verify(node5).getRemainingTimeForBackoff();
    }

    @Test
    void multipleRequestsWithNoHealthyNodes() {
        AtomicInteger i = new AtomicInteger();

        when(node3.isHealthy()).thenReturn(false);
        when(node4.isHealthy()).thenReturn(false);
        when(node5.isHealthy()).thenReturn(false);

        long[] node3Times = {4000, 3000, 1000};
        long[] node4Times = {3000, 1000, 4000};
        long[] node5Times = {1000, 3000, 4000};

        when(node3.getRemainingTimeForBackoff()).thenAnswer((Answer<Long>) invocation -> node3Times[i.get()]);
        when(node4.getRemainingTimeForBackoff()).thenAnswer((Answer<Long>) invocation -> node4Times[i.get()]);
        when(node5.getRemainingTimeForBackoff()).thenAnswer((Answer<Long>) invocation -> node5Times[i.get()]);

        var tx = new DummyTransaction();
        tx.setNodeAccountIds(nodeAccountIds);
        tx.setNodesFromNodeAccountIds(client);
        tx.setMinBackoff(Duration.ofMillis(10));
        tx.setMaxBackoff(Duration.ofMillis(1000));

        var node = tx.getNodeForExecute(1);
        assertThat(node).isEqualTo(node5);
        i.incrementAndGet();

        node = tx.getNodeForExecute(2);
        assertThat(node).isEqualTo(node4);
        i.incrementAndGet();

        node = tx.getNodeForExecute(3);
        assertThat(node).isEqualTo(node3);
    }

    @Test
    void successfulExecute() throws PrecheckStatusException, TimeoutException {
        var now = java.time.Instant.now();
        var tx = new DummyTransaction() {
            @Nullable
            @Override
            TransactionResponse mapResponse(
                    org.hiero.sdk.proto.TransactionResponse response,
                    AccountId nodeId,
                    org.hiero.sdk.proto.Transaction request) {
                return new TransactionResponse(
                                new AccountId(3),
                                TransactionId.withValidStart(new AccountId(3), now),
                                new byte[] {1, 2, 3},
                                null,
                                null)
                        .setValidateStatus(true);
            }
        };

        var nodeAccountIds = Arrays.asList(new AccountId(3), new AccountId(4), new AccountId(5));
        tx.setNodeAccountIds(nodeAccountIds);

        var txResp = org.hiero.sdk.proto.TransactionResponse.newBuilder()
                .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                .build();

        tx.blockingUnaryCall = (grpcRequest) -> txResp;
        org.hiero.sdk.TransactionResponse resp = (org.hiero.sdk.TransactionResponse) tx.execute(client);

        assertThat(resp.nodeId).isEqualTo(new AccountId(3));
        assertThat(resp.getValidateStatus()).isTrue();
        assertThat(resp.toString()).isNotNull();
    }

    @Test
    void executeWithChannelFailure() throws PrecheckStatusException, TimeoutException {
        when(node3.isHealthy()).thenReturn(true);
        when(node4.isHealthy()).thenReturn(true);

        when(node3.channelFailedToConnect(any(Instant.class))).thenReturn(true);
        when(node4.channelFailedToConnect(any(Instant.class))).thenReturn(false);

        var now = java.time.Instant.now();
        var tx = new DummyTransaction() {
            @Nullable
            @Override
            TransactionResponse mapResponse(
                    org.hiero.sdk.proto.TransactionResponse response,
                    AccountId nodeId,
                    org.hiero.sdk.proto.Transaction request) {
                return new TransactionResponse(
                        new AccountId(4),
                        TransactionId.withValidStart(new AccountId(4), now),
                        new byte[] {1, 2, 3},
                        null,
                        null);
            }
        };

        var nodeAccountIds = Arrays.asList(new AccountId(3), new AccountId(4), new AccountId(5));
        tx.setNodeAccountIds(nodeAccountIds);

        var txResp = org.hiero.sdk.proto.TransactionResponse.newBuilder()
                .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                .build();

        tx.blockingUnaryCall = (grpcRequest) -> txResp;
        org.hiero.sdk.TransactionResponse resp = (org.hiero.sdk.TransactionResponse) tx.execute(client);

        verify(node3).channelFailedToConnect(any(Instant.class));
        verify(node4).channelFailedToConnect(any(Instant.class));
        assertThat(resp.nodeId).isEqualTo(new AccountId(4));
    }

    @Test
    void executeWithAllUnhealthyNodes() throws PrecheckStatusException, TimeoutException {
        AtomicInteger i = new AtomicInteger();

        // 1st round, pick node3, fail channel connect
        // 2nd round, pick node4, fail channel connect
        // 3rd round, pick node5, fail channel connect
        // 4th round, pick node 3, wait for delay, channel connect ok
        when(node3.isHealthy()).thenAnswer((Answer<Boolean>) inv -> i.get() == 0);
        when(node4.isHealthy()).thenAnswer((Answer<Boolean>) inv -> i.get() == 0);
        when(node5.isHealthy()).thenAnswer((Answer<Boolean>) inv -> i.get() == 0);

        when(node3.channelFailedToConnect(any(Instant.class))).thenAnswer((Answer<Boolean>) inv -> i.get() == 0);
        when(node4.channelFailedToConnect(any(Instant.class))).thenAnswer((Answer<Boolean>) inv -> i.get() == 0);
        when(node5.channelFailedToConnect(any(Instant.class)))
                .thenAnswer((Answer<Boolean>) inv -> i.getAndIncrement() == 0);

        when(node3.getRemainingTimeForBackoff()).thenReturn(500L);
        when(node4.getRemainingTimeForBackoff()).thenReturn(600L);
        when(node5.getRemainingTimeForBackoff()).thenReturn(700L);

        var now = java.time.Instant.now();
        var tx = new DummyTransaction() {
            @Nullable
            @Override
            TransactionResponse mapResponse(
                    org.hiero.sdk.proto.TransactionResponse response,
                    AccountId nodeId,
                    org.hiero.sdk.proto.Transaction request) {
                return new TransactionResponse(
                        new AccountId(3),
                        TransactionId.withValidStart(new AccountId(3), now),
                        new byte[] {1, 2, 3},
                        null,
                        null);
            }
        };

        var nodeAccountIds = Arrays.asList(new AccountId(3), new AccountId(4), new AccountId(5));
        tx.setNodeAccountIds(nodeAccountIds);

        var txResp = org.hiero.sdk.proto.TransactionResponse.newBuilder()
                .setNodeTransactionPrecheckCode(ResponseCodeEnum.OK)
                .build();

        tx.blockingUnaryCall = (grpcRequest) -> txResp;
        org.hiero.sdk.TransactionResponse resp = (org.hiero.sdk.TransactionResponse) tx.execute(client);

        verify(node3, times(2)).channelFailedToConnect(any(Instant.class));
        verify(node4).channelFailedToConnect(any(Instant.class));
        verify(node5).channelFailedToConnect(any(Instant.class));
        assertThat(resp.nodeId).isEqualTo(new AccountId(3));
    }

    @Test
    void executeExhaustRetries() {
        when(node3.isHealthy()).thenReturn(true);
        when(node4.isHealthy()).thenReturn(true);
        when(node5.isHealthy()).thenReturn(true);

        when(node3.channelFailedToConnect(any(Instant.class))).thenReturn(true);
        when(node4.channelFailedToConnect(any(Instant.class))).thenReturn(true);
        when(node5.channelFailedToConnect(any(Instant.class))).thenReturn(true);

        var tx = new DummyTransaction();
        var nodeAccountIds = Arrays.asList(new AccountId(3), new AccountId(4), new AccountId(5));
        tx.setNodeAccountIds(nodeAccountIds);
        assertThatExceptionOfType(MaxAttemptsExceededException.class).isThrownBy(() -> tx.execute(client));
    }

    @Test
    void executeRetriableErrorDuringCall() {
        AtomicInteger i = new AtomicInteger();

        when(node3.isHealthy()).thenReturn(true);
        when(node4.isHealthy()).thenReturn(true);

        when(node3.channelFailedToConnect(any(Instant.class))).thenReturn(false);
        when(node4.channelFailedToConnect(any(Instant.class))).thenReturn(false);

        var tx = new DummyTransaction();
        var nodeAccountIds = Arrays.asList(new AccountId(3), new AccountId(4), new AccountId(5));
        tx.setNodeAccountIds(nodeAccountIds);

        tx.blockingUnaryCall = (grpcRequest) -> {
            if (i.getAndIncrement() == 0) {
                throw new StatusRuntimeException(io.grpc.Status.UNAVAILABLE);
            } else {
                throw new StatusRuntimeException(io.grpc.Status.ABORTED);
            }
        };

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> tx.execute(client));

        verify(node3).channelFailedToConnect(any(Instant.class));
        verify(node4).channelFailedToConnect(any(Instant.class));
    }

    @Test
    void testChannelFailedToConnectTimeout() {
        TransactionResponse transactionResponse = new TransactionResponse(
                new AccountId(3),
                TransactionId.withValidStart(new AccountId(3), java.time.Instant.now()),
                new byte[] {1, 2, 3},
                null,
                null);
        var tx = new DummyTransaction();

        tx.blockingUnaryCall = (grpcRequest) -> {
            throw new StatusRuntimeException(io.grpc.Status.UNAVAILABLE);
        };

        when(node3.isHealthy()).thenReturn(true);
        when(node3.channelFailedToConnect(any(Instant.class))).thenReturn(true);

        assertThatExceptionOfType(MaxAttemptsExceededException.class)
                .isThrownBy(() -> transactionResponse.getReceipt(client, Duration.ofSeconds(2)));
    }

    @Test
    void executeQueryDelay() throws PrecheckStatusException, TimeoutException {
        when(node3.isHealthy()).thenReturn(true);
        when(node4.isHealthy()).thenReturn(true);

        when(node3.channelFailedToConnect()).thenReturn(false);
        when(node4.channelFailedToConnect()).thenReturn(false);

        AtomicInteger i = new AtomicInteger();
        var tx = new DummyQuery() {
            @Override
            Status mapResponseStatus(org.hiero.sdk.proto.Response response) {
                return Status.RECEIPT_NOT_FOUND;
            }

            @Override
            ExecutionState getExecutionState(Status status, Response response) {
                return i.getAndIncrement() == 0 ? ExecutionState.RETRY : ExecutionState.SUCCESS;
            }
        };
        var nodeAccountIds = Arrays.asList(new AccountId(3), new AccountId(4), new AccountId(5));
        tx.setNodeAccountIds(nodeAccountIds);

        var receipt = org.hiero.sdk.proto.TransactionReceipt.newBuilder()
                .setStatus(ResponseCodeEnum.OK)
                .build();
        var receiptResp = org.hiero.sdk.proto.TransactionGetReceiptResponse.newBuilder()
                .setReceipt(receipt)
                .build();

        var resp = Response.newBuilder().setTransactionGetReceipt(receiptResp).build();
        tx.blockingUnaryCall = (grpcRequest) -> resp;
        tx.execute(client);

        verify(node3).channelFailedToConnect(any(Instant.class));
        verify(node4).channelFailedToConnect(any(Instant.class));
    }

    @Test
    void executeUserError() throws PrecheckStatusException, TimeoutException {
        when(node3.isHealthy()).thenReturn(true);
        when(node3.channelFailedToConnect()).thenReturn(false);

        var tx = new DummyTransaction() {
            @Override
            Status mapResponseStatus(org.hiero.sdk.proto.TransactionResponse response) {
                return Status.ACCOUNT_DELETED;
            }
        };
        var nodeAccountIds = Arrays.asList(new AccountId(3), new AccountId(4), new AccountId(5));
        tx.setNodeAccountIds(nodeAccountIds);

        var txResp = org.hiero.sdk.proto.TransactionResponse.newBuilder()
                .setNodeTransactionPrecheckCode(ResponseCodeEnum.ACCOUNT_DELETED)
                .build();

        tx.blockingUnaryCall = (grpcRequest) -> txResp;
        assertThatExceptionOfType(PrecheckStatusException.class).isThrownBy(() -> tx.execute(client));

        verify(node3).channelFailedToConnect(any(Instant.class));
    }

    @Test
    void shouldRetryReturnsCorrectStates() {
        var tx = new DummyTransaction();

        assertThat(tx.getExecutionState(Status.PLATFORM_TRANSACTION_NOT_CREATED, null))
                .isEqualTo(ExecutionState.SERVER_ERROR);
        assertThat(tx.getExecutionState(Status.PLATFORM_NOT_ACTIVE, null)).isEqualTo(ExecutionState.SERVER_ERROR);
        assertThat(tx.getExecutionState(Status.BUSY, null)).isEqualTo(ExecutionState.RETRY);
        assertThat(tx.getExecutionState(Status.OK, null)).isEqualTo(ExecutionState.SUCCESS);
        assertThat(tx.getExecutionState(Status.ACCOUNT_DELETED, null)).isEqualTo(ExecutionState.REQUEST_ERROR);
    }

    @Test
    void shouldSetMaxRetry() {
        var tx = new DummyTransaction();

        tx.setMaxRetry(1);

        assertThat(tx.getMaxRetry()).isEqualTo(1);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> tx.setMaxRetry(0));
    }

    static class DummyTransaction<T extends Transaction<T>>
            extends Executable<
                    T, org.hiero.sdk.proto.Transaction, org.hiero.sdk.proto.TransactionResponse, TransactionResponse> {

        @Override
        void onExecute(Client client) {}

        @Nullable
        @Override
        CompletableFuture<Void> onExecuteAsync(Client client) {
            return null;
        }

        @Nullable
        @Override
        org.hiero.sdk.proto.Transaction makeRequest() {
            return null;
        }

        @Nullable
        @Override
        TransactionResponse mapResponse(
                org.hiero.sdk.proto.TransactionResponse response,
                AccountId nodeId,
                org.hiero.sdk.proto.Transaction request) {
            return null;
        }

        @Override
        Status mapResponseStatus(org.hiero.sdk.proto.TransactionResponse response) {
            return Status.OK;
        }

        @Nullable
        @Override
        MethodDescriptor<org.hiero.sdk.proto.Transaction, org.hiero.sdk.proto.TransactionResponse>
                getMethodDescriptor() {
            return null;
        }

        @Nullable
        @Override
        TransactionId getTransactionIdInternal() {
            return null;
        }
    }

    static class DummyQuery extends Query<TransactionReceipt, TransactionReceiptQuery> {
        @Override
        void onExecute(Client client) {}

        @Override
        TransactionReceipt mapResponse(Response response, AccountId nodeId, org.hiero.sdk.proto.Query request) {
            return null;
        }

        @Override
        Status mapResponseStatus(org.hiero.sdk.proto.Response response) {
            return Status.OK;
        }

        @Override
        MethodDescriptor<org.hiero.sdk.proto.Query, Response> getMethodDescriptor() {
            return null;
        }

        @Override
        void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {}

        @Override
        ResponseHeader mapResponseHeader(Response response) {
            return null;
        }

        @Override
        QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
            return null;
        }

        @Override
        void validateChecksums(Client client) {}
    }
}
