// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * AccountUpdateParams for account update method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record AccountUpdateParams(
        Optional<String> key,
        Optional<Boolean> receiverSignatureRequired,
        Optional<String> autoRenewPeriod,
        Optional<String> memo,
        Optional<String> expirationTime,
        Optional<Long> maxAutoTokenAssociations,
        Optional<String> stakedAccountId,
        Optional<String> accountId,
        Optional<String> stakedNodeId,
        Optional<Boolean> declineStakingReward,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static AccountUpdateParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedKey = Optional.ofNullable((String) jrpcParams.get("key"));
        var parsedReceiverSignatureRequired =
                Optional.ofNullable((Boolean) jrpcParams.get("receiverSignatureRequired"));
        var parsedAutoRenewPeriod = Optional.ofNullable((String) jrpcParams.get("autoRenewPeriod"));
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedMaxAutoTokenAssociations = Optional.ofNullable((Long) jrpcParams.get("maxAutoTokenAssociations"));
        var parsedStakedAccountId = Optional.ofNullable((String) jrpcParams.get("stakedAccountId"));
        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedStakedNodeId = Optional.ofNullable((String) jrpcParams.get("stakedNodeId"));
        var parsedExpirationTime = Optional.ofNullable((String) jrpcParams.get("expirationTime"));
        var parsedDeclineStakingReward = Optional.ofNullable((Boolean) jrpcParams.get("declineStakingReward"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new AccountUpdateParams(
                parsedKey,
                parsedReceiverSignatureRequired,
                parsedAutoRenewPeriod,
                parsedMemo,
                parsedExpirationTime,
                parsedMaxAutoTokenAssociations,
                parsedStakedAccountId,
                parsedAccountId,
                parsedStakedNodeId,
                parsedDeclineStakingReward,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
