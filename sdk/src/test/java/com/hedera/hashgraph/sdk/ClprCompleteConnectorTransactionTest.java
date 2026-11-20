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

class ClprCompleteConnectorTransactionTest {
    private static final byte[] CHANNEL_ID = bytes32(0x01);
    private static final byte[] SALT = bytes32(0x02);
    private static final PrivateKey OWNERSHIP_KEY =
            PrivateKey.fromStringED25519("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60");
    private static final byte[] CONNECTOR_ID =
            ClprRegisterConnectorTransaction.computeConnectorId(CHANNEL_ID, OWNERSHIP_KEY.getPublicKey(), SALT);
    private static final ContractId CONNECTOR_CONTRACT = ContractId.fromString("0.0.888");

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

    private static ClprCompleteConnectorTransaction unsigned() {
        var adminKeys = new KeyList();
        adminKeys.add(ClprTestSupport.TEST_PRIVATE_KEY.getPublicKey());
        return new ClprCompleteConnectorTransaction()
                .setChannelId(CHANNEL_ID)
                .setConnectorId(CONNECTOR_ID)
                .setSalt(SALT)
                .setConnectorContract(CONNECTOR_CONTRACT)
                .setAdminKey(adminKeys)
                .setLockedStake(Hbar.fromTinybars(500));
    }

    private ClprCompleteConnectorTransaction spawnTestTransaction() {
        return prepare(unsigned().setOwnershipProof(OWNERSHIP_KEY)).freeze().sign(ClprTestSupport.TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = (ClprCompleteConnectorTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
        assertThat(tx2.getChannelId()).isEqualTo(CHANNEL_ID);
        assertThat(tx2.getConnectorId()).isEqualTo(CONNECTOR_ID);
        assertThat(tx2.getSalt()).isEqualTo(SALT);
        assertThat(tx2.getConnectorContract()).isEqualTo(CONNECTOR_CONTRACT);
        assertThat(tx2.getAdminKey()).isInstanceOf(KeyList.class);
        assertThat(tx2.getLockedStake()).isEqualTo(Hbar.fromTinybars(500));
        assertThat(tx2.getSignature()).isEqualTo(tx.getSignature());
        assertThat(tx2.getSignatureScheme()).isEqualTo(ClprSignatureScheme.ED25519);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new ClprCompleteConnectorTransaction();
        var tx2 = (ClprCompleteConnectorTransaction) Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.getLockedStake()).isEqualTo(Hbar.ZERO);
        assertThat(tx2.getAdminKey()).isNull();
        assertThat(tx2.getSignatureScheme()).isNull();
    }

    @Test
    void rejectsMismatchedConnectorId() {
        var tx = unsigned().setConnectorId(bytes32(0x03));
        assertThatThrownBy(() -> tx.setOwnershipProof(OWNERSHIP_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "ClprCompleteConnectorTransaction: 'connectorId' does not match channelId, ownership public key and salt.");
        assertThat(tx.getSignature()).isNull();
    }

    @Test
    void ownershipProofRequiresSalt() {
        var tx = new ClprCompleteConnectorTransaction().setChannelId(CHANNEL_ID).setConnectorId(CONNECTOR_ID);
        assertThatThrownBy(() -> tx.setOwnershipProof(OWNERSHIP_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprCompleteConnectorTransaction: 'salt' must be explicitly set before calling setOwnershipProof().");
    }

    @Test
    void changingBoundFieldsClearsProof() {
        var tx = unsigned().setOwnershipProof(OWNERSHIP_KEY);
        tx.setSalt(SALT.clone()).setChannelId(CHANNEL_ID.clone()).setConnectorId(CONNECTOR_ID.clone());
        assertThat(tx.getSignature()).isNotNull();
        tx.setSalt(bytes32(0x09));
        assertThat(tx.getSignature()).isNull();

        tx = unsigned().setOwnershipProof(OWNERSHIP_KEY).setConnectorId(bytes32(0x09));
        assertThat(tx.getSignature()).isNull();

        tx = unsigned().setOwnershipProof(OWNERSHIP_KEY).setChannelId(bytes32(0x09));
        assertThat(tx.getSignature()).isNull();
    }

    @Test
    void requiresAdminKey() {
        var tx = prepare(new ClprCompleteConnectorTransaction()
                .setChannelId(CHANNEL_ID)
                .setConnectorId(CONNECTOR_ID)
                .setSalt(SALT)
                .setOwnershipProof(OWNERSHIP_KEY));
        assertThatThrownBy(tx::freeze)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprCompleteConnectorTransaction: 'adminKey' must be explicitly set before freezing or executing.");
    }

    @Test
    void requiresProof() {
        assertThatThrownBy(() -> prepare(unsigned()).freeze())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "ClprCompleteConnectorTransaction: a complete ownership proof must be set before freezing or executing.");
    }

    @Test
    void rejectsNegativeStake() {
        assertThatThrownBy(() -> new ClprCompleteConnectorTransaction().setLockedStake(Hbar.fromTinybars(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "ClprCompleteConnectorTransaction: 'lockedStake' must be an integer in 0..2^63-1 tinybars.");
    }

    @Test
    void frozenSetterThrows() {
        var tx = spawnTestTransaction();
        assertThatThrownBy(() -> tx.setLockedStake(Hbar.ZERO)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotBeScheduled() {
        assertThatThrownBy(() -> new ClprCompleteConnectorTransaction().schedule())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
