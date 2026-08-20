// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileContentsResponseTest {
    @Test
    void shouldSerializeFileContentResponseWithAllFields() {
        var response = new FileContentsResponse("file contents");
        var json = serializeToJson(response);
        Assertions.assertTrue(json.contains("\"contents\":\"file contents\""));
    }

    @Test
    void shouldSerializeFileContentResponseNullFields() {
        var response = new FileContentsResponse(null);
        var json = serializeToJson(response);
        Assertions.assertTrue(json.contains("\"contents\":null"));
    }
}
