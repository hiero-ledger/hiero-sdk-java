// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.clpr;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * Parameters for {@code completeClprChannel}.
 * Hex byte strings have no {@code 0x} prefix.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record ClprCompleteChannelParams(
        Optional<String> channelId,
        Optional<String> ownershipPrivateKey,
        Optional<String> verifierContract,
        Optional<String> configProofBytes,
        Optional<String> endpointManifestProofBytes,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static ClprCompleteChannelParams parse(Map<String, Object> jrpcParams) throws Exception {
        return new ClprCompleteChannelParams(
                Optional.ofNullable((String) jrpcParams.get("channelId")),
                Optional.ofNullable((String) jrpcParams.get("ownershipPrivateKey")),
                Optional.ofNullable((String) jrpcParams.get("verifierContract")),
                Optional.ofNullable((String) jrpcParams.get("configProofBytes")),
                Optional.ofNullable((String) jrpcParams.get("endpointManifestProofBytes")),
                JSONRPCParamParser.parseCommonTransactionParams(jrpcParams),
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
