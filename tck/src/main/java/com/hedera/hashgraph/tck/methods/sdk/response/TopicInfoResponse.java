// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.List;
import lombok.Data;

@Data
public class TopicInfoResponse {
    private final String topicId;
    private final String topicMemo;
    private final String sequenceNumber;
    private final String runningHash;
    private final String adminKey;
    private final String submitKey;
    private final String autoRenewAccountId;
    private final String autoRenewPeriod;
    private final String expirationTime;
    private final String feeScheduleKey;
    private final List<String> feeExemptKeys;
    private final List<CustomFeeResponse> customFees;
    private final String ledgerId;
    private final String cost;

    @Data
    public static class CustomFeeResponse {
        private final String feeCollectorAccountId;
        private final Boolean allCollectorsAreExempt;
        private final FixedFeeResponse fixedFee;
    }

    @Data
    public static class FixedFeeResponse {
        private final String amount;
        private final String denominatingTokenId;
    }
}
