// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenClaimAirdropParams implements JSONRPC2Param {
    private Optional<String> senderAccountId;
    private Optional<String> receiverAccountId;
    private Optional<String> tokenId;
    private Optional<List<String>> serialNumbers;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

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
