// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Arrays;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.hiero.sdk.proto.SignaturePair;

/**
 * Encapsulate the ED25519 public key.
 */
class PublicKeyED25519 extends PublicKey {
    private final byte[] keyData;

    /**
     * Constructor.
     *
     * @param keyData                   the byte array representing the key
     */
    private PublicKeyED25519(byte[] keyData) {
        this.keyData = keyData;
    }

    /**
     * Create a key from a byte array representation.
     *
     * @param publicKey                 the byte array representing the key
     * @return                          the new key
     */
    static PublicKeyED25519 fromBytesInternal(byte[] publicKey) {
        if (publicKey.length == Ed25519.PUBLIC_KEY_SIZE) {
            // Validate the key if it's not all zero public key, see HIP-540
            if (!Arrays.equals(publicKey, new byte[32])) {
                // Will throw if the key is invalid
                new Ed25519PublicKeyParameters(publicKey, 0);
            }
            // If this is a 32 byte string, assume an Ed25519 public key
            return new PublicKeyED25519(publicKey);
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
    static PublicKeyED25519 fromSubjectKeyInfoInternal(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        return new PublicKeyED25519(subjectPublicKeyInfo.getPublicKeyData().getBytes());
    }

    @Override
    ByteString extractSignatureFromProtobuf(SignaturePair pair) {
        return pair.getEd25519();
    }

    @Override
    public boolean verify(byte[] message, byte[] signature) {
        return Ed25519.verify(signature, 0, keyData, 0, message, 0, message.length);
    }

    @Override
    org.hiero.sdk.proto.Key toProtobufKey() {
        return org.hiero.sdk.proto.Key.newBuilder()
                .setEd25519(ByteString.copyFrom(keyData))
                .build();
    }

    @Override
    SignaturePair toSignaturePairProtobuf(byte[] signature) {
        return SignaturePair.newBuilder()
                .setPubKeyPrefix(ByteString.copyFrom(keyData))
                .setEd25519(ByteString.copyFrom(signature))
                .build();
    }

    @Override
    public byte[] toBytesDER() {
        try {
            return new SubjectPublicKeyInfo(new AlgorithmIdentifier(ID_ED25519), keyData).getEncoded("DER");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] toBytes() {
        return toBytesRaw();
    }

    @Override
    public byte[] toBytesRaw() {
        return keyData;
    }

    @Override
    public EvmAddress toEvmAddress() {
        throw new IllegalStateException("unsupported operation on Ed25519PublicKey");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PublicKeyED25519 publicKey = (PublicKeyED25519) o;
        return Arrays.equals(keyData, publicKey.keyData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(keyData);
    }

    @Override
    public boolean isED25519() {
        return true;
    }

    @Override
    public boolean isECDSA() {
        return false;
    }
}
