// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileInfoResponseTest {
    @Test
    void shouldSerializeFileInfoResponseWithAllFields() {
        var response = new FileInfoResponse(
                "0.0.1", "10", "1787170588.538185104", true, "file memo", "256", List.of("key1", "key2"));
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"fileId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"size\":\"10\""));
        Assertions.assertTrue(json.contains("\"expirationTime\":\"1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"isDeleted\":true"));
        Assertions.assertTrue(json.contains("\"memo\":\"file memo\""));
        Assertions.assertTrue(json.contains("\"ledgerId\":\"256\""));
        Assertions.assertTrue(json.contains("\"keys\":[\"key1\",\"key2\"]"));
    }

    @Test
    void shouldSerializeFileInfoResponseNullFields() {
        var response = new FileInfoResponse(null, null, null, null, null, null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"fileId\":null"));
        Assertions.assertTrue(json.contains("\"size\":null"));
        Assertions.assertTrue(json.contains("\"expirationTime\":null"));
        Assertions.assertTrue(json.contains("\"isDeleted\":null"));
        Assertions.assertTrue(json.contains("\"memo\":null"));
        Assertions.assertTrue(json.contains("\"ledgerId\":null"));
        Assertions.assertTrue(json.contains("\"keys\":null"));
    }
}
