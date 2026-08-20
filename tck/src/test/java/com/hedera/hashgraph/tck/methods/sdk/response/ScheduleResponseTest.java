// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.tck.methods.sdk.response.schedule.ScheduleResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ScheduleResponseTest {
    @Test
    void shouldSerializeScheduleResponseWithAllFields() {
        var response = new ScheduleResponse("0.0.1", "0.0.4951978@1787170588.538185104", Status.SUCCESS);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"scheduleId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"transactionId\":\"0.0.4951978@1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"status\":\"SUCCESS\""));
    }

    @Test
    void shouldSerializeScheduleResponseNullFields() {
        var response = new ScheduleResponse(null, null, null);
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"scheduleId\":null"));
        Assertions.assertTrue(json.contains("\"transactionId\":null"));
        Assertions.assertTrue(json.contains("\"status\":null"));
    }
}
