// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Represent the topicInfo response.
 *
 * @param topicId the ID of the topic
 * @param topicMemo the publicly visible memo about the topic
 * @param sequenceNumber the sequence number of the last message submitted to the topic
 * @param runningHash the running hash
 * @param adminKey the admin key of the topic
 * @param submitKey the submit key of the topic
 * @param autoRenewAccountId the account ID that pays for auto-renewal
 * @param autoRenewPeriod the auto-renewal period in seconds
 * @param expirationTime the expiration time of the topic
 * @param feeScheduleKey the fee schedule key of the topic
 * @param feeExemptKeys the list of fee exempt keys
 * @param customFees the custom fees associated with the topic
 * @param ledgerId the ledger ID of the network
 */
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

    /**
     * Represent customFee response.
     *
     * @param feeCollectorAccountId the ID of the account to which all fees will be sent when assessed
     * @param allCollectorsAreExempt whether all fee collector accounts are exempt from being * charged fees when transferring the token
     * @param fixedFee the parameters of the Fixed Fee to assess
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CustomFeeResponse(
            @Nullable String feeCollectorAccountId, Boolean allCollectorsAreExempt, FixedFeeResponse fixedFee) {}

    /**
     * Represent fixedFee response.
     *
     * @param amount the amount to be assessed as a fee
     * @param denominatingTokenId the ID of the token to use to assess the fee
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FixedFeeResponse(String amount, @Nullable String denominatingTokenId) {}
}
