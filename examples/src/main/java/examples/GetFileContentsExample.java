// SPDX-License-Identifier: Apache-2.0
package examples;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * How to get file contents.
 *
 */
class GetFileContentsExample {

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
        System.out.println("Get File Contents Example Start!");

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
        // Content to be stored in the file.
        byte[] fileContents = "Hedera is great!".getBytes(StandardCharsets.UTF_8);

        // Create the new file and set its properties.
        System.out.println("Creating new file...");
        TransactionResponse fileCreateTxResponse = new FileCreateTransaction()
                // The public key of the owner of the file.
                .setKeys(operatorPublicKey)
                // Contents of the file.
                .setContents(fileContents)
                .setMaxTransactionFee(Hbar.from(2))
                .execute(client);

        FileId newFileId = Objects.requireNonNull(fileCreateTxResponse.getReceipt(client).fileId);
        Objects.requireNonNull(newFileId);
        System.out.println("Created new file with ID: " + newFileId);

        /*
         * Step 2:
         * Get file contents and print them.
         */
        ByteString contents = new FileContentsQuery().setFileId(newFileId).execute(client);

        Objects.requireNonNull(contents);
        // Prints query results to console.
        System.out.println("File contents: " + contents.toStringUtf8());

        /*
         * Clean up:
         * Delete created file.
         */
        new FileDeleteTransaction().setFileId(newFileId).execute(client).getReceipt(client);

        client.close();

        System.out.println("Get File Contents Example Complete!");
    }
}
