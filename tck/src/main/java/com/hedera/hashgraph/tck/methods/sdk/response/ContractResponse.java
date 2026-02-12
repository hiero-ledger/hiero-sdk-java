// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.hedera.hashgraph.sdk.Status;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;

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
            ContractResponse.ContractInfoQueryResponse.StakingInfoResponse stakingInfo)
            implements JSONAware {
        public record StakingInfoResponse(
                Boolean declineStakingReward,
                String stakePeriodStart,
                String pendingReward,
                String stakedToMe,
                String stakedAccountId,
                String stakedNodeId)
                implements JSONAware {
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
