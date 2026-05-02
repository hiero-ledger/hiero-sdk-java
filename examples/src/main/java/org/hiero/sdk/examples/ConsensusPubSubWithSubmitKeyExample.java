// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.AccountId;
import org.hiero.sdk.Client;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.PublicKey;
import org.hiero.sdk.TopicCreateTransaction;
import org.hiero.sdk.TopicDeleteTransaction;
import org.hiero.sdk.TopicId;
import org.hiero.sdk.TopicMessageQuery;
import org.hiero.sdk.TopicMessageSubmitTransaction;
import org.hiero.sdk.TransactionResponse;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * How to operate with a private HCS topic.
 * <p>
 * Create a new HCS topic with a single ED25519 Submit Key,
 * publish a number of messages to the topic signed by the Submit Key
 * and subscribe to the topic (no key required).
 */
class ConsensusPubSubWithSubmitKeyExample {

    private static final int TOTAL_MESSAGES = 5;

    private static final CountDownLatch MESSAGES_LATCH = new CountDownLatch(TOTAL_MESSAGES);

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
        System.out.println("Consensus Service Submit Message To The Private Topic And Subscribe Example Start!");

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
         * Generate ED25519 key pair (Submit Key to use with the topic).
         */
        System.out.println("Generating ED25519 key pair...");
        PrivateKey submitPrivateKey = PrivateKey.generateED25519();
        PublicKey submitPublicKey = submitPrivateKey.getPublicKey();

        /*
         * Step 2:
         * Create new HCS topic with the key right above as the topic's Submit Key required to sign all future
         * ConsensusMessageSubmitTransactions for that topic.
         */
        System.out.println("Creating new HCS topic...");
        TransactionResponse topicCreateTxResponse = new TopicCreateTransaction()
                .setTopicMemo("HCS topic with Submit Key")
                .setAdminKey(operatorPublicKey)
                .setSubmitKey(submitPublicKey)
                .execute(client);

        TopicId hederaTopicId = Objects.requireNonNull(topicCreateTxResponse.getReceipt(client).topicId);
        System.out.println(
                "Created topic with ID: " + hederaTopicId + " and public ED25519 submit key: " + submitPrivateKey);

        /*
         * Step 3:
         * Wait 5 seconds (to ensure data propagated to mirror nodes).
         */
        System.out.println("Wait 5 seconds (to ensure data propagated to mirror nodes) ...");
        Thread.sleep(5_000);

        /*
         * Step 4:
         * Subscribe to messages on the topic, printing out the received message and metadata as it is published by the
         * Hedera mirror node.
         */
        System.out.println("Setting up a mirror client...");
        new TopicMessageQuery()
                .setTopicId(hederaTopicId)
                .setStartTime(Instant.ofEpochSecond(0))
                .subscribe(client, (resp) -> {
                    String messageAsString = new String(resp.contents, StandardCharsets.UTF_8);
                    System.out.println("Topic message received!" + " | Time: "
                            + resp.consensusTimestamp + " | Content: "
                            + messageAsString);
                    MESSAGES_LATCH.countDown();
                });

        /*
         * Step 5:
         * Publish a list of messages to a topic, signing each transaction with the topic's Submit Key.
         */
        Random randomGenerator = new Random();
        for (int i = 0; i <= TOTAL_MESSAGES; i++) {
            String message = "random message " + randomGenerator.nextLong();

            System.out.println("Publishing message to the topic: " + message);

            new TopicMessageSubmitTransaction()
                    .setTopicId(hederaTopicId)
                    .setMessage(message)
                    .freezeWith(client)

                    // The transaction is automatically signed by the payer.
                    // Due to the topic having a submitKey requirement, additionally sign the transaction with that key.
                    .sign(submitPrivateKey)
                    .execute(client)
                    .transactionId
                    .getReceipt(client);

            Thread.sleep(2_000);
        }

        // Wait 60 seconds to receive all the messages. Fail if not received.
        boolean allMessagesReceived = MESSAGES_LATCH.await(60, TimeUnit.SECONDS);

        /*
         * Clean up:
         * Delete created topic.
         */
        new TopicDeleteTransaction().setTopicId(hederaTopicId).execute(client).getReceipt(client);

        client.close();

        // Fail if messages weren't received.
        if (!allMessagesReceived) {
            throw new TimeoutException("Not all topic messages were received! (Fail)");
        }

        System.out.println("Consensus Service Submit Message To The Private Topic And Subscribe Example Complete!");
    }
}

