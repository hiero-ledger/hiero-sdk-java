// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.contract;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * UpdateContractParams for contract update method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
public class UpdateContractParams implements JSONRPC2Param {
    private Optional<String> contractId;
    private Optional<String> adminKey;
    private Optional<String> autoRenewPeriod;
    private Optional<String> autoRenewAccountId;
    private Optional<String> stakedAccountId;
    private Optional<String> stakedNodeId;
    private Optional<Boolean> declineStakingReward;
    private Optional<String> memo;
    private Optional<Long> maxAutomaticTokenAssociations;
    private Optional<String> expirationTime;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    public static UpdateContractParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedContractId = Optional.ofNullable((String) jrpcParams.get("contractId"));
        var parsedAdminKey = Optional.ofNullable((String) jrpcParams.get("adminKey"));
        var parsedAutoRenewPeriod = Optional.ofNullable((String) jrpcParams.get("autoRenewPeriod"));
        var parsedAutoRenewAccountId = Optional.ofNullable((String) jrpcParams.get("autoRenewAccountId"));
        var parsedStakedAccountId = Optional.ofNullable((String) jrpcParams.get("stakedAccountId"));
        var parsedStakedNodeId = Optional.ofNullable((String) jrpcParams.get("stakedNodeId"));
        var parsedDeclineStakingReward = Optional.ofNullable((Boolean) jrpcParams.get("declineStakingReward"));
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedMaxAutomaticTokenAssociations =
                Optional.ofNullable((Long) jrpcParams.get("maxAutomaticTokenAssociations"));
        var parsedExpirationTime = Optional.ofNullable((String) jrpcParams.get("expirationTime"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new UpdateContractParams(
                parsedContractId,
                parsedAdminKey,
                parsedAutoRenewPeriod,
                parsedAutoRenewAccountId,
                parsedStakedAccountId,
                parsedStakedNodeId,
                parsedDeclineStakingReward,
                parsedMemo,
                parsedMaxAutomaticTokenAssociations,
                parsedExpirationTime,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
