// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SetupParams for SDK client
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SetupParams extends JSONRPC2Param {
    private String operatorAccountId;
    private String operatorPrivateKey;
    private Optional<String> nodeIp;
    private Optional<String> nodeAccountId;
    private Optional<String> mirrorNetworkIp;
    private String sessionId;

    @Override
    public SetupParams parse(Map<String, Object> jrpcParams) throws ClassCastException {
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
