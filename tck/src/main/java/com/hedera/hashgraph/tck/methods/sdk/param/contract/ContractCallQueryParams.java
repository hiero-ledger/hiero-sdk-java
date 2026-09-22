// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.contract;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContractCallQueryParams implements JSONRPC2Param {
    private String contractId;
    private String gas;
    private String functionParameters;
    private String maxResultSize;
    private String senderAccountId;
    private String sessionId;

    public static ContractCallQueryParams parse(Map<String, Object> jrpcParams) throws Exception {
        Objects.requireNonNull(jrpcParams, "jrpcParams must not be null");

        var parsedContractId = (String) jrpcParams.get("contractId");
        var parsedGas = (String) jrpcParams.get("gas");
        var parsedFunctionParameters = (String) jrpcParams.get("functionParameters");
        var parsedMaxResultSize = (String) jrpcParams.get("maxResultSize");
        var parsedAccountId = (String) jrpcParams.get("senderAccountId");

        return new ContractCallQueryParams(
                parsedContractId,
                parsedGas,
                parsedFunctionParameters,
                parsedMaxResultSize,
                parsedAccountId,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
