// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * GetAccountInfoParams for account info query method
 */
@Getter
@AllArgsConstructor
public class GetAccountInfoParams implements JSONRPC2Param {
    private String sessionId;
    private String accountId;

    public static GetAccountInfoParams parse(Map<String, Object> jrpcParams) {
        return new GetAccountInfoParams(
                JSONRPCParamParser.parseSessionId(jrpcParams), (String) jrpcParams.get("accountId"));
    }
}
