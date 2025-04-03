// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

/**
 * How to create and manage HCS topic using a threshold key as the adminKey and going through a key rotation to a new
 * set of keys.
 * <p>
 * Create a new HCS topic with a 2-of-3 threshold key for the Admin Key and
 * update the HCS topic to a 3-of-4 threshold key for the adminKey.
 */
class TopicWithAdminKeyExample {

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
        System.out.println("Topic With Admin (Threshold) Key Example Start!");

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
         * Generate the initial key pairs that are part of the Admin Key's Threshold Key.
         *
         * Three ED25519 keys part of a 2-of-3 threshold key.
         */
        System.out.println("Generating ED25519 key pairs...");
        PrivateKey[] initialAdminPrivateKeys = new PrivateKey[3];
        PublicKey[] initialAdminPublicKeys = new PublicKey[3];
        Arrays.setAll(initialAdminPrivateKeys, i -> PrivateKey.generate());
        Arrays.setAll(initialAdminPublicKeys, i -> initialAdminPrivateKeys[i].getPublicKey());

        /*
         * Step 2:
         * Create the Threshold Key.
         */
        System.out.println("Creating a Key List (threshold key)...");
        KeyList thresholdKey = KeyList.withThreshold(2);
        Collections.addAll(thresholdKey, initialAdminPublicKeys);
        System.out.println("Created a Key List: " + thresholdKey);

        /*
         * Step 3:
         * Create the topic create transaction with Threshold Key.
         */
        System.out.println("Creating topic create transaction...");
        Transaction<?> topicCreateTx = new TopicCreateTransaction()
                .setTopicMemo("demo topic")
                .setAdminKey(thresholdKey)
                .setFeeScheduleKey(thresholdKey)
                .freezeWith(client);

        /*
         * Step 4:
         * Sign the topic create transaction with 2 of 3 keys that are part of the Admin Key Threshold Key.
         */
        Arrays.stream(initialAdminPrivateKeys, 0, 2).forEach(k -> {
            System.out.println("Signing topic create transaction with key " + k);
            topicCreateTx.sign(k);
        });

        /*
         * Step 5:
         * Execute the topic create transaction.
         */
        TransactionResponse topicCreateTxResponse = topicCreateTx.execute(client);
        TopicId hederaTopicId = topicCreateTxResponse.getReceipt(client).topicId;
        Objects.requireNonNull(hederaTopicId);
        System.out.println("Created new topic (" + hederaTopicId + ") with 2-of-3 threshold key as admin key.");

        /*
         * Step 6:
         * Generate the new key pairs that are part of the Admin Key's Threshold Key.
         *
         * Four ED25519 keys part of a 3-of-4 threshold key.
         */
        System.out.println("Generating new ED25519 key pairs...");
        PrivateKey[] newAdminKeys = new PrivateKey[4];
        PublicKey[] newAdminPublicKeys = new PublicKey[4];
        Arrays.setAll(newAdminKeys, i -> PrivateKey.generate());
        Arrays.setAll(newAdminPublicKeys, i -> newAdminKeys[i].getPublicKey());

        /*
         * Step 7:
         * Create the new threshold key.
         */
        System.out.println("Creating new Key List (threshold key)...");
        KeyList newThresholdKey = KeyList.withThreshold(3);
        Collections.addAll(newThresholdKey, newAdminPublicKeys);
        System.out.println("Created new Key List: " + thresholdKey);

        /*
         * Step 8:
         * Create the topic update transaction with the new threshold key.
         */
        System.out.println("Creating topic update transaction...");
        Transaction<?> topicUpdateTx = new TopicUpdateTransaction()
                .setTopicId(hederaTopicId)
                .setTopicMemo("This topic will be updated")
                .setAdminKey(newThresholdKey)
                .setFeeScheduleKey(newThresholdKey)
                .freezeWith(client);

        /*
         * Step 9:
         * Sign the topic update transaction with the initial Admin Key.
         *
         * 2 of the 3 keys already part of the topic's Admin Key.
         */
        Arrays.stream(initialAdminPrivateKeys, 0, 2).forEach(k -> {
            System.out.println("Signing topic update transaction with initial admin key " + k);
            topicUpdateTx.sign(k);
        });

        /*
         * Step 9:
         * Sign the topic update transaction with the new Admin Key.
         * 3 of 4 keys already part of the topic's Admin Key.
         */
        Arrays.stream(newAdminKeys, 0, 3).forEach(k -> {
            System.out.println("Signing topic update transaction with new admin key " + k);
            topicUpdateTx.sign(k);
        });

        /*
         * Step 10:
         * Execute the topic update transaction.
         */
        TransactionResponse topicUpdateTxResponse = topicUpdateTx.execute(client);

        // Retrieve results post-consensus.
        topicUpdateTxResponse.getReceipt(client);
        System.out.println("Updated topic (" + hederaTopicId + ") with 3-of-4 threshold key as admin key.");

        /*
         * Step 11:
         * Query the topic info and output it.
         */
        TopicInfo hederaTopicInfo =
                new TopicInfoQuery().setTopicId(hederaTopicId).execute(client);
        System.out.println("Topic info: " + hederaTopicInfo);

        /*
         * Clean up:
         * Delete created topic.
         */
        var topicDeleteTransaction =
                new TopicDeleteTransaction().setTopicId(hederaTopicId).freezeWith(client);

        Arrays.stream(newAdminKeys, 0, 3).forEach(k -> {
            topicDeleteTransaction.sign(k);
        });

        topicDeleteTransaction.execute(client).getReceipt(client);

        client.close();

        System.out.println("Topic With Admin (Threshold) Key Example Complete!");
    }
}
