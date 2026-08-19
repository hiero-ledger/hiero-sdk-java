// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.Data;
import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicInfoResponse {
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

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomFeeResponse {
        @Nullable
        private final String feeCollectorAccountId;

        private final Boolean allCollectorsAreExempt;
        private final FixedFeeResponse fixedFee;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FixedFeeResponse {
        private final String amount;
        @Nullable
        private final String denominatingTokenId;
    }
}
