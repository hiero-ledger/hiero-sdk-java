// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Collections;
import java.util.Objects;

/**
 * FULL TOKEN TRANSFER
 *
 * This example demonstrates the complete lifecycle of a Hedera token transfer:
 *
 * Step 1: Create and configure the SDK Client
 * Step 2: Generate ED25519 key pairs
 * Step 3: Create accounts (Alice and Bob)
 * Step 4: Create a fungible token
 * Step 5: Associate the token with accounts
 * Step 6: Grant KYC permissions
 * Step 7: Transfer tokens from treasury to Alice
 * Step 8: Transfer tokens between Alice and Bob
 *
 * This helps developers understand the full token workflow using the SDK.
 */
class TransferTokensExample {

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
        System.out.println("Transfer Tokens Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        PublicKey operatorPublicKey = OPERATOR_KEY.getPublicKey();

        /*
         * Step 1:
         * Generate ED25519 key pairs.
         */
        System.out.println("Generating ED25519 key pairs for accounts...");
        PrivateKey alicePrivateKey = PrivateKey.generateED25519();
        PublicKey alicePublicKey = alicePrivateKey.getPublicKey();
        PrivateKey bobPrivateKey = PrivateKey.generateED25519();
        PublicKey bobPublicKey = bobPrivateKey.getPublicKey();

        /*
         * Step 2:
         * Create two new accounts.
         */
        System.out.println("Creating accounts...");
        Hbar initialBalance = Hbar.from(1);
        TransactionResponse aliceAccountCreateTxResponse = new AccountCreateTransaction()
                // The only required property here is key.
                .setKeyWithoutAlias(alicePublicKey)
                .setInitialBalance(initialBalance)
                .execute(client);

        // This will wait for the receipt to become available.
        TransactionReceipt aliceAccountCreateTxReceipt = aliceAccountCreateTxResponse.getReceipt(client);
        AccountId aliceAccountId = Objects.requireNonNull(aliceAccountCreateTxReceipt.accountId);
        Objects.requireNonNull(aliceAccountId);
        System.out.println("Created Alice's account with ID: " + aliceAccountId);

        TransactionResponse bobAccountCreateTxResponse = new AccountCreateTransaction()
                // The only required property here is key.
                .setKeyWithoutAlias(bobPublicKey)
                .setInitialBalance(initialBalance)
                .execute(client);

        // This will wait for the receipt to become available.
        TransactionReceipt bobAccountCreateTxReceipt = bobAccountCreateTxResponse.getReceipt(client);
        AccountId bobAccountId = Objects.requireNonNull(bobAccountCreateTxReceipt.accountId);
        Objects.requireNonNull(bobAccountId);
        System.out.println("Created Bob's account with ID: " + bobAccountId);

        /*
         * Step 3:
         * Create a Fungible Token.
         */
        System.out.println("Creating Fungible Token...");
        TransactionResponse tokenCreateTxResponse = new TokenCreateTransaction()
                .setNodeAccountIds(Collections.singletonList(bobAccountCreateTxResponse.nodeId))
                .setTokenName("Example Fungible Token for Transfer demo")
                .setTokenSymbol("EFT")
                .setDecimals(3)
                .setInitialSupply(1_000_000)
                .setTreasuryAccountId(OPERATOR_ID)
                .setAdminKey(operatorPublicKey)
                .setFreezeKey(operatorPublicKey)
                .setWipeKey(operatorPublicKey)
                .setKycKey(operatorPublicKey)
                .setSupplyKey(operatorPublicKey)
                .setFreezeDefault(false)
                .execute(client);

        TokenId tokenId = Objects.requireNonNull(tokenCreateTxResponse.getReceipt(client).tokenId);
        Objects.requireNonNull(tokenId);
        System.out.println("Created Fungible Token with ID: " + tokenId);

        /*
         * Step 4:
         * Associate the token with created accounts.
         */
        System.out.println("Associating the token with created accounts...");
        new TokenAssociateTransaction()
                .setNodeAccountIds(Collections.singletonList(tokenCreateTxResponse.nodeId))
                .setAccountId(aliceAccountId)
                .setTokenIds(Collections.singletonList(tokenId))
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("Associated account " + aliceAccountId + " with token " + tokenId);

        new TokenAssociateTransaction()
                .setNodeAccountIds(Collections.singletonList(tokenCreateTxResponse.nodeId))
                .setAccountId(bobAccountId)
                .setTokenIds(Collections.singletonList(tokenId))
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("Associated account " + bobAccountId + " with token " + tokenId);

        /*
         * Step 5:
         * Grant token KYC for created accounts.
         */
        System.out.println("Granting token KYC for created accounts...");
        new TokenGrantKycTransaction()
                .setNodeAccountIds(Collections.singletonList(tokenCreateTxResponse.nodeId))
                .setAccountId(aliceAccountId)
                .setTokenId(tokenId)
                .execute(client)
                .getReceipt(client);

        System.out.println("Granted KYC for account " + aliceAccountId + " on token " + tokenId);

        new TokenGrantKycTransaction()
                .setNodeAccountIds(Collections.singletonList(tokenCreateTxResponse.nodeId))
                .setAccountId(bobAccountId)
                .setTokenId(tokenId)
                .execute(client)
                .getReceipt(client);

        System.out.println("Granted KYC for account " + bobAccountId + " on token " + tokenId);

        /*
         * Step 6:
         * Transfer tokens from the operator (treasury) to Alice's account.
         */
        System.out.println("Transferring tokens from operator's (treasury) account to the `accountId1`...");
        new TransferTransaction()
                .setNodeAccountIds(Collections.singletonList(tokenCreateTxResponse.nodeId))
                .addTokenTransfer(tokenId, OPERATOR_ID, -10)
                .addTokenTransfer(tokenId, aliceAccountId, 10)
                .execute(client)
                .getReceipt(client);

        System.out.println("Sent 10 tokens from account " + OPERATOR_ID + " to account " + aliceAccountId + " on token "
                + tokenId);

        /*
         * Step 6:
         * Transfer 10 tokens from the Alice to Bob.
         */
        System.out.println("Transferring tokens from the `accountId1` to the `accountId2`...");
        new TransferTransaction()
                .setNodeAccountIds(Collections.singletonList(tokenCreateTxResponse.nodeId))
                .addTokenTransfer(tokenId, aliceAccountId, -10)
                .addTokenTransfer(tokenId, bobAccountId, 10)
                .freezeWith(client)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("Sent 10 tokens from account " + aliceAccountId + " to account " + bobAccountId
                + " on token " + tokenId);

        /*
         * Step 6:
         * Transfer 10 tokens from Bob to Alice.
         */
        System.out.println("Transferring tokens from the `accountId2` to the `accountId1`...");
        new TransferTransaction()
                .setNodeAccountIds(Collections.singletonList(tokenCreateTxResponse.nodeId))
                .addTokenTransfer(tokenId, bobAccountId, -10)
                .addTokenTransfer(tokenId, aliceAccountId, 10)
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("Sent 10 tokens from account " + bobAccountId + " to account " + aliceAccountId
                + " on token " + tokenId);

        /*
         * Clean up:
         * Delete created accounts and tokens.
         */
        new TokenWipeTransaction()
                .setNodeAccountIds(Collections.singletonList(tokenCreateTxResponse.nodeId))
                .setTokenId(tokenId)
                .setAccountId(aliceAccountId)
                .setAmount(10)
                .execute(client)
                .getReceipt(client);

        new TokenDeleteTransaction()
                .setNodeAccountIds(Collections.singletonList(tokenCreateTxResponse.nodeId))
                .setTokenId(tokenId)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(aliceAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(bobAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Example complete!");
    }
}
