// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TopicResponseTest {
    @Test
    void shouldSerializeTopicResponseWithAllFields() {
        var response = new TopicResponse("0.0.1", Status.SUCCESS);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"topicId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void shouldSerializeTopicResponseNullFields() {
        var response = new TopicResponse(null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"topicId\":null"));
        Assertions.assertTrue(json.contains("\"status\":null"));
    }
}
