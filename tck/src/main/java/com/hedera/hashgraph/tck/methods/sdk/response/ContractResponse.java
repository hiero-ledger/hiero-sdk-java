// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;

public record ContractResponse(String contractId, Status status) {

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

        public record StakingInfoResponse(
                Boolean declineStakingReward,
                String stakePeriodStart,
                String pendingReward,
                String stakedToMe,
                String stakedAccountId,
                String stakedNodeId) {}
    }
}
