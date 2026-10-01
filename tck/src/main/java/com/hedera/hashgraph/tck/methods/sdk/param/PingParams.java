// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;

/**
 * PingParams for the ping method
 */
public record PingParams(String nodeAccountId, String sessionId) implements JSONRPC2Param {
    public static PingParams parse(Map<String, Object> jrpcParams) {
        return new PingParams((String) jrpcParams.get("nodeAccountId"), JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
