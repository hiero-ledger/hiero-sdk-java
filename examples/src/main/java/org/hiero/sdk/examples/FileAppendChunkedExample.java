// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Collections;
import java.util.Objects;

/**
 * How to append to already created file.
 */
class FileAppendChunkedExample {

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
        System.out.println("Big File Append Example Start!");

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
         * Step 2:
         * Query file info to check its size after creation.
         */
        FileInfo fileInfoAfterCreate = new FileInfoQuery().setFileId(newFileId).execute(client);

        System.out.println("Created file size after create (according to `FileInfoQuery`): " + fileInfoAfterCreate.size
                + " bytes.");

        /*
         * Step 3:
         * Create new file contents that will be appended to a file.
         */
        StringBuilder contents = new StringBuilder();
        for (int i = 0; i <= 4096 * 9; i++) {
            contents.append("1");
        }

        /*
         * Step 4:
         * Append new file contents to a file.
         */
        System.out.println("Appending new contents to the created file...");
        new FileAppendTransaction()
                .setNodeAccountIds(Collections.singletonList(fileCreateTxResponse.nodeId))
                .setFileId(newFileId)
                .setContents(contents.toString())
                .setMaxChunks(40)
                .setMaxTransactionFee(Hbar.from(100))
                .freezeWith(client)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 5:
         * Query file info to check its size after append.
         */
        FileInfo fileInfoAfterAppend = new FileInfoQuery().setFileId(newFileId).execute(client);

        if (fileInfoAfterCreate.size < fileInfoAfterAppend.size) {
            System.out.println(
                    "File size after append (according to `FileInfoQuery`): " + fileInfoAfterAppend.size + " bytes.");
        } else {
            throw new Exception("File append was unsuccessful! (Fail)");
        }

        /*
         * Clean up:
         * Delete created file.
         */
        new FileDeleteTransaction().setFileId(newFileId).execute(client).getReceipt(client);

        client.close();

        System.out.println("Big File Append Example Complete!");
    }
}

