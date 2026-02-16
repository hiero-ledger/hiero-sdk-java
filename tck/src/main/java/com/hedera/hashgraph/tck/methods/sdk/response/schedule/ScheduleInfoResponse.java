// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.schedule;

import jakarta.annotation.Nullable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScheduleInfoResponse {
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
}
