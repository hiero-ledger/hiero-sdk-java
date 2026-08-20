// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContractByteCodeResponseTest {
    @Test
    void shouldSerializeContractByteCodeResponseWithAllFields() {
        var response = new ContractByteCodeResponse("0.0.1", "bytecode...");
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"contractId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"bytecode\":\"bytecode...\""));
    }

    @Test
    void shouldSerializeContractByteCodeResponseNullFields() {
        var response = new ContractByteCodeResponse(null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"contractId\":null"));
        Assertions.assertTrue(json.contains("\"bytecode\":null"));
    }
}
