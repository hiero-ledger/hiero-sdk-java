// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.transfer;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TransferCryptoParams for transfer crypto method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransferCryptoParams extends JSONRPC2Param {
    private Optional<List<TransferParams>> transfers;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedTransfers = Optional.ofNullable(jrpcParams.get("transfers"))
                .filter(obj -> obj instanceof List)
                .map(obj -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> transfersList = (List<Map<String, Object>>) obj;
                    return transfersList.stream()
                            .map(transferMap -> {
                                try {
                                    return TransferParams.parse(transferMap);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .toList();
                });

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new TransferCryptoParams(
                parsedTransfers, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
