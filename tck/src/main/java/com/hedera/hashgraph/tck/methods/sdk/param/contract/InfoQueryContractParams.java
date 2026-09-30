// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.contract;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * InfoQueryContractParams for contract info query method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record InfoQueryContractParams(
        Optional<String> contractId, Optional<String> queryPayment, Optional<String> maxQueryPayment, String sessionId)
        implements JSONRPC2Param {
    public static InfoQueryContractParams parse(Map<String, Object> jrpcParams) throws Exception {
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
