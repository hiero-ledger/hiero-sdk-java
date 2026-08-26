// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.tck.methods.sdk.response.token.TokenBurnResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenBurnResponseTest {
    @Test
    void shouldSerializeTokenBurnResponseWithAllFields() {
        var response = new TokenBurnResponse("0.0.1", Status.SUCCESS, "100");
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"tokenId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
        Assertions.assertTrue(json.contains("\"newTotalSupply\":\"100\""));
    }

    @Test
    void shouldSerializeTokenBurnResponseNullFields() {
        var response = new TokenBurnResponse(null, null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"tokenId\":null"));
        Assertions.assertTrue(json.contains("\"status\":null"));
        Assertions.assertTrue(json.contains("\"newTotalSupply\":null"));
    }
}
