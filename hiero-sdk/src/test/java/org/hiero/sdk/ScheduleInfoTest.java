// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import org.hiero.sdk.proto.CryptoDeleteTransactionBody;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ScheduleInfoTest {
    private static final PublicKey unusedPublicKey = PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10")
            .getPublicKey();

    final Instant validStart = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    ScheduleInfo spawnScheduleInfoExample() {
        return new ScheduleInfo(
                ScheduleId.fromString("1.2.3"),
                AccountId.fromString("4.5.6"),
                AccountId.fromString("2.3.4"),
                SchedulableTransactionBody.newBuilder()
                        .setCryptoDelete(CryptoDeleteTransactionBody.newBuilder()
                                .setDeleteAccountID(
                                        AccountId.fromString("6.6.6").toProtobuf())
                                .build())
                        .build(),
                KeyList.of(unusedPublicKey),
                unusedPublicKey,
                TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart),
                "memo",
                validStart,
                validStart,
                null,
                LedgerId.TESTNET,
                true);
    }

    ScheduleInfo spawnScheduleInfoDeletedExample() {
        return new ScheduleInfo(
                ScheduleId.fromString("1.2.3"),
                AccountId.fromString("4.5.6"),
                AccountId.fromString("2.3.4"),
                SchedulableTransactionBody.newBuilder()
                        .setCryptoDelete(CryptoDeleteTransactionBody.newBuilder()
                                .setDeleteAccountID(
                                        AccountId.fromString("6.6.6").toProtobuf())
                                .build())
                        .build(),
                KeyList.of(unusedPublicKey),
                unusedPublicKey,
                TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart),
                "memo",
                validStart,
                null,
                validStart,
                LedgerId.TESTNET,
                true);
    }

    @Test
    void shouldSerialize() throws Exception {
        var originalScheduleInfo = spawnScheduleInfoExample();
        byte[] scheduleInfoBytes = originalScheduleInfo.toBytes();
        var copyScheduleInfo = ScheduleInfo.fromBytes(scheduleInfoBytes);
        assertThat(copyScheduleInfo.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .isEqualTo(originalScheduleInfo.toString().replaceAll("@[A-Za-z0-9]+", ""));
        SnapshotMatcher.expect(originalScheduleInfo.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void shouldSerializeDeleted() throws Exception {
        var originalScheduleInfo = spawnScheduleInfoDeletedExample();
        byte[] scheduleInfoBytes = originalScheduleInfo.toBytes();
        var copyScheduleInfo = ScheduleInfo.fromBytes(scheduleInfoBytes);
        assertThat(copyScheduleInfo.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .isEqualTo(originalScheduleInfo.toString().replaceAll("@[A-Za-z0-9]+", ""));
        SnapshotMatcher.expect(originalScheduleInfo.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }
}
