// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.schedule;

import com.hedera.hashgraph.sdk.Status;

public record ScheduleResponse(String scheduleId, String transactionId, Status status) {}
