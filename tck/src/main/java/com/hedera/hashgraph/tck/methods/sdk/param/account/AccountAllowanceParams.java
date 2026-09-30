// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AccountAllowanceParams(
        Optional<List<AllowanceParams>> allowances,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static AccountAllowanceParams parse(Map<String, Object> jrpcParams) throws Exception {

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);
        var parsedAllowances = JSONRPCParamParser.parseAllowances(jrpcParams);

        return new AccountAllowanceParams(
                parsedAllowances, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
