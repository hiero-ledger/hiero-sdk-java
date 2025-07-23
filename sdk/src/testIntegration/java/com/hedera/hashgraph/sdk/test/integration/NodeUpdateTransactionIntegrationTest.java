// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Endpoint;
import com.hedera.hashgraph.sdk.NodeUpdateTransaction;
import com.hedera.hashgraph.sdk.PrivateKey;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NodeUpdateTransactionIntegrationTest {

    @Test
    @Disabled("The test has to be disabled so it doesn't fail calls to local-node")
    @DisplayName("Can execute NodeUpdateTransaction")
    void canExecuteNodeUpdateTransaction() throws Exception {
        // Set the network
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));

        try (var client = Client.forNetwork(network).setMirrorNetwork(List.of("localhost:5600"))) {

            // Set the operator to be account 0.0.2
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Set up grpcWebProxyEndpoint address
            var grpcWebProxyEndpoint =
                    new Endpoint().setDomainName("testWebUpdated.com").setPort(123456);

            var response = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setDescription("testUpdated")
                    .setDeclineReward(true)
                    .setGrpcWebProxyEndpoint(grpcWebProxyEndpoint)
                    .execute(client);

            response.getReceipt(client);
        }
    }

    @Test
    @Disabled("The test has to be disabled so it doesn't fail calls to local-node")
    @DisplayName("Can delete gRPC web proxy endpoint")
    void canDeleteGrpcWebProxyEndpoint() throws Exception {
        // Set the network
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));

        try (var client = Client.forNetwork(network).setMirrorNetwork(List.of("localhost:5600"))) {

            // Set the operator to be account 0.0.2
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            var response = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .deleteGrpcWebProxyEndpoint()
                    .execute(client);

            response.getReceipt(client);
        }
    }
}
