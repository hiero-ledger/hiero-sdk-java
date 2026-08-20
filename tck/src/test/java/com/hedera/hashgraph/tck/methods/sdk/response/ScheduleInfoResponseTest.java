// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import static com.hedera.hashgraph.tck.methods.ResponseSerializationTestHelper.serializeToJson;

import com.hedera.hashgraph.tck.methods.sdk.response.schedule.ScheduleInfoResponse;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ScheduleInfoResponseTest {
    @Test
    void shouldSerializeScheduleInfoResponseWithAllFields() {
        var response = new ScheduleInfoResponse(
                "0.0.1",
                "0.0.2",
                "0.0.3",
                "adminKey",
                List.of("key1", "key2"),
                "schedule memo",
                "1787170588.538185104",
                "true",
                "true",
                "0.0.4951978@1787170588.538185104",
                false,
                "10");
        var json = serializeToJson(response);

        Assertions.assertTrue(json.contains("\"scheduleId\":\"0.0.1\""));
        Assertions.assertTrue(json.contains("\"creatorAccountId\":\"0.0.2\""));
        Assertions.assertTrue(json.contains("\"payerAccountId\":\"0.0.3\""));
        Assertions.assertTrue(json.contains("\"adminKey\":\"adminKey\""));
        Assertions.assertTrue(json.contains("\"signers\":[\"key1\",\"key2\"]"));
        Assertions.assertTrue(json.contains("\"scheduleMemo\":\"schedule memo\""));
        Assertions.assertTrue(json.contains("\"expirationTime\":\"1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"executed\":\"true\""));
        Assertions.assertTrue(json.contains("\"deleted\":\"true\""));
        Assertions.assertTrue(json.contains("\"scheduledTransactionId\":\"0.0.4951978@1787170588.538185104\""));
        Assertions.assertTrue(json.contains("\"waitForExpiry\":false"));
        Assertions.assertTrue(json.contains("\"cost\":\"10\""));
    }

    @Test
    void shouldSerializeScheduleInfoResponseNullFields() {
        var response = new ScheduleInfoResponse(null, null, null, null, null, null, null, null, null, null, null, null);
        var json = serializeToJson(response);

        Assertions.assertFalse(json.contains("\"scheduleId\""));
        Assertions.assertFalse(json.contains("\"creatorAccountId\""));
        Assertions.assertFalse(json.contains("\"payerAccountId\""));
        Assertions.assertFalse(json.contains("\"adminKey\""));
        Assertions.assertFalse(json.contains("\"signers\""));
        Assertions.assertFalse(json.contains("\"scheduleMemo\""));
        Assertions.assertFalse(json.contains("\"expirationTime\""));
        Assertions.assertFalse(json.contains("\"executed\""));
        Assertions.assertFalse(json.contains("\"deleted\""));
        Assertions.assertFalse(json.contains("\"scheduledTransactionId\""));
        Assertions.assertFalse(json.contains("\"waitForExpiry\""));
        Assertions.assertFalse(json.contains("\"cost\""));
    }
}
