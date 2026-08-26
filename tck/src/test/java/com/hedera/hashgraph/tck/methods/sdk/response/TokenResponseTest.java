// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.tck.methods.sdk.response.token.TokenResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenResponseTest {
    @Test
    void shouldSerializeTokenResponseWithAllFields() {
        var response = new TokenResponse("0.0.1", Status.SUCCESS);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"tokenId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void shouldSerializeTokenResponseNullFields() {
        var response = new TokenResponse(null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"tokenId\":null"));
        Assertions.assertTrue(json.contains("\"status\":null"));
    }
}
