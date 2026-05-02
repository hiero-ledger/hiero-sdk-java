// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Arrays;
import java.util.Objects;

/**
 * How to set and update token's metadata.
 * <p>
 * HIP-646: Fungible Token Metadata Field.
 * Addition of the metadata field to Fungible Tokens (FT),
 * taking after the Non-Fungible Token (NFT) metadata field which was added in HIP-17.
 * <p>
 * HIP-765: NFT Collection Token Metadata Field
 * Addition of the metadata field to Non-Fungible Token Class,
 * taking after the individual Non-Fungible Token (NFT) metadata field, which was added in HIP-17.
 */
public class TokenMetadataExample {

    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * Operator's account ID.
     * Used to sign and pay for operations on Hedera.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file.
     * Network can be: localhost, testnet, previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file.
     * Log levels can be: TRACE, DEBUG, INFO, WARN, ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL,
     * for example via VM options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        System.out.println("Token Metadata (HIP-646 and HIP-765) Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Generate ED25519 key pairs.
         */
        PrivateKey adminPrivateKey = PrivateKey.generateED25519();
        PublicKey adminPublicKey = adminPrivateKey.getPublicKey();

        PrivateKey metadataPrivateKey = PrivateKey.generateED25519();
        PublicKey metadataPublicKey = metadataPrivateKey.getPublicKey();

        /*
         * Step 2:
         * The beginning of the first example (mutable token's metadata).
         *
         * Create a mutable fungible token with a metadata, but without a Metadata Key.
         */
        System.out.println("The beginning of the first example (mutable token's metadata).");
        byte[] initialTokenMetadata = new byte[] {1, 1, 1, 1, 1};

        System.out.println("Creating mutable Fungible Token using the Hedera Token Service...");
        var mutableFungibleTokenId = new TokenCreateTransaction()
                .setTokenName("HIP-646 Mutable FT")
                .setTokenSymbol("HIP646MFT")
                .setTokenMetadata(initialTokenMetadata)
                // The same flow can be executed with a TokenType.NON_FUNGIBLE_UNIQUE (i.e. HIP-765).
                .setTokenType(TokenType.FUNGIBLE_COMMON)
                .setTreasuryAccountId(OPERATOR_ID)
                .setDecimals(3)
                .setInitialSupply(1_000_000)
                .setAdminKey(adminPublicKey)
                .freezeWith(client)
                .sign(adminPrivateKey)
                .execute(client)
                .getReceipt(client)
                .tokenId;
        Objects.requireNonNull(mutableFungibleTokenId);
        System.out.println("Created mutable Fungible Token with ID: " + mutableFungibleTokenId);

        /*
         * Step 3:
         * Query and output mutable Fungible Token info after its creation.
         */
        var mutableFungibleTokenInfo_AfterCreation =
                new TokenInfoQuery().setTokenId(mutableFungibleTokenId).execute(client);

        // Check that metadata was set correctly.
        if (Arrays.equals(mutableFungibleTokenInfo_AfterCreation.metadata, initialTokenMetadata)) {
            System.out.println("Mutable Fungible Token metadata after creation: "
                    + Arrays.toString(mutableFungibleTokenInfo_AfterCreation.metadata));
        } else {
            throw new Exception("Mutable Fungible Token metadata was not set correctly! (Fail)");
        }

        /*
         * Step 4:
         * Update mutable Fungible Token metadata.
         */
        byte[] updatedTokenMetadata = new byte[] {2, 2, 2, 2, 2};
        System.out.println("Updating mutable Fungible Token metadata...");
        new TokenUpdateTransaction()
                .setTokenId(mutableFungibleTokenId)
                .setTokenMetadata(updatedTokenMetadata)
                .freezeWith(client)
                .sign(adminPrivateKey)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 5:
         * Query and output mutable Fungible Token info after its metadata was updated.
         */
        var mutableFungibleTokenInfo_AfterMetadataUpdate =
                new TokenInfoQuery().setTokenId(mutableFungibleTokenId).execute(client);

        // Check that metadata was updated correctly.
        if (Arrays.equals(mutableFungibleTokenInfo_AfterMetadataUpdate.metadata, updatedTokenMetadata)) {
            System.out.println("Mutable Fungible Token metadata after update: "
                    + Arrays.toString(mutableFungibleTokenInfo_AfterMetadataUpdate.metadata));
        } else {
            throw new Exception("Mutable Fungible Token metadata was not updated correctly! (Fail)");
        }

        /*
         * Step 6:
         * The beginning of the second example (immutable token's metadata).
         *
         * Create an immutable Fungible Token with a metadata key and a metadata.
         */
        System.out.println("The beginning of the second example (immutable token's metadata).");

        System.out.println("Creating immutable Fungible Token using the Hedera Token Service...");
        var immutableFungibleTokenId = new TokenCreateTransaction()
                .setTokenName("HIP-646 Immutable FT")
                .setTokenSymbol("HIP646IMMFT")
                .setTokenMetadata(initialTokenMetadata)
                // The same flow can be executed with a TokenType.NON_FUNGIBLE_UNIQUE (i.e. HIP-765).
                .setTokenType(TokenType.FUNGIBLE_COMMON)
                .setTreasuryAccountId(OPERATOR_ID)
                .setMetadataKey(metadataPublicKey)
                .setDecimals(3)
                .setInitialSupply(1_000_000)
                .execute(client)
                .getReceipt(client)
                .tokenId;
        Objects.requireNonNull(immutableFungibleTokenId);
        System.out.println("Created an immutable Fungible Token with ID: " + immutableFungibleTokenId);

        /*
         * Step 7:
         * Query and output immutable Fungible Token info after its creation.
         */
        var immutableFungibleTokenTokenInfo_AfterCreation =
                new TokenInfoQuery().setTokenId(immutableFungibleTokenId).execute(client);

        // Check that metadata was set correctly.
        if (Arrays.equals(immutableFungibleTokenTokenInfo_AfterCreation.metadata, initialTokenMetadata)) {
            System.out.println("Immutable Fungible Token metadata after creation: "
                    + Arrays.toString(immutableFungibleTokenTokenInfo_AfterCreation.metadata));
        } else {
            throw new Exception("Immutable Fungible Token metadata was not set correctly! (Fail)");
        }

        /*
         * Step 8:
         * Update immutable Fungible Token metadata.
         */
        System.out.println("Updating immutable Fungible Token metadata...");
        new TokenUpdateTransaction()
                .setTokenId(immutableFungibleTokenId)
                .setTokenMetadata(updatedTokenMetadata)
                .freezeWith(client)
                .sign(metadataPrivateKey)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 5:
         * Query and output immutable Fungible Token info after its metadata was updated.
         */
        var immutableFungibleTokenInfo_AfterMetadataUpdate =
                new TokenInfoQuery().setTokenId(immutableFungibleTokenId).execute(client);

        // Check that metadata was updated correctly.
        if (Arrays.equals(immutableFungibleTokenInfo_AfterMetadataUpdate.metadata, updatedTokenMetadata)) {
            System.out.println("Immutable Fungible Token metadata after update: "
                    + Arrays.toString(immutableFungibleTokenInfo_AfterMetadataUpdate.metadata));
        } else {
            throw new Exception("Immutable Fungible Token metadata was not updated correctly! (Fail)");
        }

        /*
         * Clean up:
         * Delete created mutable token.
         */
        new TokenDeleteTransaction()
                .setTokenId(mutableFungibleTokenId)
                .freezeWith(client)
                .sign(adminPrivateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Token Metadata (HIP-646 and HIP-765) Example Complete!");
    }
}

