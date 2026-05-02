// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.hiero.sdk.proto.CryptoCreateTransactionBody;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AccountCreateTransactionTest {
    private static final PrivateKey privateKeyED25519 = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    PrivateKey privateKeyECDSA =
            PrivateKey.fromStringECDSA("7f109a9e3b0d8ecfba9cc23a3614433ce0fa7ddcc80f2a8f10b222179a5a80d6");

    final Instant validStart = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    AccountCreateTransaction spawnTestTransaction() {
        return new AccountCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setKeyWithAlias(privateKeyECDSA)
                .setKeyWithAlias(privateKeyED25519, privateKeyECDSA)
                .setKeyWithoutAlias(privateKeyED25519)
                .setInitialBalance(Hbar.fromTinybars(450))
                .setProxyAccountId(AccountId.fromString("0.0.1001"))
                .setAccountMemo("some dumb memo")
                .setReceiverSignatureRequired(true)
                .setAutoRenewPeriod(Duration.ofHours(10))
                .setStakedAccountId(AccountId.fromString("0.0.3"))
                .setAlias("0x5c562e90feaf0eebd33ea75d21024f249d451417")
                .setMaxAutomaticTokenAssociations(100)
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .freeze()
                .sign(privateKeyED25519);
    }

    AccountCreateTransaction spawnTestTransaction2() {
        return new AccountCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setKeyWithAlias(privateKeyECDSA)
                .setKeyWithAlias(privateKeyED25519, privateKeyECDSA)
                .setKeyWithoutAlias(privateKeyED25519)
                .setInitialBalance(Hbar.fromTinybars(450))
                .setProxyAccountId(AccountId.fromString("0.0.1001"))
                .setAccountMemo("some dumb memo")
                .setReceiverSignatureRequired(true)
                .setAutoRenewPeriod(Duration.ofHours(10))
                .setStakedNodeId(4L)
                .setMaxAutomaticTokenAssociations(100)
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .freeze()
                .sign(privateKeyED25519);
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = AccountCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldSerialize2() {
        SnapshotMatcher.expect(spawnTestTransaction2().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes2() throws Exception {
        var tx = spawnTestTransaction2();
        var tx2 = AccountCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new AccountCreateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void propertiesTest() {
        var tx = spawnTestTransaction();

        assertThat(tx.getKey()).isEqualTo(privateKeyED25519);
        assertThat(tx.getInitialBalance()).isEqualTo(Hbar.fromTinybars(450));
        assertThat(tx.getReceiverSignatureRequired()).isTrue();
        assertThat(tx.getProxyAccountId()).hasToString("0.0.1001");
        assertThat(tx.getAutoRenewPeriod().toHours()).isEqualTo(10);
        assertThat(tx.getMaxAutomaticTokenAssociations()).isEqualTo(100);
        assertThat(tx.getAccountMemo()).isEqualTo("some dumb memo");
        assertThat(tx.getStakedAccountId()).hasToString("0.0.3");
        assertThat(tx.getStakedNodeId()).isNull();
        assertThat(tx.getDeclineStakingReward()).isFalse();
        assertThat(tx.getAlias()).isEqualTo(EvmAddress.fromString("0x5c562e90feaf0eebd33ea75d21024f249d451417"));
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setCryptoCreateAccount(CryptoCreateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(AccountCreateTransaction.class);
    }
}
