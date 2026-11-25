// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

/**
 * The mutable proof lifecycle shared by the CLPR completion transactions.
 * <p>
 * Transactions choose their bound fields and signing preimages. This helper stores only the immutable
 * proof, never the ownership private key.
 */
final class OwnershipProofState {
    private @Nullable OwnershipProof proof;

    /**
     * Clears the proof unless a bound value is set again to identical bytes.
     */
    void clearIfChanged(byte @Nullable [] previous, byte[] next) {
        if (previous == null || !Arrays.equals(previous, next)) {
            proof = null;
        }
    }

    void sign(PrivateKey privateKey, byte[] preimage) {
        proof = ClprOwnership.sign(privateKey, preimage);
    }

    /**
     * Restores wire proof fields. An absent proof stays null, even though its scheme defaults to ECDSA.
     */
    void restore(
            ByteString publicKey, ByteString signature, com.hedera.hashgraph.sdk.proto.ClprSignatureScheme scheme) {
        proof = publicKey.isEmpty() && signature.isEmpty()
                ? null
                : new OwnershipProof(
                        ClprSignatureScheme.fromProtobuf(scheme), publicKey.toByteArray(), signature.toByteArray());
    }

    byte @Nullable [] getPublicKey() {
        return proof == null ? null : proof.getPublicKey();
    }

    byte @Nullable [] getSignature() {
        return proof == null ? null : proof.getSignature();
    }

    @Nullable
    ClprSignatureScheme getSignatureScheme() {
        return proof == null ? null : proof.scheme;
    }

    boolean isComplete() {
        return proof != null && proof.isComplete();
    }

    void requireComplete(String transactionName) {
        if (!isComplete()) {
            throw new IllegalStateException(
                    transactionName + ": a complete ownership proof must be set before freezing or executing.");
        }
    }
}
