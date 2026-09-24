// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MirrorNodeAccountBalanceParams for the mirror node account balance query method
 */
@Getter
@AllArgsConstructor
public class MirrorNodeAccountBalanceParams implements JSONRPC2Param {
    private String accountId;
    private String sessionId;

    public static MirrorNodeAccountBalanceParams parse(Map<String, Object> jrpcParams) {
        return new MirrorNodeAccountBalanceParams(
                (String) jrpcParams.get("accountId"), JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
