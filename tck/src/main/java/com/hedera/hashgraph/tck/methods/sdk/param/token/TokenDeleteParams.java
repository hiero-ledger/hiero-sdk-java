// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TokenDeleteParams for token delete method
 */
@Getter
@AllArgsConstructor
public class TokenDeleteParams implements JSONRPC2Param {
    private Optional<String> tokenId;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    public static TokenDeleteParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new TokenDeleteParams(
                parsedTokenId, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
