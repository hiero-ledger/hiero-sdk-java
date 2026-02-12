// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.contract;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ContractCallQueryParams extends JSONRPC2Param {
    private String contractId;
    private String gas;
    private String functionParameters;
    private String maxResultSize;
    private String senderAccountId;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        if (jrpcParams == null) {
            throw new IllegalArgumentException("jrpcParams cannot be null");
        }

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
