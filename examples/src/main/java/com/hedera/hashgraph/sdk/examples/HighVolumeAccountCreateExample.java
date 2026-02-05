// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * Create a Hedera account using high-volume throttles.
 */
class HighVolumeAccountCreateExample {

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
        System.out.println("High-Volume Account Create Example Start!");

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
         * Generate ED25519 private and public key pair for the account.
         */
        PrivateKey privateKey = PrivateKey.generateED25519();
        PublicKey publicKey = privateKey.getPublicKey();
        System.out.println("Future account private key: " + privateKey);
        System.out.println("Future account public key: " + publicKey);

        /*
         * Step 2:
         * Create a new account using high-volume throttles and set a fee limit.
         */
        System.out.println("Creating new account with high-volume throttles...");
        TransactionResponse accountCreateTxResponse = new AccountCreateTransaction()
                .setKeyWithoutAlias(publicKey)
                .setInitialBalance(Hbar.from(1))
                .setHighVolume(true)
                .setMaxTransactionFee(Hbar.from(5))
                .execute(client);

        // This will wait for the receipt to become available.
        TransactionReceipt accountCreateTxReceipt = accountCreateTxResponse.getReceipt(client);
        AccountId newAccountId = accountCreateTxReceipt.accountId;
        Objects.requireNonNull(newAccountId);
        System.out.println("Created account with ID: " + newAccountId);

        /*
         * Clean up:
         * Delete created account.
         */
        new AccountDeleteTransaction()
                .setTransferAccountId(OPERATOR_ID)
                .setAccountId(newAccountId)
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("High-Volume Account Create Example Complete!");
    }
}
