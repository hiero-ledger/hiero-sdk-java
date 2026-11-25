// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * Ownership signature schemes supported by HIP-1535 CLPR.
 */
public enum ClprSignatureScheme {
    /**
     * ECDSA over secp256k1. The public key is the 64-byte uncompressed {@code x || y} point.
     */
    ECDSA_SECP256K1(com.hedera.hashgraph.sdk.proto.ClprSignatureScheme.ECDSA_SECP256K1),

    /**
     * Ed25519. The public key is the raw 32-byte key.
     */
    ED25519(com.hedera.hashgraph.sdk.proto.ClprSignatureScheme.ED25519);

    final com.hedera.hashgraph.sdk.proto.ClprSignatureScheme code;

    ClprSignatureScheme(com.hedera.hashgraph.sdk.proto.ClprSignatureScheme code) {
        this.code = code;
    }

    static ClprSignatureScheme fromProtobuf(com.hedera.hashgraph.sdk.proto.ClprSignatureScheme code) {
        return switch (code) {
            case ECDSA_SECP256K1 -> ECDSA_SECP256K1;
            case ED25519 -> ED25519;
            default -> throw new IllegalArgumentException("unrecognized CLPR signature scheme: " + code);
        };
    }

    com.hedera.hashgraph.sdk.proto.ClprSignatureScheme toProtobuf() {
        return code;
    }
}
