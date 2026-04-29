// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import com.hedera.hashgraph.sdk.BlockNodeApi;
import com.hedera.hashgraph.sdk.BlockNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.GeneralServiceEndpoint;
import com.hedera.hashgraph.sdk.MirrorNodeServiceEndpoint;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.RegisteredNodeAddressBookQuery;
import com.hedera.hashgraph.sdk.RegisteredNodeCreateTransaction;
import com.hedera.hashgraph.sdk.RegisteredServiceEndpoint;
import com.hedera.hashgraph.sdk.RpcRelayServiceEndpoint;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RegisteredNodeAddressBookQueryIntegrationTest {
    @Test
    @DisplayName("Should query registered node using node id")
    void canCreateAndVerifyRegisteredNodeWithPolling() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var adminKey = PrivateKey.generateED25519();
            var description = "Test Registered Node";

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
                            .setPort(8080));

            System.out.println(testEnv.client);

            var response = new RegisteredNodeCreateTransaction()
                    .setAdminKey(adminKey)
                    .setDescription(description)
                    .setServiceEndpoints(serviceEndpoints)
                    .freezeWith(testEnv.client)
                    .sign(adminKey)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);
            var nodeId = receipt.registeredNodeId;

            // Wait for mirror node to update
            Thread.sleep(5000);

            var registeredNodeBook = new RegisteredNodeAddressBookQuery()
                    .setRegisteredNodeId(nodeId)
                    .execute(testEnv.client);

            Assertions.assertThat(registeredNodeBook.registeredNodes).hasSize(1);

            var node = registeredNodeBook.registeredNodes.get(0);

            Assertions.assertThat(node.description).isEqualTo(description);
            Assertions.assertThat(node.serviceEndpoints).hasSize(4);

            Assertions.assertThat(node.serviceEndpoints)
                    .filteredOn(e -> e instanceof BlockNodeServiceEndpoint)
                    .first()
                    .extracting("domainName")
                    .isEqualTo("test.block.com");
        }
    }
}
