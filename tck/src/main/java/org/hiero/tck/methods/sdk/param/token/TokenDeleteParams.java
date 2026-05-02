// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.token;

import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.methods.sdk.param.CommonTransactionParams;
import org.hiero.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TokenDeleteParams for token delete method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenDeleteParams extends JSONRPC2Param {
    private Optional<String> tokenId;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new TokenDeleteParams(
                parsedTokenId, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}

