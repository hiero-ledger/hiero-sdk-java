// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * HIP-1139: Immutable Topics Example
 *
 * This example demonstrates how to create immutable topics using "dead keys" -
 * keys that cannot be used to sign transactions, effectively making topics
 * immutable for administrative changes and/or message submissions.
 *
 * Based on the HIP-1139 integration tests, this example covers:
 * 1. Preventing message submission when Submit Key is updated to dead key
 * 2. Allowing message submission but preventing admin updates when Admin Key is updated to empty key list
 * 3. Making topic fully immutable when both Admin and Submit keys are updated to dead keys
 * 4. Successfully updating Submit Key to dead key when topic has only Submit Key
 * 5. Making public topic administratively immutable when Admin Key is updated to empty key list
 * 6. Failing message submission when Submit Key is already dead
 * 7. Successfully updating Submit Key to dead key with valid Admin Key signature
 * 8. Successfully updating Submit Key from dead key to valid key with Admin Key signature
 */
class ImmutableTopicsExample {

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
        System.out.println("HIP-1139: Immutable Topics Example Start!");

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
         * Demo 1: Prevent message submission when Submit Key is updated to dead key
         */
        System.out.println("\n=== Demo 1: Prevent message submission when Submit Key is updated to dead key ===");

        // Create a private topic with both Admin and Submit Keys
        var adminKey1 = PrivateKey.generateECDSA();
        var submitKey1 = PrivateKey.generateECDSA();

        var response1 = new TopicCreateTransaction()
                .setAdminKey(adminKey1.getPublicKey())
                .setSubmitKey(submitKey1.getPublicKey())
                .freezeWith(client)
                .sign(adminKey1)
                .sign(submitKey1)
                .execute(client);

        var topicId1 = Objects.requireNonNull(response1.getReceipt(client).topicId);
        System.out.println("Created topic with ID: " + topicId1);

        // Verify initial message submission works
        new TopicMessageSubmitTransaction()
                .setTopicId(topicId1)
                .setMessage("Test message before dead key")
                .freezeWith(client)
                .sign(submitKey1)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Initial message submitted successfully");

        // Update Submit Key to dead key using valid Submit Key signature
        var deadKey = PublicKey.fromBytes(new byte[32]);
        new TopicUpdateTransaction()
                .setTopicId(topicId1)
                .setSubmitKey(deadKey)
                .freezeWith(client)
                .sign(submitKey1)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Submit key updated to dead key");

        // Verify that no further messages can be submitted
        try {
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId1)
                    .setMessage("Test message after dead key")
                    .execute(client)
                    .getReceipt(client);
            System.out.println("✗ ERROR: Message submission should have failed!");
        } catch (ReceiptStatusException e) {
            if (e.getMessage().contains("INVALID_SIGNATURE")) {
                System.out.println("✓ Message submission correctly failed: INVALID_SIGNATURE");
            } else {
                throw e;
            }
        }

        /*
         * Demo 2: Allow message submission but prevent admin updates when Admin Key is updated to empty key list
         */
        System.out.println(
                "\n=== Demo 2: Allow message submission but prevent admin updates when Admin Key is updated to empty key list ===");

        // Create a private topic with both Admin and Submit Keys
        var adminKey2 = PrivateKey.generateECDSA();
        var submitKey2 = PrivateKey.generateECDSA();

        var response2 = new TopicCreateTransaction()
                .setAdminKey(adminKey2.getPublicKey())
                .setSubmitKey(submitKey2.getPublicKey())
                .freezeWith(client)
                .sign(adminKey2)
                .sign(submitKey2)
                .execute(client);

        var topicId2 = Objects.requireNonNull(response2.getReceipt(client).topicId);
        System.out.println("Created topic with ID: " + topicId2);

        // Update Admin Key to empty key list using valid Admin Key signature
        new TopicUpdateTransaction()
                .setTopicId(topicId2)
                .setAdminKey(new KeyList())
                .setAutoRenewAccountId(AccountId.fromString("0.0.0"))
                .freezeWith(client)
                .sign(adminKey2)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Admin key updated to empty key list");

        // Verify messages can still be submitted with the submit key
        new TopicMessageSubmitTransaction()
                .setTopicId(topicId2)
                .setMessage("Message after admin key dead")
                .freezeWith(client)
                .sign(submitKey2)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Message submission still works");

        // Verify that no further administrative updates can be made
        try {
            new TopicUpdateTransaction()
                    .setTopicId(topicId2)
                    .setTopicMemo("Cannot update memo")
                    .execute(client)
                    .getReceipt(client);
            System.out.println("✗ ERROR: Admin operation should have failed!");
        } catch (ReceiptStatusException e) {
            if (e.getMessage().contains("UNAUTHORIZED")) {
                System.out.println("✓ Admin operation correctly failed: UNAUTHORIZED");
            } else {
                throw e;
            }
        }

        /*
         * Demo 3: Make topic fully immutable when both Admin and Submit keys are updated to dead keys
         */
        System.out.println(
                "\n=== Demo 3: Make topic fully immutable when both Admin and Submit keys are updated to dead keys ===");

        // Create a private topic with both Admin and Submit Keys
        var adminKey3 = PrivateKey.generateECDSA();
        var submitKey3 = PrivateKey.generateECDSA();

        var response3 = new TopicCreateTransaction()
                .setAdminKey(adminKey3.getPublicKey())
                .setSubmitKey(submitKey3.getPublicKey())
                .freezeWith(client)
                .sign(adminKey3)
                .sign(submitKey3)
                .execute(client);

        var topicId3 = Objects.requireNonNull(response3.getReceipt(client).topicId);
        System.out.println("Created topic with ID: " + topicId3);

        // Update both Submit Key and Admin Key to dead keys with valid Admin Key signature
        new TopicUpdateTransaction()
                .setTopicId(topicId3)
                .setSubmitKey(deadKey)
                .setAdminKey(new KeyList())
                .setAutoRenewAccountId(AccountId.fromString("0.0.0"))
                .freezeWith(client)
                .sign(adminKey3)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Both submit and admin keys updated to dead keys");

        // Verify that message submission fails
        try {
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId3)
                    .setMessage("Message should fail")
                    .execute(client)
                    .getReceipt(client);
            System.out.println("✗ ERROR: Message submission should have failed!");
        } catch (ReceiptStatusException e) {
            if (e.getMessage().contains("INVALID_SIGNATURE")) {
                System.out.println("✓ Message submission correctly failed: INVALID_SIGNATURE");
            } else {
                throw e;
            }
        }

        // Verify that administrative updates fail
        try {
            new TopicUpdateTransaction()
                    .setTopicId(topicId3)
                    .setTopicMemo("Should fail")
                    .execute(client)
                    .getReceipt(client);
            System.out.println("✗ ERROR: Admin operation should have failed!");
        } catch (ReceiptStatusException e) {
            if (e.getMessage().contains("UNAUTHORIZED")) {
                System.out.println("✓ Admin operation correctly failed: UNAUTHORIZED");
            } else {
                throw e;
            }
        }

        /*
         * Demo 4: Successfully update Submit Key to dead key when topic has only Submit Key
         */
        System.out.println(
                "\n=== Demo 4: Successfully update Submit Key to dead key when topic has only Submit Key ===");

        // Create a private topic with only Submit Key (no Admin Key)
        var submitKey4 = PrivateKey.generateECDSA();

        var response4 = new TopicCreateTransaction()
                .setSubmitKey(submitKey4.getPublicKey())
                .freezeWith(client)
                .sign(submitKey4)
                .execute(client);

        var topicId4 = Objects.requireNonNull(response4.getReceipt(client).topicId);
        System.out.println("Created topic with ID: " + topicId4);

        // Verify initial message submission works
        new TopicMessageSubmitTransaction()
                .setTopicId(topicId4)
                .setMessage("Test message before dead key")
                .freezeWith(client)
                .sign(submitKey4)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Initial message submitted successfully");

        // Update Submit Key to dead key with valid Submit Key signature
        new TopicUpdateTransaction()
                .setTopicId(topicId4)
                .setSubmitKey(deadKey)
                .freezeWith(client)
                .sign(submitKey4)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Submit key updated to dead key");

        // Verify that no more messages can be submitted
        try {
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId4)
                    .setMessage("Message should fail")
                    .execute(client)
                    .getReceipt(client);
            System.out.println("✗ ERROR: Message submission should have failed!");
        } catch (ReceiptStatusException e) {
            if (e.getMessage().contains("INVALID_SIGNATURE")) {
                System.out.println("✓ Message submission correctly failed: INVALID_SIGNATURE");
            } else {
                throw e;
            }
        }

        /*
         * Demo 5: Make public topic administratively immutable when Admin Key is updated to empty key list
         */
        System.out.println(
                "\n=== Demo 5: Make public topic administratively immutable when Admin Key is updated to empty key list ===");

        // Create a public topic with Admin Key but no Submit Key
        var adminKey5 = PrivateKey.generateECDSA();

        var response5 = new TopicCreateTransaction()
                .setAdminKey(adminKey5.getPublicKey())
                .freezeWith(client)
                .sign(adminKey5)
                .execute(client);

        var topicId5 = Objects.requireNonNull(response5.getReceipt(client).topicId);
        System.out.println("Created public topic with ID: " + topicId5);

        // Verify initial message submission works (no submit key required)
        new TopicMessageSubmitTransaction()
                .setTopicId(topicId5)
                .setMessage("Public message before dead admin key")
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Public message submitted successfully");

        // Update Admin Key to empty key list with valid Admin Key signature
        new TopicUpdateTransaction()
                .setTopicId(topicId5)
                .setAdminKey(new KeyList())
                .setAutoRenewAccountId(AccountId.fromString("0.0.0"))
                .freezeWith(client)
                .sign(adminKey5)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Admin key updated to empty key list");

        // Verify message submission still works (topic remains public)
        new TopicMessageSubmitTransaction()
                .setTopicId(topicId5)
                .setMessage("Public message after dead admin key")
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Public message submission still works");

        // Verify that administrative updates fail
        try {
            new TopicUpdateTransaction()
                    .setTopicId(topicId5)
                    .setTopicMemo("Should fail")
                    .freezeWith(client)
                    .sign(adminKey5)
                    .execute(client)
                    .getReceipt(client);
            System.out.println("✗ ERROR: Admin operation should have failed!");
        } catch (ReceiptStatusException e) {
            if (e.getMessage().contains("UNAUTHORIZED")) {
                System.out.println("✓ Admin operation correctly failed: UNAUTHORIZED");
            } else {
                throw e;
            }
        }

        /*
         * Demo 6: Fail message submission when Submit Key is dead from creation
         */
        System.out.println("\n=== Demo 6: Fail message submission when Submit Key is dead from creation ===");

        // Create a topic with dead Submit Key from the start
        var adminKey6 = PrivateKey.generateECDSA();

        var response6 = new TopicCreateTransaction()
                .setAdminKey(adminKey6.getPublicKey())
                .setSubmitKey(deadKey)
                .freezeWith(client)
                .sign(adminKey6)
                .execute(client);

        var topicId6 = Objects.requireNonNull(response6.getReceipt(client).topicId);
        System.out.println("Created topic with dead submit key, ID: " + topicId6);

        // Attempt message submission should fail
        try {
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId6)
                    .setMessage("Should fail")
                    .execute(client)
                    .getReceipt(client);
            System.out.println("✗ ERROR: Message submission should have failed!");
        } catch (ReceiptStatusException e) {
            if (e.getMessage().contains("INVALID_SIGNATURE")) {
                System.out.println("✓ Message submission correctly failed: INVALID_SIGNATURE");
            } else {
                throw e;
            }
        }

        /*
         * Demo 7: Successfully update Submit Key to dead key with valid Admin Key signature
         */
        System.out.println(
                "\n=== Demo 7: Successfully update Submit Key to dead key with valid Admin Key signature ===");

        // Create a topic with both Admin and Submit Keys
        var adminKey7 = PrivateKey.generateECDSA();
        var submitKey7 = PrivateKey.generateECDSA();

        var response7 = new TopicCreateTransaction()
                .setAdminKey(adminKey7.getPublicKey())
                .setSubmitKey(submitKey7.getPublicKey())
                .freezeWith(client)
                .sign(adminKey7)
                .sign(submitKey7)
                .execute(client);

        var topicId7 = Objects.requireNonNull(response7.getReceipt(client).topicId);
        System.out.println("Created topic with ID: " + topicId7);

        // Update Submit Key to dead key using Admin Key signature (should succeed)
        new TopicUpdateTransaction()
                .setTopicId(topicId7)
                .setSubmitKey(deadKey)
                .freezeWith(client)
                .sign(adminKey7)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Submit key updated to dead key using admin key");

        // Verify the update was successful by checking topic info
        var info7 = new TopicInfoQuery().setTopicId(topicId7).execute(client);

        if (info7.submitKey.toString().equals(deadKey.toString())) {
            System.out.println("✓ Topic info confirms submit key is now dead");
        } else {
            System.out.println("✗ ERROR: Submit key was not updated correctly!");
        }

        /*
         * Demo 8: Successfully update Submit Key from dead key to valid key with Admin Key signature
         */
        System.out.println(
                "\n=== Demo 8: Successfully update Submit Key from dead key to valid key with Admin Key signature ===");

        // Create a topic with Admin Key and dead Submit Key
        var adminKey8 = PrivateKey.generateECDSA();

        var response8 = new TopicCreateTransaction()
                .setAdminKey(adminKey8.getPublicKey())
                .setSubmitKey(deadKey)
                .freezeWith(client)
                .sign(adminKey8)
                .execute(client);

        var topicId8 = Objects.requireNonNull(response8.getReceipt(client).topicId);
        System.out.println("Created topic with dead submit key, ID: " + topicId8);

        // Update Submit Key from dead key to valid key using Admin Key signature
        var newSubmitKey8 = PrivateKey.generateECDSA();
        new TopicUpdateTransaction()
                .setTopicId(topicId8)
                .setSubmitKey(newSubmitKey8.getPublicKey())
                .freezeWith(client)
                .sign(adminKey8)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Submit key restored from dead key to valid key");

        // Verify the update was successful by submitting a message
        new TopicMessageSubmitTransaction()
                .setTopicId(topicId8)
                .setMessage("Message with restored submit key")
                .freezeWith(client)
                .sign(newSubmitKey8)
                .execute(client)
                .getReceipt(client);
        System.out.println("✓ Message submitted successfully with restored key");

        // Verify topic info shows the new key
        var info8 = new TopicInfoQuery().setTopicId(topicId8).execute(client);

        if (info8.submitKey.toString().equals(newSubmitKey8.getPublicKey().toString())) {
            System.out.println("✓ Topic info confirms submit key is now functional");
        } else {
            System.out.println("✗ ERROR: Submit key was not restored correctly!");
        }

        client.close();

        System.out.println("\nHIP-1139: Immutable Topics Example Complete!");
        System.out.println("\nSummary of demonstrated patterns:");
        System.out.println("✓ Submit key immutability using dead keys");
        System.out.println("✓ Admin key immutability using empty key lists");
        System.out.println("✓ Full topic immutability (both submit and admin)");
        System.out.println("✓ Submit-only topic immutability");
        System.out.println("✓ Public topic admin immutability");
        System.out.println("✓ Dead key creation and validation");
        System.out.println("✓ Admin-controlled submit key management");
        System.out.println("✓ Submit key restoration from dead to functional");
    }
}
