// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.ClprTestSupport.prepare;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ClprUpdateLedgerConfigurationTransactionTest {
    private static final ClprThrottles THROTTLES = ClprThrottles.of(10, 4096, 1_000_000, 100, 1 << 20, 0, 5);

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

    private ClprUpdateLedgerConfigurationTransaction spawnTestTransaction() {
        return prepare(new ClprUpdateLedgerConfigurationTransaction().setThrottles(THROTTLES))
                .freeze()
                .sign(ClprTestSupport.TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = (ClprUpdateLedgerConfigurationTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(tx2.getThrottles()).isEqualTo(THROTTLES);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ClprUpdateLedgerConfigurationTransaction();
        var tx2 = (ClprUpdateLedgerConfigurationTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.getThrottles()).isNull();
    }

    @Test
    void bodyContainsOnlyThrottles() {
        var body = new ClprUpdateLedgerConfigurationTransaction()
                .setThrottles(THROTTLES)
                .build()
                .build();
        assertThat(body.getConfiguration().getChainId()).isEmpty();
        assertThat(body.getConfiguration().hasTimestamp()).isFalse();
        assertThat(body.getConfiguration().getThrottles()).isEqualTo(THROTTLES.toProtobuf());
    }

    @Test
    void requiresThrottles() {
        assertThatThrownBy(() ->
                        prepare(new ClprUpdateLedgerConfigurationTransaction()).freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprUpdateLedgerConfigurationTransaction: 'throttles' must be explicitly set before freezing or executing.");
    }

    @Test
    void frozenSetterThrows() {
        var tx = spawnTestTransaction();
        assertThatThrownBy(() -> tx.setThrottles(THROTTLES)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotBeScheduled() {
        assertThatThrownBy(() -> new ClprUpdateLedgerConfigurationTransaction().schedule())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
