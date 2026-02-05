// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.contract;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * InfoQueryContractParams for contract info query method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InfoQueryContractParams extends JSONRPC2Param {
    private Optional<String> contractId;
    private Optional<String> queryPayment;
    private Optional<String> maxQueryPayment;
    private String sessionId;

    @Override
    public InfoQueryContractParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedContractId = Optional.ofNullable((String) jrpcParams.get("contractId"));
        var parsedQueryPayment = Optional.ofNullable((String) jrpcParams.get("queryPayment"));
        var parsedMaxQueryPayment = Optional.ofNullable((String) jrpcParams.get("maxQueryPayment"));

        return new InfoQueryContractParams(
                parsedContractId,
                parsedQueryPayment,
                parsedMaxQueryPayment,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
