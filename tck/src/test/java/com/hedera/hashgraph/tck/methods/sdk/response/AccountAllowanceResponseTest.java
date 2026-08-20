// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccountAllowanceResponseTest {
    @Test
    void shouldSerializeAllowanceResponseWithAllFields() {
        var response = new AccountAllowanceResponse(Status.SUCCESS);
        var json = serializeToJson(response);
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void shouldSerializeAllowanceResponseWithNull() {
        var response = new AccountAllowanceResponse(null);
        var json = serializeToJson(response);
        Assertions.assertTrue(json.contains("\"status\":null"));
    }
}
