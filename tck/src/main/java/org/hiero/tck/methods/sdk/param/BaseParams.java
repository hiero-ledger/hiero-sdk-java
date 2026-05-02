// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.util.JSONRPCParamParser;

/**
 * Base parameters that carry the session identifier for JSON-RPC calls.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BaseParams extends JSONRPC2Param {
    private String sessionId;

    @Override
    public BaseParams parse(Map<String, Object> jrpcParams) {
        return new BaseParams(JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
