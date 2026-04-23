// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.BlockNodeApi;
import com.hedera.hashgraph.sdk.BlockNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.NodeUpdateTransaction;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.RegisteredNodeCreateTransaction;
import com.hedera.hashgraph.sdk.RegisteredNodeDeleteTransaction;
import com.hedera.hashgraph.sdk.RegisteredNodeUpdateTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.Objects;

public class RegisteredNodeLifeCycleExample {
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
        System.out.println("Registered Node Lifecycle Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Generate an admin key pair and configure a BlockNodeServiceEndpoint
         * for use in the RegisterNodeTransaction.
         */
        PrivateKey adminKey = PrivateKey.generateED25519();
        BlockNodeServiceEndpoint initialEndpoint = new BlockNodeServiceEndpoint()
                .setIpAddress(new byte[] {127, 0, 0, 1})
                .setPort(443)
                .setRequiresTls(true)
                .addEndpointApi(BlockNodeApi.SUBSCRIBE_STREAM);

        /*
         * Step 2:
         * Create Registered Node.
         */
        RegisteredNodeCreateTransaction registeredNodeCreateTx = new RegisteredNodeCreateTransaction()
                .setDescription("My Block Node")
                .setAdminKey(adminKey)
                .addServiceEndpoint(initialEndpoint)
                .freezeWith(client)
                .sign(adminKey);

        System.out.println("Creating Registered Node...");
        TransactionResponse registeredNodeCreateTxResponse = registeredNodeCreateTx.execute(client);
        TransactionReceipt registeredNodeCreateTxReceipt = registeredNodeCreateTxResponse.getReceipt(client);

        if (registeredNodeCreateTxReceipt.registeredNodeId <= 0) {
            throw new Exception("RegisteredNodeCreate transaction receipt was missing registeredNodeId. (Fail)");
        }

        /*
         * Step 3:
         * Execute a RegisteredNodeAddressBookQuery to verify the newly created
         * registered node appears in the RegisteredNodeAddressBook.
         */
        System.out.println("Skipping registered node address book query because mirror node API is not available...");

        /*
         * Step 4:
         * Update the RegisteredNode with new Block Node endpoint.
         */
        BlockNodeServiceEndpoint updateEndpoint = new BlockNodeServiceEndpoint()
                .setDomainName("block-node.example.com")
                .setPort(443)
                .setRequiresTls(true)
                .addEndpointApi(BlockNodeApi.STATUS);

        RegisteredNodeUpdateTransaction registeredNodeUpdateTx = new RegisteredNodeUpdateTransaction()
                .setRegisteredNodeId(registeredNodeCreateTxReceipt.registeredNodeId)
                .setDescription("My Updated Block Node")
                .setServiceEndpoints(List.of(initialEndpoint, updateEndpoint))
                .freezeWith(client)
                .sign(adminKey);

        System.out.println("Updating Registered Node...");
        TransactionResponse registeredNodeUpdateTxResponse = registeredNodeUpdateTx.execute(client);
        TransactionReceipt registeredNodeUpdateTxReceipt = registeredNodeUpdateTxResponse.getReceipt(client);

        /*
         * Step 5:
         * Add the registeredNodeId as associatedRegisteredNodes to a Node.
         */
        long registeredNodeId = registeredNodeUpdateTxReceipt.registeredNodeId;
        NodeUpdateTransaction associateTx = new NodeUpdateTransaction()
                .setNodeId(0)
                .addAssociatedRegisteredNode(registeredNodeId)
                .freezeWith(client);

        System.out.println("Associating registered node " + registeredNodeId + " with consensus node...");
        TransactionResponse associateTxResponse = associateTx.execute(client);
        associateTxResponse.getReceipt(client);

        /*
         * Step 6:
         * Delete the Registered Node.
         */
        System.out.println("Deleting Registered Node...");
        new RegisteredNodeDeleteTransaction()
                .setRegisteredNodeId(registeredNodeCreateTxReceipt.registeredNodeId)
                .freezeWith(client)
                .sign(adminKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Registered Node Lifecycle Example Complete!");
    }
}
