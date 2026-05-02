// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.SystemDeleteTransactionBody;
import org.hiero.sdk.proto.TimestampSeconds;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SystemDeleteTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final FileId testFileId = FileId.fromString("4.2.0");
    private static final ContractId testContractId = ContractId.fromString("0.6.9");
    final Instant validStart = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerializeFile() {
        SnapshotMatcher.expect(spawnTestTransactionFile().toString()).toMatchSnapshot();
    }

    private SystemDeleteTransaction spawnTestTransactionFile() {
        return new SystemDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setFileId(FileId.fromString("0.0.444"))
                .setExpirationTime(validStart)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldSerializeContract() {
        SnapshotMatcher.expect(spawnTestTransactionContract().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new SystemDeleteTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private SystemDeleteTransaction spawnTestTransactionContract() {
        return new SystemDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setContractId(ContractId.fromString("0.0.444"))
                .setExpirationTime(validStart)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytesContract() throws Exception {
        var tx = spawnTestTransactionContract();
        var tx2 = ScheduleDeleteTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesFile() throws Exception {
        var tx = spawnTestTransactionFile();
        var tx2 = SystemDeleteTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setSystemDelete(SystemDeleteTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(SystemDeleteTransaction.class);
    }

    @Test
    void constructSystemDeleteTransactionFromTransactionBodyProtobuf() {
        var transactionBodyWithFileId = SystemDeleteTransactionBody.newBuilder()
                .setFileID(testFileId.toProtobuf())
                .setExpirationTime(TimestampSeconds.newBuilder().setSeconds(validStart.getEpochSecond()));

        var transactionBodyWithContractId = SystemDeleteTransactionBody.newBuilder()
                .setContractID(testContractId.toProtobuf())
                .setExpirationTime(TimestampSeconds.newBuilder().setSeconds(validStart.getEpochSecond()));

        var txWithFileId = TransactionBody.newBuilder()
                .setSystemDelete(transactionBodyWithFileId)
                .build();
        var systemDeleteTransactionWithFileId = new SystemDeleteTransaction(txWithFileId);

        var txWithContractId = TransactionBody.newBuilder()
                .setSystemDelete(transactionBodyWithContractId)
                .build();
        var systemDeleteTransactionWithContractId = new SystemDeleteTransaction(txWithContractId);

        assertNotNull(systemDeleteTransactionWithFileId.getFileId());
        assertThat(systemDeleteTransactionWithFileId.getFileId()).isEqualTo(testFileId);
        assertNull(systemDeleteTransactionWithFileId.getContractId());
        assertThat(systemDeleteTransactionWithFileId.getExpirationTime().getEpochSecond())
                .isEqualTo(validStart.getEpochSecond());

        assertNull(systemDeleteTransactionWithContractId.getFileId());
        assertNotNull(systemDeleteTransactionWithContractId.getContractId());
        assertThat(systemDeleteTransactionWithContractId.getContractId()).isEqualTo(testContractId);
        assertThat(systemDeleteTransactionWithContractId.getExpirationTime().getEpochSecond())
                .isEqualTo(validStart.getEpochSecond());
    }

    @Test
    void getSetFileId() {
        var systemDeleteTransaction = new SystemDeleteTransaction().setFileId(testFileId);
        assertNotNull(systemDeleteTransaction.getFileId());
        assertThat(systemDeleteTransaction.getFileId()).isEqualTo(testFileId);
    }

    @Test
    void getSetFileIdFrozen() {
        var tx = spawnTestTransactionFile();
        assertThrows(IllegalStateException.class, () -> tx.setFileId(testFileId));
    }

    @Test
    void getSetContractId() {
        var systemDeleteTransaction = new SystemDeleteTransaction().setContractId(testContractId);
        assertNotNull(systemDeleteTransaction.getContractId());
        assertThat(systemDeleteTransaction.getContractId()).isEqualTo(testContractId);
    }

    @Test
    void getSetContractIdFrozen() {
        var tx = spawnTestTransactionContract();
        assertThrows(IllegalStateException.class, () -> tx.setContractId(testContractId));
    }

    @Test
    void getSetExpirationTime() {
        var systemDeleteTransaction = new SystemDeleteTransaction().setExpirationTime(validStart);
        assertNotNull(systemDeleteTransaction.getExpirationTime());
        assertThat(systemDeleteTransaction.getExpirationTime().getEpochSecond()).isEqualTo(validStart.getEpochSecond());
    }

    @Test
    void getSetExpirationTimeFrozen() {
        var tx = spawnTestTransactionFile();
        assertThrows(IllegalStateException.class, () -> tx.setExpirationTime(validStart));
    }

    @Test
    void resetFileId() {
        var systemDeleteTransaction = new SystemDeleteTransaction();
        systemDeleteTransaction.setFileId(testFileId);
        systemDeleteTransaction.setContractId(testContractId);

        assertNull(systemDeleteTransaction.getFileId());
        assertNotNull(systemDeleteTransaction.getContractId());
    }

    @Test
    void resetContractId() {
        var systemDeleteTransaction = new SystemDeleteTransaction();
        systemDeleteTransaction.setContractId(testContractId);
        systemDeleteTransaction.setFileId(testFileId);

        assertNull(systemDeleteTransaction.getContractId());
        assertNotNull(systemDeleteTransaction.getFileId());
    }
}
