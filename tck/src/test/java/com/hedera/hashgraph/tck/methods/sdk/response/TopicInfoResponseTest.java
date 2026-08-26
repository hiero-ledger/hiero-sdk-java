// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TopicInfoResponseTest {
    @Test
    void shouldSerializeTopicInfoResponseWithAllFields() {
        var response = new TopicInfoResponse(
                "0.0.1",
                "topic memo",
                "1",
                "hash",
                "adminKey",
                "submitKey",
                "0.0.2",
                "1787170588.538185104",
                "1787170588.538185105",
                "feescheduleKey",
                List.of("key1", "key2"),
                List.of(new TopicInfoResponse.CustomFeeResponse(
                        "0.0.1", true, new TopicInfoResponse.FixedFeeResponse("100", "0.0.1"))),
                "256");
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"topicId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"topicMemo\":\"topic memo\""));
        Assertions.assertTrue(json.contains("\"sequenceNumber\":\"1\""));
        Assertions.assertTrue(json.contains("\"runningHash\":\"hash\""));
        Assertions.assertTrue(json.contains("\"adminKey\":\"adminKey\""));
        Assertions.assertTrue(json.contains("\"submitKey\":\"submitKey\""));
        Assertions.assertTrue(json.contains("\"autoRenewAccountId\":\"0.0.2\""));
        Assertions.assertTrue(json.contains("\"autoRenewPeriod\":\"1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"expirationTime\":\"1787170588.538185105\""));
        Assertions.assertTrue(json.contains("\"feeScheduleKey\":\"feescheduleKey\""));
        Assertions.assertTrue(json.contains("\"feeExemptKeys\":[\"key1\",\"key2\"]"));
        Assertions.assertTrue(
                json.contains(
                        "\"customFees\":[{\"feeCollectorAccountId\":\"0.0.1\",\"allCollectorsAreExempt\":true,\"fixedFee\":{\"amount\":\"100\",\"denominatingTokenId\":\"0.0.1\"}}]"));
        Assertions.assertTrue(json.contains("\"ledgerId\":\"256\""));
    }

    @Test
    void shouldSerializeTopicInfoResponseNullFields() {
        var response =
                new TopicInfoResponse(null, null, null, null, null, null, null, null, null, null, null, null, null);
        var json = serializeToJson(response);

        Assertions.assertFalse(json.contains("\"topicId\""));
        Assertions.assertFalse(json.contains("\"topicMemo\""));
        Assertions.assertFalse(json.contains("\"sequenceNumber\""));
        Assertions.assertFalse(json.contains("\"runningHash\""));
        Assertions.assertFalse(json.contains("\"adminKey\""));
        Assertions.assertFalse(json.contains("\"submitKey\""));
        Assertions.assertFalse(json.contains("\"autoRenewAccountId\""));
        Assertions.assertFalse(json.contains("\"autoRenewPeriod\""));
        Assertions.assertFalse(json.contains("\"expirationTime\""));
        Assertions.assertFalse(json.contains("\"feeScheduleKey\""));
        Assertions.assertFalse(json.contains("\"feeExemptKeys\""));
        Assertions.assertFalse(json.contains("\"customFees\""));
        Assertions.assertFalse(json.contains("\"ledgerId\""));
    }
}
