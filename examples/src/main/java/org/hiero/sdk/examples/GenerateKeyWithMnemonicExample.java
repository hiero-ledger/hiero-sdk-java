// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.Mnemonic;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.PublicKey;

/**
 * How to generate ED25519 key with mnemonic phrase.
 */
class GenerateKeyWithMnemonicExample {

    public static void main(String[] args) {
        System.out.println("Generate ED25519 Key With Mnemonic Phrase Example Start!");

        System.out.println("Generating random 24-word mnemonic from the BIP-39 standard English word list...");
        Mnemonic mnemonic24 = Mnemonic.generate24();
        System.out.println("Generated 24-word mnemonic: " + mnemonic24);

        System.out.println("Recovering an ED25519 private key from the 24-word mnemonic phrase above...");
        PrivateKey privateKey24 = mnemonic24.toStandardEd25519PrivateKey("", 0);
        System.out.println("Recovered ED25519 private key: " + privateKey24);

        System.out.println("Deriving a public key from the above private key...");
        PublicKey publicKey24 = privateKey24.getPublicKey();
        System.out.println("Public key: " + publicKey24);

        System.out.println("---");

        System.out.println("Generating random 12-word mnemonic from the BIP-39 standard English word list...");
        Mnemonic mnemonic12 = Mnemonic.generate12();
        System.out.println("Generated 12-word mnemonic: " + mnemonic12);

        System.out.println("Recovering an ED25519 private key from the 12-word mnemonic phrase above...");
        PrivateKey privateKey12 = mnemonic12.toStandardEd25519PrivateKey("", 0);
        System.out.println("Recovered ED25519 private key: " + privateKey12);

        System.out.println("Deriving a public key from the above private key...");
        PublicKey publicKey12 = privateKey12.getPublicKey();
        System.out.println("Public key: " + publicKey12);

        System.out.println("Generate ED25519 Key With Mnemonic Phrase Example Complete!");
    }
}

