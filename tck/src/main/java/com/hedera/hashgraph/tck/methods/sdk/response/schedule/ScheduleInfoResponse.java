// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleInfoResponse(
        String scheduleId,
        String creatorAccountId,
        String payerAccountId,
        @Nullable String adminKey,
        List<String> signers,
        String scheduleMemo,
        @Nullable String expirationTime,
        @Nullable String executed,
        @Nullable String deleted,
        @Nullable String scheduledTransactionId,
        Boolean waitForExpiry,
        @Nullable String cost) {
    public static ScheduleInfoResponse forCostOnly(String cost) {
        return new ScheduleInfoResponse(null, null, null, null, null, null, null, null, null, null, null, cost);
    }
}
