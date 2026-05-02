// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.response.schedule;

import org.hiero.sdk.Status;
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

