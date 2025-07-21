// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.proto.AccountAmount;
import com.hedera.hashgraph.sdk.proto.AccountID;
import com.hedera.hashgraph.sdk.proto.CryptoTransferTransactionBody;
import com.hedera.hashgraph.sdk.proto.Duration;
import com.hedera.hashgraph.sdk.proto.SignatureMap;
import com.hedera.hashgraph.sdk.proto.SignaturePair;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.Timestamp;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionID;
import com.hedera.hashgraph.sdk.proto.TransactionList;
import com.hedera.hashgraph.sdk.proto.TransferList;
import java.util.Arrays;
import java.util.Objects;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TransactionIntegrationTest {

    @Test
    @DisplayName("transaction hash in transaction record is equal to the derived transaction hash")
    void transactionHashInTransactionRecordIsEqualToTheDerivedTransactionHash() throws Exception {

        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();

            var transaction = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            var expectedHash = transaction.getTransactionHashPerNode();

            var response = transaction.execute(testEnv.client);

            var record = response.getRecord(testEnv.client);

            assertThat(expectedHash.get(response.nodeId)).containsExactly(record.transactionHash.toByteArray());

            var accountId = record.receipt.accountId;
            assertThat(accountId).isNotNull();

            var transactionId = transaction.getTransactionId();
            assertThat(transactionId.getReceipt(testEnv.client)).isNotNull();
            assertThat(transactionId.getReceiptAsync(testEnv.client).get()).isNotNull();
            assertThat(transactionId.getRecord(testEnv.client)).isNotNull();
            assertThat(transactionId.getRecordAsync(testEnv.client).get()).isNotNull();
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName("incomplete transaction can be serialized into bytes, deserialized and be equal to the original one")
    void canSerializeDeserializeCompareFields() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var publicKey = adminKey.getPublicKey();

            var accountCreateTransaction =
                    new AccountCreateTransaction().setKeyWithoutAlias(publicKey).setInitialBalance(new Hbar(1L));

            var expectedNodeAccountIds = accountCreateTransaction.getNodeAccountIds();
            var expectedBalance = new Hbar(1L);

            var transactionBytesSerialized = accountCreateTransaction.toBytes();
            AccountCreateTransaction accountCreateTransactionDeserialized =
                    (AccountCreateTransaction) Transaction.fromBytes(transactionBytesSerialized);

            assertThat(expectedNodeAccountIds).isEqualTo(accountCreateTransactionDeserialized.getNodeAccountIds());
            assertThat(expectedBalance).isEqualTo(accountCreateTransactionDeserialized.getInitialBalance());
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(accountCreateTransactionDeserialized::getTransactionId);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName(
            "incomplete transaction with node account ids can be serialized into bytes, deserialized and be equal to the original one")
    void canSerializeWithNodeAccountIdsDeserializeCompareFields() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var publicKey = adminKey.getPublicKey();

            var nodeAccountIds = testEnv.client.getNetwork().values().stream().toList();

            var accountCreateTransaction = new AccountCreateTransaction()
                    .setNodeAccountIds(nodeAccountIds)
                    .setKeyWithoutAlias(publicKey)
                    .setInitialBalance(new Hbar(1L));

            var expectedNodeAccountIds = accountCreateTransaction.getNodeAccountIds();
            var expectedBalance = new Hbar(1L);

            var transactionBytesSerialized = accountCreateTransaction.toBytes();
            AccountCreateTransaction accountCreateTransactionDeserialized =
                    (AccountCreateTransaction) Transaction.fromBytes(transactionBytesSerialized);

            assertThat(expectedNodeAccountIds.size())
                    .isEqualTo(accountCreateTransactionDeserialized
                            .getNodeAccountIds()
                            .size());
            assertThat(expectedBalance).isEqualTo(accountCreateTransactionDeserialized.getInitialBalance());
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(accountCreateTransactionDeserialized::getTransactionId);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName("incomplete transaction can be serialized into bytes, deserialized and executed")
    void canSerializeDeserializeAndExecuteIncompleteTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var publicKey = adminKey.getPublicKey();

            var accountCreateTransaction =
                    new AccountCreateTransaction().setKeyWithoutAlias(publicKey).setInitialBalance(new Hbar(1L));

            var transactionBytesSerialized = accountCreateTransaction.toBytes();
            AccountCreateTransaction accountCreateTransactionDeserialized =
                    (AccountCreateTransaction) Transaction.fromBytes(transactionBytesSerialized);

            var txReceipt =
                    accountCreateTransactionDeserialized.execute(testEnv.client).getReceipt(testEnv.client);

            new AccountDeleteTransaction()
                    .setAccountId(txReceipt.accountId)
                    .setTransferAccountId(testEnv.client.getOperatorAccountId())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName("incomplete transaction with node account ids can be serialized into bytes, deserialized and executed")
    void canSerializeDeserializeAndExecuteIncompleteTransactionWithNodeAccountIds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var publicKey = adminKey.getPublicKey();

            var nodeAccountIds = testEnv.client.getNetwork().values().stream().toList();

            var accountCreateTransaction = new AccountCreateTransaction()
                    .setNodeAccountIds(nodeAccountIds)
                    .setKeyWithoutAlias(publicKey)
                    .setInitialBalance(new Hbar(1L));

            var transactionBytesSerialized = accountCreateTransaction.toBytes();
            AccountCreateTransaction accountCreateTransactionDeserialized =
                    (AccountCreateTransaction) Transaction.fromBytes(transactionBytesSerialized);

            var txReceipt =
                    accountCreateTransactionDeserialized.execute(testEnv.client).getReceipt(testEnv.client);

            new AccountDeleteTransaction()
                    .setAccountId(txReceipt.accountId)
                    .setTransferAccountId(testEnv.client.getOperatorAccountId())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName("incomplete transaction can be serialized into bytes, deserialized, edited and executed")
    void canSerializeDeserializeEditExecuteCompareFields() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var publicKey = adminKey.getPublicKey();

            var accountCreateTransaction = new AccountCreateTransaction().setKeyWithoutAlias(publicKey);

            var expectedBalance = new Hbar(1L);
            var nodeAccountIds = testEnv.client.getNetwork().values().stream().toList();

            var transactionBytesSerialized = accountCreateTransaction.toBytes();
            AccountCreateTransaction accountCreateTransactionDeserialized =
                    (AccountCreateTransaction) Transaction.fromBytes(transactionBytesSerialized);

            var txReceipt = accountCreateTransactionDeserialized
                    .setInitialBalance(new Hbar(1L))
                    .setNodeAccountIds(nodeAccountIds)
                    .setTransactionId(TransactionId.generate(testEnv.client.getOperatorAccountId()))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThat(expectedBalance).isEqualTo(accountCreateTransactionDeserialized.getInitialBalance());

            new AccountDeleteTransaction()
                    .setAccountId(txReceipt.accountId)
                    .setTransferAccountId(testEnv.client.getOperatorAccountId())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName(
            "incomplete transaction with node account ids can be serialized into bytes, deserialized, edited and executed")
    void canSerializeDeserializeEditExecuteCompareFieldsIncompleteTransactionWithNodeAccountIds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var publicKey = adminKey.getPublicKey();

            var nodeAccountIds = testEnv.client.getNetwork().values().stream().toList();

            var accountCreateTransaction = new AccountCreateTransaction()
                    .setNodeAccountIds(nodeAccountIds)
                    .setKeyWithoutAlias(publicKey);

            var expectedBalance = new Hbar(1L);

            var transactionBytesSerialized = accountCreateTransaction.toBytes();
            AccountCreateTransaction accountCreateTransactionDeserialized =
                    (AccountCreateTransaction) Transaction.fromBytes(transactionBytesSerialized);

            var txReceipt = accountCreateTransactionDeserialized
                    .setInitialBalance(new Hbar(1L))
                    .setTransactionId(TransactionId.generate(testEnv.client.getOperatorAccountId()))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThat(expectedBalance).isEqualTo(accountCreateTransactionDeserialized.getInitialBalance());

            new AccountDeleteTransaction()
                    .setAccountId(txReceipt.accountId)
                    .setTransferAccountId(testEnv.client.getOperatorAccountId())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName("complete frozen and signed transaction can be serialized into bytes, deserialized (x2) and executed")
    void canFreezeSignSerializeDeserializeReserializeAndExecute() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var adminKey = PrivateKey.generateECDSA();
            var publicKey = adminKey.getPublicKey();

            var evmAddress = publicKey.toEvmAddress();
            var initialBalance = new Hbar(1L);
            var autoRenewPeriod = java.time.Duration.ofSeconds(2592000);
            var memo = "test account memo";
            var maxAutomaticTokenAssociations = 4;

            var accountCreateTransaction = new AccountCreateTransaction()
                    .setKeyWithoutAlias(publicKey)
                    .setInitialBalance(initialBalance)
                    .setReceiverSignatureRequired(true)
                    .setAutoRenewPeriod(autoRenewPeriod)
                    .setAccountMemo(memo)
                    .setMaxAutomaticTokenAssociations(maxAutomaticTokenAssociations)
                    .setDeclineStakingReward(true)
                    .setAlias(evmAddress)
                    .freezeWith(testEnv.client)
                    .sign(adminKey);

            var transactionBytesSerialized = accountCreateTransaction.toBytes();
            AccountCreateTransaction accountCreateTransactionDeserialized =
                    (AccountCreateTransaction) Transaction.fromBytes(transactionBytesSerialized);

            var transactionBytesReserialized = accountCreateTransactionDeserialized.toBytes();
            assertThat(transactionBytesSerialized).isEqualTo(transactionBytesReserialized);

            AccountCreateTransaction accountCreateTransactionReserialized =
                    (AccountCreateTransaction) Transaction.fromBytes(transactionBytesReserialized);

            var txResponse = accountCreateTransactionReserialized.execute(testEnv.client);

            var accountId = txResponse.getReceipt(testEnv.client).accountId;

            new AccountDeleteTransaction()
                    .setAccountId(accountId)
                    .setTransferAccountId(testEnv.client.getOperatorAccountId())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);
        }
    }

    @Test
    @DisplayName("complete frozen transaction can be serialized into bytes, deserialized, signature added and executed")
    void canFreezeSerializeDeserializeAddSignatureAndExecute() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key = PrivateKey.generateED25519();

            var transaction = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            var expectedHash = transaction.getTransactionHashPerNode();

            var response = transaction.execute(testEnv.client);

            var record = response.getRecord(testEnv.client);

            assertThat(expectedHash.get(response.nodeId)).containsExactly(record.transactionHash.toByteArray());

            var accountId = record.receipt.accountId;
            assertThat(accountId).isNotNull();

            var deleteTransaction = new AccountDeleteTransaction()
                    .setAccountId(accountId)
                    .setTransferAccountId(testEnv.operatorId)
                    .freezeWith(testEnv.client);

            var updateBytes = deleteTransaction.toBytes();

            var sig1 = key.signTransaction(deleteTransaction);

            var deleteTransaction2 = Transaction.fromBytes(updateBytes);

            deleteTransaction2.addSignature(key.getPublicKey(), sig1).execute(testEnv.client);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName(
            "file append chunked transaction can be frozen, signed, serialized into bytes, deserialized and be equal to the original one")
    void canFreezeSignSerializeDeserializeAndCompareFileAppendChunkedTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var privateKey = PrivateKey.generateED25519();

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            Thread.sleep(5000);

            var info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(28);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys.getThreshold()).isNull();
            assertThat(info.keys).isEqualTo(KeyList.of(testEnv.operatorKey));

            var fileAppendTransaction = new FileAppendTransaction()
                    .setFileId(fileId)
                    .setContents(Contents.BIG_CONTENTS)
                    .freezeWith(testEnv.client)
                    .sign(privateKey);

            var transactionBytesSerialized = fileAppendTransaction.toBytes();
            FileAppendTransaction fileAppendTransactionDeserialized =
                    (FileAppendTransaction) Transaction.fromBytes(transactionBytesSerialized);

            var transactionBytesReserialized = fileAppendTransactionDeserialized.toBytes();
            assertThat(transactionBytesSerialized).isEqualTo(transactionBytesReserialized);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName(
            "incomplete file append chunked transaction can be serialized into bytes, deserialized, edited and executed")
    void canSerializeDeserializeExecuteFileAppendChunkedTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            Thread.sleep(5000);

            var info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(28);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys.getThreshold()).isNull();
            assertThat(info.keys).isEqualTo(KeyList.of(testEnv.operatorKey));

            var fileAppendTransaction =
                    new FileAppendTransaction().setFileId(fileId).setContents(Contents.BIG_CONTENTS);

            var transactionBytesSerialized = fileAppendTransaction.toBytes();
            FileAppendTransaction fileAppendTransactionDeserialized =
                    (FileAppendTransaction) Transaction.fromBytes(transactionBytesSerialized);

            fileAppendTransactionDeserialized.execute(testEnv.client).getReceipt(testEnv.client);

            var contents = new FileContentsQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(contents.toStringUtf8()).isEqualTo("[e2e::FileCreateTransaction]" + Contents.BIG_CONTENTS);

            info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(13522);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys.getThreshold()).isNull();
            assertThat(info.keys).isEqualTo(KeyList.of(testEnv.operatorKey));

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName(
            "incomplete file append chunked transaction with node account ids can be serialized into bytes, deserialized, edited and executed")
    void canSerializeDeserializeExecuteIncompleteFileAppendChunkedTransactionWithNodeAccountIds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var nodeAccountIds = testEnv.client.getNetwork().values().stream().toList();

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents("[e2e::FileCreateTransaction]")
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            Thread.sleep(5000);

            var info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(28);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys.getThreshold()).isNull();
            assertThat(info.keys).isEqualTo(KeyList.of(testEnv.operatorKey));

            var fileAppendTransaction = new FileAppendTransaction()
                    .setNodeAccountIds(nodeAccountIds)
                    .setFileId(fileId)
                    .setContents(Contents.BIG_CONTENTS);

            var transactionBytesSerialized = fileAppendTransaction.toBytes();
            FileAppendTransaction fileAppendTransactionDeserialized =
                    (FileAppendTransaction) Transaction.fromBytes(transactionBytesSerialized);

            fileAppendTransactionDeserialized
                    .setTransactionId(TransactionId.generate(testEnv.client.getOperatorAccountId()))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var contents = new FileContentsQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(contents.toStringUtf8()).isEqualTo("[e2e::FileCreateTransaction]" + Contents.BIG_CONTENTS);

            info = new FileInfoQuery().setFileId(fileId).execute(testEnv.client);

            assertThat(info.fileId).isEqualTo(fileId);
            assertThat(info.size).isEqualTo(13522);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.keys).isNotNull();
            assertThat(info.keys.getThreshold()).isNull();
            assertThat(info.keys).isEqualTo(KeyList.of(testEnv.operatorKey));

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName(
            "topic message submit chunked transaction can be frozen, signed, serialized into bytes, deserialized and be equal to the original one")
    void canFreezeSignSerializeDeserializeAndCompareTopicMessageSubmitChunkedTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var privateKey = PrivateKey.generateED25519();

            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setTopicMemo("[e2e::TopicCreateTransaction]")
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            Thread.sleep(5000);

            var info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.topicId).isEqualTo(topicId);
            assertThat(info.topicMemo).isEqualTo("[e2e::TopicCreateTransaction]");
            assertThat(info.sequenceNumber).isEqualTo(0);
            assertThat(info.adminKey).isEqualTo(testEnv.operatorKey);

            var topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMaxChunks(15)
                    .setMessage(Contents.BIG_CONTENTS)
                    .freezeWith(testEnv.client)
                    .sign(privateKey);

            var transactionBytesSerialized = topicMessageSubmitTransaction.toBytes();
            TopicMessageSubmitTransaction fileAppendTransactionDeserialized =
                    (TopicMessageSubmitTransaction) Transaction.fromBytes(transactionBytesSerialized);

            var transactionBytesReserialized = fileAppendTransactionDeserialized.toBytes();
            assertThat(transactionBytesSerialized).isEqualTo(transactionBytesReserialized);

            new TopicDeleteTransaction()
                    .setTopicId(topicId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName(
            "incomplete topic message submit chunked transaction can be serialized into bytes, deserialized, edited and executed")
    void canSerializeDeserializeExecuteIncompleteTopicMessageSubmitChunkedTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setTopicMemo("[e2e::TopicCreateTransaction]")
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            Thread.sleep(5000);

            var info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.topicId).isEqualTo(topicId);
            assertThat(info.topicMemo).isEqualTo("[e2e::TopicCreateTransaction]");
            assertThat(info.sequenceNumber).isEqualTo(0);
            assertThat(info.adminKey).isEqualTo(testEnv.operatorKey);

            var topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMaxChunks(15)
                    .setMessage(Contents.BIG_CONTENTS);

            var transactionBytesSerialized = topicMessageSubmitTransaction.toBytes();
            TopicMessageSubmitTransaction topicMessageSubmitTransactionDeserialized =
                    (TopicMessageSubmitTransaction) Transaction.fromBytes(transactionBytesSerialized);

            var responses = topicMessageSubmitTransactionDeserialized.executeAll(testEnv.client);

            for (var resp : responses) {
                resp.getReceipt(testEnv.client);
            }

            info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.topicId).isEqualTo(topicId);
            assertThat(info.topicMemo).isEqualTo("[e2e::TopicCreateTransaction]");
            assertThat(info.sequenceNumber).isEqualTo(14);
            assertThat(info.adminKey).isEqualTo(testEnv.operatorKey);

            new TopicDeleteTransaction()
                    .setTopicId(topicId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    /**
     * @notice E2E-HIP-745
     * @url https://hips.hedera.com/hip/hip-745
     */
    @Test
    @DisplayName(
            "incomplete topic message submit chunked transaction with node account ids can be serialized into bytes, deserialized, edited and executed")
    void canSerializeDeserializeExecuteIncompleteTopicMessageSubmitChunkedTransactionWithNodeAccountIds()
            throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var nodeAccountIds = testEnv.client.getNetwork().values().stream().toList();

            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setTopicMemo("[e2e::TopicCreateTransaction]")
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            Thread.sleep(5000);

            var info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.topicId).isEqualTo(topicId);
            assertThat(info.topicMemo).isEqualTo("[e2e::TopicCreateTransaction]");
            assertThat(info.sequenceNumber).isEqualTo(0);
            assertThat(info.adminKey).isEqualTo(testEnv.operatorKey);

            var topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                    .setNodeAccountIds(nodeAccountIds)
                    .setTopicId(topicId)
                    .setMaxChunks(15)
                    .setMessage(Contents.BIG_CONTENTS);

            var transactionBytesSerialized = topicMessageSubmitTransaction.toBytes();
            TopicMessageSubmitTransaction topicMessageSubmitTransactionDeserialized =
                    (TopicMessageSubmitTransaction) Transaction.fromBytes(transactionBytesSerialized);

            var responses = topicMessageSubmitTransactionDeserialized.executeAll(testEnv.client);

            for (var resp : responses) {
                resp.getReceipt(testEnv.client);
            }

            info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.topicId).isEqualTo(topicId);
            assertThat(info.topicMemo).isEqualTo("[e2e::TopicCreateTransaction]");
            assertThat(info.sequenceNumber).isEqualTo(14);
            assertThat(info.adminKey).isEqualTo(testEnv.operatorKey);

            new TopicDeleteTransaction()
                    .setTopicId(topicId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    // TODO: this test has a bunch of things hard-coded into it, which is kinda
    // dumb, but it's a good idea for a test.
    // Any way to fix it and bring it back?
    @Disabled
    @Test
    @DisplayName("transaction can be serialized into bytes, deserialized, signature added and executed")
    void transactionFromToBytes2() {
        assertThatNoException().isThrownBy(() -> {
            var id = TransactionId.generate(new AccountId(0, 0, 542348));

            var transactionBodyBuilder = TransactionBody.newBuilder();
            transactionBodyBuilder
                    .setTransactionID(TransactionID.newBuilder()
                            .setTransactionValidStart(Timestamp.newBuilder()
                                    .setNanos(id.validStart.getNano())
                                    .setSeconds(id.validStart.getEpochSecond())
                                    .build())
                            .setAccountID(AccountID.newBuilder()
                                    .setAccountNum(542348)
                                    .setRealmNum(0)
                                    .setShardNum(0)
                                    .build())
                            .build())
                    .setNodeAccountID(AccountID.newBuilder()
                            .setAccountNum(3)
                            .setRealmNum(0)
                            .setShardNum(0)
                            .build())
                    .setTransactionFee(200_000_000)
                    .setTransactionValidDuration(
                            Duration.newBuilder().setSeconds(120).build())
                    .setGenerateRecord(false)
                    .setMemo("")
                    .setCryptoTransfer(CryptoTransferTransactionBody.newBuilder()
                            .setTransfers(TransferList.newBuilder()
                                    .addAccountAmounts(AccountAmount.newBuilder()
                                            .setAccountID(AccountID.newBuilder()
                                                    .setAccountNum(47439)
                                                    .setRealmNum(0)
                                                    .setShardNum(0)
                                                    .build())
                                            .setAmount(10)
                                            .build())
                                    .addAccountAmounts(AccountAmount.newBuilder()
                                            .setAccountID(AccountID.newBuilder()
                                                    .setAccountNum(542348)
                                                    .setRealmNum(0)
                                                    .setShardNum(0)
                                                    .build())
                                            .setAmount(-10)
                                            .build())
                                    .build())
                            .build());
            var bodyBytes = transactionBodyBuilder.build().toByteString();

            var key1 = PrivateKey.fromString(
                    "302e020100300506032b6570042204203e7fda6dde63c3cdb3cb5ecf5264324c5faad7c9847b6db093c088838b35a110");
            var key2 = PrivateKey.fromString(
                    "302e020100300506032b65700422042032d3d5a32e9d06776976b39c09a31fbda4a4a0208223da761c26a2ae560c1755");
            var key3 = PrivateKey.fromString(
                    "302e020100300506032b657004220420195a919056d1d698f632c228dbf248bbbc3955adf8a80347032076832b8299f9");
            var key4 = PrivateKey.fromString(
                    "302e020100300506032b657004220420b9962f17f94ffce73a23649718a11638cac4b47095a7a6520e88c7563865be62");
            var key5 = PrivateKey.fromString(
                    "302e020100300506032b657004220420fef68591819080cd9d48b0cbaa10f65f919752abb50ffb3e7411ac66ab22692e");

            var publicKey1 = key1.getPublicKey();
            var publicKey2 = key2.getPublicKey();
            var publicKey3 = key3.getPublicKey();
            var publicKey4 = key4.getPublicKey();
            var publicKey5 = key5.getPublicKey();

            var signature1 = key1.sign(bodyBytes.toByteArray());
            var signature2 = key2.sign(bodyBytes.toByteArray());
            var signature3 = key3.sign(bodyBytes.toByteArray());
            var signature4 = key4.sign(bodyBytes.toByteArray());
            var signature5 = key5.sign(bodyBytes.toByteArray());

            var signedBuilder = SignedTransaction.newBuilder();
            signedBuilder
                    .setBodyBytes(bodyBytes)
                    .setSigMap(SignatureMap.newBuilder()
                            .addSigPair(SignaturePair.newBuilder()
                                    .setEd25519(ByteString.copyFrom(signature1))
                                    .setPubKeyPrefix(ByteString.copyFrom(publicKey1.toBytes()))
                                    .build())
                            .addSigPair(SignaturePair.newBuilder()
                                    .setEd25519(ByteString.copyFrom(signature2))
                                    .setPubKeyPrefix(ByteString.copyFrom(publicKey2.toBytes()))
                                    .build())
                            .addSigPair(SignaturePair.newBuilder()
                                    .setEd25519(ByteString.copyFrom(signature3))
                                    .setPubKeyPrefix(ByteString.copyFrom(publicKey3.toBytes()))
                                    .build())
                            .addSigPair(SignaturePair.newBuilder()
                                    .setEd25519(ByteString.copyFrom(signature4))
                                    .setPubKeyPrefix(ByteString.copyFrom(publicKey4.toBytes()))
                                    .build())
                            .addSigPair(SignaturePair.newBuilder()
                                    .setEd25519(ByteString.copyFrom(signature5))
                                    .setPubKeyPrefix(ByteString.copyFrom(publicKey5.toBytes()))
                                    .build()));
            var byts = signedBuilder.build().toByteString();

            byts = TransactionList.newBuilder()
                    .addTransactionList(com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                            .setSignedTransactionBytes(byts)
                            .build())
                    .build()
                    .toByteString();

            var tx = (TransferTransaction) Transaction.fromBytes(byts.toByteArray());

            try (var testEnv = new IntegrationTestEnv(1)) {

                assertThat(tx.getHbarTransfers()
                                .get(new AccountId(0, 0, 542348))
                                .toTinybars())
                        .isEqualTo(-10);
                assertThat(tx.getHbarTransfers().get(new AccountId(0, 0, 47439)).toTinybars())
                        .isEqualTo(10);

                assertThat(tx.getNodeAccountIds()).isNotNull();
                assertThat(tx.getNodeAccountIds().size()).isEqualTo(1);
                assertThat(tx.getNodeAccountIds().get(0)).isEqualTo(new AccountId(0, 0, 3));

                var signatures = tx.getSignatures();
                assertThat(Arrays.toString(
                                signatures.get(new AccountId(0, 0, 3)).get(publicKey1)))
                        .isEqualTo(Arrays.toString(signature1));
                assertThat(Arrays.toString(
                                signatures.get(new AccountId(0, 0, 3)).get(publicKey2)))
                        .isEqualTo(Arrays.toString(signature2));
                assertThat(Arrays.toString(
                                signatures.get(new AccountId(0, 0, 3)).get(publicKey3)))
                        .isEqualTo(Arrays.toString(signature3));
                assertThat(Arrays.toString(
                                signatures.get(new AccountId(0, 0, 3)).get(publicKey4)))
                        .isEqualTo(Arrays.toString(signature4));
                assertThat(Arrays.toString(
                                signatures.get(new AccountId(0, 0, 3)).get(publicKey5)))
                        .isEqualTo(Arrays.toString(signature5));

                var resp = tx.execute(testEnv.client);

                resp.getReceipt(testEnv.client);
            }
        });
    }

    /**
     * Integration test for AddSignature functionality.
     * Tests the ability to get signable transaction body bytes and add signatures
     * back to the transaction using the add signature method.
     */
    @Test
    @DisplayName("AddSignature - can get signable body bytes, sign externally, and add signatures back")
    void canAddSignatureToTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            // Step 1: Create a new key for the account
            var newKey = PrivateKey.generateED25519();

            // Step 2: Create account with the new key
            var createResponse = new AccountCreateTransaction()
                    .setKeyWithoutAlias(newKey.getPublicKey())
                    .setNodeAccountIds(
                            testEnv.client.getNetwork().values().stream().toList())
                    .execute(testEnv.client);

            var createReceipt = createResponse.getReceipt(testEnv.client);
            var accountId = Objects.requireNonNull(createReceipt.accountId);
            var nodeId = createResponse.nodeId;

            // Step 3: Create account delete transaction and freeze it
            var deleteTransaction = new AccountDeleteTransaction()
                    .setNodeAccountIds(Arrays.asList(nodeId))
                    .setAccountId(accountId)
                    .setTransferAccountId(testEnv.client.getOperatorAccountId())
                    .freezeWith(testEnv.client);

            // Step 4: Get signable body bytes list
            var signableBodyList = deleteTransaction.getSignableNodeBodyBytesList();
            assertThat(signableBodyList).isNotEmpty();

            // Step 5: Sign each signable body externally and add signatures back
            for (var signableBody : signableBodyList) {
                byte[] signature = newKey.sign(signableBody.getBody());

                deleteTransaction = deleteTransaction.addSignature(
                        newKey.getPublicKey(), signature, signableBody.getTransactionID(), signableBody.getNodeID());
            }

            var deleteResponse = deleteTransaction.execute(testEnv.client);
            var deleteReceipt = deleteResponse.getReceipt(testEnv.client);

            assertThat(deleteReceipt.status).isEqualTo(Status.SUCCESS);
        }
    }
}
