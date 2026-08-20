// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContractResponseTest {
    @Test
    void shouldSerializeContractResponseWithAllFields() {
        var response = new ContractResponse("0.0.1", Status.SUCCESS);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"contractId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void shouldSerializeContractResponseNullFields() {
        var response = new ContractResponse(null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"contractId\":null"));
        Assertions.assertTrue(json.contains("\"status\":null"));
    }
}
