// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.NodeUpdateTransaction;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration test for NodeUpdateTransaction functionality, specifically testing
 * the Dynamic Address Book (DAB) enhancement for updating node account IDs.
 *
 * This test verifies the complete flow of updating a node's account ID with
 * proper signatures from both the node admin key and the account ID key.
 */
class NodeUpdateAccountIdIntegrationTest {

    @Test
    @DisplayName("NodeUpdateTransaction should succeed when updating account ID with proper signatures")
    void shouldSucceedWhenUpdatingNodeAccountIdWithProperSignatures() throws Exception {
        // Set up the local network with 2 nodes
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:50212", new AccountId(0, 0, 4));

        try (var client = Client.forNetwork(network)
                .setMirrorNetwork(List.of("localhost:5600"))
                .setTransportSecurity(false)
                .setVerifyCertificates(false)) {
            // Set the operator to be account 0.0.2
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Given: A node with an existing account ID
            // First, create a new account that will be used as the new node account ID
            var newAccountKey = PrivateKey.generateED25519();
            var newAccountCreateTransaction = new AccountCreateTransaction()
                    .setKey(newAccountKey.getPublicKey())
                    .setInitialBalance(Hbar.from(10))
                    .setMaxTransactionFee(Hbar.from(1));

            var newAccountResponse = newAccountCreateTransaction.execute(client);
            var newAccountReceipt = newAccountResponse.getReceipt(client);
            var newAccountId = newAccountReceipt.accountId;

            // Create a node admin key
            var nodeAdminKey = PrivateKey.generateED25519();

            // When: A NodeUpdateTransaction is submitted to change the account ID
            var nodeUpdateTransaction = new NodeUpdateTransaction()
                    .setNodeId(0) // Using node 0 as in the existing test
                    .setAccountId(newAccountId)
                    .setAdminKey(nodeAdminKey.getPublicKey())
                    .setMaxTransactionFee(Hbar.from(10))
                    .setTransactionMemo("Update node account ID for DAB testing");

            // Sign with both the node admin key and the new account key
            nodeUpdateTransaction.freezeWith(client)
                    .sign(nodeAdminKey)
                    .sign(newAccountKey);

            // Then: The transaction should succeed
            assertThatCode(() -> {
                var response = nodeUpdateTransaction.execute(client);
                assertThat(response).isNotNull();

                // Verify the transaction was successful by checking the receipt
                var receipt = response.getReceipt(client);
                assertThat(receipt.status).isEqualTo(Status.SUCCESS);
            }).doesNotThrowAnyException();
        }
    }
}
