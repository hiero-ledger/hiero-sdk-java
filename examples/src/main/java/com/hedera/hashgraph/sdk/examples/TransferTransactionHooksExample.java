// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * How to work with TransferTransaction with various types of hooks.
 * <p>
 * This example demonstrates how to use TransferTransaction with different types of hooks:
 * - HBAR transfers with fungible hooks
 * - NFT transfers with sender and receiver hooks
 * - Fungible token transfers with hooks
 * <p>
 * The example includes prerequisite setup (creating accounts, tokens, and NFTs)
 * to demonstrate the TransferTransaction with hooks functionality.
 */
class TransferTransactionHooksExample {

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
     * HEDERA_NETWORK defaults to localhost if not specified in dotenv file.
     * Network can be: localhost, testnet, previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "localhost");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file.
     * Log levels can be: TRACE, DEBUG, INFO, WARN, ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL,
     * for example via VM options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        System.out.println("Transfer Transaction Hooks Example Start!");

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
         * Set up prerequisites: Use existing accounts and create tokens.
         * Note: This is not part of TransferTransaction itself, but required for the example.
         */
        System.out.println("Setting up prerequisites...");

        // Use existing accounts (following TransferCryptoExample pattern)
        AccountId senderAccountId = OPERATOR_ID; // Operator is typically the sender
        AccountId receiverAccountId = AccountId.fromString("0.0.3");

        // Create a fungible token
        TokenId fungibleTokenId = createFungibleToken(client);

        // Create an NFT token and mint an NFT
        TokenId nftTokenId = createNftToken(client);
        NftId nftId = mintNft(client, nftTokenId);

        /*
         * Step 2:
         * Demonstrate TransferTransaction API with hooks (demonstration only).
         * Note: This shows the API structure - actual execution requires hooks to exist on the network.
         */
        System.out.println("\n=== TransferTransaction with Hooks API Demonstration ===");

        // Create different hooks for different transfer types (for demonstration)
        System.out.println("Creating hook call objects (demonstration)...");

        // HBAR transfer with pre-tx allowance hook
        FungibleHookCall hbarHook = new FungibleHookCall(
                1001L, new EvmHookCall(new byte[] {0x01, 0x02}, 20000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

        // NFT sender hook (pre-hook)
        NftHookCall nftSenderHook =
                new NftHookCall(1002L, new EvmHookCall(new byte[] {0x03, 0x04}, 20000L), NftHookType.PRE_HOOK_SENDER);

        // NFT receiver hook (pre-hook)
        NftHookCall nftReceiverHook =
                new NftHookCall(1003L, new EvmHookCall(new byte[] {0x05, 0x06}, 20000L), NftHookType.PRE_HOOK_RECEIVER);

        // Fungible token transfer with pre-post allowance hook
        FungibleHookCall fungibleTokenHook = new FungibleHookCall(
                1004L, new EvmHookCall(new byte[] {0x07, 0x08}, 20000L), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);

        // Build TransferTransaction with hooks (demonstration)
        System.out.println("Building TransferTransaction with hooks...");
        new TransferTransaction()
                // HBAR transfers with hook
                .addHbarTransferWithHook(senderAccountId, Hbar.from(-100), hbarHook)
                .addHbarTransfer(receiverAccountId, Hbar.from(100))

                // NFT transfer with sender and receiver hooks
                .addNftTransferWithHook(nftId, senderAccountId, receiverAccountId, nftSenderHook, nftReceiverHook)

                // Fungible token transfers with hook
                .addTokenTransferWithHook(fungibleTokenId, senderAccountId, -1000, fungibleTokenHook)
                .addTokenTransfer(fungibleTokenId, receiverAccountId, 1000);

        System.out.println("TransferTransaction built successfully with the following hook calls:");
        System.out.println("  - HBAR transfer with pre-tx allowance hook (ID: 1001)");
        System.out.println("  - NFT transfer with sender hook (ID: 1002) and receiver hook (ID: 1003)");
        System.out.println("  - Fungible token transfer with pre-post allowance hook (ID: 1004)");

        // Demonstrate the API without executing (since hooks don't exist)
        System.out.println("\nNote: This demonstrates the TransferTransaction API with hooks.");
        System.out.println(
                "To actually execute this transaction, the hooks (IDs 1001-1004) must exist on the network.");
        System.out.println("The transaction would be executed with: transferTx.execute(client)");

        // Show a simple transfer without hooks that actually works
        System.out.println("\n=== Executing Simple Transfer (without hooks) ===");
        try {
            TransactionResponse simpleTransferResponse = new TransferTransaction()
                    .addHbarTransfer(senderAccountId, Hbar.from(-1))
                    .addHbarTransfer(receiverAccountId, Hbar.from(1))
                    .execute(client);

            simpleTransferResponse.getReceipt(client);
            System.out.println("Successfully executed simple HBAR transfer!");
            System.out.println("Transaction ID: " + simpleTransferResponse.transactionId);
        } catch (Exception e) {
            System.err.println("Failed to execute simple transfer: " + e.getMessage());
        }

        client.close();
        System.out.println("Transfer Transaction Hooks Example Complete!");
    }

    /**
     * Creates a fungible token for the example.
     */
    private static TokenId createFungibleToken(Client client) throws Exception {
        System.out.println("Creating fungible token...");

        TransactionResponse tokenCreateResponse = new TokenCreateTransaction()
                .setTokenName("Example Fungible Token")
                .setTokenSymbol("EFT")
                .setTokenType(TokenType.FUNGIBLE_COMMON)
                .setDecimals(2)
                .setInitialSupply(10000)
                .setTreasuryAccountId(OPERATOR_ID)
                .setAdminKey(OPERATOR_KEY)
                .setSupplyKey(OPERATOR_KEY)
                .execute(client);

        TransactionReceipt tokenCreateReceipt = tokenCreateResponse.getReceipt(client);
        TokenId tokenId = tokenCreateReceipt.tokenId;
        Objects.requireNonNull(tokenId);

        System.out.println("Created fungible token with ID: " + tokenId);
        return tokenId;
    }

    /**
     * Creates an NFT token for the example.
     */
    private static TokenId createNftToken(Client client) throws Exception {
        System.out.println("Creating NFT token...");

        TransactionResponse tokenCreateResponse = new TokenCreateTransaction()
                .setTokenName("Example NFT Token")
                .setTokenSymbol("ENT")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setTreasuryAccountId(OPERATOR_ID)
                .setAdminKey(OPERATOR_KEY)
                .setSupplyKey(OPERATOR_KEY)
                .execute(client);

        TransactionReceipt tokenCreateReceipt = tokenCreateResponse.getReceipt(client);
        TokenId tokenId = tokenCreateReceipt.tokenId;
        Objects.requireNonNull(tokenId);

        System.out.println("Created NFT token with ID: " + tokenId);
        return tokenId;
    }

    /**
     * Mints an NFT for the example.
     */
    private static NftId mintNft(Client client, TokenId tokenId) throws Exception {
        System.out.println("Minting NFT...");

        // Create metadata for the NFT
        byte[] metadata = "Example NFT Metadata".getBytes(StandardCharsets.UTF_8);

        TransactionResponse mintResponse = new TokenMintTransaction()
                .setTokenId(tokenId)
                .addMetadata(metadata)
                .execute(client);

        TransactionReceipt mintReceipt = mintResponse.getReceipt(client);
        long serialNumber = mintReceipt.serials.getFirst();
        NftId nftId = new NftId(tokenId, serialNumber);

        System.out.println("Minted NFT with ID: " + nftId);
        return nftId;
    }
}
