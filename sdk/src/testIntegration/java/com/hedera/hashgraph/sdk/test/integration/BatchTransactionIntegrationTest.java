// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.hedera.hashgraph.sdk.*;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BatchTransactionIntegrationTest {

    @Test
    @DisplayName("Can create batch transaction")
    void canCreateBatchTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateECDSA();
            var tx = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            // create new Batch Transaction
            BatchTransaction batchTransaction = new BatchTransaction().addInnerTransaction(tx);

            try {
                batchTransaction.execute(testEnv.client).getReceipt(testEnv.client);
            } catch (ReceiptStatusException receiptStatusException) {
                Thread.sleep(3000);
                if (receiptStatusException.receipt.status == Status.INNER_TRANSACTION_FAILED) {
                    new BatchTransaction()
                            .addInnerTransaction(tx)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client);
                }
            }

            var accountIdInnerTransaction =
                    batchTransaction.getInnerTransactionIds().get(0).accountId;

            var execute = new AccountInfoQuery()
                    .setAccountId(accountIdInnerTransaction)
                    .execute(testEnv.client);
            assertThat(accountIdInnerTransaction).isEqualTo(execute.accountId);
        }
    }

    @Test
    @DisplayName("Can execute a large batch transaction up to maximum request size")
    void canExecuteLargeBatchTransactionUpToMaximumRequestSize() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            BatchTransaction batchTransaction = new BatchTransaction();
            // 50 is the maximum limit for internal transaction inside a BatchTransaction
            for (int i = 0; i < 25; i++) {
                var key = PrivateKey.generateECDSA();
                var tx = new AccountCreateTransaction()
                        .setKeyWithoutAlias(key)
                        .setInitialBalance(new Hbar(1))
                        .batchify(testEnv.client, testEnv.operatorKey);

                batchTransaction.addInnerTransaction(tx);
            }

            try {
                batchTransaction.execute(testEnv.client).getReceipt(testEnv.client);
            } catch (ReceiptStatusException receiptStatusException) {
                if (receiptStatusException.receipt.status == Status.INNER_TRANSACTION_FAILED) {
                    Thread.sleep(3000);
                    new BatchTransaction()
                            .setInnerTransactions(batchTransaction.getInnerTransactions())
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client);
                }
            }
        }
    }

    @Test
    @DisplayName("Batch Transaction with empty inner transaction's list should throw an error")
    void batchTransactionWithoutInnerTransactionsShouldThrowAnError() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            BatchTransaction batchTransaction = new BatchTransaction();

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        batchTransaction.execute(testEnv.client).getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.BATCH_LIST_EMPTY.toString());
        }
    }

    @Test
    @DisplayName("Blacklisted inner transaction should throw an error")
    void batchTransactionWithBlacklistedInnerTransactionShouldThrowAnError() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            BatchTransaction batchTransaction = new BatchTransaction();

            var tx1 = new FreezeTransaction()
                    .setFileId(FileId.fromString("4.5.6"))
                    .setFileHash(Hex.decode("1723904587120938954702349857"))
                    .setStartTime(Instant.now())
                    .setFreezeType(FreezeType.FREEZE_ONLY)
                    .batchify(testEnv.client, testEnv.operatorKey);

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        batchTransaction
                                .addInnerTransaction(tx1)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.BATCH_TRANSACTION_IN_BLACKLIST.toString());
        }
    }

    @Test
    @DisplayName("Invalid batch key set to inner transaction should throw an error")
    void batchTransactionWithInvalidBatchKeyInsideInnerTransactionShouldThrowAnError() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            BatchTransaction batchTransaction = new BatchTransaction();

            var key = PrivateKey.generateECDSA();
            var invalidKey = PrivateKey.generateECDSA();

            var tx1 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, invalidKey.getPublicKey());

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        batchTransaction
                                .addInnerTransaction(tx1)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @DisplayName("Non-batch transaction with batch key should throw an error")
    void nonBatchTransactionWithBatchKeyShouldThrowAnError() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateECDSA();

            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> {
                        new AccountCreateTransaction()
                                .setKeyWithoutAlias(key)
                                // without setting this property it is not possible to test this use case
                                .setNodeAccountIds(Collections.singletonList(AccountId.fromString("0.0.3")))
                                .batchify(testEnv.client, testEnv.operatorKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.BATCH_KEY_SET_ON_NON_INNER_TRANSACTION.toString());
        }
    }

    @Test
    @DisplayName("Chunked inner transactions should be executed successfully")
    void chunkedInnerTransactionsShouldBeExecutedSuccessfully() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setTopicMemo("[e2e::TopicCreateTransaction]")
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            var topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMaxChunks(15)
                    .setMessage(Contents.BIG_CONTENTS)
                    .batchify(testEnv.client, testEnv.operatorKey);
            try {
                new BatchTransaction()
                        .addInnerTransaction(topicMessageSubmitTransaction)
                        .execute(testEnv.client)
                        .getReceipt(testEnv.client);
            } catch (ReceiptStatusException receiptStatusException) {
                Thread.sleep(3000);
                if (receiptStatusException.receipt.status == Status.INNER_TRANSACTION_FAILED) {
                    new BatchTransaction()
                            .addInnerTransaction(topicMessageSubmitTransaction)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client);
                }
            }
        }
    }

    @Test
    @DisplayName("Successful inner transaction should incur fees even though one failed")
    void successfulInnerTransactionsShouldIncurFeesEvenThoughOneFailed() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var initialBalance =
                    new AccountInfoQuery().setAccountId(testEnv.operatorId).execute(testEnv.client).balance;

            var key = PrivateKey.generateECDSA();

            var tx1 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            var tx2 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            var tx3 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setReceiverSignatureRequired(true)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new BatchTransaction()
                            .addInnerTransaction(tx1)
                            .addInnerTransaction(tx2)
                            .addInnerTransaction(tx3)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.INNER_TRANSACTION_FAILED.toString());

            var finalBalance =
                    new AccountInfoQuery().setAccountId(testEnv.operatorId).execute(testEnv.client).balance;

            assertThat(finalBalance.getValue().intValue())
                    .isLessThan(initialBalance.getValue().intValue());
        }
    }
}
