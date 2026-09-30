// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record NodeDeleteParams(
        Optional<String> nodeId, Optional<CommonTransactionParams> commonTransactionParams, String sessionId)
        implements JSONRPC2Param {
    public static NodeDeleteParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedNodeId = Optional.ofNullable((String) jrpcParams.get("nodeId"));
        var parsedCommonTx = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new NodeDeleteParams(parsedNodeId, parsedCommonTx, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
