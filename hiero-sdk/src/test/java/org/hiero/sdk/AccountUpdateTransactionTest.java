// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.hiero.sdk.proto.CryptoUpdateTransactionBody;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AccountUpdateTransactionTest {
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

    AccountUpdateTransaction spawnTestTransaction() {
        return new AccountUpdateTransaction()
                .setKey(unusedPrivateKey)
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setAccountId(AccountId.fromString("0.0.2002"))
                .setProxyAccountId(AccountId.fromString("0.0.1001"))
                .setAutoRenewPeriod(Duration.ofHours(10))
                .setExpirationTime(Instant.ofEpochSecond(1554158543))
                .setReceiverSignatureRequired(false)
                .setMaxAutomaticTokenAssociations(100)
                .setAccountMemo("Some memo")
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setStakedAccountId(AccountId.fromString("0.0.3"))
                .freeze()
                .sign(unusedPrivateKey);
    }

    AccountUpdateTransaction spawnTestTransaction2() {
        return new AccountUpdateTransaction()
                .setKey(unusedPrivateKey)
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setAccountId(AccountId.fromString("0.0.2002"))
                .setProxyAccountId(AccountId.fromString("0.0.1001"))
                .setAutoRenewPeriod(Duration.ofHours(10))
                .setExpirationTime(Instant.ofEpochSecond(1554158543))
                .setReceiverSignatureRequired(false)
                .setMaxAutomaticTokenAssociations(100)
                .setAccountMemo("Some memo")
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setStakedNodeId(4L)
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = AccountUpdateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new AccountUpdateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldSerialize2() {
        SnapshotMatcher.expect(spawnTestTransaction2().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes2() throws Exception {
        var tx = spawnTestTransaction2();
        var tx2 = AccountUpdateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setCryptoUpdateAccount(CryptoUpdateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(AccountUpdateTransaction.class);
    }
}
