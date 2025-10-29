// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NodeUpdateParams extends JSONRPC2Param {
    private Optional<String> nodeId;
    private Optional<String> accountId;
    private Optional<String> description;
    private Optional<List<ServiceEndpointParams>> gossipEndpoints;
    private Optional<List<ServiceEndpointParams>> serviceEndpoints;
    private Optional<String> gossipCaCertificate;
    private Optional<String> grpcCertificateHash;
    private Optional<ServiceEndpointParams> grpcWebProxyEndpoint;
    private Optional<String> adminKey;
    private Optional<Boolean> declineReward;
    private Optional<CommonTransactionParams> commonTransactionParams;

    @Override
    public NodeUpdateParams parse(Map<String, Object> jrpcParams) throws Exception {
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
                parsedCommonTx);
    }
}
