// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;

/**
 * Represent the contract response.
 *
 * @param contractId the ID of the contract
 * @param status the status of the submitted transaction
 */
public record ContractResponse(String contractId, Status status) {

    /**
     * Represent the contractInfo query response.
     *
     * @param contractId the ID of the contract
     * @param accountId the account ID associated with the contract
     * @param contractAccountId the contract account ID
     * @param adminKey the admin key controlling the contract
     * @param expirationTime the expiration time of the contract
     * @param autoRenewPeriod the auto_renew period in seconds
     * @param autoRenewAccountId the account ID for auto_renewal
     * @param storage the storage used by the contract
     * @param contractMemo the memo associated with the contract
     * @param balance the contract balance in tinybars
     * @param isDeleted state whether the contract is deleted
     * @param maxAutomaticTokenAssociations the maximum number of automatic token associations
     * @param ledgerId the ledger ID
     * @param stakingInfo the staking information
     */
    public record ContractInfoQueryResponse(
            String contractId,
            String accountId,
            String contractAccountId,
            String adminKey,
            String expirationTime,
            String autoRenewPeriod,
            String autoRenewAccountId,
            String storage,
            String contractMemo,
            String balance,
            Boolean isDeleted,
            String maxAutomaticTokenAssociations,
            String ledgerId,
            StakingInfoResponse stakingInfo) {

        /**
         * Represent the stakingInfo.
         *
         * @param declineStakingReward state whether staking rewards are declined
         * @param stakePeriodStart the stake period start timestamp
         * @param pendingReward the pending reward in tinybars
         * @param stakedToMe the amount staked to this contract in tinybars
         * @param stakedAccountId the account ID staked to
         * @param stakedNodeId the node ID staked to
         */
        public record StakingInfoResponse(
                Boolean declineStakingReward,
                String stakePeriodStart,
                String pendingReward,
                String stakedToMe,
                String stakedAccountId,
                String stakedNodeId) {}
    }
}
