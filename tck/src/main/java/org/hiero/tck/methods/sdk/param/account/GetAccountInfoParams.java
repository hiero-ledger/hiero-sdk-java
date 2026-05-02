// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.account;

import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.util.JSONRPCParamParser;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GetAccountInfoParams for account info query method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GetAccountInfoParams extends JSONRPC2Param {
    private String sessionId;
    private String accountId;

    @Override
    public GetAccountInfoParams parse(Map<String, Object> jrpcParams) {
        return new GetAccountInfoParams(
                JSONRPCParamParser.parseSessionId(jrpcParams), (String) jrpcParams.get("accountId"));
    }
}

