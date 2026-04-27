// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.BlockNodeApi;
import com.hedera.hashgraph.sdk.BlockNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.GeneralServiceEndpoint;
import com.hedera.hashgraph.sdk.MirrorNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.RegisteredNodeCreateTransaction;
import com.hedera.hashgraph.sdk.RegisteredServiceEndpoint;
import com.hedera.hashgraph.sdk.RpcRelayServiceEndpoint;
import com.hedera.hashgraph.sdk.Status;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RegisteredNodeCreateTransactionIntegrationTest {
    @Test
    @DisplayName("Can create a registered node with blockNodeServiceEndpoint")
    void canCreateRegisteredNodeWithBlockNode() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            List<RegisteredServiceEndpoint> serviceEndpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("test.block.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS));

            var response = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test description")
                    .setServiceEndpoints(serviceEndpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
            assertThat(receipt.registeredNodeId).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Can create a registered node with mirrorNodeServiceEndpoint")
    void canCreateRegisteredNodeWitMirrorNode() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            List<RegisteredServiceEndpoint> serviceEndpoints = List.of(new MirrorNodeServiceEndpoint()
                    .setDomainName("test.mirror.com")
                    .setPort(443));

            System.out.println(serviceEndpoints);

            var response = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test description")
                    .setServiceEndpoints(serviceEndpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
            assertThat(receipt.registeredNodeId).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Can create a registered node with rpcRelayServiceEndpoint")
    void canCreateRegisteredNodeWithRpcRelay() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            List<RegisteredServiceEndpoint> serviceEndpoints = List.of(
                    new RpcRelayServiceEndpoint().setDomainName("test.rpc.com").setPort(443));

            var response = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test description")
                    .setServiceEndpoints(serviceEndpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
            assertThat(receipt.registeredNodeId).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Can create a registered node with generalServiceEndpoint")
    void canCreateRegisteredNodeWithGeneralService() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            List<RegisteredServiceEndpoint> serviceEndpoints = List.of(new GeneralServiceEndpoint()
                    .setDomainName("test.general.com")
                    .setDescription("GeneralEndpoint")
                    .setPort(443));

            var response = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test description")
                    .setServiceEndpoints(serviceEndpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
            assertThat(receipt.registeredNodeId).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Can create a registered node with multiple service endpoints")
    void canCreateRegisteredNodeWithMultipleServiceEndpoints() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            List<RegisteredServiceEndpoint> serviceEndpoints = List.of(
                    new BlockNodeServiceEndpoint()
                            .setDomainName("test.block.com")
                            .setPort(443)
                            .addEndpointApi(BlockNodeApi.STATUS),
                    new MirrorNodeServiceEndpoint()
                            .setIpAddress(new byte[] {127, 0, 0, 1})
                            .setPort(443));

            System.out.println(serviceEndpoints);

            var response = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test description")
                    .setServiceEndpoints(serviceEndpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
            assertThat(receipt.registeredNodeId).isGreaterThan(0);
        }
    }
}
