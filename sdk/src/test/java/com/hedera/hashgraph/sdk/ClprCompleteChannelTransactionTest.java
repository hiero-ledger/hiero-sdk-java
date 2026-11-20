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

class ClprCompleteChannelTransactionTest {
    private static final byte[] CHANNEL_ID = bytes32(0x01);
    private static final PrivateKey OWNERSHIP_KEY =
            PrivateKey.fromStringED25519("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60");
    private static final ContractId VERIFIER = ContractId.fromString("0.0.777");

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

    private ClprCompleteChannelTransaction spawnTestTransaction() {
        return prepare(new ClprCompleteChannelTransaction()
                        .setChannelId(CHANNEL_ID)
                        .setVerifierContract(VERIFIER)
                        .setConfigProofBytes(new byte[] {1, 2})
                        .setEndpointManifestProofBytes(new byte[] {3, 4})
                        .setOwnershipProof(OWNERSHIP_KEY))
                .freeze()
                .sign(ClprTestSupport.TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = (ClprCompleteChannelTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(tx2.getChannelId()).isEqualTo(CHANNEL_ID);
        assertThat(tx2.getVerifierContract()).isEqualTo(VERIFIER);
        assertThat(tx2.getConfigProofBytes()).containsExactly(1, 2);
        assertThat(tx2.getEndpointManifestProofBytes()).containsExactly(3, 4);
        assertThat(tx2.getPublicKey()).isEqualTo(tx.getPublicKey());
        assertThat(tx2.getSignature()).isEqualTo(tx.getSignature());
        assertThat(tx2.getSignatureScheme()).isEqualTo(ClprSignatureScheme.ED25519);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ClprCompleteChannelTransaction();
        var tx2 = (ClprCompleteChannelTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.getChannelId()).isNull();
        assertThat(tx2.getSignatureScheme()).isNull();
        assertThat(tx2.getPublicKey()).isNull();
    }

    @Test
    void ecdsaProofUsesSchemeZero() {
        var tx = new ClprCompleteChannelTransaction()
                .setChannelId(CHANNEL_ID)
                .setOwnershipProof(PrivateKey.generateECDSA());
        assertThat(tx.getSignatureScheme()).isEqualTo(ClprSignatureScheme.ECDSA_SECP256K1);
        assertThat(tx.getPublicKey()).hasSize(64);
        assertThat(tx.getSignature()).hasSize(64);
    }

    @Test
    void ownershipProofRequiresChannelId() {
        assertThatThrownBy(() -> new ClprCompleteChannelTransaction().setOwnershipProof(OWNERSHIP_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprCompleteChannelTransaction: 'channelId' must be explicitly set before calling setOwnershipProof().");
    }

    @Test
    void changingChannelIdClearsProof() {
        var tx = new ClprCompleteChannelTransaction().setChannelId(CHANNEL_ID).setOwnershipProof(OWNERSHIP_KEY);
        tx.setChannelId(bytes32(0x01));
        assertThat(tx.getSignature()).isNotNull();
        tx.setChannelId(bytes32(0x02));
        assertThat(tx.getSignature()).isNull();
        assertThat(tx.getPublicKey()).isNull();
        assertThat(tx.getSignatureScheme()).isNull();
    }

    @Test
    void requiresProofToFreeze() {
        assertThatThrownBy(() -> prepare(new ClprCompleteChannelTransaction().setChannelId(CHANNEL_ID))
                        .freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprCompleteChannelTransaction: a complete ownership proof must be set before freezing or executing.");
    }

    @Test
    void verifierAndProofBytesAreOptional() {
        var tx = prepare(new ClprCompleteChannelTransaction()
                        .setChannelId(CHANNEL_ID)
                        .setOwnershipProof(OWNERSHIP_KEY))
                .freeze();
        assertThat(tx.getVerifierContract()).isNull();
        assertThat(tx.getConfigProofBytes()).isEmpty();
    }

    @Test
    void frozenSetterThrows() {
        var tx = spawnTestTransaction();
        assertThatThrownBy(() -> tx.setOwnershipProof(OWNERSHIP_KEY)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotBeScheduled() {
        assertThatThrownBy(() -> new ClprCompleteChannelTransaction().schedule())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
