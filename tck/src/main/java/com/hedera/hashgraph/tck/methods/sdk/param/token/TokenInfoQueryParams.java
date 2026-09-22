// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenInfoQueryParams implements JSONRPC2Param {
    private Optional<String> tokenId;
    private String sessionId;

    public static TokenInfoQueryParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));

        return new TokenInfoQueryParams(parsedTokenId, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
