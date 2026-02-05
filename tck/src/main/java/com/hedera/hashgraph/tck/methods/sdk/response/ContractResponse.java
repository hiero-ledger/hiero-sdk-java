// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;

@Data
public class ContractResponse {
    private final String contractId;
    private final Status status;

    @Data
    public static class ContractInfoQueryResponse implements JSONAware {
        private final String contractId;
        private final String accountId;
        private final String contractAccountId;
        private final String adminKey;
        private final String expirationTime;
        private final String autoRenewPeriod;
        private final String autoRenewAccountId;
        private final String storage;
        private final String contractMemo;
        private final String balance;
        private final Boolean isDeleted;
        private final String maxAutomaticTokenAssociations;
        private final String ledgerId;
        private final StakingInfoResponse stakingInfo;

        @Data
        public static class StakingInfoResponse implements JSONAware {
            private final Boolean declineStakingReward;
            private final String stakePeriodStart;
            private final String pendingReward;
            private final String stakedToMe;
            private final String stakedAccountId;
            private final String stakedNodeId;

            @Override
            public String toJSONString() {
                JSONObject json = new JSONObject();
                if (declineStakingReward != null) {
                    json.put("declineStakingReward", declineStakingReward);
                }
                if (stakePeriodStart != null) {
                    json.put("stakePeriodStart", stakePeriodStart);
                }
                if (pendingReward != null) {
                    json.put("pendingReward", pendingReward);
                }
                if (stakedToMe != null) {
                    json.put("stakedToMe", stakedToMe);
                }
                if (stakedAccountId != null) {
                    json.put("stakedAccountId", stakedAccountId);
                }
                if (stakedNodeId != null) {
                    json.put("stakedNodeId", stakedNodeId);
                }
                return json.toJSONString();
            }
        }

        @Override
        public String toJSONString() {
            JSONObject json = new JSONObject();
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("contractId", contractId);
            values.put("accountId", accountId);
            values.put("contractAccountId", contractAccountId);
            values.put("adminKey", adminKey);
            values.put("expirationTime", expirationTime);
            values.put("autoRenewPeriod", autoRenewPeriod);
            values.put("autoRenewAccountId", autoRenewAccountId);
            values.put("storage", storage);
            values.put("contractMemo", contractMemo);
            values.put("balance", balance);
            values.put("isDeleted", isDeleted);
            values.put("maxAutomaticTokenAssociations", maxAutomaticTokenAssociations);
            values.put("ledgerId", ledgerId);
            values.put("stakingInfo", stakingInfo);

            values.forEach((key, value) -> {
                if (value != null) {
                    json.put(key, value);
                }
            });
            return json.toJSONString();
        }
    }
}
