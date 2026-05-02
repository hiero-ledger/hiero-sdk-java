// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * How to send large message to the private HCS topic and how to subscribe to the topic to receive it.
 */
class ConsensusPubSubChunkedExample {

    private static final CountDownLatch LARGE_MESSAGE_LATCH = new CountDownLatch(1);

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
        System.out.println("Consensus Service Submit Large Message And Subscribe Example Start!");

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
         * Generate ED25519 key pair (Submit Key to use with the topic).
         */
        System.out.println("Generating ED25519 key pair...");
        PrivateKey submitPrivateKey = PrivateKey.generateED25519();
        PublicKey submitPublicKey = submitPrivateKey.getPublicKey();

        /*
         * Step 2:
         * Create new HCS topic.
         */
        System.out.println("Creating new topic...");

        TopicId hederaTopicID = new TopicCreateTransaction()
                .setTopicMemo("hedera-sdk-java/ConsensusPubSubChunkedExample")
                .setAdminKey(operatorPublicKey)
                .setSubmitKey(submitPublicKey)
                .execute(client)
                .getReceipt(client)
                .topicId;
        Objects.requireNonNull(hederaTopicID);

        System.out.println("Created new topic with ID: " + hederaTopicID);

        /*
         * Step 3:
         * Wait 10 seconds (to ensure data propagated to mirror nodes).
         */
        System.out.println("Wait 5 seconds (to ensure data propagated to mirror nodes) ...");
        Thread.sleep(5_000);

        /*
         * Step 4:
         * Subscribe to messages on the topic, printing out the received message and metadata as it is published by the
         * Hedera mirror node.
         */
        System.out.println("Setting up a mirror client...");
        new TopicMessageQuery().setTopicId(hederaTopicID).subscribe(client, topicMessage -> {
            System.out.println("Topic message received!" + " | Time: "
                    + topicMessage.consensusTimestamp + " | Sequence No.: "
                    + topicMessage.sequenceNumber + " | Size: "
                    + topicMessage.contents.length + " bytes.");
            LARGE_MESSAGE_LATCH.countDown();
        });

        /*
         * Step 5:
         * Send large message to the topic created previously.
         */
        // Get a large file to send.
        String largeMessage = readResources("util/large_message.txt");

        // Prepare a message send transaction that requires a submit key from "somewhere else".
        Transaction<?> topicMessageSubmitTx = new TopicMessageSubmitTransaction()
                // This is value 10 by default,
                // increasing so large message will "fit".
                .setMaxChunks(15)
                .setTopicId(hederaTopicID)
                .setMessage(largeMessage)
                // Sign with the operator or "sender" of the message,
                // this is the party who will be charged the transaction fee.
                .signWithOperator(client);

        // Serialize to bytes, so we can be signed "somewhere else" by the submit key.
        byte[] transactionBytes = topicMessageSubmitTx.toBytes();

        // Now pretend we sent those bytes across the network.
        // Parse them into a transaction, so we can sign as the submit key.
        topicMessageSubmitTx = Transaction.fromBytes(transactionBytes);

        // View out the message size from the parsed transaction.
        // This can be useful to display what we are about to sign.
        long transactionMessageSize = ((TopicMessageSubmitTransaction) topicMessageSubmitTx)
                .getMessage()
                .size();
        System.out.println("Preparing to submit a message to the created topic (size of the message: "
                + transactionMessageSize + " bytes)...");

        // Sign with that Submit Key.
        topicMessageSubmitTx.sign(submitPrivateKey);

        // Now actually submit the transaction and get the receipt to ensure there were no errors.
        topicMessageSubmitTx.execute(client).getReceipt(client);

        // Wait 60 seconds to receive the message. Fail if not received.
        boolean largeMessageReceived = LARGE_MESSAGE_LATCH.await(60, TimeUnit.SECONDS);

        /*
         * Clean up:
         * Delete created topic.
         */
        new TopicDeleteTransaction().setTopicId(hederaTopicID).execute(client).getReceipt(client);

        client.close();

        // Fail if message wasn't received.
        if (!largeMessageReceived) {
            throw new TimeoutException("Large topic message was not received! (Fail)");
        }

        System.out.println("Consensus Service Submit Large Message And Subscribe Example Complete!");
    }

    private static String readResources(String filename) {
        InputStream inputStream = ConsensusPubSubChunkedExample.class.getResourceAsStream(filename);
        StringBuilder bigContents = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream), UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                bigContents.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return bigContents.toString();
    }
}

