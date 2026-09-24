// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * SetupParams for SDK client
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record SetupParams(
        String operatorAccountId,
        String operatorPrivateKey,
        Optional<String> nodeIp,
        Optional<String> nodeAccountId,
        Optional<String> mirrorNetworkIp,
        String sessionId)
        implements JSONRPC2Param {
    public static SetupParams parse(Map<String, Object> jrpcParams) throws ClassCastException {
        var parsedOperatorAccountId = (String) jrpcParams.get("operatorAccountId");
        var parsedOperatorPrivateKey = (String) jrpcParams.get("operatorPrivateKey");
        var parsedNodeIp = Optional.ofNullable((String) jrpcParams.get("nodeIp"));
        var parsedNodeAccountId = Optional.ofNullable((String) jrpcParams.get("nodeAccountId"));
        var parsedMirrorNetworkIp = Optional.ofNullable((String) jrpcParams.get("mirrorNetworkIp"));

        return new SetupParams(
                parsedOperatorAccountId,
                parsedOperatorPrivateKey,
                parsedNodeIp,
                parsedNodeAccountId,
                parsedMirrorNetworkIp,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
