// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record TokenClaimAirdropParams(
        Optional<String> senderAccountId,
        Optional<String> receiverAccountId,
        Optional<String> tokenId,
        Optional<List<String>> serialNumbers,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static TokenClaimAirdropParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedSenderAccountId = Optional.ofNullable((String) jrpcParams.get("senderAccountId"));
        var parsedReceiverAccountId = Optional.ofNullable((String) jrpcParams.get("receiverAccountId"));
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var parsedSerialNumbers = Optional.ofNullable((List<String>) jrpcParams.get("serialNumbers"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new TokenClaimAirdropParams(
                parsedSenderAccountId,
                parsedReceiverAccountId,
                parsedTokenId,
                parsedSerialNumbers,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
