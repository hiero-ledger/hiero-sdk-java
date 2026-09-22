// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountBalanceQueryParams implements JSONRPC2Param {
    private Optional<String> accountId;
    private Optional<String> contractId;
    private String sessionId;

    public static AccountBalanceQueryParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedContractId = Optional.ofNullable((String) jrpcParams.get("contractId"));

        return new AccountBalanceQueryParams(
                parsedAccountId, parsedContractId, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
