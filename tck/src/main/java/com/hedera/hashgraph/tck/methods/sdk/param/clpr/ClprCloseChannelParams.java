// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.clpr;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * Parameters for {@code closeClprChannel}.
 * Hex byte strings have no {@code 0x} prefix.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record ClprCloseChannelParams(
        Optional<String> channelId,
        Optional<String> ownershipCommitment,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static ClprCloseChannelParams parse(Map<String, Object> jrpcParams) throws Exception {
        return new ClprCloseChannelParams(
                Optional.ofNullable((String) jrpcParams.get("channelId")),
                Optional.ofNullable((String) jrpcParams.get("ownershipCommitment")),
                JSONRPCParamParser.parseCommonTransactionParams(jrpcParams),
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
