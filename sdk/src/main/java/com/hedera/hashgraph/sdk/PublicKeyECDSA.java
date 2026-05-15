// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.Crypto.calcKeccak256;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.SignaturePair;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;

/**
 * Encapsulate the ECDSA public key.
 */
class PublicKeyECDSA extends PublicKey {
    // Compressed 33 byte form
    private byte[] keyData;

    /**
     * Constructor.
     *
     * @param keyData                   the byte array representing the key
     */
    private PublicKeyECDSA(byte[] keyData) {
        this.keyData = keyData;
    }

    /**
     * Create a key from a byte array representation.
     *
     * @param publicKey                 the byte array representing the key
     * @return                          the new key
     */
    static PublicKeyECDSA fromBytesInternal(byte[] publicKey) {
        // Validate the key if it's not all zero public key, see HIP-540
        if (Arrays.equals(publicKey, new byte[33])) {
            return new PublicKeyECDSA(publicKey);
        }
        if (publicKey.length == 33 || publicKey.length == 65) {
            return new PublicKeyECDSA(
                    // compress and validate the key
                    Key.ECDSA_SECP256K1_CURVE.getCurve().decodePoint(publicKey).getEncoded(true));
        }

        // Assume a DER-encoded public key descriptor
        return fromSubjectKeyInfoInternal(SubjectPublicKeyInfo.getInstance(publicKey));
    }

    /**
     * Create a key from a subject public key info object.
     *
     * @param subjectPublicKeyInfo      the subject public key info object
     * @return                          the new public key
     */
    static PublicKeyECDSA fromSubjectKeyInfoInternal(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        return fromBytesInternal(subjectPublicKeyInfo.getPublicKeyData().getBytes());
    }

    @Override
    ByteString extractSignatureFromProtobuf(SignaturePair pair) {
        return pair.getECDSASecp256K1();
    }

    @Override
    public boolean verify(byte[] message, byte[] signature) {
        var hash = calcKeccak256(message);

        ECDSASigner signer = new ECDSASigner();
        signer.init(
                false,
                new ECPublicKeyParameters(
                        Key.ECDSA_SECP256K1_CURVE.getCurve().decodePoint(keyData), Key.ECDSA_SECP256K1_DOMAIN));

        BigInteger r = new BigInteger(1, Arrays.copyOf(signature, 32));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(signature, 32, 64));

        return signer.verifySignature(hash, r, s);
    }

    @Override
    com.hedera.hashgraph.sdk.proto.Key toProtobufKey() {
        return com.hedera.hashgraph.sdk.proto.Key.newBuilder()
                .setECDSASecp256K1(ByteString.copyFrom(keyData))
                .build();
    }

    @Override
    SignaturePair toSignaturePairProtobuf(byte[] signature) {
        return SignaturePair.newBuilder()
                .setPubKeyPrefix(ByteString.copyFrom(keyData))
                .setECDSASecp256K1(ByteString.copyFrom(signature))
                .build();
    }

    @Override
    public byte[] toBytesDER() {
        try {
            return new SubjectPublicKeyInfo(new AlgorithmIdentifier(ID_EC_PUBLIC_KEY, ID_ECDSA_SECP256K1), keyData)
                    .getEncoded("DER");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] toBytes() {
        return toBytesDER();
    }

    @Override
    public byte[] toBytesRaw() {
        return Arrays.copyOf(keyData, keyData.length);
    }

    @Override
    public EvmAddress toEvmAddress() {
        // Calculate the Keccak-256 hash of the uncompressed key without "04" prefix
        byte[] uncompressed =
                Key.ECDSA_SECP256K1_CURVE.getCurve().decodePoint(toBytesRaw()).getEncoded(false);
        byte[] keccakBytes = calcKeccak256(Arrays.copyOfRange(uncompressed, 1, uncompressed.length));

        // Return the last 20 bytes
        return EvmAddress.fromBytes(Arrays.copyOfRange(keccakBytes, 12, 32));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PublicKeyECDSA publicKey = (PublicKeyECDSA) o;
        return Arrays.equals(keyData, publicKey.keyData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(keyData);
    }

    @Override
    public boolean isED25519() {
        return false;
    }

    @Override
    public boolean isECDSA() {
        return true;
    }
}
