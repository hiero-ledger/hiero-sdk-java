// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BurnTokenParams(
        Optional<String> tokenId,
        Optional<String> amount,
        Optional<List<String>> metadata,
        Optional<List<String>> serialNumbers,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static BurnTokenParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var parsedAmount = Optional.ofNullable((String) jrpcParams.get("amount"));
        var parsedMetadata = Optional.ofNullable((List<String>) jrpcParams.get("metadata"));
        var parsedSerialNumbers = Optional.ofNullable((List<String>) jrpcParams.get("serialNumbers"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new BurnTokenParams(
                parsedTokenId,
                parsedAmount,
                parsedMetadata,
                parsedSerialNumbers,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
