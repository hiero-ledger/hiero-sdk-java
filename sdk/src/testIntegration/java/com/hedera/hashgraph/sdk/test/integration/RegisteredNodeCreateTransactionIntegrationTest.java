package com.hedera.hashgraph.sdk.test.integration;

import com.hedera.hashgraph.sdk.BlockNodeApi;
import com.hedera.hashgraph.sdk.BlockNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.RegisteredNodeCreateTransaction;
import com.hedera.hashgraph.sdk.RegisteredServiceEndpoint;
import com.hedera.hashgraph.sdk.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;


public class RegisteredNodeCreateTransactionIntegrationTest {
    @Test
    @DisplayName("Can create a registered node")
    void canCreateRegisteredNode() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            List<RegisteredServiceEndpoint> serviceEndpoints = List.of(
                new BlockNodeServiceEndpoint()
                    .setDomainName("test.block.com")
                    .setPort(443)
                    .setEndpointApi(BlockNodeApi.STATUS)
            );

            var response = new RegisteredNodeCreateTransaction()
                .setAdminKey(key)
                .setDescription("test description")
                .setServiceEndpoints(serviceEndpoints)
                .freezeWith(testEnv.client)
                .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
            assertThat(receipt.registeredNodeId).isGreaterThan(0);
        }
    }
}
