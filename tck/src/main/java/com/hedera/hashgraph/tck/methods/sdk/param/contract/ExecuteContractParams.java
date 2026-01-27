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
 * ExecuteContractParams for contract execute method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteContractParams extends JSONRPC2Param {
    private String contractId;
    private Optional<String> gas;
    private Optional<String> amount;
    private Optional<String> functionParameters; // hex string
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public ExecuteContractParams parse(Map<String, Object> jrpcParams) throws Exception {
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
