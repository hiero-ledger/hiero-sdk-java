// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;

/**
 * GetAccountInfoParams for account info query method
 */
public record GetAccountInfoParams(String sessionId, String accountId) implements JSONRPC2Param {
    public static GetAccountInfoParams parse(Map<String, Object> jrpcParams) {
        return new GetAccountInfoParams(
                JSONRPCParamParser.parseSessionId(jrpcParams), (String) jrpcParams.get("accountId"));
    }
}
