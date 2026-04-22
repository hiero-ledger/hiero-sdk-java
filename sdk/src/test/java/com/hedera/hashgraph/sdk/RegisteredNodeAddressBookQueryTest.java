// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RegisteredNodeAddressBookQueryTest {
    private RegisteredNodeAddressBookQuery query;
    private Client mockClient;

    @BeforeEach
    void setUp() {
        query = Mockito.spy(new RegisteredNodeAddressBookQuery());
        mockClient = Mockito.mock(Client.class);
        Mockito.when(mockClient.getMirrorRestBaseUrl()).thenReturn("https://testnet.mirrornode.hedera.com");
    }

    @Test
    void testExecuteSuccess() throws ExecutionException, InterruptedException {
        var mockResponse = "{" + "  \"registered_nodes\": ["
                + "    {"
                + "      \"registered_node_id\": 123,"
                + "      \"description\": \"Test Node\","
                + "      \"admin_key\": {"
                + "        \"_type\": \"ED25519\","
                + "        \"key\": \"302a300506032b6570032100e0c8ec27b039a7d094a6132049386d9a0d8e8751508241473919e1c455f75605\""
                + "      },"
                + "      \"service_endpoints\": ["
                + "        {"
                + "          \"type\": \"BLOCK_NODE\","
                + "          \"port\": 50211,"
                + "          \"requires_tls\": true,"
                + "          \"ip_address\": \"127.0.0.1\","
                + "          \"block_node\": { \"endpoint_apis\": [\"OTHER\"] }"
                + "        }"
                + "      ]"
                + "    }"
                + "  ]"
                + "}";

        Mockito.doReturn(CompletableFuture.completedFuture(mockResponse))
                .when(query)
                .executeMirrorNodeRequest(Mockito.any(Client.class));

        query.setRegisteredNodeId(123L);
        var result = query.execute(mockClient);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.registeredNodes).hasSize(1);

        var node = result.registeredNodes.get(0);
        Assertions.assertThat(node.registeredNodeId).isEqualTo(123L);
        Assertions.assertThat(node.description).isEqualTo("Test Node");
        Assertions.assertThat(node.adminKey).isNotNull();
        Assertions.assertThat(node.adminKey instanceof PublicKey).isTrue();

        var endpoint = node.serviceEndpoints.get(0);
        Assertions.assertThat(endpoint instanceof BlockNodeServiceEndpoint).isTrue();
        Assertions.assertThat(endpoint.getPort()).isEqualTo(50211);
        Assertions.assertThat(endpoint.isRequiresTls()).isTrue();
    }
}
