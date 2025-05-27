// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.hedera.hashgraph.sdk.*;
import java.time.Instant;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Disabled
class BatchTransactionIntegrationTest {

    @Test
    @RetryTest(maxAttempts = 5)
    @DisplayName("Can create batch transaction")
    void canCreateBatchTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateECDSA();
            var tx = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            var batchTransaction = new BatchTransaction().addInnerTransaction(tx);
            batchTransaction.execute(testEnv.client).getReceipt(testEnv.client);

            var accountIdInnerTransaction =
                    batchTransaction.getInnerTransactionIds().get(0).accountId;

            var execute = new AccountInfoQuery()
                    .setAccountId(accountIdInnerTransaction)
                    .execute(testEnv.client);
            assertThat(accountIdInnerTransaction).isEqualTo(execute.accountId);
        }
    }

    @Test
    @RetryTest(maxAttempts = 5)
    @DisplayName("Can execute from/toBytes")
    void canExecuteFromToBytes() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateECDSA();
            var tx = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            var batchTransaction = new BatchTransaction().addInnerTransaction(tx);
            var batchTransactionBytes = batchTransaction.toBytes();
            var batchTransactionFromBytes = BatchTransaction.fromBytes(batchTransactionBytes);
            batchTransactionFromBytes.execute(testEnv.client).getReceipt(testEnv.client);

            var accountIdInnerTransaction =
                    batchTransaction.getInnerTransactionIds().get(0).accountId;

            var execute = new AccountInfoQuery()
                    .setAccountId(accountIdInnerTransaction)
                    .execute(testEnv.client);
            assertThat(accountIdInnerTransaction).isEqualTo(execute.accountId);
        }
    }

    @Test
    @RetryTest(maxAttempts = 5)
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

            batchTransaction.execute(testEnv.client).getReceipt(testEnv.client);

            for (var innerTransactionID : batchTransaction.getInnerTransactionIds()) {
                var receipt = new TransactionReceiptQuery()
                        .setTransactionId(innerTransactionID)
                        .execute(testEnv.client);
                assertThat(receipt.status).isEqualTo(Status.SUCCESS);
            }
        }
    }

    @Test
    @DisplayName("Batch Transaction with empty inner transaction's list should throw an error")
    void batchTransactionWithoutInnerTransactionsShouldThrowAnError() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(
                            () -> new BatchTransaction().execute(testEnv.client).getReceipt(testEnv.client))
                    .withMessageContaining(Status.BATCH_LIST_EMPTY.toString());
        }
    }

    @Test
    @DisplayName("Blacklisted inner transaction should throw an error")
    void batchTransactionWithBlacklistedInnerTransactionShouldThrowAnError() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var freezeTransaction = new FreezeTransaction()
                    .setFileId(FileId.fromString("4.5.6"))
                    .setFileHash(Hex.decode("1723904587120938954702349857"))
                    .setStartTime(Instant.now())
                    .setFreezeType(FreezeType.FREEZE_ONLY)
                    .batchify(testEnv.client, testEnv.operatorKey);

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new BatchTransaction().addInnerTransaction(freezeTransaction))
                    .withMessageContaining("Transaction type FreezeTransaction is not allowed in a batch transaction");

            var key = PrivateKey.generateECDSA();
            var tx = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            var batchTransaction =
                    new BatchTransaction().addInnerTransaction(tx).batchify(testEnv.client, testEnv.operatorKey);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new BatchTransaction().addInnerTransaction(batchTransaction))
                    .withMessageContaining("Transaction type BatchTransaction is not allowed in a batch transaction");
        }
    }

    @Test
    @DisplayName("Invalid batch key set to inner transaction should throw an error")
    void batchTransactionWithInvalidBatchKeyInsideInnerTransactionShouldThrowAnError() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            BatchTransaction batchTransaction = new BatchTransaction();

            var key = PrivateKey.generateECDSA();
            var invalidKey = PrivateKey.generateECDSA();

            var tx = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, invalidKey.getPublicKey());

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> batchTransaction
                            .addInnerTransaction(tx)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @RetryTest(maxAttempts = 5)
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
            new BatchTransaction()
                    .addInnerTransaction(topicMessageSubmitTransaction)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.sequenceNumber).isEqualTo(1);
        }
    }

    @Test
    @RetryTest(maxAttempts = 5)
    @DisplayName("Can execute with different batch keys")
    void canExecuteWithDifferentBatchKeys() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var batchKey1 = PrivateKey.generateED25519();
            var batchKey2 = PrivateKey.generateED25519();
            var batchKey3 = PrivateKey.generateED25519();

            var key1 = PrivateKey.generateECDSA();
            var account1 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key1)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;
            assertThat(account1).isNotNull();
            var batchedTransfer1 = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, Hbar.fromTinybars(100))
                    .addHbarTransfer(account1, Hbar.fromTinybars(100).negated())
                    .setTransactionId(TransactionId.generate(account1))
                    .setBatchKey(batchKey1)
                    .freezeWith(testEnv.client)
                    .sign(key1);

            var key2 = PrivateKey.generateECDSA();
            var account2 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key2)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;
            assertThat(account2).isNotNull();
            var batchedTransfer2 = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, Hbar.fromTinybars(100))
                    .addHbarTransfer(account2, Hbar.fromTinybars(100).negated())
                    .setTransactionId(TransactionId.generate(account2))
                    .setBatchKey(batchKey2)
                    .freezeWith(testEnv.client)
                    .sign(key2);

            var key3 = PrivateKey.generateECDSA();
            var account3 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key3)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;
            assertThat(account3).isNotNull();
            var batchedTransfer3 = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, Hbar.fromTinybars(100))
                    .addHbarTransfer(account3, Hbar.fromTinybars(100).negated())
                    .setTransactionId(TransactionId.generate(account3))
                    .setBatchKey(batchKey3)
                    .freezeWith(testEnv.client)
                    .sign(key3);

            var receipt = new BatchTransaction()
                    .addInnerTransaction(batchedTransfer1)
                    .addInnerTransaction(batchedTransfer2)
                    .addInnerTransaction(batchedTransfer3)
                    .freezeWith(testEnv.client)
                    .sign(batchKey1)
                    .sign(batchKey2)
                    .sign(batchKey3)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @RetryTest(maxAttempts = 5)
    @DisplayName("Successful inner transaction should incur fees even though one failed")
    void successfulInnerTransactionsShouldIncurFeesEvenThoughOneFailed() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var initialBalance =
                    new AccountInfoQuery().setAccountId(testEnv.operatorId).execute(testEnv.client).balance;

            var key1 = PrivateKey.generateECDSA();
            var tx1 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key1)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            var key2 = PrivateKey.generateECDSA();
            var tx2 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key2)
                    .setInitialBalance(new Hbar(1))
                    .batchify(testEnv.client, testEnv.operatorKey);

            var key3 = PrivateKey.generateECDSA();
            var tx3 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key3)
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

    @Test
    @DisplayName("Transaction should fail when batchified but not part of a batch")
    void transactionShouldFailWhenBatchified() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new TopicCreateTransaction()
                            .setAdminKey(testEnv.operatorKey)
                            .setTopicMemo("[e2e::TopicCreateTransaction]")
                            .batchify(testEnv.client, key)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining("Cannot execute batchified transaction outside of BatchTransaction");
        }
    }
}
