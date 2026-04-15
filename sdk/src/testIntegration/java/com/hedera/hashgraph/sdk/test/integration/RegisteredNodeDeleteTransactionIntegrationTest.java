// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.hedera.hashgraph.sdk.BlockNodeApi;
import com.hedera.hashgraph.sdk.BlockNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.PrivateKey;
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
}
