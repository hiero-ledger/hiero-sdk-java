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
import javax.annotation.Nullable;

/**
 * Query the mirror node for the RegisteredAddressBook.
 */
public class RegisteredNodeAddressBookQuery {
    private long registeredNodeId;

    /**
     * Sets the ID of the registered node to retrieve.
     *
     * @param registeredNodeId The unique identifier of the node.
     * @return {@code this}
     */
    public RegisteredNodeAddressBookQuery setRegisteredNodeId(long registeredNodeId) {
        this.registeredNodeId = registeredNodeId;
        return this;
    }

    /**
     * Returns the set registered node ID.
     *
     * @return The registered node ID.
     */
    public long getRegisteredNodeId() {
        return registeredNodeId;
    }

    /**
     * Executes the query with the user supplied client
     *
     * @param client The Client instance to perform the operation with
     * @return The registeredAddressBook
     * @throws ExecutionException
     * @throws InterruptedException
     */
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

    /**
     * Converts the Mirror Node JSON response to {@link RegisteredNodeAddressBook}.
     */
    private RegisteredNodeAddressBook parseRegisterNodeAddressBook(String json) {
        List<RegisteredNode> registeredNodes = new ArrayList<>();

        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        JsonArray registeredNodesJSON = jsonObject.getAsJsonArray("registered_nodes");

        for (JsonElement node : registeredNodesJSON) {
            registeredNodes.add(parseRegisteredNode(node.getAsJsonObject()));
        }

        return new RegisteredNodeAddressBook(registeredNodes);
    }

    /**
     * Parses a single node entry from the Mirror Node 'registered_nodes' array.
     */
    private RegisteredNode parseRegisteredNode(JsonObject nodeJson) {
        long id = nodeJson.get("registered_node_id").getAsLong();
        String description = nodeJson.get("description").getAsString();
        PublicKey adminKey = parseJsonKey(nodeJson.get("admin_key").getAsJsonObject());

        List<RegisteredServiceEndpoint> endpoints = new ArrayList<>();
        for (JsonElement endpoint : nodeJson.getAsJsonArray("service_endpoints")) {
            endpoints.add(parseJSONServiceEndpoint(endpoint.getAsJsonObject()));
        }

        return new RegisteredNode(id, adminKey, description, endpoints);
    }

    /**
     * Parses a single service endpoint from a JSON object.
     */
    private RegisteredServiceEndpoint parseJSONServiceEndpoint(JsonObject serviceEndpoint) {
        Objects.requireNonNull(serviceEndpoint, "serviceEndpoint must not be null");

        String type = serviceEndpoint.get("type").getAsString().toUpperCase();
        int port = serviceEndpoint.get("port").getAsInt();
        boolean requiresTls = serviceEndpoint.get("requires_tls").getAsBoolean();

        String domainName = serviceEndpoint.has("domain_name")
                        && !serviceEndpoint.get("domain_name").isJsonNull()
                ? serviceEndpoint.get("domain_name").getAsString()
                : null;

        byte[] ipAddress = parseIpAddress(serviceEndpoint);

        RegisteredServiceEndpointBase<?> registeredServiceEndpoint =
                switch (type) {
                    case "BLOCK_NODE" -> buildBlockNodeEndpoint(serviceEndpoint.getAsJsonObject("block_node"));
                    case "MIRROR_NODE" -> buildMirrorNodeEndpoint(serviceEndpoint.getAsJsonObject("mirror_node"));
                    case "RPC_RELAY" -> buildRpcRelayEndpoint(serviceEndpoint.getAsJsonObject("rpc_relay"));
                    case "GENERAL_SERVICE" -> buildGeneralEndpoint(serviceEndpoint.getAsJsonObject("general_service"));
                    default -> throw new IllegalArgumentException("Unknown type for serviceEndpoint " + type);
                };

        return registeredServiceEndpoint
                .setIpAddress(ipAddress)
                .setDomainName(domainName)
                .setPort(port)
                .setRequiresTls(requiresTls);
    }

    /**
     * Parses the admin key from the JSON representation.
     */
    private PublicKey parseJsonKey(JsonObject adminKey) {
        Objects.requireNonNull(adminKey, "adminKey must not be null");
        String type = adminKey.get("_type").getAsString() != null
                ? adminKey.get("_type").getAsString()
                : "";

        String key = adminKey.get("key").getAsString();
        return switch (type) {
            case "ED25519" -> PublicKey.fromStringED25519(key);
            case "ECDSA_SECP256K1" -> PublicKey.fromStringECDSA(key);
            default -> PublicKey.fromString(key);
        };
    }

    @Nullable
    private byte[] parseIpAddress(JsonObject json) {
        if (json.has("ip_address") && !json.get("ip_address").isJsonNull()) {
            String rawIp = json.get("ip_address").getAsString();
            if (!rawIp.isEmpty()) {
                try {
                    return InetAddress.getByName(rawIp).getAddress();
                } catch (UnknownHostException ignored) {
                }
            }
        }

        return null;
    }

    private BlockNodeServiceEndpoint buildBlockNodeEndpoint(JsonObject blockNode) {
        Objects.requireNonNull(blockNode, "blockNode must not be null");

        List<String> apis = new ArrayList<>();
        if (blockNode.has("endpoint_apis")) {
            for (JsonElement api : blockNode.getAsJsonArray("endpoint_apis")) {
                apis.add(api.getAsString());
            }
        }

        return new BlockNodeServiceEndpoint()
                .setEndpointApis(
                        apis.stream().map(a -> BlockNodeApi.valueOf(a)).collect(Collectors.toUnmodifiableList()));
    }

    private MirrorNodeServiceEndpoint buildMirrorNodeEndpoint(JsonObject mirrorNode) {
        Objects.requireNonNull(mirrorNode, "mirrorNode must not be null");
        return new MirrorNodeServiceEndpoint();
    }

    private RpcRelayServiceEndpoint buildRpcRelayEndpoint(JsonObject rpcRelay) {
        Objects.requireNonNull(rpcRelay, "rpcRelay must not be null");
        return new RpcRelayServiceEndpoint();
    }

    private GeneralServiceEndpoint buildGeneralEndpoint(JsonObject generalService) {
        Objects.requireNonNull(generalService, "generalService must not be null");

        String description = generalService.has("description")
                        && !generalService.get("description").isJsonNull()
                ? generalService.get("description").getAsString()
                : null;

        return new GeneralServiceEndpoint().setDescription(description);
    }
}
