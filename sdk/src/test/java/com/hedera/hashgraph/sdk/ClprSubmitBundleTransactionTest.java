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

class ClprSubmitBundleTransactionTest {
    private static final byte[] CHANNEL_ID = bytes32(0x01);
    private static final byte[] PAYLOAD = {1, 2, 3, 4, 5};

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

    private ClprSubmitBundleTransaction spawnTestTransaction() {
        return prepare(new ClprSubmitBundleTransaction()
                        .setChannelId(CHANNEL_ID)
                        .setBundlePayload(PAYLOAD))
                .freeze()
                .sign(ClprTestSupport.TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = (ClprSubmitBundleTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(tx2.getChannelId()).isEqualTo(CHANNEL_ID);
        assertThat(tx2.getBundlePayload()).isEqualTo(PAYLOAD);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ClprSubmitBundleTransaction();
        assertThat(Transaction.fromBytes(tx.toBytes()).toString()).isEqualTo(tx.toString());
    }

    @Test
    void copiesPayload() {
        var payload = PAYLOAD.clone();
        var tx = new ClprSubmitBundleTransaction().setBundlePayload(payload);
        payload[0] = 9;
        tx.getBundlePayload()[1] = 9;
        assertThat(tx.getBundlePayload()).isEqualTo(PAYLOAD);
    }

    @Test
    void emptyPayloadIsAllowed() {
        var tx = prepare(new ClprSubmitBundleTransaction()
                        .setChannelId(CHANNEL_ID)
                        .setBundlePayload(new byte[0]))
                .freeze();
        assertThat(tx.getBundlePayload()).isEmpty();
    }

    @Test
    void requiresChannelId() {
        assertThatThrownBy(() -> prepare(new ClprSubmitBundleTransaction().setBundlePayload(PAYLOAD))
                        .freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprSubmitBundleTransaction: 'channelId' must be explicitly set before freezing or executing.");
    }

    @Test
    void frozenSetterThrows() {
        var tx = spawnTestTransaction();
        assertThatThrownBy(() -> tx.setBundlePayload(PAYLOAD)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotBeScheduled() {
        assertThatThrownBy(() -> new ClprSubmitBundleTransaction()
                        .setChannelId(CHANNEL_ID)
                        .schedule())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
