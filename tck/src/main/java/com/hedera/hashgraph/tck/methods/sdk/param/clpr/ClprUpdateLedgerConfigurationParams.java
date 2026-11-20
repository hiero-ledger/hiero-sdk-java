// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.clpr;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * Parameters for {@code updateClprLedgerConfiguration}.
 * Hex byte strings have no {@code 0x} prefix.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record ClprUpdateLedgerConfigurationParams(
        Optional<ClprThrottlesParams> throttles,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static ClprUpdateLedgerConfigurationParams parse(Map<String, Object> jrpcParams) throws Exception {
        return new ClprUpdateLedgerConfigurationParams(
                jrpcParams.get("throttles") instanceof Map<?, ?> throttles
                        ? Optional.of(ClprThrottlesParams.parse(asStringMap(throttles)))
                        : Optional.empty(),
                JSONRPCParamParser.parseCommonTransactionParams(jrpcParams),
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
