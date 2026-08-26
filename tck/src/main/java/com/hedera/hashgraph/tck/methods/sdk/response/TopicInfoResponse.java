// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TopicInfoResponse(
        String topicId,
        String topicMemo,
        String sequenceNumber,
        String runningHash,
        @Nullable String adminKey,
        @Nullable String submitKey,
        @Nullable String autoRenewAccountId,
        String autoRenewPeriod,
        String expirationTime,
        @Nullable String feeScheduleKey,
        @Nullable List<String> feeExemptKeys,
        @Nullable List<CustomFeeResponse> customFees,
        String ledgerId) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CustomFeeResponse(
            @Nullable String feeCollectorAccountId, Boolean allCollectorsAreExempt, FixedFeeResponse fixedFee) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FixedFeeResponse(String amount, @Nullable String denominatingTokenId) {}
}
