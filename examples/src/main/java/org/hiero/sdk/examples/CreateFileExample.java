// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to create a file.
 */
class CreateFileExample {

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
        System.out.println("Create File Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        var operatorPublicKey = OPERATOR_KEY.getPublicKey();

        /*
         * Step 1:
         * Submit the file create transaction.
         */
        // The file is required to be a byte array,
        // you can easily use the bytes of a file instead.
        String fileContents = "Hedera hashgraph is great!";

        System.out.println("Creating new file...");
        TransactionResponse fileCreateTxResponse = new FileCreateTransaction()
                // Use the same key as the operator to "own" this file.
                .setKeys(operatorPublicKey)
                .setContents(fileContents)
                // The default max fee of 1 Hbar is not enough to create a file (starts around ~1.1 Hbar).
                .setMaxTransactionFee(Hbar.from(2))
                .execute(client);

        TransactionReceipt fileCreateTxReceipt = fileCreateTxResponse.getReceipt(client);
        FileId newFileId = fileCreateTxReceipt.fileId;
        Objects.requireNonNull(newFileId);
        System.out.println("Created new file with ID: " + newFileId);

        /*
         * Clean up:
         * Delete created file.
         */
        new FileDeleteTransaction().setFileId(newFileId).execute(client).getReceipt(client);

        client.close();

        System.out.println("Create File Example Complete!");
    }
}

