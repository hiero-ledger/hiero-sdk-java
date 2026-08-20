// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EthereumTransactionResponseTest {
    @Test
    void shouldSerializeEthereumTransactionResponseWithAllFields() {
        var response = new EthereumTransactionResponse("SUCCESS", "0.0.1");
        var json = serializeToJson(response);
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
        Assertions.assertTrue(json.contains("\"contractId\":\"0.0.1\""));
    }

    @Test
    void shouldSerializeEthereumTransactionResponseNullFields() {
        var response = new EthereumTransactionResponse(null, null);
        var json = serializeToJson(response);
        Assertions.assertTrue(json.contains("\"status\":null"));
        Assertions.assertTrue(json.contains("\"contractId\":null"));
    }
}
