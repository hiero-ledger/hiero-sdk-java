// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.schedule;

import com.hedera.hashgraph.sdk.Status;

/**
 * Represent response for the schedule related transaction.
 *
 * @param scheduleId the ID of the created schedule
 * @param transactionId the ID of the scheduled transaction
 * @param status the status of the submitted transaction
 */
public record ScheduleResponse(String scheduleId, String transactionId, Status status) {}
