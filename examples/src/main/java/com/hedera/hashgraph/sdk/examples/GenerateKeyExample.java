// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.EvmAddress;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;

/**
 * How to generate an ECDSA (secp256k1) key pair and derive the EVM address from the public key.
 *
 * <p>ECDSA keys with an EVM address derived from the public key are the recommended choice for new
 * Hedera accounts when you want compatibility with Ethereum tooling (wallets, Hardhat, ethers.js,
 * etc.). For legacy Hedera-only flows you can still use {@link PrivateKey#generateED25519()}.
 */
class GenerateKeyExample {

    public static void main(String[] args) {
        System.out.println("Generate ECDSA key pair and EVM address example start");

        System.out.println("Generating an ECDSA (secp256k1) private key...");
        PrivateKey privateKey = PrivateKey.generateECDSA();
        System.out.println("Private key: " + privateKey);

        System.out.println("Deriving the public key from the private key");
        PublicKey publicKey = privateKey.getPublicKey();
        System.out.println("Public key: " + publicKey);

        System.out.println("Deriving the EVM address (last 20 bytes of Keccak-256 of the uncompressed public key)");
        EvmAddress evmAddress = publicKey.toEvmAddress();
        System.out.println("EVM address: 0x" + evmAddress);

        System.out.println("Generate ECDSA key pair and EVM address example complete");
    }
}
