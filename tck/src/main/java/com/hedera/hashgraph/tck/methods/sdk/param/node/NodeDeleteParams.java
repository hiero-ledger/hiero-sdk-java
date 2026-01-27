// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NodeDeleteParams extends JSONRPC2Param {
    private Optional<String> nodeId;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public NodeDeleteParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedNodeId = Optional.ofNullable((String) jrpcParams.get("nodeId"));
        var parsedCommonTx = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new NodeDeleteParams(parsedNodeId, parsedCommonTx, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
