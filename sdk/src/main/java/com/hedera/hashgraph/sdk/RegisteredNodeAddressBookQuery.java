// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.EntityIdHelper.performQueryToMirrorNodeAsync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

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
        System.out.println(json);
        return parseRegisterNodeAddressBook(json);
    }

    CompletableFuture<String> executeMirrorNodeRequest(Client client) {
        Objects.requireNonNull(client, "client must not be null");
        String apiEndpoint = "/network/registered-nodes?registerednode.id=" + registeredNodeId;
        String baseUrl = client.getMirrorRestBaseUrl();

        // For localhost registered node calls, override to use port 8084 unless system property overrides
        if (baseUrl.contains("localhost:5551") || baseUrl.contains("127.0.0.1:5551")) {
            String registeredNodePort = System.getProperty("hedera.mirror.registerednode.port");
            if (registeredNodePort != null && !registeredNodePort.isEmpty()) {
                baseUrl = baseUrl.replace(":5551", ":" + registeredNodePort);
            } else {
                baseUrl = baseUrl.replace(":5551", ":8084");
            }
        }

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
            registeredNodes.add(RegisteredNode.fromJson(node.getAsJsonObject()));
        }

        return new RegisteredNodeAddressBook(registeredNodes);
    }
}
