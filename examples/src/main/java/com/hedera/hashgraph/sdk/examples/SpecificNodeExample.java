// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * How to communicate with specific nodes in the Hedera network.
 * <p>
 * This example demonstrates three different methods to target specific nodes
 * for operations on the Hedera network. This can be useful for:
 * <ul>
 *   <li>Testing connectivity to specific nodes</li>
 *   <li>Debugging node-specific issues</li>
 *   <li>Monitoring health of particular nodes</li>
 *   <li>Sending transactions through specific nodes</li>
 * </ul>
 */
class SpecificNodeExample {

    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * Operator's account ID.
     * Used to sign and pay for operations on Hedera.
     */
    private static final AccountId OPERATOR_ID =
        AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
        PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file.
     * Network can be: localhost, testnet, previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file.
     * Log levels can be: TRACE, DEBUG, INFO, WARN, ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL,
     * for example via VM options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        System.out.println("Specific Node Communication Example Start!");

        /*
         * Method 1: Direct node specification
         * Directly specify the node you want to communicate with.
         */
        System.out.println("\nExample 1: Direct node specification");
        communicateWithSpecificNodeDirect();

        /*
         * Method 2: Extract from network map
         * Extract a specific node from the full network map.
         */
        System.out.println("\nExample 2: Extract from network map");
        communicateWithSpecificNodeFromNetworkMap();

        /*
         * Method 3: TLS with specific node
         * Communicate with a specific node using TLS.
         */
        System.out.println("\nExample 3: TLS with specific node");
        communicateWithSpecificNodeUsingTLS();

        System.out.println("\nSpecific Node Communication Example Complete!");
    }

    /**
     * Method 1: Directly specify the node you want to communicate with.
     * <p>
     * This approach creates a Client with a custom network map containing
     * only the specific node you want to target.
     */
    private static void communicateWithSpecificNodeDirect() throws PrecheckStatusException, TimeoutException {
        /*
         * Step 1:
         * Create a network map with only one specific node
         */
        Map<String, AccountId> networkMap = new HashMap<>();
        networkMap.put("0.testnet.hedera.com:50211", new AccountId(3));

        /*
         * Step 2:
         * Create client with the custom network map
         */
        Client client = Client.forNetwork(networkMap);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 3:
         * Get the node from the network
         */
        var network = client.getNetwork();
        var nodes = new ArrayList<>(network.values());
        var node = nodes.get(0);

        /*
         * Step 4:
         * Set max node attempts to 1 to limit retries
         *
         * Note: This limits how many times the SDK will retry this node if it returns
         * a bad gRPC status. The SDK will only use this one node because we've configured
         * only one node in our network map above.
         */
        client.setMaxNodeAttempts(1);

        /*
         * Step 5:
         * Ping the node to test connectivity
         */
        System.out.println("Pinging node: " + node);
        client.ping(node);
        System.out.println("Ping successful");

        /*
         * Clean up:
         */
        client.close();
    }

    /**
     * Method 2: Extract a specific node from the full network map.
     * <p>
     * This approach starts with a standard Client, then extracts a specific node
     * from the network map and creates a client with only that node.
     */
    private static void communicateWithSpecificNodeFromNetworkMap()
        throws PrecheckStatusException, TimeoutException, InterruptedException {
        /*
         * Step 1:
         * Initialize a standard client
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 2:
         * Get the full network map and extract a specific node
         */
        var network = client.getNetwork();
        Map.Entry<String, AccountId> firstNodeEntry = network.entrySet().iterator().next();
        String nodeAddress = firstNodeEntry.getKey();
        AccountId nodeAccountId = firstNodeEntry.getValue();

        System.out.println("Selected node: " + nodeAddress + " (Account ID: " + nodeAccountId + ")");

        /*
         * Step 3:
         * Create a new map with only the specific node
         */
        Map<String, AccountId> specificNodeMap = Map.of(nodeAddress, nodeAccountId);

        /*
         * Step 4:
         * Update the client to use only the specific node
         */
        client.setNetwork(specificNodeMap);

        /*
         * Step 5:
         * Ping all nodes (which is now just the one specific node)
         */
        client.pingAll();
        System.out.println("Ping to specific node successful");

        /*
         * Clean up:
         */
        client.close();
    }

    /**
     * Method 3: Communicate with a specific node using TLS.
     * <p>
     * This approach demonstrates how to properly use TLS when communicating
     * with specific nodes. Note that we extract the node from the network map
     * rather than creating it directly, ensuring TLS certificates are properly loaded.
     */
    private static void communicateWithSpecificNodeUsingTLS()
        throws PrecheckStatusException, TimeoutException, InterruptedException {
        /*
         * Step 1:
         * Initialize client with TLS enabled
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK)
            .setTransportSecurity(true)
            .setVerifyCertificates(true);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 2:
         * Get the network map and extract a specific node
         */
        var network = client.getNetwork();
        var firstNode = network.entrySet().iterator().next();

        System.out.println("Selected node with TLS: " + firstNode.getKey());

        /*
         * Step 3:
         * Create a new map with only the selected node
         */
        Map<String, AccountId> specificNode = Map.of(firstNode.getKey(), firstNode.getValue());

        /*
         * Step 4:
         * Set the client to use only the specific node
         */
        client.setNetwork(specificNode);

        /*
         * Step 5:
         * Ping the node to test TLS-secured connectivity
         */
        client.pingAll();
        System.out.println("TLS-secured ping to specific node successful");

        /*
         * Clean up:
         */
        client.close();
    }
}
