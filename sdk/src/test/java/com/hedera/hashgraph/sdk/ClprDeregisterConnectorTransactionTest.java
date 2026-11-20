// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.ClprTestSupport.bytes32;
import static com.hedera.hashgraph.sdk.ClprTestSupport.prepare;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ClprDeregisterConnectorTransactionTest {
    private static final byte[] CHANNEL_ID = bytes32(0x01);
    private static final byte[] CONNECTOR_ID = bytes32(0x02);
    private static final AccountId RECIPIENT = AccountId.fromString("0.0.1234");

    @BeforeAll
    static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    private ClprDeregisterConnectorTransaction spawnTestTransaction() {
        return prepare(new ClprDeregisterConnectorTransaction()
                        .setChannelId(CHANNEL_ID)
                        .setConnectorId(CONNECTOR_ID)
                        .setStakeRecipient(RECIPIENT))
                .freeze()
                .sign(ClprTestSupport.TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = (ClprDeregisterConnectorTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(tx2.getChannelId()).isEqualTo(CHANNEL_ID);
        assertThat(tx2.getConnectorId()).isEqualTo(CONNECTOR_ID);
        assertThat(tx2.getStakeRecipient()).isEqualTo(RECIPIENT);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ClprDeregisterConnectorTransaction();
        var tx2 = (ClprDeregisterConnectorTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.getStakeRecipient()).isNull();
    }

    @Test
    void requiresStakeRecipient() {
        assertThatThrownBy(() -> prepare(new ClprDeregisterConnectorTransaction()
                                .setChannelId(CHANNEL_ID)
                                .setConnectorId(CONNECTOR_ID))
                        .freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprDeregisterConnectorTransaction: 'stakeRecipient' must be explicitly set before freezing or executing.");
    }

    @Test
    void requiresConnectorId() {
        assertThatThrownBy(() -> prepare(new ClprDeregisterConnectorTransaction()
                                .setChannelId(CHANNEL_ID)
                                .setStakeRecipient(RECIPIENT))
                        .freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprDeregisterConnectorTransaction: 'connectorId' must be explicitly set before freezing or executing.");
    }

    @Test
    void frozenSetterThrows() {
        var tx = spawnTestTransaction();
        assertThatThrownBy(() -> tx.setStakeRecipient(RECIPIENT)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotBeScheduled() {
        assertThatThrownBy(() -> new ClprDeregisterConnectorTransaction().schedule())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
