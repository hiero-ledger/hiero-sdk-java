// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.ethereum;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EthereumTransactionParams extends JSONRPC2Param {
    private String ethereumData;
    private String callDataFileId;
    private String maxGasAllowance;
    private CommonTransactionParams commonTransactionParams;
    private String sessionId;

    @Override
    public EthereumTransactionParams parse(Map<String, Object> jrpcParams) throws Exception {
        Objects.requireNonNull(jrpcParams, "jrpcParams must not be null");

        var parsedEthereumData = (String) jrpcParams.get("ethereumData");
        var parsedCallDataFileId = (String) jrpcParams.get("callDataFileId");
        var parsedMaxGasAllowance = (String) jrpcParams.get("maxGasAllowance");

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new EthereumTransactionParams(
                parsedEthereumData,
                parsedCallDataFileId,
                parsedMaxGasAllowance,
                parsedCommonTransactionParams.orElse(null),
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
