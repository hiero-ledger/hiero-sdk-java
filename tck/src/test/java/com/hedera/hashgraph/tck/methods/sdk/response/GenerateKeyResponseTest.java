// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GenerateKeyResponseTest {
    @Test
    void shouldSerializeGenerateKeyResponseWithAllFields() {
        var response = new GenerateKeyResponse("testKey", List.of("key1", "key2"));
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"key\":\"testKey\""));
        Assertions.assertTrue(json.contains("\"privateKeys\":[\"key1\",\"key2\"]"));
    }

    @Test
    void shouldSerializeGenerateKeyResponseNullFields() {
        var response = new GenerateKeyResponse(null, new ArrayList<>());
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"key\":null"));
        Assertions.assertTrue(json.contains("\"privateKeys\":[]"));
    }
}
