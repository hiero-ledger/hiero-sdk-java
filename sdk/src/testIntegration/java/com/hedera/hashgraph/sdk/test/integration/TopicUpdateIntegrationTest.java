// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TopicCreateTransaction;
import com.hedera.hashgraph.sdk.TopicDeleteTransaction;
import com.hedera.hashgraph.sdk.TopicInfoQuery;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TopicUpdateTransaction;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TopicUpdateIntegrationTest {
    @Test
    @DisplayName("Can update topic")
    void canUpdateTopic() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setAutoRenewAccountId(testEnv.operatorId)
                    .setTopicMemo("[e2e::TopicCreateTransaction]")
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            new TopicUpdateTransaction()
                    .clearAutoRenewAccountId()
                    .setTopicMemo("hello")
                    .setTopicId(topicId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var topicInfo = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(topicInfo.topicMemo).isEqualTo("hello");
            assertThat(topicInfo.autoRenewAccountId).isNull();

            new TopicDeleteTransaction()
                    .setTopicId(topicId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    // HIP-1139: Immutable Topics Tests
    @Test
    @DisplayName("Should prevent message submission when Submit Key is updated to dead key")
    void shouldPreventMessageSubmissionWhenSubmitKeyUpdatedToDeadKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a private topic with both Admin and Submit Keys
            var adminKey = PrivateKey.generateECDSA();
            var submitKey = PrivateKey.generateECDSA();

            var response = new TopicCreateTransaction()
                    .setAdminKey(adminKey.getPublicKey())
                    .setSubmitKey(submitKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .sign(submitKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Verify initial message submission works
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Test message before dead key")
                    .freezeWith(testEnv.client)
                    .sign(submitKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Update Submit Key to dead key using valid Admin Key signature
            var deadKey = PublicKey.fromBytes(new byte[32]);
            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setSubmitKey(deadKey)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify that no further messages can be submitted
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicMessageSubmitTransaction()
                                .setTopicId(topicId)
                                .setMessage("Test message after dead key")
                                .freezeWith(testEnv.client)
                                .sign(submitKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_SIGNATURE");
        }
    }

    @Test
    @DisplayName(
            "Should allow message submission but prevent admin updates when Admin Key is updated to empty key list")
    void shouldAllowMessageSubmissionButPreventAdminUpdatesWhenAdminKeyUpdatedToEmptyKeyList() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a private topic with both Admin and Submit Keys
            var adminKey = PrivateKey.generateECDSA();
            var submitKey = PrivateKey.generateECDSA();

            var response = new TopicCreateTransaction()
                    .setAdminKey(adminKey.getPublicKey())
                    .setSubmitKey(submitKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .sign(submitKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Update Admin Key to empty key list using valid Admin Key signature
            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setAdminKey(new KeyList())
                    .setAutoRenewAccountId(AccountId.fromString("0.0.0"))
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify messages can still be submitted with the submit key
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Message after admin key dead")
                    .freezeWith(testEnv.client)
                    .sign(submitKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify that no further administrative updates can be made
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicUpdateTransaction()
                                .setTopicId(topicId)
                                .setTopicMemo("Cannot update memo")
                                .freezeWith(testEnv.client)
                                .sign(adminKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("UNAUTHORIZED");
        }
    }

    @Test
    @DisplayName("Should make topic fully immutable when both Admin and Submit keys are updated to dead keys")
    void shouldMakeTopicFullyImmutableWhenBothKeysUpdatedToDeadKeys() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a private topic with both Admin and Submit Keys
            var adminKey = PrivateKey.generateECDSA();
            var submitKey = PrivateKey.generateECDSA();

            var response = new TopicCreateTransaction()
                    .setAdminKey(adminKey.getPublicKey())
                    .setSubmitKey(submitKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .sign(submitKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Update both Submit Key and Admin Key to dead keys with valid Admin Key signature
            var deadKey = PublicKey.fromBytes(new byte[32]);
            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setSubmitKey(deadKey)
                    .setAdminKey(new KeyList())
                    .setAutoRenewAccountId(AccountId.fromString("0.0.0"))
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify that message submission fails
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicMessageSubmitTransaction()
                                .setTopicId(topicId)
                                .setMessage("Message should fail")
                                .freezeWith(testEnv.client)
                                .sign(submitKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_SIGNATURE");

            // Verify that administrative updates fail
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicUpdateTransaction()
                                .setTopicId(topicId)
                                .setTopicMemo("Should fail")
                                .freezeWith(testEnv.client)
                                .sign(adminKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("UNAUTHORIZED");
        }
    }

    @Test
    @DisplayName("Should successfully update Submit Key to dead key when topic has only Submit Key")
    void shouldSuccessfullyUpdateSubmitKeyToDeadKeyWhenTopicHasOnlySubmitKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a private topic with only Submit Key (no Admin Key)
            var submitKey = PrivateKey.generateECDSA();

            var response = new TopicCreateTransaction()
                    .setSubmitKey(submitKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(submitKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Verify initial message submission works
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Test message before dead key")
                    .freezeWith(testEnv.client)
                    .sign(submitKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Update Submit Key to dead key with valid Submit Key signature
            var deadKey = PublicKey.fromBytes(new byte[32]);
            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setSubmitKey(deadKey)
                    .freezeWith(testEnv.client)
                    .sign(submitKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify that no more messages can be submitted
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicMessageSubmitTransaction()
                                .setTopicId(topicId)
                                .setMessage("Message should fail")
                                .freezeWith(testEnv.client)
                                .sign(submitKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_SIGNATURE");
        }
    }

    @Test
    @DisplayName("Should make public topic administratively immutable when Admin Key is updated to empty key list")
    void shouldMakePublicTopicAdministrativelyImmutableWhenAdminKeyUpdatedToEmptyKeyList() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a public topic with Admin Key but no Submit Key
            var adminKey = PrivateKey.generateECDSA();

            var response = new TopicCreateTransaction()
                    .setAdminKey(adminKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Verify initial message submission works (no submit key required)
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Public message before dead admin key")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Update Admin Key to empty key list with valid Admin Key signature
            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setAdminKey(new KeyList())
                    .setAutoRenewAccountId(AccountId.fromString("0.0.0"))
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify message submission still works (topic remains public)
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Public message after dead admin key")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify that administrative updates fail
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicUpdateTransaction()
                                .setTopicId(topicId)
                                .setTopicMemo("Should fail")
                                .freezeWith(testEnv.client)
                                .sign(adminKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("UNAUTHORIZED");
        }
    }

    @Test
    @DisplayName("Should fail message submission when Submit Key is dead")
    void shouldFailMessageSubmissionWhenSubmitKeyIsDead() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a topic with dead Submit Key
            var adminKey = PrivateKey.generateECDSA();
            var deadKey = PublicKey.fromBytes(new byte[32]);

            var response = new TopicCreateTransaction()
                    .setAdminKey(adminKey.getPublicKey())
                    .setSubmitKey(deadKey)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Attempt message submission with any key should fail
            var someKey = PrivateKey.generateECDSA();
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicMessageSubmitTransaction()
                                .setTopicId(topicId)
                                .setMessage("Should fail")
                                .freezeWith(testEnv.client)
                                .sign(someKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_SIGNATURE");
        }
    }

    @Test
    @DisplayName("Should fail to update Submit Key to dead key without valid Submit Key signature")
    void shouldFailToUpdateSubmitKeyToDeadKeyWithoutValidSubmitKeySignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a topic with Submit Key
            var submitKey = PrivateKey.generateECDSA();

            var response = new TopicCreateTransaction()
                    .setSubmitKey(submitKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(submitKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Attempt to update Submit Key without proper signature
            var deadKey = PublicKey.fromBytes(new byte[32]);
            var unauthorizedKey = PrivateKey.generateECDSA();

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicUpdateTransaction()
                                .setTopicId(topicId)
                                .setSubmitKey(deadKey)
                                .freezeWith(testEnv.client)
                                .sign(unauthorizedKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_SIGNATURE");
        }
    }

    @Test
    @DisplayName("Should fail to update Admin Key to dead key without valid Admin Key signature")
    void shouldFailToUpdateAdminKeyToDeadKeyWithoutValidAdminKeySignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a topic with Admin Key
            var adminKey = PrivateKey.generateECDSA();

            var response = new TopicCreateTransaction()
                    .setAdminKey(adminKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Attempt to update Admin Key without proper signature
            var deadKey = PublicKey.fromBytes(new byte[32]);
            var unauthorizedKey = PrivateKey.generateECDSA();

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicUpdateTransaction()
                                .setTopicId(topicId)
                                .setAdminKey(deadKey)
                                .setAutoRenewAccountId(AccountId.fromString("0.0.0"))
                                .freezeWith(testEnv.client)
                                .sign(unauthorizedKey)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining("INVALID_SIGNATURE");
        }
    }

    @Test
    @DisplayName("Should successfully update Submit Key to dead key with valid Admin Key signature")
    void shouldSuccessfullyUpdateSubmitKeyToDeadKeyWithValidAdminKeySignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a topic with both Admin and Submit Keys
            var adminKey = PrivateKey.generateECDSA();
            var submitKey = PrivateKey.generateECDSA();

            var response = new TopicCreateTransaction()
                    .setAdminKey(adminKey.getPublicKey())
                    .setSubmitKey(submitKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .sign(submitKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Update Submit Key to dead key using Admin Key signature (should succeed)
            var deadKey = PublicKey.fromBytes(new byte[32]);
            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setSubmitKey(deadKey)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify the update was successful by checking topic info
            var info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.submitKey.toString()).isEqualTo(deadKey.toString());
        }
    }

    @Test
    @DisplayName("Should successfully update Submit Key from dead key to valid key with Admin Key signature")
    void shouldSuccessfullyUpdateSubmitKeyFromDeadKeyToValidKeyWithAdminKeySignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a topic with Admin Key and dead Submit Key
            var adminKey = PrivateKey.generateECDSA();
            var deadKey = PublicKey.fromBytes(new byte[32]);

            var response = new TopicCreateTransaction()
                    .setAdminKey(adminKey.getPublicKey())
                    .setSubmitKey(deadKey)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Update Submit Key from dead key to valid key using Admin Key signature
            var newSubmitKey = PrivateKey.generateECDSA();
            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setSubmitKey(newSubmitKey.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify the update was successful by submitting a message
            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Message with restored submit key")
                    .freezeWith(testEnv.client)
                    .sign(newSubmitKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify topic info shows the new key
            var info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.submitKey.toString())
                    .isEqualTo(newSubmitKey.getPublicKey().toString());
        }
    }
}
