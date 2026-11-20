// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.clpr;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * Parameters for the paid CLPR proof queries. Payments are decimal tinybar strings.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record ClprQueryParams(Optional<String> queryPayment, Optional<String> maxQueryPayment, String sessionId)
        implements JSONRPC2Param {
    public static ClprQueryParams parse(Map<String, Object> jrpcParams) {
        return new ClprQueryParams(
                Optional.ofNullable((String) jrpcParams.get("queryPayment")),
                Optional.ofNullable((String) jrpcParams.get("maxQueryPayment")),
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
