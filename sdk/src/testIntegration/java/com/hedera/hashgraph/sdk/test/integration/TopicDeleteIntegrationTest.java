// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TopicCreateTransaction;
import com.hedera.hashgraph.sdk.TopicDeleteTransaction;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TopicDeleteIntegrationTest {
    @Test
    @DisplayName("Can delete topic")
    void canDeleteTopic() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setTopicMemo("[e2e::TopicCreateTransaction]")
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            new TopicDeleteTransaction()
                    .setTopicId(topicId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot delete immutable topic")
    void cannotDeleteImmutableTopic() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new TopicCreateTransaction().execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TopicDeleteTransaction()
                                .setTopicId(topicId)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.UNAUTHORIZED.toString());
        }
    }
}
