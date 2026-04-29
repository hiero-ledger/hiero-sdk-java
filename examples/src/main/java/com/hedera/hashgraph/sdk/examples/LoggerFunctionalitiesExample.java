// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TopicCreateTransaction;
import com.hedera.hashgraph.sdk.TopicDeleteTransaction;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How SDK Logger works.
 *
 */
public class LoggerFunctionalitiesExample {

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

    public static void main(String[] args) throws Exception {
        System.out.println("Logger Functionalities Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        /*
         * Step 1:
         * Instantiate debug- and info-level loggers.
         */
        var debugLogger = new Logger(LogLevel.DEBUG);
        var infoLogger = new Logger(LogLevel.INFO);

        /*
         * Step 2:
         * Attach debug logger to the SDK Client.
         */
        client.setLogger(debugLogger);

        /*
         * Step 3:
         * Generate ED25519 private and public keys.
         */
        var privateKey = PrivateKey.generateED25519();
        var publicKey = privateKey.getPublicKey();

        /*
         * Step 4:
         * "Create" account.
         */
        System.out.println("\"Creating\" new account...");
        var aliasAccountId = publicKey.toAccountId(0, 0);
        var operatorPublicKey = OPERATOR_KEY.getPublicKey();

        /*
         * Step 4:
         * Transfer 10 tinybars from operator's account to newly created account to init it on Hedera network.
         */
        System.out.println("Transferring Hbar to the the new account...");
        new TransferTransaction()
                .addHbarTransfer(OPERATOR_ID, Hbar.from(1).negated())
                .addHbarTransfer(aliasAccountId, Hbar.from(1))
                .setTransactionMemo("")
                .execute(client);

        /*
         * Step 5:
         * Create a topic with attached info logger.
         */
        System.out.println("Creating new topic...(with attached info logger).");
        TopicId hederaTopicId = new TopicCreateTransaction()
                .setLogger(infoLogger)
                .setTopicMemo("Hedera topic")
                .setAdminKey(operatorPublicKey)
                .execute(client)
                .getReceipt(client)
                .topicId;
        Objects.requireNonNull(hederaTopicId);

        /*
         * Step 6:
         * Set the level of the infoLogger from info to error.
         */
        infoLogger.setLevel(LogLevel.ERROR);

        /*
         * Step 7:
         * Create a topic with attached info logger.
         *
         * This should not display any logs because currently there are no warn logs predefined in the SDK.
         */
        System.out.println("Creating new topic...(with attached info logger).");
        var logisticsTopicId = new TopicCreateTransaction()
                .setLogger(infoLogger)
                .setTopicMemo("Logistics topic")
                .setAdminKey(operatorPublicKey)
                .execute(client)
                .getReceipt(client)
                .topicId;
        Objects.requireNonNull(logisticsTopicId);

        /*
         * Step 8:
         * Silence the debugLogger - no logs should be shown.
         *
         * This can also be achieved by calling .setLevel(LogLevel.Silent).
         */
        debugLogger.setSilent(true);

        /*
         * Step 9:
         * Create a topic with attached debug logger.
         * This should not display any logs because logger was silenced.
         */
        System.out.println("Creating new topic...(with attached debug logger).");
        var supplyChainTopicId = new TopicCreateTransaction()
                .setLogger(debugLogger)
                .setTopicMemo("Supply chain topic")
                .setAdminKey(operatorPublicKey)
                .execute(client)
                .getReceipt(client)
                .topicId;
        Objects.requireNonNull(supplyChainTopicId);

        /*
         * Step 10:
         * Unsilence the debugLogger - applies back the old log level before silencing.
         */
        debugLogger.setSilent(false);

        /*
         * Step 11:
         * Create a topic with attached debug logger.
         *
         * Should produce logs.
         */
        System.out.println("Creating new topic...(with attached debug logger).");
        var chatTopicId = new TopicCreateTransaction()
                .setLogger(debugLogger)
                .setTopicMemo("Chat topic")
                .setAdminKey(operatorPublicKey)
                .execute(client)
                .getReceipt(client)
                .topicId;
        Objects.requireNonNull(chatTopicId);

        /*
         * Clean up:
         * Delete created topics.
         */
        new TopicDeleteTransaction().setTopicId(hederaTopicId).execute(client).getReceipt(client);

        new TopicDeleteTransaction()
                .setTopicId(logisticsTopicId)
                .execute(client)
                .getReceipt(client);

        new TopicDeleteTransaction()
                .setTopicId(supplyChainTopicId)
                .execute(client)
                .getReceipt(client);

        new TopicDeleteTransaction().setTopicId(chatTopicId).execute(client).getReceipt(client);

        client.close();

        System.out.println("Logger Functionalities Example Complete!");
    }
}
