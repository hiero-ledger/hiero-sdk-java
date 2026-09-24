// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record NodeUpdateParams(
        Optional<String> nodeId,
        Optional<String> accountId,
        Optional<String> description,
        Optional<List<ServiceEndpointParams>> gossipEndpoints,
        Optional<List<ServiceEndpointParams>> serviceEndpoints,
        Optional<String> gossipCaCertificate,
        Optional<String> grpcCertificateHash,
        Optional<ServiceEndpointParams> grpcWebProxyEndpoint,
        Optional<String> adminKey,
        Optional<Boolean> declineReward,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static NodeUpdateParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedNodeId = Optional.ofNullable((String) jrpcParams.get("nodeId"));
        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedDescription = Optional.ofNullable((String) jrpcParams.get("description"));

        Optional<List<ServiceEndpointParams>> parsedGossipEndpoints = Optional.empty();
        if (jrpcParams.containsKey("gossipEndpoints")) {
            JSONArray arr = (JSONArray) jrpcParams.get("gossipEndpoints");
            parsedGossipEndpoints = Optional.of(arr.stream()
                    .map(o -> ServiceEndpointParams.parse((JSONObject) o))
                    .toList());
        }

        Optional<List<ServiceEndpointParams>> parsedServiceEndpoints = Optional.empty();
        if (jrpcParams.containsKey("serviceEndpoints")) {
            JSONArray arr = (JSONArray) jrpcParams.get("serviceEndpoints");
            parsedServiceEndpoints = Optional.of(arr.stream()
                    .map(o -> ServiceEndpointParams.parse((JSONObject) o))
                    .toList());
        }

        var parsedGossipCert = Optional.ofNullable((String) jrpcParams.get("gossipCaCertificate"));
        var parsedGrpcCertHash = Optional.ofNullable((String) jrpcParams.get("grpcCertificateHash"));

        Optional<ServiceEndpointParams> parsedGrpcWebProxy = Optional.empty();
        if (jrpcParams.containsKey("grpcWebProxyEndpoint")) {
            parsedGrpcWebProxy =
                    Optional.of(ServiceEndpointParams.parse((JSONObject) jrpcParams.get("grpcWebProxyEndpoint")));
        }

        var parsedAdminKey = Optional.ofNullable((String) jrpcParams.get("adminKey"));
        var parsedDeclineReward = Optional.ofNullable((Boolean) jrpcParams.get("declineReward"));
        var parsedCommonTx = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new NodeUpdateParams(
                parsedNodeId,
                parsedAccountId,
                parsedDescription,
                parsedGossipEndpoints,
                parsedServiceEndpoints,
                parsedGossipCert,
                parsedGrpcCertHash,
                parsedGrpcWebProxy,
                parsedAdminKey,
                parsedDeclineReward,
                parsedCommonTx,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
