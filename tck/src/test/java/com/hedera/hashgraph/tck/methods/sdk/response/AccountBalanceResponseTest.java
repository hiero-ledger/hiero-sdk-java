// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.TokenId;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccountBalanceResponseTest {
    @Test
    void shouldSerializeAccountBalanceResponseWithAllFields() {
        var response = new AccountBalanceResponse(
                "1",
                Map.of(TokenId.fromString("0.0.1"), 100L, TokenId.fromString("0.0.2"), 200L),
                Map.of(TokenId.fromString("0.0.1"), 10, TokenId.fromString("0.0.2"), 12));
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"hbars\":\"1\""));
        Assertions.assertTrue(json.contains("\"tokenBalances\""));
        Assertions.assertTrue(json.contains("\"0.0.1\":100"));
        Assertions.assertTrue(json.contains("\"0.0.2\":200"));

        Assertions.assertTrue(json.contains("\"tokenDecimals\""));
        Assertions.assertTrue(json.contains("\"0.0.1\":10"));
        Assertions.assertTrue(json.contains("\"0.0.2\":12"));
    }

    @Test
    void shouldSerializeAccountBalanceResponseWithEmptyMaps() {
        var response = new AccountBalanceResponse("1", Map.of(), Map.of());
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"hbars\":\"1\""));
        Assertions.assertTrue(json.contains("\"tokenBalances\":{}"));
        Assertions.assertTrue(json.contains("\"tokenDecimals\":{}"));
    }

    @Test
    void shouldSerializeAccountBalanceResponseNullFields() {
        var response = new AccountBalanceResponse(null, null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"hbars\":null"));
        Assertions.assertTrue(json.contains("\"tokenBalances\":null"));
        Assertions.assertTrue(json.contains("\"tokenDecimals\":null"));
    }
}
