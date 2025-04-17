// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BatchTransactionTest {

    private static final PrivateKey privateKeyED25519 = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final PrivateKey privateKeyECDSA =
            PrivateKey.fromStringECDSA("7f109a9e3b0d8ecfba9cc23a3614433ce0fa7ddcc80f2a8f10b222179a5a80d6");

    static final Instant validStart = Instant.ofEpochSecond(1554158542);

    private static final List<Transaction> INNER_TRANSACTIONS = List.of(
            spawnTestTransactionAccountCreate(),
            spawnTestTransactionAccountCreate(),
            spawnTestTransactionAccountCreate());

    private static AccountCreateTransaction spawnTestTransactionAccountCreate() {
        return new AccountCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setKeyWithAlias(privateKeyECDSA)
                .setKeyWithAlias(privateKeyED25519, privateKeyECDSA)
                .setKeyWithoutAlias(privateKeyED25519)
                .setInitialBalance(Hbar.fromTinybars(450))
                .setAccountMemo("some memo")
                .setReceiverSignatureRequired(true)
                .setAutoRenewPeriod(Duration.ofHours(10))
                .setStakedAccountId(AccountId.fromString("0.0.3"))
                .setAlias("0x5c562e90feaf0eebd33ea75d21024f249d451417")
                .setMaxAutomaticTokenAssociations(100)
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setBatchKey(privateKeyECDSA)
                .freeze()
                .sign(privateKeyED25519);
    }

    private BatchTransaction spawnTestTransaction() {
        var batchKey = PrivateKey.generateECDSA();

        return new BatchTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setInnerTransactions(INNER_TRANSACTIONS)
                .freeze()
                .sign(batchKey);
    }

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = BatchTransaction.fromBytes(tx.toBytes());
        assertThat(tx2).hasToString(tx.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new BatchTransaction();
        var tx2 = BatchTransaction.fromBytes(tx.toBytes());
        assertThat(tx2).hasToString(tx.toString());
    }

    @Test
    void getInnerTransactionsShouldReturnCorrectTransactions() {
        var batchTransaction = spawnTestTransaction();
        assertThat(batchTransaction.getInnerTransactions())
                .isNotNull()
                .hasSize(3)
                .isEqualTo(INNER_TRANSACTIONS);
    }

    @Test
    void setInnerTransactionsShouldUpdateTransactions() {
        var batchTransaction = new BatchTransaction();
        List<Transaction> newInnerTransactions =
                List.of(spawnTestTransactionAccountCreate(), spawnTestTransactionAccountCreate());

        batchTransaction.setInnerTransactions(newInnerTransactions);

        assertThat(batchTransaction.getInnerTransactions())
                .isNotNull()
                .hasSize(2)
                .isEqualTo(newInnerTransactions);
    }

    @Test
    void addInnerTransactionShouldAppendTransaction() {
        var batchTransaction = new BatchTransaction();
        var newTransaction = spawnTestTransactionAccountCreate();

        batchTransaction.addInnerTransaction(newTransaction);

        assertThat(batchTransaction.getInnerTransactions())
                .isNotNull()
                .hasSize(1)
                .contains(newTransaction);
    }

    @Test
    void getInnerTransactionIdsShouldReturnCorrectIds() {
        var batchTransaction = spawnTestTransaction();
        var expectedTransactionId = TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart);

        var transactionIds = batchTransaction.getInnerTransactionIds();

        assertThat(transactionIds).isNotNull().hasSize(3).allSatisfy(id -> assertThat(id)
                .isEqualTo(expectedTransactionId));
    }

    @Test
    void shouldAllowChainedSetters() {
        var batchTransaction = new BatchTransaction()
                .setNodeAccountIds(Collections.singletonList(AccountId.fromString("0.0.5005")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .addInnerTransaction(spawnTestTransactionAccountCreate())
                .freeze();

        assertThat(batchTransaction.getInnerTransactions()).hasSize(1);
        assertThat(batchTransaction.getNodeAccountIds()).hasSize(1);
        assertThat(batchTransaction.getTransactionId()).isNotNull();
    }

    @Test
    void shouldRejectFreezeTransaction() {
        var batchTransaction = new BatchTransaction();
        var freezeTransaction = new FreezeTransaction()
                .setStartTime(Instant.now())
                .setFreezeType(FreezeType.FREEZE_ONLY)
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .freeze();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(freezeTransaction))
                .withMessageContaining("FreezeTransaction is not allowed in a batch transaction");
    }

    @Test
    void shouldRejectBatchTransaction() {
        var batchTransaction = new BatchTransaction();
        var innerBatchTransaction = new BatchTransaction()
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .freeze();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(innerBatchTransaction))
                .withMessageContaining("BatchTransaction is not allowed in a batch transaction");
    }

    @Test
    void shouldRejectBlacklistedTransactionInList() {
        var batchTransaction = new BatchTransaction();
        var validTransaction = spawnTestTransactionAccountCreate();
        var freezeTransaction = new FreezeTransaction()
                .setStartTime(Instant.now())
                .setFreezeType(FreezeType.FREEZE_ONLY)
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .freeze();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> batchTransaction.setInnerTransactions(List.of(validTransaction, freezeTransaction)))
                .withMessageContaining("FreezeTransaction is not allowed in a batch transaction");
    }

    @Test
    void shouldRejectNullTransaction() {
        var batchTransaction = new BatchTransaction();

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(null));
    }

    @Test
    void shouldRejectNullTransactionList() {
        var batchTransaction = new BatchTransaction();

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> batchTransaction.setInnerTransactions(null));
    }

    @Test
    void shouldRejectUnfrozenTransaction() {
        var batchTransaction = new BatchTransaction();
        var unfrozenTransaction = new AccountCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(unfrozenTransaction))
                .withMessageContaining("Inner transaction should be frozen");
    }

    @Test
    void shouldRejectTransactionAfterFreeze() {
        var batchTransaction = new BatchTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .freeze();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(spawnTestTransactionAccountCreate()))
                .withMessageContaining("transaction is immutable");
    }

    @Test
    void shouldRejectTransactionListAfterFreeze() {
        var batchTransaction = new BatchTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .freeze();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> batchTransaction.setInnerTransactions(INNER_TRANSACTIONS))
                .withMessageContaining("transaction is immutable");
    }

    @Test
    void shouldAllowEmptyTransactionListBeforeExecution() {
        var batchTransaction = new BatchTransaction();
        batchTransaction.setInnerTransactions(Collections.emptyList());

        assertThat(batchTransaction.getInnerTransactions()).isNotNull().isEmpty();
    }

    @Test
    void shouldPreserveTransactionOrder() {
        var batchTransaction = new BatchTransaction();
        var transaction1 = spawnTestTransactionAccountCreate();
        var transaction2 = spawnTestTransactionAccountCreate();
        var transaction3 = spawnTestTransactionAccountCreate();

        List<Transaction> transactions = Arrays.asList(transaction1, transaction2, transaction3);
        batchTransaction.setInnerTransactions(transactions);

        assertThat(batchTransaction.getInnerTransactions()).containsExactly(transaction1, transaction2, transaction3);
    }

    @Test
    void shouldCreateDefensiveCopyOfTransactionList() {
        var batchTransaction = new BatchTransaction();
        var mutableList = new ArrayList<>(INNER_TRANSACTIONS);

        batchTransaction.setInnerTransactions(mutableList);
        mutableList.clear();

        assertThat(batchTransaction.getInnerTransactions())
                .isNotNull()
                .hasSize(3)
                .isEqualTo(INNER_TRANSACTIONS);
    }

    @Test
    void shouldRejectTransactionWithoutBatchKey() {
        var batchTransaction = new BatchTransaction();
        var transactionWithoutBatchKey = new AccountCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .freeze();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(transactionWithoutBatchKey))
                .withMessageContaining("Batch key needs to be set");
    }

    @Test
    void shouldValidateAllTransactionsInList() {
        var batchTransaction = new BatchTransaction();
        var validTransaction = spawnTestTransactionAccountCreate();
        var transactionWithoutBatchKey = new AccountCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .freeze();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() ->
                        batchTransaction.setInnerTransactions(List.of(validTransaction, transactionWithoutBatchKey)))
                .withMessageContaining("Batch key needs to be set");
    }

    @Test
    void shouldValidateMultipleConditions() {
        var batchTransaction = new BatchTransaction();

        // Test unfrozen transaction with no batch key
        var unfrozenTransactionWithoutBatchKey = new AccountCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(unfrozenTransactionWithoutBatchKey))
                .withMessageContaining("Inner transaction should be frozen");

        // Test frozen transaction with no batch key
        var frozenTransactionWithoutBatchKey = unfrozenTransactionWithoutBatchKey.freeze();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(frozenTransactionWithoutBatchKey))
                .withMessageContaining("Batch key needs to be set");

        // Test blacklisted transaction with batch key
        var blacklistedTransaction = new FreezeTransaction()
                .setStartTime(Instant.now())
                .setFreezeType(FreezeType.FREEZE_ONLY)
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setBatchKey(privateKeyECDSA)
                .freeze();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(blacklistedTransaction))
                .withMessageContaining("FreezeTransaction is not allowed in a batch transaction");
    }

    @Test
    void shouldAcceptValidTransaction() {
        var batchTransaction = new BatchTransaction();
        var validTransaction = new AccountCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setBatchKey(privateKeyECDSA)
                .freeze();

        batchTransaction.addInnerTransaction(validTransaction);

        assertThat(batchTransaction.getInnerTransactions())
                .isNotNull()
                .hasSize(1)
                .contains(validTransaction);
    }

    @Test
    void shouldValidateTransactionStateInOrder() {
        var batchTransaction = new BatchTransaction();
        var transaction = new AccountCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart));

        // First check should be for frozen state
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(transaction))
                .withMessageContaining("Inner transaction should be frozen");

        // After freezing, next check should be for batch key
        var frozenTransaction = transaction.freeze();
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> batchTransaction.addInnerTransaction(frozenTransaction))
                .withMessageContaining("Batch key needs to be set");
    }
}
