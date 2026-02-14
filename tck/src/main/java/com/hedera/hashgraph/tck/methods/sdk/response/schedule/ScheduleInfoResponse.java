// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.schedule;

import java.util.List;

public record ScheduleInfoResponse(
        String scheduleId,
        String creatorAccountId,
        String payerAccountId,
        String adminKey,
        List<String> signers,
        String scheduleMemo,
        String expirationTime,
        String executed,
        String deleted,
        String scheduledTransactionId,
        Boolean waitForExpiry,
        String cost) {

    public static ScheduleInfoResponse forCostOnly(String cost) {
        return new ScheduleInfoResponse(null, null, null, null, null, null, null, null, null, null, null, cost);
    }
}
