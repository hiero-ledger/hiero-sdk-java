// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Example demonstrating how to communicate with specific nodes in Hedera network.
 * This can be useful for:
 * - Testing connectivity to specific nodes
 * - Debugging node-specific issues
 * - Monitoring health of particular nodes
 * - Sending transactions through specific nodes
 */
public final class SpecificNodeExample {

    // see `.env.sample` in the repository root for how to specify these values
    // or set environment variables with the same names
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));
    // HEDERA_NETWORK defaults to testnet if not specified in dotenv
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    private SpecificNodeExample() {}

    public static void main(String[] args) throws PrecheckStatusException, TimeoutException, InterruptedException {
        System.out.println("Example 1: Direct node specification");
        communicateWithSpecificNodeDirect();

        System.out.println("\nExample 2: Extract from network map");
        communicateWithSpecificNodeFromNetworkMap();

        System.out.println("\nExample 3: TLS with specific node");
        communicateWithSpecificNodeUsingTLS();
    }

    /**
     * Method 1: Directly specify the node you want to communicate with.
     * This approach creates a Client with a custom network map containing
     * only the specific node you want to target.
     */
    public static void communicateWithSpecificNodeDirect() throws PrecheckStatusException, TimeoutException {
        // Create a network map with only one specific node
        Map<String, AccountId> networkMap = new HashMap<>();
        networkMap.put("0.testnet.hedera.com:50211", new AccountId(3));

        // Create client with the custom network map
        Client client = Client.forNetwork(networkMap);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        // Get the node from the network
        var network = client.getNetwork();
        var nodes = new ArrayList<>(network.values());
        var node = nodes.get(0);

        // Set max node attempts to 1 to ensure only this node is used
        client.setMaxNodeAttempts(1);

        // Ping the node to test connectivity
        System.out.println("Pinging node: " + node);
        client.ping(node);
        System.out.println("Ping successful");
    }

    /**
     * Method 2: Extract a specific node from the full network map.
     * This approach starts with a standard Client initialized using ClientHelper,
     * then extracts a specific node from the network map and creates a new client
     * with only that node.
     */
    public static void communicateWithSpecificNodeFromNetworkMap()
            throws PrecheckStatusException, TimeoutException, InterruptedException {
        // Initialize a standard client using ClientHelper
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        // Get the full network map
        var network = client.getNetwork();

        // Extract the first node entry from the network map
        Map.Entry<String, AccountId> firstNodeEntry =
                network.entrySet().iterator().next();
        String nodeAddress = firstNodeEntry.getKey();
        AccountId nodeAccountId = firstNodeEntry.getValue();

        System.out.println("Selected node: " + nodeAddress + " (Account ID: " + nodeAccountId + ")");

        // Create a new map with only the specific node
        Map<String, AccountId> specificNodeMap = Map.of(nodeAddress, nodeAccountId);

        // Update the client to use only the specific node
        client.setNetwork(specificNodeMap);

        // Ping all nodes (which is now just the one specific node)
        client.pingAll();
        System.out.println("Ping to specific node successful");
    }

    /**
     * Method 3: Communicate with a specific node using TLS.
     * This approach demonstrates how to properly use TLS when communicating
     * with specific nodes. Note that we extract the node from the network map
     * rather than creating it directly, ensuring TLS certificates are properly loaded.
     */
    public static void communicateWithSpecificNodeUsingTLS()
            throws PrecheckStatusException, TimeoutException, InterruptedException {
        // Initialize client with TLS enabled using ClientHelper
        Client client =
                ClientHelper.forName(HEDERA_NETWORK).setTransportSecurity(true).setVerifyCertificates(true);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        // Get the network map and extract the first node
        var network = client.getNetwork();
        var firstNode = network.entrySet().iterator().next();

        // Create a new map with only the selected node
        Map<String, AccountId> specificNode = Map.of(firstNode.getKey(), firstNode.getValue());

        System.out.println("Selected node with TLS: " + firstNode.getKey());

        // Set the client to use only the specific node
        client.setNetwork(specificNode);

        // Ping the node to test TLS-secured connectivity
        client.pingAll();
        System.out.println("TLS-secured ping to specific node successful");
    }
}
