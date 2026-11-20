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

class ClprCloseChannelTransactionTest {
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

    private ClprCloseChannelTransaction spawnTestTransaction() {
        return prepare(new ClprCloseChannelTransaction()
                        .setChannelId(CHANNEL_ID)
                        .setOwnershipCommitment(COMMITMENT))
                .freeze()
                .sign(ClprTestSupport.TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = (ClprCloseChannelTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(tx2.getChannelId()).isEqualTo(CHANNEL_ID);
        assertThat(tx2.getOwnershipCommitment()).isEqualTo(COMMITMENT);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx2 = (ClprCloseChannelTransaction) Transaction.fromBytes(new ClprCloseChannelTransaction().toBytes());
        assertThat(tx2.getChannelId()).isNull();
        assertThat(tx2.getOwnershipCommitment()).isNull();
    }

    @Test
    void commitmentIsOptional() {
        var tx = prepare(new ClprCloseChannelTransaction().setChannelId(CHANNEL_ID))
                .freeze();
        assertThat(tx.getOwnershipCommitment()).isNull();
    }

    @Test
    void requiresChannelId() {
        assertThatThrownBy(() -> prepare(new ClprCloseChannelTransaction().setOwnershipCommitment(COMMITMENT))
                        .freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprCloseChannelTransaction: 'channelId' must be explicitly set before freezing or executing.");
    }

    @Test
    void rejectsWrongLength() {
        assertThatThrownBy(() -> new ClprCloseChannelTransaction().setChannelId(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("channelId must be exactly 32 bytes");
    }

    @Test
    void frozenSetterThrows() {
        var tx = spawnTestTransaction();
        assertThatThrownBy(() -> tx.setChannelId(CHANNEL_ID)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotBeScheduled() {
        assertThatThrownBy(() -> new ClprCloseChannelTransaction()
                        .setChannelId(CHANNEL_ID)
                        .schedule())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
