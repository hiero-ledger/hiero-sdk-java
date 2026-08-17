// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Example demonstrating how to reject both fungible tokens (FTs)
 * and non-fungible tokens (NFTs).
 *
 * <p>This example walks through the complete workflow, including:
 * <ul>
 *   <li>Client configuration using operator credentials</li>
 *   <li>Creating accounts with ED25519 keys</li>
 *   <li>Creating fungible and non-fungible tokens</li>
 *   <li>Associating tokens with accounts</li>
 *   <li>Performing token rejection operations</li>
 *   <li>Cleaning up resources (accounts and tokens)</li>
 * </ul>
 *
 * <p>Environment variables required:
 * <ul>
 *   <li><b>OPERATOR_ID</b> - Account ID for transactions</li>
 *   <li><b>OPERATOR_KEY</b> - Private key for signing transactions</li>
 * </ul>
 */
class TokenRejectExample {

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
        System.out.println("Token Reject (HIP-904) Example Start!");

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
        System.out.println("Generating ED25519 key pairs...");
        PrivateKey treasuryAccountPrivateKey = PrivateKey.generateED25519();
        PublicKey treasuryAccountPublicKey = treasuryAccountPrivateKey.getPublicKey();
        PrivateKey receiverAccountPrivateKey = PrivateKey.generateED25519();
        PublicKey receiverAccountPublicKey = receiverAccountPrivateKey.getPublicKey();

        /*
         * Step 2:
         * Create accounts for this example.
         */
        System.out.println("Creating treasury and receiver accounts...");
        // Create a treasury account.
        var treasuryAccountId = new AccountCreateTransaction()
                .setKeyWithoutAlias(treasuryAccountPublicKey)
                .setMaxAutomaticTokenAssociations(100)
                .freezeWith(client)
                .sign(treasuryAccountPrivateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(treasuryAccountId);

        // Create a receiver account with unlimited max auto associations (-1).
        var receiverAccountId = new AccountCreateTransaction()
                .setKeyWithoutAlias(receiverAccountPublicKey)
                .setMaxAutomaticTokenAssociations(-1)
                .freezeWith(client)
                .sign(receiverAccountPrivateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(receiverAccountId);

        /*
         * Step 3:
         * Create tokens for this example.
         */
        System.out.println("Creating FT and NFT...");
        // Create a Fungible Token.
        final int FUNGIBLE_TOKEN_SUPPLY = 1_000_000;
        TokenId fungibleTokenId = new TokenCreateTransaction()
                .setTokenName("HIP-904 FT")
                .setTokenSymbol("HIP904FT")
                .setDecimals(0)
                .setInitialSupply(FUNGIBLE_TOKEN_SUPPLY)
                .setMaxSupply(FUNGIBLE_TOKEN_SUPPLY)
                .setTreasuryAccountId(treasuryAccountId)
                .setSupplyType(TokenSupplyType.FINITE)
                .setAdminKey(treasuryAccountPublicKey)
                .freezeWith(client)
                .sign(treasuryAccountPrivateKey)
                .execute(client)
                .getReceipt(client)
                .tokenId;
        Objects.requireNonNull(fungibleTokenId);

        // Create NFT.
        TokenId nftId = new TokenCreateTransaction()
                .setTokenName("HIP-904 NFT")
                .setTokenSymbol("HIP904NFT")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setTreasuryAccountId(treasuryAccountId)
                .setSupplyType(TokenSupplyType.FINITE)
                .setMaxSupply(3)
                .setAdminKey(treasuryAccountPublicKey)
                .setSupplyKey(treasuryAccountPublicKey)
                .freezeWith(client)
                .sign(treasuryAccountPrivateKey)
                .execute(client)
                .getReceipt(client)
                .tokenId;
        Objects.requireNonNull(nftId);

        /*
         * Step 4:
         * Mint three NFTs.
         */
        System.out.println("Minting three NFTs...");
        var tokenMintTxReceipt = new TokenMintTransaction()
                .setTokenId(nftId)
                .setMetadata(generateNftMetadata((byte) 3))
                .freezeWith(client)
                .sign(treasuryAccountPrivateKey)
                .execute(client)
                .getReceipt(client);
        var nftSerials = tokenMintTxReceipt.serials;

        /*
         * Step 5:
         * Transfer tokens to the receiver.
         */
        System.out.println("Transferring tokens to the receiver...");
        new TransferTransaction()
                .addTokenTransfer(fungibleTokenId, treasuryAccountId, -1_000)
                .addTokenTransfer(fungibleTokenId, receiverAccountId, 1_000)
                .addNftTransfer(nftId.nft(nftSerials.get(0)), treasuryAccountId, receiverAccountId)
                .addNftTransfer(nftId.nft(nftSerials.get(1)), treasuryAccountId, receiverAccountId)
                .addNftTransfer(nftId.nft(nftSerials.get(2)), treasuryAccountId, receiverAccountId)
                .freezeWith(client)
                .sign(treasuryAccountPrivateKey)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 6:
         * Check receiver account balance.
         */
        var receiverAccountBalance =
                new AccountBalanceQuery().setAccountId(receiverAccountId).execute(client);

        if (receiverAccountBalance.tokens.get(fungibleTokenId) == 1_000) {
            System.out.println("Receiver account has: " + receiverAccountBalance.tokens.get(fungibleTokenId)
                    + " example fungible tokens.");
        } else {
            throw new Exception("Failed to transfer Fungible Token to the receiver account!");
        }

        if (receiverAccountBalance.tokens.get(nftId) == 3) {
            System.out.println("Receiver account has: " + receiverAccountBalance.tokens.get(nftId) + " example NFTs.");
        } else {
            throw new Exception("Failed to transfer NFT to the receiver account!");
        }

        /*
         * Step 7:
         * Reject the fungible token.
         */
        System.out.println("Receiver rejects example fungible tokens...");
        new TokenRejectTransaction()
                .setOwnerId(receiverAccountId)
                .addTokenId(fungibleTokenId)
                .freezeWith(client)
                .sign(receiverAccountPrivateKey)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 8:
         * Execute the token reject flow -- reject NFTs.
         */
        System.out.println("Receiver rejects example NFTs...");
        new TokenRejectFlow()
                .setOwnerId(receiverAccountId)
                .setNftIds(List.of(
                        nftId.nft(nftSerials.get(0)), nftId.nft(nftSerials.get(1)), nftId.nft(nftSerials.get(2))))
                .freezeWith(client)
                .sign(receiverAccountPrivateKey)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 9:
         * Check receiver account balance after token reject.
         */
        var receiverAccountBalance_AfterTokenReject =
                new AccountBalanceQuery().setAccountId(receiverAccountId).execute(client);

        if (receiverAccountBalance_AfterTokenReject.tokens.get(fungibleTokenId) == 0) {
            System.out.println("Receiver account has (after rejecting tokens): "
                    + receiverAccountBalance_AfterTokenReject.tokens.get(fungibleTokenId)
                    + " example fungible tokens.");
        } else {
            throw new Exception("Failed to reject Fungible Token!");
        }

        if (receiverAccountBalance_AfterTokenReject.tokens.get(nftId) == null) {
            System.out.println("Receiver account has (after rejecting tokens): "
                    + receiverAccountBalance_AfterTokenReject.tokens.get(nftId) + " example NFTs.");
        } else {
            throw new Exception("Failed to reject NFT!");
        }

        /*
         * Step 10:
         * Check treasury account balance after token reject.
         */
        var treasuryAccountBalance =
                new AccountBalanceQuery().setAccountId(treasuryAccountId).execute(client);

        if (treasuryAccountBalance.tokens.get(fungibleTokenId) == FUNGIBLE_TOKEN_SUPPLY) {
            System.out.println("Treasury account has: " + treasuryAccountBalance.tokens.get(fungibleTokenId)
                    + " example fungible tokens.");
        } else {
            throw new Exception("Failed to transfer Fungible Token to the treasury account during token rejection!");
        }

        if (treasuryAccountBalance.tokens.get(nftId) == 3) {
            System.out.println("Receiver account has: " + receiverAccountBalance.tokens.get(nftId) + " example NFTs.");
        } else {
            throw new Exception("Failed to transfer NFT to the treasury account during token rejection!");
        }

        /*
         * Clean up:
         * Delete created accounts and tokens.
         */
        new AccountDeleteTransaction()
                .setAccountId(treasuryAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(treasuryAccountPrivateKey)
                .execute(client);

        new AccountDeleteTransaction()
                .setAccountId(receiverAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(receiverAccountPrivateKey)
                .execute(client);

        new TokenDeleteTransaction()
                .setTokenId(fungibleTokenId)
                .freezeWith(client)
                .sign(treasuryAccountPrivateKey)
                .execute(client)
                .getReceipt(client);

        new TokenDeleteTransaction()
                .setTokenId(nftId)
                .freezeWith(client)
                .sign(treasuryAccountPrivateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Token Reject (HIP-904) Example Complete!");
    }

    private static List<byte[]> generateNftMetadata(byte metadataCount) {
        List<byte[]> metadatas = new ArrayList<>();

        for (byte i = 0; i < metadataCount; i++) {
            byte[] md = {i};
            metadatas.add(md);
        }

        return metadatas;
    }
}
