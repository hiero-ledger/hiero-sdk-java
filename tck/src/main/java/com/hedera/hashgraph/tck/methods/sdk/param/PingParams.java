// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * PingParams for the ping method
 */
@Getter
@AllArgsConstructor
public class PingParams implements JSONRPC2Param {
    private String nodeAccountId;
    private String sessionId;

    public static PingParams parse(Map<String, Object> jrpcParams) {
        return new PingParams((String) jrpcParams.get("nodeAccountId"), JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
