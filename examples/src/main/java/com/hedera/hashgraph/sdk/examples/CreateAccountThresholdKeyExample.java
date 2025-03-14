// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Collections;
import java.util.Objects;

/**
 * How to create a Hedera account with threshold key.
 */
class CreateAccountThresholdKeyExample {

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
        System.out.println("Create Account With Threshold Key Example Start!");

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
         * Generate three new Ed25519 private, public key pairs.
         *
         * You do not need the private keys to create the Threshold Key List,
         * you only need the public keys, and if you're doing things correctly,
         * you probably shouldn't have these private keys.
         */
        PrivateKey[] privateKeys = new PrivateKey[3];
        PublicKey[] publicKeys = new PublicKey[3];
        for (int i = 0; i < 3; i++) {
            PrivateKey key = PrivateKey.generateED25519();
            privateKeys[i] = key;
            publicKeys[i] = key.getPublicKey();
        }

        System.out.println("Generating public keys...");
        for (Key publicKey : publicKeys) {
            System.out.println("Generated public key: " + publicKey);
        }

        /*
         * Step 2:
         * Create a Key List.
         *
         * Require 2 of the 3 keys we generated to sign on anything modifying this account.
         */
        KeyList thresholdKey = KeyList.withThreshold(2);
        Collections.addAll(thresholdKey, publicKeys);

        /*
         * Step 2:
         * Create a new account setting a Key List from a previous step as an account's key.
         */
        System.out.println("Creating new account...");
        TransactionResponse accountCreateTxResponse = new AccountCreateTransaction()
                .setKeyWithoutAlias(thresholdKey)
                .setInitialBalance(Hbar.from(1))
                .execute(client);

        TransactionReceipt accountCreateTxReceipt = accountCreateTxResponse.getReceipt(client);
        AccountId newAccountId = Objects.requireNonNull(accountCreateTxReceipt.accountId);
        Objects.requireNonNull(newAccountId);
        System.out.println("Created account with ID: " + newAccountId);

        /*
         * Step 2:
         * Create a transfer transaction from a newly created account to demonstrate the signing process (threshold).
         */
        System.out.println("Transferring 1 Hbar from a newly created account...");
        TransactionResponse transferTxResponse = new TransferTransaction()
                .addHbarTransfer(newAccountId, Hbar.from(1).negated())
                .addHbarTransfer(new AccountId(0, 0, 3), Hbar.from(1))
                // To manually sign, you must explicitly build the Transaction.
                .freezeWith(client)
                // We sign with 2 of the 3 keys.
                .sign(privateKeys[0])
                .sign(privateKeys[1])
                .execute(client);

        // (Important!) Wait for the transfer to reach the consensus.
        transferTxResponse.getReceipt(client);

        Hbar accountBalanceAfterTransfer =
                new AccountBalanceQuery().setAccountId(newAccountId).execute(client).hbars;

        System.out.println("New account's Hbar balance after transfer: " + accountBalanceAfterTransfer);

        /*
         * Clean up:
         * Delete created account.
         */
        new AccountDeleteTransaction()
                .setTransferAccountId(OPERATOR_ID)
                .setAccountId(newAccountId)
                .freezeWith(client)
                .sign(privateKeys[0])
                .sign(privateKeys[1])
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Create Account With Threshold Key Example Complete!");
    }
}
