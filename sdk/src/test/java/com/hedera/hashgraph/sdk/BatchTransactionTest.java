// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
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
}
