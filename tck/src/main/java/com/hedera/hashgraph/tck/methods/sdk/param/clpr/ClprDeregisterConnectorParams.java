// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.clpr;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * Parameters for {@code deregisterClprConnector}.
 * Hex byte strings have no {@code 0x} prefix.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record ClprDeregisterConnectorParams(
        Optional<String> channelId,
        Optional<String> connectorId,
        Optional<String> stakeRecipient,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static ClprDeregisterConnectorParams parse(Map<String, Object> jrpcParams) throws Exception {
        return new ClprDeregisterConnectorParams(
                Optional.ofNullable((String) jrpcParams.get("channelId")),
                Optional.ofNullable((String) jrpcParams.get("connectorId")),
                Optional.ofNullable((String) jrpcParams.get("stakeRecipient")),
                JSONRPCParamParser.parseCommonTransactionParams(jrpcParams),
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
