// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Base parameters that carry the session identifier for JSON-RPC calls.
 */
@Getter
@AllArgsConstructor
public class BaseParams implements JSONRPC2Param {
    private String sessionId;

    public static BaseParams parse(Map<String, Object> jrpcParams) {
        return new BaseParams(JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
