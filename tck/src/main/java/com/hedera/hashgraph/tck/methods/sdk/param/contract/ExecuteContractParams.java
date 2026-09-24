// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.contract;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * ExecuteContractParams for contract execute method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record ExecuteContractParams(
        String contractId,
        Optional<String> gas,
        Optional<String> amount,
        Optional<String> functionParameters, // hex string
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static ExecuteContractParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedContractId = (String) jrpcParams.get("contractId");
        var parsedGas = Optional.ofNullable((String) jrpcParams.get("gas"));
        var parsedAmount = Optional.ofNullable((String) jrpcParams.get("amount"));
        var parsedFunctionParameters = Optional.ofNullable((String) jrpcParams.get("functionParameters"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new ExecuteContractParams(
                parsedContractId,
                parsedGas,
                parsedAmount,
                parsedFunctionParameters,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
