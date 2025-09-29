// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ScheduleCreateTransactionTest {
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
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    private ScheduleCreateTransaction spawnTestTransaction() {
        var transferTransaction = new TransferTransaction()
                .addHbarTransfer(AccountId.fromString("0.0.555"), new Hbar(-10))
                .addHbarTransfer(AccountId.fromString("0.0.333"), new Hbar(10));
        return transferTransaction
                .schedule()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setAdminKey(unusedPrivateKey)
                .setPayerAccountId(AccountId.fromString("0.0.222"))
                .setScheduleMemo("hi")
                .setMaxTransactionFee(new Hbar(1))
                .setExpirationTime(validStart)
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = ScheduleCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ScheduleCreateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldSupportExpirationTimeDurationBytesRoundTrip() throws Exception {
        var transferTransaction = new TransferTransaction()
                .addHbarTransfer(AccountId.fromString("0.0.555"), new Hbar(-10))
                .addHbarTransfer(AccountId.fromString("0.0.333"), new Hbar(10));

        var tx = transferTransaction
                .schedule()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setAdminKey(unusedPrivateKey)
                .setPayerAccountId(AccountId.fromString("0.0.222"))
                .setScheduleMemo("with-duration")
                .setMaxTransactionFee(new Hbar(1))
                .setExpirationTime(Duration.ofSeconds(1234));

        // When expiration is set via Duration, Instant getter should be null
        assertThat(tx.getExpirationTime()).isNull();

        var tx2 = (ScheduleCreateTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(tx2.getExpirationTime()).isEqualTo(Instant.ofEpochSecond(1234));
    }

    @Test
    void setExpirationTimeDurationOnFrozenTransactionShouldThrow() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setExpirationTime(Duration.ofSeconds(1)));
    }

    @Test
    void getSetExpirationTimeInstant() {
        var instant = Instant.ofEpochSecond(1_234_567L);
        var tx = new ScheduleCreateTransaction().setExpirationTime(instant);
        assertThat(tx.getExpirationTime()).isEqualTo(instant);
    }
}
