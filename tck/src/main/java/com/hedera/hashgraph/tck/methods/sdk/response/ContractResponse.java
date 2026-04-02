// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractResponse {
    private String contractId;
    private Status status;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContractInfoQueryResponse {
        private String contractId;
        private String accountId;
        private String contractAccountId;
        private String adminKey;
        private String expirationTime;
        private String autoRenewPeriod;
        private String autoRenewAccountId;
        private String storage;
        private String contractMemo;
        private String balance;
        private Boolean isDeleted;
        private String maxAutomaticTokenAssociations;
        private String ledgerId;
        private StakingInfoResponse stakingInfo;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class StakingInfoResponse {
            private Boolean declineStakingReward;
            private String stakePeriodStart;
            private String pendingReward;
            private String stakedToMe;
            private String stakedAccountId;
            private String stakedNodeId;
        }
    }
}
