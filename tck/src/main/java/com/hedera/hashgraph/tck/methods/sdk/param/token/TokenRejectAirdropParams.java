// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record TokenRejectAirdropParams(
        Optional<String> ownerAccountId,
        Optional<List<String>> tokenIds,
        Optional<List<String>> serialNumbers,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static TokenRejectAirdropParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedOwnerAccountId = Optional.ofNullable((String) jrpcParams.get("ownerId"));
        var parsedTokenIds = Optional.ofNullable((List<String>) jrpcParams.get("tokenIds"));
        var parsedSerialNumbers = Optional.ofNullable((List<String>) jrpcParams.get("serialNumbers"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new TokenRejectAirdropParams(
                parsedOwnerAccountId,
                parsedTokenIds,
                parsedSerialNumbers,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
