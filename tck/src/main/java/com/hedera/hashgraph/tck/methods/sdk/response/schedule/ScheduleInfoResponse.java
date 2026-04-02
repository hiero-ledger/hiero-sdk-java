// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.schedule;

import jakarta.annotation.Nullable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;

@Data
@AllArgsConstructor
public class ScheduleInfoResponse implements JSONAware {
    private String scheduleId;
    private String creatorAccountId;
    private String payerAccountId;

    @Nullable
    private String adminKey;

    private List<String> signers;
    private String scheduleMemo;

    @Nullable
    private String expirationTime;

    @Nullable
    private String executed;

    @Nullable
    private String deleted;

    @Nullable
    private String scheduledTransactionId;

    private Boolean waitForExpiry;

    @Nullable
    private String cost;

    public static ScheduleInfoResponse forCostOnly(String cost) {
        return new ScheduleInfoResponse(null, null, null, null, null, null, null, null, null, null, null, cost);
    }

    @Override
    public String toJSONString() {
        JSONObject json = new JSONObject();
        putIfNotNull(json, "scheduleId", scheduleId);
        putIfNotNull(json, "creatorAccountId", creatorAccountId);
        putIfNotNull(json, "payerAccountId", payerAccountId);
        putIfNotNull(json, "adminKey", adminKey);
        putIfNotNull(json, "signers", signers);
        putIfNotNull(json, "scheduleMemo", scheduleMemo);
        putIfNotNull(json, "expirationTime", expirationTime);
        putIfNotNull(json, "executed", executed);
        putIfNotNull(json, "deleted", deleted);
        putIfNotNull(json, "scheduledTransactionId", scheduledTransactionId);
        putIfNotNull(json, "waitForExpiry", waitForExpiry);
        putIfNotNull(json, "cost", cost);
        return json.toJSONString();
    }

    private void putIfNotNull(JSONObject json, String key, Object value) {
        if (value != null) {
            json.put(key, value);
        }
    }
}
