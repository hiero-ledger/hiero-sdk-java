// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * AccountCreateParams for account create method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record AccountCreateParams(
        Optional<String> key,
        Optional<String> initialBalance,
        Optional<Boolean> receiverSignatureRequired,
        Optional<String> autoRenewPeriod,
        Optional<String> memo,
        Optional<Long> maxAutoTokenAssociations,
        Optional<String> stakedAccountId,
        Optional<String> stakedNodeId,
        Optional<Boolean> declineStakingReward,
        Optional<String> alias,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static AccountCreateParams parse(Map<String, Object> jrpcParams) throws Exception {
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
