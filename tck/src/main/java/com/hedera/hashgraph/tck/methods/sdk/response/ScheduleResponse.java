// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;
import lombok.Data;

@Data
public class ScheduleResponse {
    private final String scheduleId;
    private final String transactionId;
    private final Status status;

    public ScheduleResponse(String scheduleId, String transactionId, Status status) {
        this.scheduleId = scheduleId;
        this.transactionId = transactionId;
        this.status = status;
    }
}
