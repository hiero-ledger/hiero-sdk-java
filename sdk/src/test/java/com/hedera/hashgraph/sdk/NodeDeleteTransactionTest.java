// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.proto.NodeDeleteTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NodeDeleteTransactionTest {

    private static final PrivateKey TEST_PRIVATE_KEY = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final long TEST_NODE_ID = 420;

    final Instant TEST_VALID_START = Instant.ofEpochSecond(1554158542);

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

    private NodeDeleteTransaction spawnTestTransaction() {
        return new NodeDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), TEST_VALID_START))
                .setNodeId(TEST_NODE_ID)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = NodeDeleteTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new NodeDeleteTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setNodeDelete(NodeDeleteTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(NodeDeleteTransaction.class);
    }

    @Test
    void constructNodeDeleteTransactionFromTransactionBodyProtobuf() {
        var transactionBodyBuilder = NodeDeleteTransactionBody.newBuilder();

        transactionBodyBuilder.setNodeId(TEST_NODE_ID);

        var tx = TransactionBody.newBuilder()
                .setNodeDelete(transactionBodyBuilder.build())
                .build();
        var nodeDeleteTransaction = new NodeDeleteTransaction(tx);

        assertThat(nodeDeleteTransaction.getNodeId()).isEqualTo(TEST_NODE_ID);
    }

    @Test
    void getSetNodeId() {
        var nodeDeleteTransaction = new NodeDeleteTransaction().setNodeId(TEST_NODE_ID);
        assertThat(nodeDeleteTransaction.getNodeId()).isEqualTo(TEST_NODE_ID);
    }

    @Test
    void getSetNodeIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setNodeId(TEST_NODE_ID));
    }

    @Test
    @DisplayName("should freeze successfully when nodeId is set")
    void shouldFreezeSuccessfullyWhenNodeIdIsSet() {
        final Instant VALID_START = Instant.ofEpochSecond(1596210382);
        final AccountId ACCOUNT_ID = AccountId.fromString("0.6.9");

        var transaction = new NodeDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(ACCOUNT_ID, VALID_START))
                .setNodeId(420);

        assertThatCode(() -> transaction.freezeWith(null)).doesNotThrowAnyException();
        assertThat(transaction.getNodeId()).isEqualTo(420);
    }

    @Test
    @DisplayName("should throw error when freezing without setting nodeId")
    void shouldThrowErrorWhenFreezingWithoutSettingNodeId() {
        final Instant VALID_START = Instant.ofEpochSecond(1596210382);
        final AccountId ACCOUNT_ID = AccountId.fromString("0.6.9");

        var transaction = new NodeDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(ACCOUNT_ID, VALID_START));

        var exception = assertThrows(IllegalStateException.class, () -> transaction.freezeWith(null));
        assertThat(exception.getMessage())
                .isEqualTo("NodeDeleteTransaction: 'nodeId' must be explicitly set before calling freeze().");
    }

    @Test
    @DisplayName("should throw error when freezing with nodeId null")
    void shouldThrowErrorWhenFreezingWithZeroNodeId() {
        final Instant VALID_START = Instant.ofEpochSecond(1596210382);
        final AccountId ACCOUNT_ID = AccountId.fromString("0.6.9");

        var transaction = new NodeDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(ACCOUNT_ID, VALID_START));

        var exception = assertThrows(IllegalStateException.class, () -> transaction.freezeWith(null));
        assertThat(exception.getMessage())
                .isEqualTo("NodeDeleteTransaction: 'nodeId' must be explicitly set before calling freeze().");
    }

    @Test
    @DisplayName("should freeze successfully with actual client when nodeId is set")
    void shouldFreezeSuccessfullyWithActualClientWhenNodeIdIsSet() {
        final Instant VALID_START = Instant.ofEpochSecond(1596210382);
        final AccountId ACCOUNT_ID = AccountId.fromString("0.6.9");

        var transaction = new NodeDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(ACCOUNT_ID, VALID_START))
                .setNodeId(420);

        var mockClient = Client.forTestnet();

        assertThatCode(() -> transaction.freezeWith(mockClient)).doesNotThrowAnyException();
        assertThat(transaction.getNodeId()).isEqualTo(420);
    }

    @Test
    @DisplayName("should throw error when getting nodeId without setting it")
    void shouldThrowErrorWhenGettingNodeIdWithoutSettingIt() {
        var transaction = new NodeDeleteTransaction();

        var exception = assertThrows(IllegalStateException.class, () -> transaction.getNodeId());
        assertThat(exception.getMessage()).isEqualTo("NodeDeleteTransaction: 'nodeId' has not been set");
    }

    @Test
    @DisplayName("should throw error when setting negative nodeId")
    void shouldThrowErrorWhenSettingNegativeNodeId() {
        var transaction = new NodeDeleteTransaction();

        var exception = assertThrows(IllegalArgumentException.class, () -> transaction.setNodeId(-1));
        assertThat(exception.getMessage()).isEqualTo("NodeDeleteTransaction: 'nodeId' must be non-negative");
    }

    @Test
    @DisplayName("should allow setting nodeId to zero")
    void shouldAllowSettingNodeIdToZero() {
        var transaction = new NodeDeleteTransaction().setNodeId(0);
        assertThat(transaction.getNodeId()).isEqualTo(0);
    }
}
