// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * An immutable CLPR ownership proof. It never contains a private key.
 */
final class OwnershipProof {
    final ClprSignatureScheme scheme;
    private final byte[] publicKey;
    private final byte[] signature;

    OwnershipProof(ClprSignatureScheme scheme, byte[] publicKey, byte[] signature) {
        this.scheme = scheme;
        this.publicKey = publicKey.clone();
        this.signature = signature.clone();
    }

    byte[] getPublicKey() {
        return publicKey.clone();
    }

    byte[] getSignature() {
        return signature.clone();
    }

    boolean isComplete() {
        return publicKey.length > 0 && signature.length > 0;
    }
}
