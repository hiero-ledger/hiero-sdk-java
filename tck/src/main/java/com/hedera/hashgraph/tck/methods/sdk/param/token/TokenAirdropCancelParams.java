// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record TokenAirdropCancelParams(
        Optional<List<PendingAirdropParams>> pendingAirdrops,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static TokenAirdropCancelParams parse(Map<String, Object> jrpcParams) throws Exception {
        @SuppressWarnings("unchecked")
        var parsedPendingAirdrops = Optional.ofNullable((List<Object>) jrpcParams.get("pendingAirdrops"))
                .map(list -> list.stream()
                        .map(item -> {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> airdropMap = (Map<String, Object>) item;
                                return PendingAirdropParams.parse(airdropMap);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to parse pending airdrop parameters", e);
                            }
                        })
                        .collect(Collectors.toList()));

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new TokenAirdropCancelParams(
                parsedPendingAirdrops, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
