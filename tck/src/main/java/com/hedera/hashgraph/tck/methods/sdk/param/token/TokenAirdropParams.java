// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.param.transfer.TransferParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TokenAirdropParams for token airdrop method
 */
public record TokenAirdropParams(
        Optional<List<TransferParams>> tokenTransfers,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static TokenAirdropParams parse(Map<String, Object> jrpcParams) throws Exception {
        @SuppressWarnings("unchecked")
        var parsedTokenTransfers = Optional.ofNullable((List<Object>) jrpcParams.get("tokenTransfers"))
                .map(list -> list.stream()
                        .map(item -> {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> transferMap = (Map<String, Object>) item;
                                return TransferParams.parse(transferMap);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to parse transfer parameters", e);
                            }
                        })
                        .collect(Collectors.toList()));

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new TokenAirdropParams(
                parsedTokenTransfers, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
