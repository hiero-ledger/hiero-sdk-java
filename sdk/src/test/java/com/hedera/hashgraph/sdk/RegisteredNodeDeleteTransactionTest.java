// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.proto.RegisteredNodeDeleteTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RegisteredNodeDeleteTransactionTest {
    private static final PrivateKey TEST_PRIVATE_KEY = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final long TEST_REGISTERED_NODE_ID = 420;

    final Instant TEST_VALID_START = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    RegisteredNodeDeleteTransaction spawnTestTransaction() {
        return new RegisteredNodeDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), TEST_VALID_START))
                .setRegisteredNodeId(TEST_REGISTERED_NODE_ID)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(TEST_PRIVATE_KEY);
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws Exception {
        var tx1 = spawnTestTransaction();
        var tx2 = RegisteredNodeDeleteTransaction.fromBytes(tx1.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx1.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx1 = new RegisteredNodeDeleteTransaction();
        var tx2 = RegisteredNodeDeleteTransaction.fromBytes(tx1.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx1.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setRegisteredNodeDelete(
                        RegisteredNodeDeleteTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);
        assertThat(tx).isInstanceOf(RegisteredNodeDeleteTransaction.class);
    }

    @Test
    void constructNodeDeleteTransactionFromTransactionBodyProtobuf() {
        var transactionBodyBuilder = RegisteredNodeDeleteTransactionBody.newBuilder();

        transactionBodyBuilder.setRegisteredNodeId(TEST_REGISTERED_NODE_ID);

        var tx = TransactionBody.newBuilder()
                .setRegisteredNodeDelete(transactionBodyBuilder.build())
                .build();
        var registeredNodeDeleteTransaction = new RegisteredNodeDeleteTransaction(tx);

        assertThat(registeredNodeDeleteTransaction.getRegisteredNodeId()).isEqualTo(TEST_REGISTERED_NODE_ID);
    }

    @Test
    void getSetRegisteredNodeId() {
        var tx = new RegisteredNodeDeleteTransaction().setRegisteredNodeId(TEST_REGISTERED_NODE_ID);
        assertThat(tx.getRegisteredNodeId()).isEqualTo(TEST_REGISTERED_NODE_ID);
    }

    @Test
    void getSetRegisteredNodeIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setRegisteredNodeId(TEST_REGISTERED_NODE_ID));
    }

    @Test
    void shouldThrowErrorWhenGettingRegisteredNodeIdWithoutSettingIt() {
        var tx = new RegisteredNodeDeleteTransaction();

        var exception = assertThrows(IllegalStateException.class, () -> tx.getRegisteredNodeId());
        assertThat(exception.getMessage())
                .isEqualTo("RegisteredNodeDeleteTransaction: 'registeredNodeId' has not been set");
    }

    @Test
    void shouldThrowErrorWhenSettingNegativeRegisteredNodeId() {
        var tx = new RegisteredNodeDeleteTransaction();

        var exception = assertThrows(IllegalArgumentException.class, () -> tx.setRegisteredNodeId(-1));
        assertThat(exception.getMessage())
                .isEqualTo("RegisteredNodeDeleteTransaction: 'registeredNodeId' must be non-negative");
    }

    @Test
    void shouldAllowSettingRegisteredNodeIdToZero() {
        var tx = new RegisteredNodeDeleteTransaction().setRegisteredNodeId(0);
        assertThat(tx.getRegisteredNodeId()).isEqualTo(0);
    }

    @Test
    void shouldFreezeSuccessfullyWhenRegisteredNodeIdIsSet() {
        final Instant VALID_START = Instant.ofEpochSecond(1596210382);
        final AccountId ACCOUNT_ID = AccountId.fromString("0.6.9");

        var tx = new RegisteredNodeDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(ACCOUNT_ID, VALID_START))
                .setRegisteredNodeId(420);

        assertThatCode(() -> tx.freezeWith(null)).doesNotThrowAnyException();
        assertThat(tx.getRegisteredNodeId()).isEqualTo(420);
    }

    @Test
    void shouldThrowErrorWhenFreezingWithZeroRegisteredNodeId() {
        final Instant VALID_START = Instant.ofEpochSecond(1596210382);
        final AccountId ACCOUNT_ID = AccountId.fromString("0.6.9");

        var tx = new RegisteredNodeDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(ACCOUNT_ID, VALID_START));

        var exception = assertThrows(IllegalStateException.class, () -> tx.freezeWith(null));
        assertThat(exception.getMessage())
                .isEqualTo(
                        "RegisteredNodeDeleteTransaction: 'registeredNodeId' must be explicitly set before calling freeze().");
    }
}
