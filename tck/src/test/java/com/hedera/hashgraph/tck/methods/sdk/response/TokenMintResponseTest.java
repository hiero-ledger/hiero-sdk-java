// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.tck.methods.sdk.response.token.TokenMintResponse;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenMintResponseTest {
    @Test
    void shouldSerializeTokenMintResponseWithAllFields() {
        var response = new TokenMintResponse("0.0.1", Status.SUCCESS, "100", List.of("1", "2"));
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"tokenId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
        Assertions.assertTrue(json.contains("\"newTotalSupply\":\"100\""));
        Assertions.assertTrue(json.contains("\"serialNumbers\":[\"1\",\"2\"]"));
    }

    @Test
    void shouldSerializeTokenMintResponseNullFields() {
        var response = new TokenMintResponse(null, null, null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"tokenId\":null"));
        Assertions.assertTrue(json.contains("\"status\":null"));
        Assertions.assertTrue(json.contains("\"newTotalSupply\":null"));
        Assertions.assertTrue(json.contains("\"serialNumbers\":null"));
    }
}
