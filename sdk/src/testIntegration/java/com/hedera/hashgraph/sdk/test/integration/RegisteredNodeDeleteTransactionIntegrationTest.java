// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.hedera.hashgraph.sdk.BlockNodeApi;
import com.hedera.hashgraph.sdk.BlockNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.NodeUpdateTransaction;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.RegisteredNodeCreateTransaction;
import com.hedera.hashgraph.sdk.RegisteredNodeDeleteTransaction;
import com.hedera.hashgraph.sdk.RegisteredServiceEndpoint;
import com.hedera.hashgraph.sdk.Status;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RegisteredNodeDeleteTransactionIntegrationTest {
    @Test
    @DisplayName("Can delete a registered node")
    void canDeleteRegisteredNode() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();

            List<RegisteredServiceEndpoint> endpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("blocks.example.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS));

            var createReceipt = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test node delete")
                    .setServiceEndpoints(endpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var nodeId = createReceipt.registeredNodeId;

            var deleteReceipt = new RegisteredNodeDeleteTransaction()
                    .setRegisteredNodeId(nodeId)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThat(deleteReceipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName("Should return REGISTERED_NODE_STILL_ASSOCIATED when deleting an associated node")
    void shouldCauseReceiptStatusWhenNodeStillAssociated() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();

            List<RegisteredServiceEndpoint> endpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("blocks.example.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS));

            var createReceipt = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test node delete")
                    .setServiceEndpoints(endpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var registeredNodeId = createReceipt.registeredNodeId;

            new NodeUpdateTransaction()
                    .setNodeId(0)
                    .addAssociatedRegisteredNode(registeredNodeId)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var deleteTx = new RegisteredNodeDeleteTransaction()
                    .setRegisteredNodeId(registeredNodeId)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            assertThatThrownBy(() -> deleteTx.getReceipt(testEnv.client))
                    .isInstanceOf(ReceiptStatusException.class)
                    .satisfies(e -> {
                        assertThat(((ReceiptStatusException) e).receipt.status)
                                .isEqualTo(Status.REGISTERED_NODE_STILL_ASSOCIATED);
                    });
        }
    }
}
