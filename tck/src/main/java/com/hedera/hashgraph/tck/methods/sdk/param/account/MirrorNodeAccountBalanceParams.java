// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;

/**
 * MirrorNodeAccountBalanceParams for the mirror node account balance query method
 */
public record MirrorNodeAccountBalanceParams(String accountId, String sessionId) implements JSONRPC2Param {
    public static MirrorNodeAccountBalanceParams parse(Map<String, Object> jrpcParams) {
        return new MirrorNodeAccountBalanceParams(
                (String) jrpcParams.get("accountId"), JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
