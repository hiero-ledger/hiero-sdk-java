// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.ByteString;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.hiero.sdk.proto.FreezeTransactionBody;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.Timestamp;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FreezeTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final FileId testFileId = FileId.fromString("4.5.6");
    private static final byte[] testFileHash = Hex.decode("1723904587120938954702349857");
    private static final FreezeType testFreezeType = FreezeType.TELEMETRY_UPGRADE;

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
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    private FreezeTransaction spawnTestTransaction() {
        return new FreezeTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setFileId(testFileId)
                .setFileHash(testFileHash)
                .setStartTime(validStart)
                .setFreezeType(testFreezeType)
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = FreezeTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new FreezeTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setFreeze(FreezeTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(FreezeTransaction.class);
    }

    @Test
    void constructFreezeTransactionFromTransactionBodyProtobuf() {
        var transactionBody = FreezeTransactionBody.newBuilder()
                .setUpdateFile(testFileId.toProtobuf())
                .setFileHash(ByteString.copyFrom(testFileHash))
                .setStartTime(Timestamp.newBuilder().setSeconds(validStart.getEpochSecond()))
                .setFreezeType(testFreezeType.code);

        var tx = TransactionBody.newBuilder().setFreeze(transactionBody).build();
        var freezeTransaction = new FreezeTransaction(tx);

        assertNotNull(freezeTransaction.getFileId());
        assertThat(freezeTransaction.getFileId()).isEqualTo(testFileId);
        assertThat(freezeTransaction.getFileHash()).isEqualTo(testFileHash);
        assertNotNull(freezeTransaction.getStartTime());
        assertThat(freezeTransaction.getStartTime().getEpochSecond()).isEqualTo(validStart.getEpochSecond());
        assertThat(freezeTransaction.getFreezeType()).isEqualTo(testFreezeType);
    }

    @Test
    void getSetFileId() {
        var freezeTransaction = new FreezeTransaction().setFileId(testFileId);
        assertNotNull(freezeTransaction.getFileId());
        assertThat(freezeTransaction.getFileId()).isEqualTo(testFileId);
    }

    @Test
    void getSetFileIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setFileId(testFileId));
    }

    @Test
    void getSetFileHash() {
        var freezeTransaction = new FreezeTransaction().setFileHash(testFileHash);
        assertNotNull(freezeTransaction.getFileHash());
        assertThat(freezeTransaction.getFileHash()).isEqualTo(testFileHash);
    }

    @Test
    void getSetFileHashFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setFileHash(testFileHash));
    }

    @Test
    void getSetStartTime() {
        var freezeTransaction = new FreezeTransaction().setStartTime(validStart);
        assertNotNull(freezeTransaction.getStartTime());
        assertThat(freezeTransaction.getStartTime().getEpochSecond()).isEqualTo(validStart.getEpochSecond());
    }

    @Test
    void getSetStartTimeFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setStartTime(validStart));
    }

    @Test
    void getSetFreezeType() {
        var freezeTransaction = new FreezeTransaction().setFreezeType(testFreezeType);
        assertThat(freezeTransaction.getFreezeType()).isEqualTo(testFreezeType);
    }

    @Test
    void getSetFreezeTypeFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setFreezeType(testFreezeType));
    }
}
