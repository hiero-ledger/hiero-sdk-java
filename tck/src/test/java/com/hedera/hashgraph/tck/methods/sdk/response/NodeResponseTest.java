// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NodeResponseTest {
    @Test
    void shouldSerializeNodeResponseWithAllFields() {
        var response = new NodeResponse("1", Status.SUCCESS);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"nodeId\":\"1\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void shouldSerializeNodeResponseNullFields() {
        var response = new NodeResponse(null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"nodeId\":null"));
        Assertions.assertTrue(json.contains("\"status\":null"));
    }
}
