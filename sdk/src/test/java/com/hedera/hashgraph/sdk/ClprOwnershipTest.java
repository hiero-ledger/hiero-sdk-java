// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.stream.Stream;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * HIP-1535 ownership test vectors.
 * <p>
 * PUBLIC TEST KEYS ONLY. Never use these keys for accounts or funds.
 * Source: hiero-ledger/sdk-collaboration-hub, proposals/hips/hip-1535.md,
 * ref d04df37ca47fe67efc11fe2838b51f22f55cfe8e, "Ownership test vectors".
 */
class ClprOwnershipTest {
    private static final byte[] CHANNEL_ID =
            Hex.decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
    private static final byte[] SALT = Hex.decode("202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f");

    record Vector(
            ClprSignatureScheme scheme,
            PrivateKey privateKey,
            String publicKey,
            String channelCommitment,
            String channelDigest,
            String channelSignature,
            String connectorId,
            String connectorCommitment,
            String connectorDigest,
            String connectorSignature) {}

    static final Vector ECDSA = new Vector(
            ClprSignatureScheme.ECDSA_SECP256K1,
            PrivateKey.fromStringECDSA("0000000000000000000000000000000000000000000000000000000000000001"),
            "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8",
            "f476ca287a171dd8aa94165fb27fadee1fc61f051840382aed326e1fd41d41b3",
            "8ae1aa597fa146ebd3aa2ceddf360668dea5e526567e92b0321816a4e895bd2d",
            "cd52448f226a599c8d61d568a6186d50bb263a2559e08c1c8389233e632d2a5839232b91d7b8974b862ff5540bae836c08792936ebd708682413e0c8e2884086",
            "532a8331032028874f4d7fa1953636f8b076876c493606f93a86d8880058b4a6",
            "647e3f78cfdbceb3714e6c4d043f60e3169cabae3d90be168aeb58796ed6533d",
            "ba76a200dbda978d3be9120def504046d2d378bc85c8da3ac8a1c80710faee01",
            "4b133aadf1d8a8c8bc6509842e245e01f71639e6e09fa2ed8ddf315faa38926f29e87b0cf850345f9cf74e82de367c7e31c96dfde6cb4603afb42750a551197d");

    static final Vector ED25519 = new Vector(
            ClprSignatureScheme.ED25519,
            PrivateKey.fromStringED25519("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60"),
            "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
            "4816bb408545af511a22953026b40500890c7fa0a44c83682e251b114a0b6496",
            "8ae1aa597fa146ebd3aa2ceddf360668dea5e526567e92b0321816a4e895bd2d",
            "8ef8546b7a464125e9bda37b52203c8a8f1c59c51deae85c6178632b4050141226d6018ed52fcf8afd2a4c141a04109220eb8c7cd9d923a06a2fff23155cab0f",
            "488d7b7fdcc061f1de9fe98666c1337f0fc90275889f2450d59e1e6e837e74b1",
            "a36b6836b720019a9fb1846df6bb4b76d13a1f276da0ebc2371b4d0eb8e30618",
            "e4b51c24e102cf97db8b2c5320d5b2664693789b2de186a4e8cd67c2f2cb0fca",
            "7374599d239cc8300e8f8f99057656a0c4b3e42f7fbaa99ab8cd3907da4d9fbe1ef28f12b7af9cd3f126fa513e3d6661b24fe790c1bd8a80275e8eaf69f1c70f");

    static Stream<Arguments> vectors() {
        return Stream.of(Arguments.of(ECDSA), Arguments.of(ED25519));
    }

    private static byte[] connectorPreimage(byte[] connectorId) {
        var preimage = Arrays.copyOf(connectorId, 52);
        System.arraycopy(ClprOwnership.CLPR_SERVICE_ADDRESS, 0, preimage, 32, 20);
        return preimage;
    }

    @ParameterizedTest
    @MethodSource("vectors")
    void encodesCanonicalPublicKey(Vector vector) {
        var publicKey = vector.privateKey().getPublicKey();
        assertThat(Hex.toHexString(ClprOwnership.encodePublicKey(publicKey))).isEqualTo(vector.publicKey());
        assertThat(ClprOwnership.schemeOf(publicKey)).isEqualTo(vector.scheme());
    }

    @ParameterizedTest
    @MethodSource("vectors")
    void computesChannelCommitment(Vector vector) {
        var commitment = ClprRegisterChannelTransaction.computeOwnershipCommitment(
                CHANNEL_ID, vector.privateKey().getPublicKey());
        assertThat(Hex.toHexString(commitment)).isEqualTo(vector.channelCommitment());
    }

    @ParameterizedTest
    @MethodSource("vectors")
    void computesConnectorIdAndCommitment(Vector vector) {
        var publicKey = vector.privateKey().getPublicKey();
        var connectorId = ClprRegisterConnectorTransaction.computeConnectorId(CHANNEL_ID, publicKey, SALT);
        assertThat(Hex.toHexString(connectorId)).isEqualTo(vector.connectorId());
        assertThat(Hex.toHexString(ClprRegisterConnectorTransaction.computeOwnershipCommitment(connectorId, publicKey)))
                .isEqualTo(vector.connectorCommitment());
    }

    @ParameterizedTest
    @MethodSource("vectors")
    void computesDigests(Vector vector) {
        assertThat(Hex.toHexString(ClprOwnership.hash(CHANNEL_ID))).isEqualTo(vector.channelDigest());
        assertThat(Hex.toHexString(ClprOwnership.hash(connectorPreimage(Hex.decode(vector.connectorId())))))
                .isEqualTo(vector.connectorDigest());
    }

    @ParameterizedTest
    @MethodSource("vectors")
    void signsChannelAndConnectorPreimages(Vector vector) {
        var channelProof = ClprOwnership.sign(vector.privateKey(), CHANNEL_ID);
        assertThat(channelProof.scheme).isEqualTo(vector.scheme());
        assertThat(Hex.toHexString(channelProof.getPublicKey())).isEqualTo(vector.publicKey());
        assertThat(Hex.toHexString(channelProof.getSignature())).isEqualTo(vector.channelSignature());

        var connectorProof =
                ClprOwnership.sign(vector.privateKey(), connectorPreimage(Hex.decode(vector.connectorId())));
        assertThat(Hex.toHexString(connectorProof.getSignature())).isEqualTo(vector.connectorSignature());
    }

    @ParameterizedTest
    @MethodSource("vectors")
    void completionTransactionsProduceVectorSignatures(Vector vector) {
        var channel =
                new ClprCompleteChannelTransaction().setChannelId(CHANNEL_ID).setOwnershipProof(vector.privateKey());
        assertThat(Hex.toHexString(channel.getSignature())).isEqualTo(vector.channelSignature());
        assertThat(Hex.toHexString(channel.getPublicKey())).isEqualTo(vector.publicKey());
        assertThat(channel.getSignatureScheme()).isEqualTo(vector.scheme());

        var connector = new ClprCompleteConnectorTransaction()
                .setChannelId(CHANNEL_ID)
                .setConnectorId(Hex.decode(vector.connectorId()))
                .setSalt(SALT)
                .setOwnershipProof(vector.privateKey());
        assertThat(Hex.toHexString(connector.getSignature())).isEqualTo(vector.connectorSignature());
    }

    @Test
    void serviceAddressMatchesVectors() {
        assertThat(Hex.toHexString(ClprOwnership.CLPR_SERVICE_ADDRESS))
                .isEqualTo("000000000000000000000000000000000000016e");
    }

    @Test
    void rejectsWrongLengthInputs() {
        var publicKey = ED25519.privateKey().getPublicKey();
        assertThatThrownBy(() -> ClprRegisterChannelTransaction.computeOwnershipCommitment(new byte[31], publicKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("channelId must be exactly 32 bytes");
        assertThatThrownBy(
                        () -> ClprRegisterConnectorTransaction.computeConnectorId(CHANNEL_ID, publicKey, new byte[33]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("salt must be exactly 32 bytes");
        assertThatThrownBy(() -> ClprRegisterConnectorTransaction.computeOwnershipCommitment(null, publicKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("connectorId must be exactly 32 bytes");
    }

    @Test
    void uncompressedPublicKeyBytes() {
        var ecdsa = ECDSA.privateKey().getPublicKey();
        assertThat(Hex.toHexString(ecdsa.toBytesRawUncompressed())).isEqualTo(ECDSA.publicKey());
        assertThat(ecdsa.toEvmAddress().toString()).isEqualTo("7e5f4552091a69125d5dfcb7b8c2659029395bdf");
        assertThatThrownBy(() -> ED25519.privateKey().getPublicKey().toBytesRawUncompressed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Uncompressed coordinates are not supported for Ed25519 keys");
    }
}
