// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

public record FreezeUnfreezeTokenParams(
        Optional<String> accountId,
        Optional<String> tokenId,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static FreezeUnfreezeTokenParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new FreezeUnfreezeTokenParams(
                parsedAccountId,
                parsedTokenId,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
