// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.contract;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * CreateContractParams for contract create method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record CreateContractParams(
        Optional<String> adminKey,
        Optional<String> autoRenewPeriod,
        Optional<String> autoRenewAccountId,
        Optional<String> initialBalance,
        Optional<String> bytecodeFileId,
        Optional<String> initcode, // hex string
        Optional<String> stakedAccountId,
        Optional<String> stakedNodeId,
        Optional<String> gas,
        Optional<Boolean> declineStakingReward,
        Optional<String> memo,
        Optional<Long> maxAutomaticTokenAssociations,
        Optional<String> constructorParameters, // hex string
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static CreateContractParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedAdminKey = Optional.ofNullable((String) jrpcParams.get("adminKey"));
        var parsedAutoRenewPeriod = Optional.ofNullable((String) jrpcParams.get("autoRenewPeriod"));
        var parsedAutoRenewAccountId = Optional.ofNullable((String) jrpcParams.get("autoRenewAccountId"));
        var parsedInitialBalance = Optional.ofNullable((String) jrpcParams.get("initialBalance"));
        var parsedBytecodeFileId = Optional.ofNullable((String) jrpcParams.get("bytecodeFileId"));
        var parsedInitcode = Optional.ofNullable((String) jrpcParams.get("initcode"));
        var parsedStakedAccountId = Optional.ofNullable((String) jrpcParams.get("stakedAccountId"));
        var parsedStakedNodeId = Optional.ofNullable((String) jrpcParams.get("stakedNodeId"));
        var parsedGas = Optional.ofNullable((String) jrpcParams.get("gas"));
        var parsedDeclineStakingReward = Optional.ofNullable((Boolean) jrpcParams.get("declineStakingReward"));
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedMaxAutomaticTokenAssociations =
                Optional.ofNullable((Long) jrpcParams.get("maxAutomaticTokenAssociations"));
        var parsedConstructorParameters = Optional.ofNullable((String) jrpcParams.get("constructorParameters"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new CreateContractParams(
                parsedAdminKey,
                parsedAutoRenewPeriod,
                parsedAutoRenewAccountId,
                parsedInitialBalance,
                parsedBytecodeFileId,
                parsedInitcode,
                parsedStakedAccountId,
                parsedStakedNodeId,
                parsedGas,
                parsedDeclineStakingReward,
                parsedMemo,
                parsedMaxAutomaticTokenAssociations,
                parsedConstructorParameters,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
