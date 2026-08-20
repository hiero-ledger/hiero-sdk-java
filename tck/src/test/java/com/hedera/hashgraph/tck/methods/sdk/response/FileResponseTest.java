// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileResponseTest {
    @Test
    void shouldSerializeFileResponseWithAllFields() {
        var response = new FileResponse("0.0.1", Status.SUCCESS);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"fileId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void shouldSerializeFileResponseNullFields() {
        var response = new FileResponse(null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"fileId\":null"));
        Assertions.assertTrue(json.contains("\"status\":null"));
    }
}
