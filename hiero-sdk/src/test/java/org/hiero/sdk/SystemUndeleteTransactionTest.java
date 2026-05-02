// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.SystemUndeleteTransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SystemUndeleteTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

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

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new SystemUndeleteTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private SystemUndeleteTransaction spawnTestTransactionFile() {
        return new SystemUndeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setFileId(FileId.fromString("0.0.444"))
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldSerializeContract() {
        SnapshotMatcher.expect(spawnTestTransactionContract().toString()).toMatchSnapshot();
    }

    private SystemUndeleteTransaction spawnTestTransactionContract() {
        return new SystemUndeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setContractId(ContractId.fromString("0.0.444"))
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
        var tx2 = SystemUndeleteTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setSystemUndelete(SystemUndeleteTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(SystemUndeleteTransaction.class);
    }
}
