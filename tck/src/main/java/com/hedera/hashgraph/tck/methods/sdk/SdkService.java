// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.BaseParams;
import com.hedera.hashgraph.tck.methods.sdk.param.SetupParams;
import com.hedera.hashgraph.tck.methods.sdk.response.SetupResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * SdkService for managing the {@link Client} setup and reset
 */
@JSONRPC2Service
public class SdkService extends AbstractJSONRPC2Service {
    private final ConcurrentMap<String, Client> clients = new ConcurrentHashMap<>();

    @JSONRPC2Method("setup")
    public SetupResponse setup(final SetupParams params) throws Exception {
        var clientExecutor = Executors.newFixedThreadPool(16);
        String clientType;
        if (params.getNodeIp().isPresent()
                && params.getNodeAccountId().isPresent()
                && params.getMirrorNetworkIp().isPresent()) {
            // Custom client setup
            Map<String, AccountId> node = new HashMap<>();
            var nodeId = AccountId.fromString(params.getNodeAccountId().get());
            node.put(params.getNodeIp().get(), nodeId);
            Client client = Client.forNetwork(node, clientExecutor);
            clientType = "custom";
            client.setMirrorNetwork(List.of(params.getMirrorNetworkIp().get()));
            registerClient(params.getSessionId(), client);
        } else {
            // Default to testnet
            Client client = Client.forTestnet(clientExecutor);
            clientType = "testnet";
            registerClient(params.getSessionId(), client);
        }

        Client client = getClient(params.getSessionId());
        client.setOperator(
                AccountId.fromString(params.getOperatorAccountId()),
                PrivateKey.fromString(params.getOperatorPrivateKey()));
        return new SetupResponse("Successfully setup " + clientType + " client.");
    }

    @JSONRPC2Method("setOperator")
    public SetupResponse setOperator(final SetupParams params) throws Exception {
        Client client = getClient(params.getSessionId());
        client.setOperator(
                AccountId.fromString(params.getOperatorAccountId()),
                PrivateKey.fromString(params.getOperatorPrivateKey()));
        return new SetupResponse("");
    }

    @JSONRPC2Method("reset")
    public SetupResponse reset(final BaseParams params) throws Exception {
        Client client = clients.remove(params.getSessionId());
        if (client != null) {
            client.close();
        }
        return new SetupResponse("");
    }

    public Client getClient(final String sessionId) {
        return Objects.requireNonNull(clients.get(sessionId), "No client found for session: " + sessionId);
    }

    private void registerClient(String sessionId, Client client) throws TimeoutException {
        Client existing = clients.put(sessionId, client);
        if (existing != null) {
            existing.close();
        }
    }
}
