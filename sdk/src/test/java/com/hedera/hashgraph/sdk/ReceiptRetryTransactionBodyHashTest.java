// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.CryptoTransferTransactionBody;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ReceiptRetryTransactionBodyHashTest {
    private static final AccountId PAYER = new AccountId(1001);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void reviewAfterEligibilityAndBeforeReceiptResubmissionPreservesReviewedState() throws Exception {
        var transaction = transaction();
        var bytes = transaction.toBytes();
        var id = transaction.getTransactionId();
        var response = response(transaction);
        var executor = Executors.newSingleThreadExecutor();
        try (var client = Client.forTestnet().setOperator(PAYER, PrivateKey.generateED25519())) {
            var receipt = executor.submit(() -> getReceiptAfterOneThrottle(response, client));
            await(transaction.eligibilityChecked);

            // The real receipt loop has read the review flag; pause before it returns eligibility and backs off.
            var hash = transaction.getTransactionBodyHash();
            transaction.continueEligibility.countDown();

            assertThatThrownBy(() -> receipt.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
            assertThat(transaction.executions).isZero();
            assertThat(transaction.isFrozen()).isTrue();
            assertThat(transaction.getTransactionId()).isEqualTo(id);
            assertThat(response.getTransactionId()).isEqualTo(id);
            assertThat(transaction.toBytes()).isEqualTo(bytes);
            assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
            assertThatThrownBy(() -> transaction.setTransactionMemo("must remain frozen"))
                    .isInstanceOf(IllegalStateException.class);
            transaction.sign(PrivateKey.generateED25519()).toBytes();
            assertThat(transaction.getTransactionBodyHash()).isEqualTo(hash);
        } finally {
            transaction.continueEligibility.countDown();
            transaction.finishRetry.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void reviewWaitsForAnAlreadyStartedReceiptResubmissionToFinishRebuilding() throws Exception {
        var transaction = transaction();
        var originalId = transaction.getTransactionId();
        var response = response(transaction);
        transaction.continueEligibility.countDown();
        var executor = Executors.newFixedThreadPool(2);
        var hashStarted = new CountDownLatch(1);
        var hashFinished = new CountDownLatch(1);
        try (var client = Client.forTestnet().setOperator(PAYER, PrivateKey.generateED25519())) {
            var receipt = executor.submit(() -> getReceiptAfterOneThrottle(response, client));
            await(transaction.retryEntered);
            assertThat(transaction.retryHeldMonitor).isTrue();
            var hash = executor.submit(() -> {
                hashStarted.countDown();
                try {
                    return transaction.getTransactionBodyHash();
                } finally {
                    hashFinished.countDown();
                }
            });
            await(hashStarted);
            assertThat(hashFinished.await(100, TimeUnit.MILLISECONDS)).isFalse();
            transaction.finishRetry.countDown();

            assertThat(receipt.get(5, TimeUnit.SECONDS).status).isEqualTo(Status.SUCCESS);
            var reviewed = hash.get(5, TimeUnit.SECONDS);
            assertThat(transaction.executions).isEqualTo(1);
            assertThat(transaction.isFrozen()).isTrue();
            assertThat(transaction.getTransactionId()).isNotEqualTo(originalId).isEqualTo(response.getTransactionId());
            assertThat(transaction.getTransactionBodyHash()).isEqualTo(reviewed);
            assertThatThrownBy(() -> transaction.setTransactionMemo("must remain frozen"))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            transaction.continueEligibility.countDown();
            transaction.finishRetry.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void receiptResubmissionRetainsExistingBehaviorWhenNoBodyReviewOccurred() throws Exception {
        var transaction = transaction();
        var originalId = transaction.getTransactionId();
        var response = response(transaction);
        transaction.continueEligibility.countDown();
        transaction.finishRetry.countDown();
        try (var client = Client.forTestnet().setOperator(PAYER, PrivateKey.generateED25519())) {
            assertThat(getReceiptAfterOneThrottle(response, client).status).isEqualTo(Status.SUCCESS);
        }

        assertThat(transaction.executions).isEqualTo(1);
        assertThat(transaction.isFrozen()).isTrue();
        assertThat(transaction.isBodyHashLocked()).isFalse();
        assertThat(transaction.getTransactionId()).isNotEqualTo(originalId).isEqualTo(response.getTransactionId());
    }

    private static RetryTransaction transaction() {
        return new RetryTransaction()
                .setTransactionId(TransactionId.withValidStart(PAYER, Instant.ofEpochSecond(1_700_000_000L)))
                .setNodeAccountIds(List.of(new AccountId(3)))
                .setRegenerateTransactionId(true)
                .freeze();
    }

    private static TransactionResponse response(RetryTransaction transaction) {
        return new TransactionResponse(
                new AccountId(3), transaction.getTransactionId(), transaction.getTransactionHash(), null, transaction);
    }

    private static TransactionReceipt getReceiptAfterOneThrottle(TransactionResponse response, Client client)
            throws Exception {
        var queries = new AtomicInteger();
        // Mock only the network query; eligibility, real backoff and resubmission run through getReceipt().
        try (var ignored = mockConstruction(TransactionReceiptQuery.class, (query, context) -> {
            when(query.setTransactionId(any(TransactionId.class))).thenReturn(query);
            when(query.setNodeAccountIds(anyList())).thenReturn(query);
            when(query.execute(client, TIMEOUT))
                    .thenAnswer(invocation -> TransactionReceipt.fromProtobuf(
                            com.hedera.hashgraph.sdk.proto.TransactionReceipt.newBuilder()
                                    .setStatus(
                                            queries.getAndIncrement() == 0
                                                    ? ResponseCodeEnum.THROTTLED_AT_CONSENSUS
                                                    : ResponseCodeEnum.SUCCESS)
                                    .build(),
                            List.of(),
                            List.of(),
                            response.getTransactionId()));
        })) {
            return response.getReceipt(client, TIMEOUT);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS))
                    .as("retry barrier reached")
                    .isTrue();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    private static final class RetryTransaction extends Transaction<RetryTransaction> {
        final CountDownLatch eligibilityChecked = new CountDownLatch(1);
        final CountDownLatch continueEligibility = new CountDownLatch(1);
        final CountDownLatch retryEntered = new CountDownLatch(1);
        final CountDownLatch finishRetry = new CountDownLatch(1);
        volatile boolean retryHeldMonitor;
        int executions;

        @Override
        boolean canRegenerateSignatures() {
            var result = super.canRegenerateSignatures();
            eligibilityChecked.countDown();
            await(continueEligibility);
            return result;
        }

        @Override
        public TransactionResponse execute(Client client, Duration timeout) {
            retryHeldMonitor = Thread.holdsLock(this);
            executions++;
            retryEntered.countDown();
            await(finishRetry);
            freezeWith(client);
            return response(this);
        }

        @Override
        void onFreeze(TransactionBody.Builder body) {
            body.setCryptoTransfer(CryptoTransferTransactionBody.getDefaultInstance());
        }

        @Override
        void onScheduled(SchedulableTransactionBody.Builder body) {
            body.setCryptoTransfer(CryptoTransferTransactionBody.getDefaultInstance());
        }

        @Override
        void validateChecksums(Client client) {}

        @Override
        MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, com.hedera.hashgraph.sdk.proto.TransactionResponse>
                getMethodDescriptor() {
            return CryptoServiceGrpc.getCryptoTransferMethod();
        }
    }
}
