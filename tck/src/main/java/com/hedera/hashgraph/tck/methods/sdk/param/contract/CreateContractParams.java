// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.contract;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CreateContractParams for contract create method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateContractParams extends JSONRPC2Param {
    private Optional<String> adminKey;
    private Optional<String> autoRenewPeriod;
    private Optional<String> autoRenewAccountId;
    private Optional<String> initialBalance;
    private Optional<String> bytecodeFileId;
    private Optional<String> initcode; // hex string
    private Optional<String> stakedAccountId;
    private Optional<String> stakedNodeId;
    private Optional<String> gas;
    private Optional<Boolean> declineStakingReward;
    private Optional<String> memo;
    private Optional<Long> maxAutomaticTokenAssociations;
    private Optional<String> constructorParameters; // hex string
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public CreateContractParams parse(Map<String, Object> jrpcParams) throws Exception {
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
