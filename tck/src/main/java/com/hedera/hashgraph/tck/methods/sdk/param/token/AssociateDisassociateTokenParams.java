// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AssociateDisassociateTokenParams(
        Optional<String> accountId,
        Optional<List<String>> tokenIds,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static AssociateDisassociateTokenParams parse(Map<String, Object> jrpcParams) throws Exception {

        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedTokenIds = Optional.ofNullable((List<String>) jrpcParams.get("tokenIds"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new AssociateDisassociateTokenParams(
                parsedAccountId,
                parsedTokenIds,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
