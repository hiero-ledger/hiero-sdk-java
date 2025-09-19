// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.proto.ContractCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ContractCreateTransactionTest {
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

    @Test
    void shouldSerialize2() {
        SnapshotMatcher.expect(spawnTestTransaction2().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ContractCreateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private ContractCreateTransaction spawnTestTransaction() {
        return new ContractCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setBytecodeFileId(FileId.fromString("0.0.3003"))
                .setAdminKey(unusedPrivateKey)
                .setGas(0)
                .setInitialBalance(Hbar.fromTinybars(1000))
                .setStakedAccountId(AccountId.fromString("0.0.3"))
                .setMaxAutomaticTokenAssociations(101)
                .setAutoRenewPeriod(Duration.ofHours(10))
                .setConstructorParameters(new byte[] {10, 11, 12, 13, 25})
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setAutoRenewAccountId(new AccountId(0, 0, 30))
                .freeze()
                .sign(unusedPrivateKey);
    }

    private ContractCreateTransaction spawnTestTransaction2() {
        return new ContractCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setBytecode(Hex.decode("deadbeef"))
                .setAdminKey(unusedPrivateKey)
                .setGas(0)
                .setInitialBalance(Hbar.fromTinybars(1000))
                .setStakedNodeId(4L)
                .setMaxAutomaticTokenAssociations(101)
                .setAutoRenewPeriod(Duration.ofHours(10))
                .setConstructorParameters(new byte[] {10, 11, 12, 13, 25})
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setAutoRenewAccountId(new AccountId(0, 0, 30))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = ContractCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytes2() throws Exception {
        var tx = spawnTestTransaction2();
        var tx2 = ContractCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx2.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setContractCreateInstance(
                        ContractCreateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(ContractCreateTransaction.class);
    }

    @Test
    void setGasShouldRejectNegativeValues() {
        var tx = new ContractCreateTransaction();
        var ex = assertThrows(IllegalArgumentException.class, () -> tx.setGas(-1));
        assertThat(ex.getMessage()).isEqualTo("Gas must be non-negative");
    }

    @Test
    void setGasShouldAcceptZeroAndPositiveValues() {
        var tx = new ContractCreateTransaction();

        tx.setGas(0);
        assertThat(tx.getGas()).isEqualTo(0);

        tx.setGas(123456);
        assertThat(tx.getGas()).isEqualTo(123456);
    }
}
