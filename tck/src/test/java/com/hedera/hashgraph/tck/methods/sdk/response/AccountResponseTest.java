// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccountResponseTest {
    @Test
    void shouldSerializeAccountResponseWithAllFields() {
        var response = new AccountResponse("0.0.1", Status.SUCCESS, "0.0.4951978@1787170588.538185104");
        var json = serializeToJson(response);
        Assertions.assertTrue(json.contains("\"accountId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
        Assertions.assertTrue(json.contains("\"transactionId\":\"0.0.4951978@1787170588.538185104\""));
    }

    @Test
    void shouldSerializeAccountResponseWithNull() {
        var response = new AccountResponse(null, null, null);
        var json = serializeToJson(response);
        Assertions.assertTrue(json.contains("\"accountId\":null"));
        Assertions.assertTrue(json.contains("\"status\":null"));
        Assertions.assertTrue(json.contains("\"transactionId\":null"));
    }
}
