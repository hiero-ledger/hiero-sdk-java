// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DeprecatedAccountBalanceQueryParams for the AccountBalanceQuery deprecation method
 */
@Getter
@AllArgsConstructor
public class DeprecatedAccountBalanceQueryParams implements JSONRPC2Param {
    private String accountId;
    private String operation;
    private String sessionId;

    public static DeprecatedAccountBalanceQueryParams parse(Map<String, Object> jrpcParams) {
        var accountId = Objects.requireNonNull((String) jrpcParams.get("accountId"), "accountId is required");
        var operation = Objects.requireNonNullElse((String) jrpcParams.get("operation"), "execute");
        if (!operation.equals("execute") && !operation.equals("getCost")) {
            throw new IllegalArgumentException("operation must be \"execute\" or \"getCost\"");
        }

        return new DeprecatedAccountBalanceQueryParams(
                accountId, operation, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
