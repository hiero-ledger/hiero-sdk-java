package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.ByteBuffer;
import java.util.Objects;

public class TopicDeadKeyExample {

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
        System.out.println("Consensus Service Submit Message To The Public Topic Example Start!");

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
        var deadKey = PublicKey.fromBytes(ByteBuffer.allocate(32).array());

        /*
         * Step 1:
         * Create new HCS topic.
         */
        System.out.println("Creating new topic...");

        TransactionResponse topicCreateTxResponse =
            new TopicCreateTransaction().setAdminKey(operatorPublicKey).execute(client);

        TransactionReceipt topicCreateTxReceipt = topicCreateTxResponse.getReceipt(client);
        TopicId topicId = topicCreateTxReceipt.topicId;
        Objects.requireNonNull(topicId);
        System.out.println("Created new topic with ID: " + topicId);

        /*
         * Step 2:
         * Submit message to the topic created in previous step.
         */
        System.out.println("Publishing message to the topic...");
        TransactionResponse topicMessageSubmitTxResponse = new TopicMessageSubmitTransaction()
            .setTopicId(topicCreateTxReceipt.topicId)
            .setMessage("Hello World")
            .execute(client);

        TransactionReceipt topicMessageSubmitTxReceipt = topicMessageSubmitTxResponse.getReceipt(client);
        System.out.println("Topic sequence number: " + topicMessageSubmitTxReceipt.topicSequenceNumber);


        /*
         * Step 3:
         * Create the topic update transaction with dead key.
         */
        System.out.println("Creating topic update transaction...");
        Transaction<?> topicUpdateTx = new TopicUpdateTransaction()
            .setTopicId(topicId)
            .setTopicMemo("This topic will be updated")
            .setAdminKey(operatorPublicKey)
            .setSubmitKey(deadKey);

        var topicUpdateReceipt = topicUpdateTx.execute(client).getReceipt(client);
        System.out.println("TopicUpdateTransaction status: " + topicUpdateReceipt.status);

        Transaction<?> topicUpdateTx1 = new TopicUpdateTransaction()
            .setTopicId(topicId)
            .setTopicMemo("This topic will be updated")
            .setAdminKey(deadKey)
            .freezeWith(client)
            .sign(OPERATOR_KEY);


        var topicUpdateReceipt1 = topicUpdateTx1.execute(client).getReceipt(client);
        System.out.println("TopicUpdateTransaction status: " + topicUpdateReceipt1.status);
        /*
         * Clean up:
         * Delete created topic.
         */
        new TopicDeleteTransaction()
            .setTopicId(topicCreateTxReceipt.topicId)
            .execute(client)
            .getReceipt(client);

        client.close();

        System.out.println("Consensus Service Submit Message To The Public Topic Example Complete!");
    }
}
