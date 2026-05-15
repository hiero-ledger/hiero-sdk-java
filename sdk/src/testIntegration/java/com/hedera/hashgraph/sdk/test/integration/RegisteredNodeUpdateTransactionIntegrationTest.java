// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.BlockNodeApi;
import com.hedera.hashgraph.sdk.BlockNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.RegisteredNodeAddressBookQuery;
import com.hedera.hashgraph.sdk.RegisteredNodeCreateTransaction;
import com.hedera.hashgraph.sdk.RegisteredNodeUpdateTransaction;
import com.hedera.hashgraph.sdk.RegisteredServiceEndpoint;
import com.hedera.hashgraph.sdk.Status;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RegisteredNodeUpdateTransactionIntegrationTest {
    @Test
    @DisplayName("Can update registered node description")
    void canUpdateRegisteredNodeDescription() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();

            List<RegisteredServiceEndpoint> endpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("initial.blocks.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.SUBSCRIBE_STREAM));

            var nodeId = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test initial node")
                    .setServiceEndpoints(endpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .registeredNodeId;

            assertThat(nodeId).isGreaterThan(0);

            var updateResponse = new RegisteredNodeUpdateTransaction()
                    .setRegisteredNodeId(nodeId)
                    .setDescription("test updated node")
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var updateReceipt = updateResponse.getReceipt(testEnv.client);
            assertThat(updateReceipt.status).isEqualTo(Status.SUCCESS);

            // Wait for mirror node to update
            Thread.sleep(5000);

            var registeredNodes = new RegisteredNodeAddressBookQuery()
                    .setRegisteredNodeId(nodeId)
                    .execute(testEnv.client)
                    .registeredNodes;
            assertThat(registeredNodes).hasSize(1);

            var registeredNode = registeredNodes.getFirst();
            assertThat(registeredNode.description).isEqualTo("test updated node");
        }
    }

    @Test
    @DisplayName("Can update registered node endpoints")
    void canUpdateRegisteredNodeServiceEndpoint() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();

            List<RegisteredServiceEndpoint> endpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("initial.blocks.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.SUBSCRIBE_STREAM));

            var nodeId = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test initial node")
                    .setServiceEndpoints(endpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .registeredNodeId;

            assertThat(nodeId).isGreaterThan(0);

            List<RegisteredServiceEndpoint> updatedEndpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("updated.blocks.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS));

            var updateResponse = new RegisteredNodeUpdateTransaction()
                    .setRegisteredNodeId(nodeId)
                    .setServiceEndpoints(updatedEndpoints)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var updateReceipt = updateResponse.getReceipt(testEnv.client);
            assertThat(updateReceipt.status).isEqualTo(Status.SUCCESS);

            // Wait for mirror node to update
            Thread.sleep(5000);

            var registeredNodes = new RegisteredNodeAddressBookQuery()
                    .setRegisteredNodeId(nodeId)
                    .execute(testEnv.client)
                    .registeredNodes;
            assertThat(registeredNodes).hasSize(1);

            var registeredNode = registeredNodes.getFirst();
            assertThat(registeredNode.serviceEndpoints).hasSize(1);
            assertThat(registeredNode.serviceEndpoints.getFirst()).isInstanceOf(BlockNodeServiceEndpoint.class);

            var endpoint = (BlockNodeServiceEndpoint) registeredNode.serviceEndpoints.getFirst();
            assertThat(endpoint.getDomainName())
                    .isEqualTo(updatedEndpoints.getFirst().getDomainName());
            assertThat(endpoint.getPort()).isEqualTo(updatedEndpoints.getFirst().getPort());
            assertThat(endpoint.getEndpointApis())
                    .isEqualTo(((BlockNodeServiceEndpoint) updatedEndpoints.getFirst()).getEndpointApis());
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

            var nodeId = new RegisteredNodeCreateTransaction()
                    .setAdminKey(oldKey)
                    .setDescription("test node")
                    .setServiceEndpoints(endpoints)
                    .freezeWith(testEnv.client)
                    .sign(oldKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .registeredNodeId;

            assertThat(nodeId).isGreaterThan(0);

            var newKey = PrivateKey.generateED25519();

            var tx = new RegisteredNodeUpdateTransaction()
                    .setRegisteredNodeId(nodeId)
                    .setAdminKey(newKey)
                    .freezeWith(testEnv.client);

            tx.sign(oldKey);
            tx.sign(newKey);

            var receipt = tx.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);

            // Wait for mirror node to update
            Thread.sleep(5000);
            var registeredNodes = new RegisteredNodeAddressBookQuery()
                    .setRegisteredNodeId(nodeId)
                    .execute(testEnv.client)
                    .registeredNodes;
            assertThat(registeredNodes).hasSize(1);

            var registeredNode = registeredNodes.getFirst();
            assertThat(registeredNode.adminKey.toString())
                    .isEqualTo(newKey.getPublicKey().toString());
        }
    }

    @Test
    @DisplayName(
            "Given an existing registered node created with an IP address endpoint when a RegisteredNodeUpdateTransaction replaces it with a domain name endpoint")
    void canReplaceIpAddrToDomain() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();

            BlockNodeServiceEndpoint ipEndpoint = new BlockNodeServiceEndpoint()
                    .setIpAddress(new byte[] {127, 0, 0, 1})
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS);

            var nodeId = new RegisteredNodeCreateTransaction()
                    .setAdminKey(key)
                    .setDescription("test registered node")
                    .setServiceEndpoints(List.of(ipEndpoint))
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .registeredNodeId;

            assertThat(nodeId).isGreaterThan(0);

            BlockNodeServiceEndpoint domainEndpoint = new BlockNodeServiceEndpoint()
                    .setDomainName("test.block.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS);

            var updateResponse = new RegisteredNodeUpdateTransaction()
                    .setRegisteredNodeId(nodeId)
                    .setServiceEndpoints(List.of(domainEndpoint))
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var updateReceipt = updateResponse.getReceipt(testEnv.client);
            assertThat(updateReceipt.status).isEqualTo(Status.SUCCESS);

            // Wait for mirror node to update
            Thread.sleep(5000);

            var registeredNodes = new RegisteredNodeAddressBookQuery()
                    .setRegisteredNodeId(nodeId)
                    .execute(testEnv.client)
                    .registeredNodes;
            assertThat(registeredNodes).hasSize(1);

            var registeredNode = registeredNodes.getFirst();
            assertThat(registeredNode.serviceEndpoints).hasSize(1);
            assertThat(registeredNode.serviceEndpoints.getFirst() instanceof BlockNodeServiceEndpoint);

            var endpoint = (BlockNodeServiceEndpoint) registeredNode.serviceEndpoints.getFirst();
            assertThat(endpoint.getDomainName()).isEqualTo(domainEndpoint.getDomainName());
            assertThat(endpoint.getPort()).isEqualTo(domainEndpoint.getPort());
            assertThat(endpoint.getEndpointApis()).isEqualTo(domainEndpoint.getEndpointApis());
        }
    }

    @Test
    @DisplayName(
            "When RegisteredNodeUpdateTransaction sets a new admin key but only the old admin key signs fails with INVALID_SIGNATURE")
    void registeredUpdateNodeFailsInvalidSignature() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var oldKey = PrivateKey.generateED25519();

            List<RegisteredServiceEndpoint> endpoints = List.of(new BlockNodeServiceEndpoint()
                    .setDomainName("blocks.example.com")
                    .setPort(443)
                    .addEndpointApi(BlockNodeApi.STATUS));

            var nodeId = new RegisteredNodeCreateTransaction()
                    .setAdminKey(oldKey)
                    .setDescription("test node")
                    .setServiceEndpoints(endpoints)
                    .freezeWith(testEnv.client)
                    .sign(oldKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .registeredNodeId;

            var newKey = PrivateKey.generateED25519();
            var tx = new RegisteredNodeUpdateTransaction()
                    .setRegisteredNodeId(nodeId)
                    .setAdminKey(newKey)
                    .freezeWith(testEnv.client);

            tx.sign(oldKey);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> tx.execute(testEnv.client).getReceipt(testEnv.client))
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @DisplayName(
            "Given a RegisteredNodeUpdateTransaction targeting a non-existent registeredNodeId fails with INVALID_REGISTERED_NODE_ID")
    void registeredUpdateNodeFailsInvalidId() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tx = new RegisteredNodeUpdateTransaction()
                    .setRegisteredNodeId(10000)
                    .freezeWith(testEnv.client);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> tx.execute(testEnv.client).getReceipt(testEnv.client))
                    .withMessageContaining(Status.INVALID_REGISTERED_NODE_ID.toString());
        }
    }
}
