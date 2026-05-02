// SPDX-License-Identifier: Apache-2.0
package examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * How to update NFTs' metadata (HIP-657).
 */
class UpdateNftsMetadataExample {

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
        System.out.println("Update Nfts Metadata Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        PublicKey operatorKeyPublic = OPERATOR_KEY.getPublicKey();

        /*
         * Step 1:
         * Generate ED25519 key pair (Metadata Key).
         */
        System.out.println("Generating ED25519 key pair...(metadata key).");
        PrivateKey metadataPrivateKey = PrivateKey.generateED25519();
        PublicKey metadataPublicKey = metadataPrivateKey.getPublicKey();

        /*
         * Step 2:
         * The beginning of the first example (mutable token's metadata).
         *
         * Create a non-fungible token (NFT) with the metadata key field set.
         */
        System.out.println("The beginning of the first example (mutable token's metadata).");
        byte[] initialMetadata = new byte[] {1};
        System.out.println("Creating mutable NFT with the metadata key field set...");
        var mutableNftCreateTx = new TokenCreateTransaction()
                .setTokenName("HIP-657 Mutable NFT")
                .setTokenSymbol("HIP657MNFT")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setTreasuryAccountId(OPERATOR_ID)
                .setAdminKey(operatorKeyPublic)
                .setSupplyKey(operatorKeyPublic)
                .setMetadataKey(metadataPublicKey)
                .freezeWith(client);

        var mutableNftCreateTxResponse = mutableNftCreateTx.sign(OPERATOR_KEY).execute(client);
        var mutableNftCreateTxReceipt = mutableNftCreateTxResponse.getReceipt(client);
        // Get the token ID of the token that was created.
        var mutableNftId = mutableNftCreateTxReceipt.tokenId;
        Objects.requireNonNull(mutableNftId);
        System.out.println("Created mutable NFT with token ID: " + mutableNftId);

        /*
         * Step 3:
         * Query for the mutable token information stored in consensus node state to see that the Metadata Key is set.
         */
        var mutableNftInfo = new TokenInfoQuery().setTokenId(mutableNftId).execute(client);

        System.out.println("Mutable NFT metadata key: " + mutableNftInfo.metadataKey);

        /*
         * Step 4:
         * Mint the first NFT and set the initial metadata for the NFT.
         */
        System.out.println("Minting NFTs...");
        var mutableNftMintTx =
                new TokenMintTransaction().setMetadata(List.of(initialMetadata)).setTokenId(mutableNftId);

        mutableNftMintTx.getMetadata().forEach(metadata -> {
            System.out.println("Setting metadata: " + Arrays.toString(metadata));
        });

        var mutableNftMintTxResponse = mutableNftMintTx.execute(client);

        // Get receipt for mint token transaction.
        var mutableNftMintTxReceipt = mutableNftMintTxResponse.getReceipt(client);
        System.out.println("Mint transaction was complete with status: " + mutableNftMintTxReceipt.status);

        var mutableNftSerials = mutableNftMintTxReceipt.serials;

        // Check that metadata on the NFT was set correctly.
        getMetadataList(client, mutableNftId, mutableNftSerials).forEach(metadata -> {
            System.out.println("Metadata after mint: " + Arrays.toString(metadata));
        });

        /*
         * Step 5:
         * Create an account to send the NFT to.
         */
        System.out.println("Creating Alice's account...");
        var aliceAccountCreateTx = new AccountCreateTransaction()
                .setKeyWithoutAlias(operatorKeyPublic)
                // If the account does not have any automatic token association,
                // slots open ONLY then associate the NFT to the account.
                .setMaxAutomaticTokenAssociations(10)
                .execute(client);

        var aliceAccountId = aliceAccountCreateTx.getReceipt(client).accountId;
        Objects.requireNonNull(aliceAccountId);
        System.out.println("Created Alice's account with ID: " + aliceAccountId);

        /*
         * Step 6:
         * Transfer the NFT to the new account.
         */
        System.out.println("Transferring the NFT to Alice's account...");
        new TransferTransaction()
                .addNftTransfer(mutableNftId.nft(mutableNftSerials.get(0)), OPERATOR_ID, aliceAccountId)
                .execute(client);

        /*
         * Step 7:
         * Update NFTs' metadata.
         */
        byte[] updatedMetadata = new byte[] {1, 2};
        System.out.println("Updating NFTs' metadata...");
        var tokenUpdateNftsTx = new TokenUpdateNftsTransaction()
                .setTokenId(mutableNftId)
                .setSerials(mutableNftSerials)
                .setMetadata(updatedMetadata)
                .freezeWith(client);

        System.out.println("Updated NFTs' metadata: " + Arrays.toString(tokenUpdateNftsTx.getMetadata()));
        var tokenUpdateNftsTxResponse =
                tokenUpdateNftsTx.sign(metadataPrivateKey).execute(client);

        // Get receipt for update NFTs metadata transaction.
        var tokenUpdateNftsTxReceipt = tokenUpdateNftsTxResponse.getReceipt(client);
        System.out.println(
                "Token update nfts metadata transaction was complete with status: " + tokenUpdateNftsTxReceipt.status);

        // Check that metadata for the NFT was updated correctly.
        getMetadataList(client, mutableNftId, mutableNftSerials).forEach(metadata -> {
            System.out.println("NFTs' metadata after update: " + Arrays.toString(metadata));
        });

        /*
         * Step 8:
         * The beginning of the second example (immutable token's metadata).
         *
         * Create a non-fungible token (NFT) with the metadata key field set.
         */
        System.out.println("The beginning of the second example (immutable token's metadata).");
        System.out.println("Creating immutable NFT with the metadata key field set...");
        var immutableNftCreateTx = new TokenCreateTransaction()
                .setTokenName("HIP-657 Immutable NFT")
                .setTokenSymbol("HIP657IMMNFT")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setTreasuryAccountId(OPERATOR_ID)
                .setSupplyKey(operatorKeyPublic)
                .setMetadataKey(metadataPublicKey)
                .freezeWith(client);

        var immutableNftCreateTxResponse =
                immutableNftCreateTx.sign(OPERATOR_KEY).execute(client);
        var immutableNftCreateTxReceipt = immutableNftCreateTxResponse.getReceipt(client);
        // Get the token ID of the token that was created.
        var immutableNftId = immutableNftCreateTxReceipt.tokenId;
        Objects.requireNonNull(immutableNftId);
        System.out.println("Created immutable NFT with token ID: " + immutableNftId);

        /*
         * Step 9:
         * Query for the mutable token information stored in consensus node state to see that the metadata key is set.
         */
        var immutableNftInfo = new TokenInfoQuery().setTokenId(immutableNftId).execute(client);

        System.out.println("Immutable NFT metadata key: " + immutableNftInfo.metadataKey);

        /*
         * Step 10:
         * Mint the first NFT and set the initial metadata for the NFT.
         */
        System.out.println("Minting NFTs...");
        var immutableNftMintTx =
                new TokenMintTransaction().setMetadata(List.of(initialMetadata)).setTokenId(immutableNftId);

        immutableNftMintTx.getMetadata().forEach(metadata -> {
            System.out.println("Setting metadata: " + Arrays.toString(metadata));
        });

        var immutableNftMintTxResponse = immutableNftMintTx.execute(client);

        // Get receipt for mint token transaction.
        var immutableNftMintTxReceipt = immutableNftMintTxResponse.getReceipt(client);
        System.out.println("Mint transaction was complete with status: " + immutableNftMintTxReceipt.status);

        var immutableNftSerials = immutableNftMintTxReceipt.serials;
        // Check that metadata on the NFT was set correctly.
        getMetadataList(client, immutableNftId, immutableNftSerials).forEach(metadata -> {
            System.out.println("Metadata after mint: " + Arrays.toString(metadata));
        });

        /*
         * Step 11:
         * Create an account to send the NFT to.
         */
        System.out.println("Creating Bob's account...");
        var bobAccountCreateTx = new AccountCreateTransaction()
                .setKeyWithoutAlias(operatorKeyPublic)
                // If the account does not have any automatic token association,
                // slots open ONLY then associate the NFT to the account.
                .setMaxAutomaticTokenAssociations(10)
                .execute(client);

        var bobAccountId = bobAccountCreateTx.getReceipt(client).accountId;
        Objects.requireNonNull(bobAccountId);
        System.out.println("Created Bob's account with ID: " + bobAccountId);

        /*
         * Step 12:
         * Transfer the NFT to the new account.
         */
        System.out.println("Transferring the NFT to Bob's account...");
        new TransferTransaction()
                .addNftTransfer(immutableNftId.nft(immutableNftSerials.get(0)), OPERATOR_ID, bobAccountId)
                .execute(client);

        /*
         * Step 13:
         * Update NFTs' metadata.
         */
        System.out.println("Updating NFTs' metadata...");
        var immutableNftUpdateNftsTx = new TokenUpdateNftsTransaction()
                .setTokenId(immutableNftId)
                .setSerials(immutableNftSerials)
                .setMetadata(updatedMetadata)
                .freezeWith(client);

        System.out.println("Updated NFTs' metadata: " + Arrays.toString(immutableNftUpdateNftsTx.getMetadata()));
        var immutableNftUpdateNftsTxResponse =
                immutableNftUpdateNftsTx.sign(metadataPrivateKey).execute(client);

        // Get receipt for update NFTs metadata transaction.
        var immutableNftUpdateNftsTxReceipt = immutableNftUpdateNftsTxResponse.getReceipt(client);
        System.out.println("Token update nfts metadata transaction was complete with status: "
                + immutableNftUpdateNftsTxReceipt.status);

        // Check that metadata for the NFT was updated correctly.
        getMetadataList(client, immutableNftId, immutableNftSerials).forEach(metadata -> {
            System.out.println("NFTs' metadata after update: " + Arrays.toString(metadata));
        });

        /*
         * Clean up:
         * Delete created mutable token.
         */
        new TokenDeleteTransaction().setTokenId(mutableNftId).execute(client).getReceipt(client);

        client.close();

        System.out.println("Update Nfts Metadata Example Complete!");
    }

    private static List<byte[]> getMetadataList(Client client, TokenId tokenId, List<Long> nftSerials) {
        return nftSerials.stream()
                .map(serial -> new NftId(tokenId, serial))
                .flatMap(nftId -> {
                    try {
                        return new TokenNftInfoQuery().setNftId(nftId).execute(client).stream();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(tokenNftInfo -> tokenNftInfo.metadata)
                .toList();
    }
}
