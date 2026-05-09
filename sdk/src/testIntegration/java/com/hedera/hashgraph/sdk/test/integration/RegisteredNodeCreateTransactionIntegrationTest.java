// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.BlockNodeApi;
import com.hedera.hashgraph.sdk.BlockNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.GeneralServiceEndpoint;
import com.hedera.hashgraph.sdk.MirrorNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.RegisteredNodeAddressBookQuery;
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
    void canCreateRegisteredNodeWithMirrorNode() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            List<RegisteredServiceEndpoint> serviceEndpoints = List.of(new MirrorNodeServiceEndpoint()
                    .setDomainName("test.mirror.com")
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
                            .setPort(443),
                    new RpcRelayServiceEndpoint().setDomainName("test.rpc.com").setPort(443),
                    new GeneralServiceEndpoint()
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

            // Wait for mirror node to update
            Thread.sleep(5000);

            var registeredNodes = new RegisteredNodeAddressBookQuery()
                    .setRegisteredNodeId(receipt.registeredNodeId)
                    .execute(testEnv.client)
                    .registeredNodes;
            assertThat(registeredNodes).hasSize(1);

            var registeredNode = registeredNodes.getFirst();
            assertThat(registeredNode.serviceEndpoints).hasSize(serviceEndpoints.size());
        }
    }

    @Test
    @DisplayName(
            "Given a RegisteredNodeCreateTransaction with no admin key set fails with a precheck status of KEY_REQUIRED")
    void registeredNodeCreateTransactionFailsInvalidId() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            List<RegisteredServiceEndpoint> serviceEndpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("test.block.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS));

            var tx = new RegisteredNodeCreateTransaction()
                    .setDescription("test description")
                    .setServiceEndpoints(serviceEndpoints)
                    .freezeWith(testEnv.client);

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> tx.execute(testEnv.client))
                    .withMessageContaining(Status.KEY_REQUIRED.toString());
        }
    }

    @Test
    @DisplayName(
            "Given a RegisteredNodeCreateTransaction with an empty service endpoints list fails with INVALID_REGISTERED_ENDPOINT")
    void registeredNodeCreateTransactionEmptyServiceEndpoints() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();

            var tx = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setServiceEndpoints(List.of())
                    .setDescription("test description")
                    .freezeWith(testEnv.client);

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> tx.execute(testEnv.client).getReceipt(testEnv.client))
                    .withMessageContaining(Status.INVALID_REGISTERED_ENDPOINT.toString());
        }
    }
}
