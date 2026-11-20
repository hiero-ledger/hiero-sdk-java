// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.clpr;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * Parameters for {@code completeClprConnector}. {@code lockedStake} is a decimal tinybar string.
 * Hex byte strings have no {@code 0x} prefix.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record ClprCompleteConnectorParams(
        Optional<String> channelId,
        Optional<String> connectorId,
        Optional<String> salt,
        Optional<String> ownershipPrivateKey,
        Optional<String> connectorContract,
        Optional<String> adminKey,
        Optional<String> lockedStake,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static ClprCompleteConnectorParams parse(Map<String, Object> jrpcParams) throws Exception {
        return new ClprCompleteConnectorParams(
                Optional.ofNullable((String) jrpcParams.get("channelId")),
                Optional.ofNullable((String) jrpcParams.get("connectorId")),
                Optional.ofNullable((String) jrpcParams.get("salt")),
                Optional.ofNullable((String) jrpcParams.get("ownershipPrivateKey")),
                Optional.ofNullable((String) jrpcParams.get("connectorContract")),
                Optional.ofNullable((String) jrpcParams.get("adminKey")),
                Optional.ofNullable((String) jrpcParams.get("lockedStake")),
                JSONRPCParamParser.parseCommonTransactionParams(jrpcParams),
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
