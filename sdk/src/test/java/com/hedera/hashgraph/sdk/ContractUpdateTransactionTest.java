// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.proto.ContractUpdateTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ContractUpdateTransactionTest {
    private static final PrivateKey privateKey = PrivateKey.fromString(
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

    @Test
    void shouldSerialize2() {
        SnapshotMatcher.expect(spawnTestTransaction2().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ContractUpdateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private ContractUpdateTransaction spawnTestTransaction() {
        return new ContractUpdateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setContractId(ContractId.fromString("0.0.5007"))
                .setAdminKey(privateKey)
                .setMaxAutomaticTokenAssociations(101)
                .setAutoRenewPeriod(Duration.ofDays(1))
                .setContractMemo("3")
                .setStakedAccountId(AccountId.fromString("0.0.3"))
                .setExpirationTime(Instant.ofEpochMilli(4))
                .setProxyAccountId(new AccountId(0, 0, 4))
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setAutoRenewAccountId(new AccountId(0, 0, 30))
                .freeze()
                .sign(privateKey);
    }

    private ContractUpdateTransaction spawnTestTransaction2() {
        return new ContractUpdateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setContractId(ContractId.fromString("0.0.5007"))
                .setAdminKey(privateKey)
                .setMaxAutomaticTokenAssociations(101)
                .setAutoRenewPeriod(Duration.ofDays(1))
                .setContractMemo("3")
                .setStakedNodeId(4L)
                .setExpirationTime(Instant.ofEpochMilli(4))
                .setProxyAccountId(new AccountId(0, 0, 4))
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setAutoRenewAccountId(new AccountId(0, 0, 30))
                .freeze()
                .sign(privateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = ContractUpdateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytes2() throws Exception {
        var tx = spawnTestTransaction2();
        var tx2 = ContractUpdateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldSupportExpirationTimeDurationBytesRoundTrip() throws Exception {
        var tx = new ContractUpdateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setContractId(ContractId.fromString("0.0.5007"))
                .setAdminKey(privateKey)
                .setMaxAutomaticTokenAssociations(101)
                .setAutoRenewPeriod(Duration.ofDays(1))
                .setContractMemo("with-duration")
                .setStakedAccountId(AccountId.fromString("0.0.3"))
                .setExpirationTime(Duration.ofSeconds(1234))
                .setProxyAccountId(new AccountId(0, 0, 4))
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setAutoRenewAccountId(new AccountId(0, 0, 30));

        // When expiration is set via Duration, Instant getter should be null
        assertThat(tx.getExpirationTime()).isNull();

        var tx2 = (ContractUpdateTransaction) Transaction.fromBytes(tx.toBytes());
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
        var tx = new ContractUpdateTransaction().setExpirationTime(instant);
        assertThat(tx.getExpirationTime()).isEqualTo(instant);
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setContractUpdateInstance(
                        ContractUpdateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(ContractUpdateTransaction.class);
    }
}
