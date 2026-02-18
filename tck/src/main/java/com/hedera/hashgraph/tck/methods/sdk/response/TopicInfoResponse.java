// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import jakarta.annotation.Nullable;
import java.util.List;
import lombok.Data;
import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;

@Data
public class TopicInfoResponse implements JSONAware {
    private final String topicId;
    private final String topicMemo;
    private final String sequenceNumber;
    private final String runningHash;

    @Nullable
    private final String adminKey;

    @Nullable
    private final String submitKey;

    @Nullable
    private final String autoRenewAccountId;

    private final String autoRenewPeriod;
    private final String expirationTime;

    @Nullable
    private final String feeScheduleKey;

    @Nullable
    private final List<String> feeExemptKeys;

    @Nullable
    private final List<CustomFeeResponse> customFees;

    private final String ledgerId;

    private static void putIfNotNull(JSONObject json, String key, Object value) {
        if (value != null) {
            json.put(key, value);
        }
    }

    @Override
    public String toJSONString() {
        JSONObject json = new JSONObject();

        putIfNotNull(json, "topicId", topicId);
        putIfNotNull(json, "topicMemo", topicMemo);
        putIfNotNull(json, "sequenceNumber", sequenceNumber);
        putIfNotNull(json, "runningHash", runningHash);
        putIfNotNull(json, "adminKey", adminKey);
        putIfNotNull(json, "submitKey", submitKey);
        putIfNotNull(json, "autoRenewAccountId", autoRenewAccountId);
        putIfNotNull(json, "autoRenewPeriod", autoRenewPeriod);
        putIfNotNull(json, "expirationTime", expirationTime);
        putIfNotNull(json, "feeScheduleKey", feeScheduleKey);
        putIfNotNull(json, "feeExemptKeys", feeExemptKeys);
        putIfNotNull(json, "customFees", customFees);
        putIfNotNull(json, "ledgerId", ledgerId);

        return json.toJSONString();
    }

    @Data
    public static class CustomFeeResponse implements JSONAware {
        @Nullable
        private final String feeCollectorAccountId;

        private final Boolean allCollectorsAreExempt;
        private final FixedFeeResponse fixedFee;

        @Override
        public String toJSONString() {
            JSONObject json = new JSONObject();

            putIfNotNull(json, "feeCollectorAccountId", feeCollectorAccountId);
            putIfNotNull(json, "allCollectorsAreExempt", allCollectorsAreExempt);
            putIfNotNull(json, "fixedFee", fixedFee);

            return json.toJSONString();
        }
    }

    @Data
    public static class FixedFeeResponse implements JSONAware {
        private final String amount;

        @Nullable
        private final String denominatingTokenId;

        @Override
        public String toJSONString() {
            JSONObject json = new JSONObject();

            putIfNotNull(json, "amount", amount);
            putIfNotNull(json, "denominatingTokenId", denominatingTokenId);

            return json.toJSONString();
        }
    }
}
