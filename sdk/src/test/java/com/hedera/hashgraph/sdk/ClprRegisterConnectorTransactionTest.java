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

class ClprRegisterConnectorTransactionTest {
    private static final byte[] CHANNEL_ID = bytes32(0x01);
    private static final byte[] COMMITMENT = bytes32(0x02);

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

    private ClprRegisterConnectorTransaction spawnTestTransaction() {
        return prepare(new ClprRegisterConnectorTransaction()
                        .setChannelId(CHANNEL_ID)
                        .setCommitment(COMMITMENT))
                .freeze()
                .sign(ClprTestSupport.TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = (ClprRegisterConnectorTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(tx2.getChannelId()).isEqualTo(CHANNEL_ID);
        assertThat(tx2.getCommitment()).isEqualTo(COMMITMENT);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ClprRegisterConnectorTransaction();
        assertThat(Transaction.fromBytes(tx.toBytes()).toString()).isEqualTo(tx.toString());
    }

    @Test
    void requiresChannelId() {
        assertThatThrownBy(() -> prepare(new ClprRegisterConnectorTransaction().setCommitment(COMMITMENT))
                        .freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprRegisterConnectorTransaction: 'channelId' must be explicitly set before freezing or executing.");
    }

    @Test
    void requiresCommitment() {
        assertThatThrownBy(() -> prepare(new ClprRegisterConnectorTransaction().setChannelId(CHANNEL_ID))
                        .freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprRegisterConnectorTransaction: 'commitment' must be explicitly set before freezing or executing.");
    }

    @Test
    void frozenSetterThrows() {
        var tx = spawnTestTransaction();
        assertThatThrownBy(() -> tx.setChannelId(CHANNEL_ID)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotBeScheduled() {
        assertThatThrownBy(() -> new ClprRegisterConnectorTransaction().schedule())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
