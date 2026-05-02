// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hiero.sdk.proto.SignaturePair;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.bouncycastle.util.encoders.Hex;

/**
 * A public key on the Hedera™ network.
 */
public abstract class PublicKey extends Key {
    /**
     * Create a public key from a byte array.
     *
     * @param publicKey                 the byte array
     * @return                          the new public key
     */
    public static PublicKey fromBytes(byte[] publicKey) {
        if (publicKey.length == Ed25519.PUBLIC_KEY_SIZE) {
            // If this is a 32 byte string, assume an Ed25519 public key
            return PublicKeyED25519.fromBytesInternal(publicKey);
        } else if (publicKey.length == 33) {
            // compressed 33 byte raw form
            return PublicKeyECDSA.fromBytesInternal(publicKey);
        } else if (publicKey.length == 65) {
            // compress the 65 byte form
            return PublicKeyECDSA.fromBytesInternal(
                    Key.ECDSA_SECP256K1_CURVE.getCurve().decodePoint(publicKey).getEncoded(true));
        }

        // Assume a DER-encoded private key descriptor
        return fromBytesDER(publicKey);
    }

    /**
     * Create a public key from a DER encoded byte array.
     *
     * @param publicKey                 the DER encoded byte array
     * @return                          the new key
     */
    public static PublicKey fromBytesDER(byte[] publicKey) {
        return PublicKey.fromSubjectKeyInfo(SubjectPublicKeyInfo.getInstance(publicKey));
    }

    /**
     * Create a public key from a byte array.
     *
     * @param publicKey                 the byte array
     * @return                          the new key
     */
    public static PublicKey fromBytesED25519(byte[] publicKey) {
        return PublicKeyED25519.fromBytesInternal(publicKey);
    }

    /**
     * Create a public key from a byte array.
     *
     * @param publicKey                 the byte array
     * @return                          the new key
     */
    public static PublicKey fromBytesECDSA(byte[] publicKey) {
        return PublicKeyECDSA.fromBytesInternal(publicKey);
    }

    /**
     * Create a public key from a string.
     *
     * @param publicKey                 the string
     * @return                          the new key
     */
    public static PublicKey fromString(String publicKey) {
        return PublicKey.fromBytes(Hex.decode(publicKey));
    }

    /**
     * Create a public key from a string.
     *
     * @param publicKey                 the string
     * @return                          the new key
     */
    public static PublicKey fromStringED25519(String publicKey) {
        return fromBytesED25519(Hex.decode(publicKey));
    }

    /**
     * Create a public key from a string.
     *
     * @param publicKey                 the string
     * @return                          the new key
     */
    public static PublicKey fromStringECDSA(String publicKey) {
        return fromBytesECDSA(Hex.decode(publicKey));
    }

    /**
     * Create a public key from a string.
     *
     * @param publicKey                 the string
     * @return                          the new key
     */
    public static PublicKey fromStringDER(String publicKey) {
        return fromBytesDER(Hex.decode(publicKey));
    }

    /**
     * Create a public key from a subject public key info object.
     *
     * @param subjectPublicKeyInfo      the subject public key info object
     * @return                          the new key
     */
    private static PublicKey fromSubjectKeyInfo(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        if (subjectPublicKeyInfo.getAlgorithm().equals(new AlgorithmIdentifier(ID_ED25519))) {
            return PublicKeyED25519.fromSubjectKeyInfoInternal(subjectPublicKeyInfo);
        } else {
            // assume ECDSA
            return PublicKeyECDSA.fromSubjectKeyInfoInternal(subjectPublicKeyInfo);
        }
    }

    /**
     * The public key from an immutable byte string.
     *
     * @param aliasBytes                the immutable byte string
     * @return                          the key
     */
    @Nullable
    static PublicKey fromAliasBytes(ByteString aliasBytes) {
        if (!aliasBytes.isEmpty()) {
            try {
                var key = Key.fromProtobufKey(org.hiero.sdk.proto.Key.parseFrom(aliasBytes));
                return (key instanceof PublicKey) ? ((PublicKey) key) : null;
            } catch (InvalidProtocolBufferException ignored) {
            }
        }
        return null;
    }

    /**
     * Verify a signature on a message with this public key.
     *
     * @param message   The array of bytes representing the message
     * @param signature The array of bytes representing the signature
     * @return boolean
     */
    public abstract boolean verify(byte[] message, byte[] signature);

    /**
     * Get the signature from a signature pair protobuf.
     *
     * @param pair                      the protobuf
     * @return                          the signature
     */
    abstract ByteString extractSignatureFromProtobuf(SignaturePair pair);

    /**
     * Is the given transaction valid?
     *
     * @param transaction               the transaction
     * @return                          is it valid
     */
    public boolean verifyTransaction(Transaction<?> transaction) {
        if (!transaction.isFrozen()) {
            transaction.freeze();
        }

        for (var signedTransaction : transaction.innerSignedTransactions) {
            var found = false;

            for (var sigPair : signedTransaction.getSigMap().getSigPairList()) {
                if (sigPair.getPubKeyPrefix().equals(ByteString.copyFrom(toBytesRaw()))) {
                    found = true;

                    if (!verify(
                            signedTransaction.getBodyBytes().toByteArray(),
                            extractSignatureFromProtobuf(sigPair).toByteArray())) {
                        return false;
                    }
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Serialize this key as a SignaturePair protobuf object
     */
    abstract SignaturePair toSignaturePairProtobuf(byte[] signature);

    @Override
    public abstract byte[] toBytes();

    /**
     * Extract the DER represented as a byte array.
     *
     * @return                          the DER represented as a byte array
     */
    public abstract byte[] toBytesDER();

    /**
     * Extract the raw byte representation.
     *
     * @return                          the raw byte representation
     */
    public abstract byte[] toBytesRaw();

    @Override
    public String toString() {
        return toStringDER();
    }

    /**
     * Extract the DER encoded string.
     *
     * @return                          the DER encoded string
     */
    public String toStringDER() {
        return Hex.toHexString(toBytesDER());
    }

    /**
     * Extract the raw string.
     *
     * @return                          the raw string
     */
    public String toStringRaw() {
        return Hex.toHexString(toBytesRaw());
    }

    /**
     * Create a new account id.
     *
     * @param shard                     the shard part
     * @param realm                     the realm part
     * @return                          the new account id
     */
    public AccountId toAccountId(@Nonnegative long shard, @Nonnegative long realm) {
        return new AccountId(shard, realm, 0, null, this, null);
    }

    /**
     * Is this an ED25519 key?
     *
     * @return                          is this an ED25519 key
     */
    public abstract boolean isED25519();

    /**
     * Is this an ECDSA key?
     *
     * @return                          is this an ECDSA key
     */
    public abstract boolean isECDSA();

    /**
     * Converts the key to EVM address
     *
     * @return                          the EVM address
     */
    public abstract EvmAddress toEvmAddress();

    /**
     * Returns an "unusable" public key.
     * “Unusable” refers to a key such as an Ed25519 0x00000... public key,
     * since it is (presumably) impossible to find the 32-byte string whose SHA-512 hash begins with 32 bytes of zeros.
     *
     * @return The "unusable" key
     */
    public static PublicKey unusableKey() {
        return PublicKey.fromStringED25519("0000000000000000000000000000000000000000000000000000000000000000");
    }
}

