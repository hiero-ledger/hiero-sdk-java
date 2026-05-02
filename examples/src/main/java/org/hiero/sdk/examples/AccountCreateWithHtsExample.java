// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * How to use auto account creation via HTS assets (HIP-542).
 * <p>
 * This means a new alias may be given anywhere in the transaction--
 * both in the Hbar transfer list, or in an HTS token transfer list. In the latter case,
 * the assessed creation fee will include at least one auto-association slot,
 * since the new account must be associated to its originating HTS assets.
 */
class AccountCreateWithHtsExample {

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
        System.out.println("Account Auto-Creation Via HTS Assets (HIP-542) Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));
        // Set the maximum fee to be paid for transactions executed by this client.
        client.setDefaultMaxTransactionFee(Hbar.from(10));

        /*
         * Step 1:
         * Generate ECDSA keys pairs.
         */
        PublicKey operatorPublicKey = OPERATOR_KEY.getPublicKey();

        System.out.println("Generating ECDSA key pairs...");

        PrivateKey supplyPrivateKey = PrivateKey.generateECDSA();
        PublicKey supplyPublicKey = supplyPrivateKey.getPublicKey();

        PrivateKey freezePrivateKey = PrivateKey.generateECDSA();
        PublicKey freezePublicKey = freezePrivateKey.getPublicKey();

        PrivateKey wipePrivateKey = PrivateKey.generateECDSA();
        PublicKey wipePublicKey = wipePrivateKey.getPublicKey();

        /*
         * Step 2:
         * The beginning of the first example (with NFT).
         *
         * Create NFT using the Hedera Token Service.
         */
        System.out.println("The beginning of the first example (with NFT)...");

        // IPFS content identifiers for the NFT metadata.
        String[] CIDs = new String[] {
            "QmNPCiNA3Dsu3K5FxDPMG5Q3fZRwVTg14EXA92uqEeSRXn",
            "QmZ4dgAgt8owvnULxnKxNe8YqpavtVCXmc1Lt2XajFpJs9",
            "QmPzY5GxevjyfMUF5vEAjtyRoigzWp47MiKAtLBduLMC1T",
            "Qmd3kGgSrAwwSrhesYcY7K54f3qD7MDo38r7Po2dChtQx5",
            "QmWgkKz3ozgqtnvbCLeh7EaR1H8u5Sshx3ZJzxkcrT3jbw",
        };

        System.out.println("Creating NFT using the Hedera Token Service...");

        TokenCreateTransaction nftCreateTx = new TokenCreateTransaction()
                .setTokenName("HIP-542 Example Collection")
                .setTokenSymbol("HIP-542")
                .setTokenName("HIP-542 NFT")
                .setTokenSymbol("HIP542NFT")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setDecimals(0)
                .setInitialSupply(0)
                .setMaxSupply(CIDs.length)
                .setTreasuryAccountId(OPERATOR_ID)
                .setSupplyType(TokenSupplyType.FINITE)
                .setAdminKey(operatorPublicKey)
                .setFreezeKey(freezePublicKey)
                .setWipeKey(wipePublicKey)
                .setSupplyKey(supplyPublicKey)
                .freezeWith(client);

        // Sign the transaction with the operator key.
        TokenCreateTransaction nftCreateTxSigned = nftCreateTx.sign(OPERATOR_KEY);

        // Submit the transaction to the Hedera network.
        TransactionResponse nftCreateTxResponse = nftCreateTxSigned.execute(client);

        // Get transaction receipt information.
        TransactionReceipt nftCreateTxReceipt = nftCreateTxResponse.getReceipt(client);
        TokenId nftTokenId = nftCreateTxReceipt.tokenId;
        Objects.requireNonNull(nftTokenId);

        System.out.println("Created NFT with token ID: " + nftTokenId);

        /*
         * Step 3:
         * Mint NFTs.
         */
        System.out.println("Minting NFTs...");
        TransactionReceipt[] nftMintTxReceipts = new TransactionReceipt[CIDs.length];
        for (int i = 0; i < CIDs.length; i++) {
            byte[] nftMetadata = CIDs[i].getBytes();

            TokenMintTransaction nftMintTx = new TokenMintTransaction()
                    .setTokenId(nftTokenId)
                    .setMetadata(List.of(nftMetadata))
                    .freezeWith(client);

            TokenMintTransaction nftMintTxSigned = nftMintTx.sign(supplyPrivateKey);
            TransactionResponse nftMintTxResponse = nftMintTxSigned.execute(client);

            nftMintTxReceipts[i] = nftMintTxResponse.getReceipt(client);

            System.out.println(
                    "Minted NFT (token ID: " + nftTokenId + ") with serial: " + nftMintTxReceipts[i].serials.get(0));
        }

        long exampleNftId = nftMintTxReceipts[0].serials.get(0);

        /*
         * Step 4:
         * Create an ECDSA public key alias.
         */
        PrivateKey alicePrivateKey = PrivateKey.generateECDSA();
        PublicKey alicePublicKey = alicePrivateKey.getPublicKey();

        System.out.println("\"Creating\" Alice's account...");

        // Assuming that the target shard and realm are known.
        // For now they are virtually always 0 and 0.
        AccountId aliceAliasAccountId = alicePublicKey.toAccountId(0, 0);

        System.out.println("Alice's account ID: " + aliceAliasAccountId);
        System.out.println("Alice's alias key: " + aliceAliasAccountId.aliasKey);

        /*
         * Step 5:
         * Transfer the NFT to Alice's public key alias using the transfer transaction.
         */
        System.out.println("Transferring NFT to Alice's account...");

        TransferTransaction nftTransferTx = new TransferTransaction()
                .addNftTransfer(nftTokenId.nft(exampleNftId), OPERATOR_ID, aliceAliasAccountId)
                .freezeWith(client);

        // Sign the transaction with the operator key.
        TransferTransaction nftTransferTxSigned = nftTransferTx.sign(OPERATOR_KEY);

        // Submit the transaction to the Hedera network.
        TransactionResponse nftTransferTxResponse = nftTransferTxSigned.execute(client);

        // Get transaction receipt information here.
        nftTransferTxResponse.getReceipt(client);

        /*
         * Step 6:
         * Get the new account ID from the child record.
         */
        List<TokenNftInfo> nftsInfo =
                new TokenNftInfoQuery().setNftId(nftTokenId.nft(exampleNftId)).execute(client);

        String nftOwnerAccountId_FromChildRecord = nftsInfo.get(0).accountId.toString();
        System.out.println("Current owner account ID: " + nftOwnerAccountId_FromChildRecord);

        /*
         * Step 7:
         * Show the normal account ID of account which owns the NFT.
         */
        String nftOwnerAccountId_FromQuery = new AccountInfoQuery()
                .setAccountId(aliceAliasAccountId)
                .execute(client)
                .accountId
                .toString();

        System.out.println("The \"normal\" account ID of the given alias: " + nftOwnerAccountId_FromQuery);

        /*
         * Step 8:
         * Validate that account ID value from the child record is equal to normal account ID value from the query.
         */
        if (nftOwnerAccountId_FromChildRecord.equals(nftOwnerAccountId_FromQuery)) {
            System.out.println("The NFT owner account ID matches the account ID created with the HTS! (Success)");
        } else {
            throw new Exception("The two account IDs does not match! (Error)");
        }

        /*
         * Step 9:
         * The beginning of the second example (with Fungible Token).
         * Create a fungible HTS token using the Hedera Token Service.
         */
        System.out.println("The beginning of the second example (with Fungible Token).");
        System.out.println("Creating Fungible Token using the Hedera Token Service...");

        TokenCreateTransaction ftCreateTx = new TokenCreateTransaction()
                .setTokenName("HIP-542 Fungible Token")
                .setTokenSymbol("HIP542FT")
                .setInitialSupply(10_000) // Total supply = 10000 / 10 ^ 2
                .setDecimals(2)
                .setTokenType(TokenType.FUNGIBLE_COMMON)
                .setTreasuryAccountId(OPERATOR_ID)
                .setAutoRenewAccountId(OPERATOR_ID)
                .setAdminKey(operatorPublicKey)
                .setWipeKey(wipePrivateKey)
                .freezeWith(client);

        // Sign the transaction with the operator key.
        TokenCreateTransaction ftCreateTxSigned = ftCreateTx.sign(OPERATOR_KEY);

        // Submit the transaction to the Hedera network.
        TransactionResponse ftCreateResponse = ftCreateTxSigned.execute(client);

        // Get transaction receipt information.
        TransactionReceipt ftCreateReceipt = ftCreateResponse.getReceipt(client);
        TokenId fungibleTokenId = ftCreateReceipt.tokenId;
        Objects.requireNonNull(fungibleTokenId);

        System.out.println("Created fungible token with ID: " + fungibleTokenId);

        /*
         * Step 10:
         * Create an ECDSA public key alias.
         */
        PrivateKey bobPrivateKey = PrivateKey.generateECDSA();
        PublicKey bobPublicKey = bobPrivateKey.getPublicKey();

        System.out.println("\"Creating\" Bob's account...");
        // Assuming that the target shard and realm are known.
        // For now, they are virtually always 0 and 0.
        AccountId bobAliasAccountId = bobPublicKey.toAccountId(0, 0);

        System.out.println("Bob's account ID: " + bobAliasAccountId);
        System.out.println("Bob's alias key: " + bobAliasAccountId.aliasKey);

        /*
         * Step 11:
         * Transfer the Fungible Token to the Bob's public key alias using the transfer transaction.
         */
        System.out.println("Transferring Fungible Token the Bob's account...");
        TransferTransaction tokenTransferTx = new TransferTransaction()
                .addTokenTransfer(fungibleTokenId, OPERATOR_ID, -10)
                .addTokenTransfer(fungibleTokenId, bobAliasAccountId, 10)
                .freezeWith(client);

        // Sign the transaction with the operator key.
        TransferTransaction tokenTransferTxSign = tokenTransferTx.sign(OPERATOR_KEY);

        // Submit the transaction to the Hedera network.
        TransactionResponse tokenTransferSubmit = tokenTransferTxSign.execute(client);

        // Get transaction receipt information.
        tokenTransferSubmit.getReceipt(client);

        /*
         * Step 12:
         * Get the new account ID from the child record.
         */
        String bobAccountInfo = new AccountInfoQuery()
                .setAccountId(bobAliasAccountId)
                .execute(client)
                .accountId
                .toString();

        System.out.println("The \"normal\" account ID of the given alias: " + bobAccountInfo);

        /*
         * Step 13:
         * Show the normal account ID of account which owns the NFT.
         */
        AccountBalance bobAccountBalances =
                new AccountBalanceQuery().setAccountId(bobAliasAccountId).execute(client);

        /*
         * Step 14:
         * Validate token balance of newly created account.
         */
        int bobFtBalance = bobAccountBalances.tokens.get(fungibleTokenId).intValue();
        if (bobFtBalance == 10) {
            System.out.println("New account was created using HTS TransferTransaction! (Success)");
        } else {
            throw new Exception("Creating account with HTS using public key alias failed! (Error)");
        }

        /*
         * Clean up:
         * Delete created accounts and tokens.
         */
        AccountId nftOwnerAccountId = AccountId.fromString(nftOwnerAccountId_FromQuery);

        new TokenWipeTransaction()
                .setTokenId(nftTokenId)
                .addSerial(exampleNftId)
                .setAccountId(nftOwnerAccountId)
                .freezeWith(client)
                .sign(wipePrivateKey)
                .execute(client)
                .getReceipt(client);

        AccountId bobAccountId = AccountId.fromString(bobAccountInfo);

        Map<TokenId, Long> bobsTokens =
                new AccountBalanceQuery().setAccountId(bobAccountId).execute(client).tokens;

        new TokenWipeTransaction()
                .setTokenId(fungibleTokenId)
                .setAmount(bobsTokens.get(fungibleTokenId))
                .setAccountId(bobAccountId)
                .freezeWith(client)
                .sign(wipePrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(nftOwnerAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(bobAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        new TokenDeleteTransaction()
                .setTokenId(nftTokenId)
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .execute(client)
                .getReceipt(client);

        new TokenDeleteTransaction()
                .setTokenId(fungibleTokenId)
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Account Auto-Creation Via HTS Assets (HIP-542) Example Complete!");
    }
}

