// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TransactionReceiptResponseTest {
    @Test
    void shouldSerializeTransactionReceiptResponseWithAllFields() {
        var response = new TransactionReceiptResponse(
                "SUCCESS",
                "0.0.1",
                "0.0.2",
                "0.0.3",
                "0.0.4",
                "0.0.5",
                "0.0.6",
                new TransactionReceiptResponse.ExchangeRate(1L, 1L, "1787170588.538185104"),
                "1",
                "hash",
                "1000L",
                "0.0.4951978@1787170588.538185104",
                List.of(1L, 2l),
                null,
                null,
                "1");
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
        Assertions.assertTrue(json.contains("\"accountId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"fileId\":\"0.0.2\""));
        Assertions.assertTrue(json.contains("\"contractId\":\"0.0.3\""));
        Assertions.assertTrue(json.contains("\"topicId\":\"0.0.4\""));
        Assertions.assertTrue(json.contains("\"tokenId\":\"0.0.5\""));
        Assertions.assertTrue(json.contains("\"scheduleId\":\"0.0.6\""));
        Assertions.assertTrue(json.contains(
                "\"exchangeRate\":{\"hbars\":1,\"cents\":1,\"expirationTime\":\"1787170588.538185104\"}"));
        Assertions.assertTrue(json.contains("\"topicSequenceNumber\":\"1\""));
        Assertions.assertTrue(json.contains("\"topicRunningHash\":\"hash\""));
        Assertions.assertTrue(json.contains("\"totalSupply\":\"1000L\""));
        Assertions.assertTrue(json.contains("\"scheduledTransactionId\":\"0.0.4951978@1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"serials\":[1,2]"));
        Assertions.assertTrue(json.contains("\"nodeId\":\"1\""));
    }

    @Test
    void shouldSerializeTransactionReceiptResponseNullFields() {
        var response = new TransactionReceiptResponse(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        var json = serializeToJson(response);

        Assertions.assertFalse(json.contains("\"status\""));
        Assertions.assertFalse(json.contains("\"accountId\""));
        Assertions.assertFalse(json.contains("\"fileId\""));
        Assertions.assertFalse(json.contains("\"contractId\""));
        Assertions.assertFalse(json.contains("\"topicId\""));
        Assertions.assertFalse(json.contains("\"tokenId\""));
        Assertions.assertFalse(json.contains("\"scheduleId\""));
        Assertions.assertFalse(json.contains("\"exchangeRate\""));
        Assertions.assertFalse(json.contains("\"topicSequenceNumber\""));
        Assertions.assertFalse(json.contains("\"topicRunningHash\""));
        Assertions.assertFalse(json.contains("\"totalSupply\""));
        Assertions.assertFalse(json.contains("\"scheduledTransactionId\""));
        Assertions.assertFalse(json.contains("\"serials\""));
        Assertions.assertFalse(json.contains("\"nodeId\""));
    }
}
