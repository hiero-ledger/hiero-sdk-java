// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.EntityIdHelper.performQueryToMirrorNodeAsync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class RegisteredNodeAddressBookQuery {
    private long registeredNodeId;

    public RegisteredNodeAddressBookQuery setRegisteredNodeId(long registeredNodeId) {
        this.registeredNodeId = registeredNodeId;
        return this;
    }

    public long getRegisteredNodeId() {
        return registeredNodeId;
    }

    public RegisteredNodeAddressBook execute(Client client) throws ExecutionException, InterruptedException {
        String json = executeMirrorNodeRequest(client).get();
        return parseRegisterNodeAddressBook(json);
    }

    CompletableFuture<String> executeMirrorNodeRequest(Client client) {
        Objects.requireNonNull(client, "client must not be null");
        String apiEndpoint = "/api/v1/network/registered-nodes?registerednode.id=" + registeredNodeId;
        String baseUrl = client.getMirrorRestBaseUrl();

        return performQueryToMirrorNodeAsync(baseUrl, apiEndpoint, null).exceptionally(ex -> {
            client.getLogger().error("Error while performing post request to Mirror Node: " + ex.getMessage());
            throw new CompletionException(ex);
        });
    }

    private RegisteredServiceEndpoint parseJSONServiceEndpoint(JsonObject serviceEndpoint) {
        Objects.requireNonNull(serviceEndpoint, "serviceEndpoint must not be null");
        String type = serviceEndpoint.get("type").getAsString().toUpperCase();

        int port = serviceEndpoint.get("port").getAsInt();
        boolean requiresTls = serviceEndpoint.get("requires_tls").getAsBoolean();

        String rawIpAddress = serviceEndpoint.has("ip_address")
                        && !serviceEndpoint.get("ip_address").isJsonNull()
                ? serviceEndpoint.get("ip_address").getAsString()
                : null;

        byte[] ipAddressBytes = null;

        if (rawIpAddress != null && !rawIpAddress.isEmpty()) {
            try {
                ipAddressBytes = InetAddress.getByName(rawIpAddress).getAddress();
            } catch (UnknownHostException ignore) {
            }
        }

        String domainName = serviceEndpoint.has("domain_name")
                        && !serviceEndpoint.get("domain_name").isJsonNull()
                ? serviceEndpoint.get("domain_name").getAsString()
                : null;

        switch (type) {
            case "BLOCK_NODE":
                List<String> apis = new ArrayList<>();
                JsonObject blockNode = serviceEndpoint.getAsJsonObject("block_node");
                if (blockNode.has("endpoint_apis")) {
                    for (JsonElement api : blockNode.getAsJsonArray("endpoint_apis")) {
                        apis.add(api.getAsString());
                    }
                }

                return new BlockNodeServiceEndpoint()
                        .setIpAddress(ipAddressBytes)
                        .setDomainName(domainName)
                        .setPort(port)
                        .setRequiresTls(requiresTls)
                        .setEndpointApis(apis.stream()
                                .map(a -> BlockNodeApi.valueOf(a))
                                .collect(Collectors.toUnmodifiableList()));

            case "MIRROR_NODE":
                // JsonObject mirrorNode = serviceEndpoint.getAsJsonObject("mirror_node");
                return new MirrorNodeServiceEndpoint()
                        .setIpAddress(ipAddressBytes)
                        .setDomainName(domainName)
                        .setPort(port)
                        .setRequiresTls(requiresTls);

            case "RPC_RELAY":
                // JsonObject rpcRelay = serviceEndpoint.getAsJsonObject("rpc_relay");
                return new RpcRelayServiceEndpoint()
                        .setIpAddress(ipAddressBytes)
                        .setDomainName(domainName)
                        .setPort(port)
                        .setRequiresTls(requiresTls);

            case "GENERAL_SERVICE":
                JsonObject generalService =
                        serviceEndpoint.get("general_service").getAsJsonObject();
                String description = generalService.has("description")
                                && !generalService.get("description").isJsonNull()
                        ? generalService.get("description").getAsString()
                        : null;
                return new GeneralServiceEndpoint()
                        .setIpAddress(ipAddressBytes)
                        .setDomainName(domainName)
                        .setPort(port)
                        .setRequiresTls(requiresTls)
                        .setDescription(description);

            default:
                throw new IllegalArgumentException("Unknown type for serviceEndpoint " + type);
        }
    }

    private PublicKey parseJsonKey(JsonObject adminKey) {
        Objects.requireNonNull(adminKey, "adminKey must not be null");
        String type = adminKey.get("_type").getAsString() != null
                ? adminKey.get("_type").getAsString()
                : "";

        String key = adminKey.get("key").getAsString();
        switch (type) {
            case "ED25519":
                return PublicKey.fromStringED25519(key);
            case "ECDSA_SECP256K1":
                return PublicKey.fromStringECDSA(key);
            default:
                return PublicKey.fromString(key);
        }
    }

    private RegisteredNodeAddressBook parseRegisterNodeAddressBook(String json) {
        List<RegisteredNode> registeredNodes = new ArrayList<>();

        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        JsonArray registeredNodesJSON = jsonObject.getAsJsonArray("registered_nodes");
        for (JsonElement node : registeredNodesJSON) {
            long id = node.getAsJsonObject().get("registered_node_id").getAsLong();
            String description = node.getAsJsonObject().get("description").getAsString();
            PublicKey adminKey =
                    parseJsonKey(node.getAsJsonObject().get("admin_key").getAsJsonObject());
            List<RegisteredServiceEndpoint> serviceEndpoints = new ArrayList<>();

            for (JsonElement endpoints : node.getAsJsonObject().getAsJsonArray("service_endpoints")) {
                serviceEndpoints.add(parseJSONServiceEndpoint(endpoints.getAsJsonObject()));
            }

            registeredNodes.add(new RegisteredNode(id, adminKey, description, serviceEndpoints));
        }

        return new RegisteredNodeAddressBook(registeredNodes);
    }
}
