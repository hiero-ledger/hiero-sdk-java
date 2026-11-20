// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;

/**
 * HIP-1535 ownership commitment and proof helpers shared by the CLPR transactions.
 */
final class ClprOwnership {
    /**
     * The CLPR system contract address used in Connector proof preimages.
     */
    static final byte[] CLPR_SERVICE_ADDRESS = Hex.decode("000000000000000000000000000000000000016e");

    private ClprOwnership() {}

    /**
     * Validates that a field is exactly 32 bytes and returns a copy.
     */
    static byte[] requireBytes32(byte[] value, String fieldName) {
        if (value == null || value.length != 32) {
            throw new IllegalArgumentException(fieldName + " must be exactly 32 bytes");
        }
        return value.clone();
    }

    /**
     * Canonical ownership key encoding: 64-byte uncompressed {@code x || y} for ECDSA secp256k1,
     * or the raw 32 bytes for Ed25519.
     */
    static byte[] encodePublicKey(PublicKey publicKey) {
        Objects.requireNonNull(publicKey, "ownership key must not be null");
        return publicKey.isECDSA() ? publicKey.toBytesRawUncompressed() : publicKey.toBytesRaw();
    }

    static ClprSignatureScheme schemeOf(PublicKey publicKey) {
        Objects.requireNonNull(publicKey, "ownership key must not be null");
        return publicKey.isECDSA() ? ClprSignatureScheme.ECDSA_SECP256K1 : ClprSignatureScheme.ED25519;
    }

    static byte[] hash(byte[]... parts) {
        int length = 0;
        for (var part : parts) {
            length += part.length;
        }
        var preimage = new byte[length];
        int offset = 0;
        for (var part : parts) {
            System.arraycopy(part, 0, preimage, offset, part.length);
            offset += part.length;
        }
        return Crypto.calcKeccak256(preimage);
    }

    /**
     * {@code keccak256(channelId || publicKey)}.
     */
    static byte[] computeChannelCommitment(byte[] channelId, PublicKey publicKey) {
        return hash(requireBytes32(channelId, "channelId"), encodePublicKey(publicKey));
    }

    /**
     * {@code keccak256(channelId || publicKey || salt)}.
     */
    static byte[] computeConnectorId(byte[] channelId, PublicKey publicKey, byte[] salt) {
        return hash(requireBytes32(channelId, "channelId"), encodePublicKey(publicKey), requireBytes32(salt, "salt"));
    }

    /**
     * {@code keccak256(connectorId || publicKey)}.
     */
    static byte[] computeConnectorCommitment(byte[] connectorId, PublicKey publicKey) {
        return hash(requireBytes32(connectorId, "connectorId"), encodePublicKey(publicKey));
    }

    /**
     * Signs a CLPR ownership preimage without retaining the private key.
     * <p>
     * ECDSA signing hashes the message with keccak256 internally and is normalized to low-S.
     * Ed25519 signs the keccak256 digest.
     */
    static OwnershipProof sign(PrivateKey privateKey, byte[] preimage) {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        var publicKey = privateKey.getPublicKey();
        var scheme = schemeOf(publicKey);
        if (scheme == ClprSignatureScheme.ED25519) {
            return new OwnershipProof(scheme, encodePublicKey(publicKey), privateKey.sign(hash(preimage)));
        }
        return new OwnershipProof(scheme, encodePublicKey(publicKey), toLowS(privateKey.sign(preimage)));
    }

    /**
     * Normalizes a 64-byte {@code r || s} secp256k1 signature to canonical low-S form, as HIP-1535 requires.
     */
    static byte[] toLowS(byte[] signature) {
        var n = Key.ECDSA_SECP256K1_CURVE.getN();
        var s = new BigInteger(1, Arrays.copyOfRange(signature, 32, 64));
        if (s.compareTo(n.shiftRight(1)) <= 0) {
            return signature;
        }
        var lowS = n.subtract(s).toByteArray();
        var normalized = Arrays.copyOf(signature, 64);
        Arrays.fill(normalized, 32, 64, (byte) 0);
        int length = Math.min(lowS.length, 32);
        System.arraycopy(lowS, lowS.length - length, normalized, 64 - length, length);
        return normalized;
    }
}
