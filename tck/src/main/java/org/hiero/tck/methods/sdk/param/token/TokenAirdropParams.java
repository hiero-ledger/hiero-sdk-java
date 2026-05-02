// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.token;

import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.methods.sdk.param.CommonTransactionParams;
import org.hiero.tck.methods.sdk.param.transfer.TransferParams;
import org.hiero.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TokenAirdropParams for token airdrop method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenAirdropParams extends JSONRPC2Param {

    private Optional<List<TransferParams>> tokenTransfers;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
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

