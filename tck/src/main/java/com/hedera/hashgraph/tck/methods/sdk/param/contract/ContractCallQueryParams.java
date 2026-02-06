// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.contract;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ContractCallQueryParams extends JSONRPC2Param {
    private Optional<String> contractId;
    private Optional<String> gas;
    private Optional<String> functionParameters;
    private Optional<String> maxResultSize;
    private Optional<String> senderAccountId;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedContractId = Optional.ofNullable((String) jrpcParams.get("contractId"));
        var parsedGas = Optional.ofNullable((String) jrpcParams.get("gas"));
        var parsedFunctionParameters = Optional.ofNullable((String) jrpcParams.get("functionParameters"));
        var parsedMaxResultSize = Optional.ofNullable((String) jrpcParams.get("maxResultSize"));
        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("senderAccountId"));

        return new ContractCallQueryParams(
                parsedContractId,
                parsedGas,
                parsedFunctionParameters,
                parsedMaxResultSize,
                parsedAccountId,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
