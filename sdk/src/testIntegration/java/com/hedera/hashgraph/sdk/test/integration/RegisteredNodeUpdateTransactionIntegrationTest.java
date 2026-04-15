// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.hedera.hashgraph.sdk.BlockNodeApi;
import com.hedera.hashgraph.sdk.BlockNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.RegisteredNodeCreateTransaction;
import com.hedera.hashgraph.sdk.RegisteredNodeUpdateTransaction;
import com.hedera.hashgraph.sdk.RegisteredServiceEndpoint;
import com.hedera.hashgraph.sdk.Status;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RegisteredNodeUpdateTransactionIntegrationTest {
    @Test
    @DisplayName("Can update registered node endpoints and description")
    void canUpdateRegisteredNode() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();

            List<RegisteredServiceEndpoint> endpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("initial.blocks.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.SUBSCRIBE_STREAM));

            var createResponse = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test initial node")
                    .setServiceEndpoints(endpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var createReceipt = createResponse.getReceipt(testEnv.client);

            var nodeId = createReceipt.registeredNodeId;

            List<RegisteredServiceEndpoint> updatedEndpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("updated.blocks.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS));

            var updateResponse = new RegisteredNodeUpdateTransaction()
                    .setRegisteredNodeId(nodeId)
                    .setDescription("test updated node")
                    .setServiceEndpoints(updatedEndpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var updateReceipt = updateResponse.getReceipt(testEnv.client);

            assertThat(updateReceipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName("Can rotate registered node admin key")
    void canRotateAdminKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var oldKey = PrivateKey.generateED25519();

            List<RegisteredServiceEndpoint> endpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("blocks.example.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS));

            var createReceipt = new RegisteredNodeCreateTransaction()
                    .setAdminKey(oldKey)
                    .setDescription("test node")
                    .setServiceEndpoints(endpoints)
                    .freezeWith(testEnv.client)
                    .sign(oldKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nodeId = createReceipt.registeredNodeId;

            var newKey = PrivateKey.generateED25519();

            var tx = new RegisteredNodeUpdateTransaction()
                    .setRegisteredNodeId(nodeId)
                    .setAdminKey(newKey)
                    .freezeWith(testEnv.client);

            tx.sign(oldKey);
            tx.sign(newKey);

            var receipt = tx.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }
}
