// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AccountCreateParams for account create method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountCreateParams extends JSONRPC2Param {
    private Optional<String> key;
    private Optional<String> initialBalance;
    private Optional<Boolean> receiverSignatureRequired;
    private Optional<String> autoRenewPeriod;
    private Optional<String> memo;
    private Optional<Long> maxAutoTokenAssociations;
    private Optional<String> stakedAccountId;
    private Optional<String> stakedNodeId;
    private Optional<Boolean> declineStakingReward;
    private Optional<String> alias;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public AccountCreateParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedKey = Optional.ofNullable((String) jrpcParams.get("key"));
        var parsedInitialBalance = Optional.ofNullable((String) jrpcParams.get("initialBalance"));
        var parsedReceiverSignatureRequired =
                Optional.ofNullable((Boolean) jrpcParams.get("receiverSignatureRequired"));
        var parsedAutoRenewPeriod = Optional.ofNullable((String) jrpcParams.get("autoRenewPeriod"));
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedMaxAutoTokenAssociations = Optional.ofNullable((Long) jrpcParams.get("maxAutoTokenAssociations"));
        var parsedStakedAccountId = Optional.ofNullable((String) jrpcParams.get("stakedAccountId"));
        var parsedStakedNodeId = Optional.ofNullable((String) jrpcParams.get("stakedNodeId"));
        var parsedDeclineStakingReward = Optional.ofNullable((Boolean) jrpcParams.get("declineStakingReward"));
        var parsedAlias = Optional.ofNullable((String) jrpcParams.get("alias"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new AccountCreateParams(
                parsedKey,
                parsedInitialBalance,
                parsedReceiverSignatureRequired,
                parsedAutoRenewPeriod,
                parsedMemo,
                parsedMaxAutoTokenAssociations,
                parsedStakedAccountId,
                parsedStakedNodeId,
                parsedDeclineStakingReward,
                parsedAlias,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
