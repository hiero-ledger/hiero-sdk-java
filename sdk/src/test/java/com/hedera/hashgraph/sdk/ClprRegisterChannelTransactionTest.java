// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.ClprTestSupport.bytes32;
import static com.hedera.hashgraph.sdk.ClprTestSupport.prepare;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.ClprRegisterChannelTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ClprRegisterChannelTransactionTest {
    private static final byte[] COMMITMENT = bytes32(0x11);

    @BeforeAll
    static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    private ClprRegisterChannelTransaction spawnTestTransaction() {
        return prepare(new ClprRegisterChannelTransaction().setOwnershipCommitment(COMMITMENT))
                .freeze()
                .sign(ClprTestSupport.TEST_PRIVATE_KEY);
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2).isInstanceOf(ClprRegisterChannelTransaction.class);
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(((ClprRegisterChannelTransaction) tx2).getOwnershipCommitment())
                .isEqualTo(COMMITMENT);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ClprRegisterChannelTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(((ClprRegisterChannelTransaction) tx2).getOwnershipCommitment())
                .isNull();
    }

    @Test
    void constructFromTransactionBody() {
        var body = TransactionBody.newBuilder()
                .setClprRegisterChannel(ClprRegisterChannelTransactionBody.newBuilder()
                        .setOwnershipCommitment(ByteString.copyFrom(COMMITMENT)))
                .build();
        assertThat(new ClprRegisterChannelTransaction(body).getOwnershipCommitment())
                .isEqualTo(COMMITMENT);
    }

    @Test
    void copiesBytes() {
        var input = bytes32(1);
        var tx = new ClprRegisterChannelTransaction().setOwnershipCommitment(input);
        input[0] = 9;
        tx.getOwnershipCommitment()[1] = 9;
        assertThat(tx.getOwnershipCommitment()).isEqualTo(bytes32(1));
    }

    @Test
    void rejectsWrongLength() {
        assertThatThrownBy(() -> new ClprRegisterChannelTransaction().setOwnershipCommitment(new byte[31]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ownershipCommitment must be exactly 32 bytes");
    }

    @Test
    void requiresCommitmentToFreeze() {
        assertThatThrownBy(() -> prepare(new ClprRegisterChannelTransaction()).freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprRegisterChannelTransaction: 'ownershipCommitment' must be explicitly set before freezing or executing.");
    }

    @Test
    void frozenSetterThrows() {
        var tx = spawnTestTransaction();
        assertThatThrownBy(() -> tx.setOwnershipCommitment(COMMITMENT)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotBeScheduled() {
        var tx = new ClprRegisterChannelTransaction().setOwnershipCommitment(COMMITMENT);
        assertThatThrownBy(tx::schedule)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("ClprRegisterChannelTransaction cannot be scheduled");
    }
}
