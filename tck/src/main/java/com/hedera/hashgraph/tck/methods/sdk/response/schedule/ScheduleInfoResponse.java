// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Represent the scheduleInfo query response
 *
 * @param scheduleId the ID of the schedule transaction
 * @param creatorAccountId the account that created the schedule transaction
 * @param payerAccountId the account that will pay for the execution of the scheduled transaction
 * @param adminKey the key that can delete the schedule transaction
 * @param signers the public keys that have signed the scheduled transaction
 * @param scheduleMemo publicly visible information about the schedule entity
 * @param expirationTime the date and time at which the schedule transaction will expire
 * @param executed the consensus time the schedule transaction was executed
 * @param deleted the consensus time the schedule transaction was deleted
 * @param scheduledTransactionId the transaction ID of the transaction being scheduled
 * @param waitForExpiry whether the scheduled transaction should wait for expiry before executing
 * @param cost the cost of the query in tinybars
 */
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
