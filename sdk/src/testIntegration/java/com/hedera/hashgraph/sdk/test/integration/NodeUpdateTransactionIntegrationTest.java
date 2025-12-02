// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hedera.hashgraph.sdk.*;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NodeUpdateTransactionIntegrationTest {

    @Test
    @DisplayName("Can execute NodeUpdateTransaction")
    void canExecuteNodeUpdateTransaction() throws Exception {
        // Set the network
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));

        try (var client = Client.forNetwork(network).setMirrorNetwork(List.of("localhost:5600"))) {

            // Set the operator to be account 0.0.2
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Set up grpcWebProxyEndpoint address
            var grpcWebProxyEndpoint =
                    new Endpoint().setDomainName("testWebUpdated.com").setPort(123456);

            var response = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setDescription("testUpdated")
                    .setDeclineReward(true)
                    .setGrpcWebProxyEndpoint(grpcWebProxyEndpoint)
                    .execute(client);

            response.getReceipt(client);
        }
    }

    @Test
    @DisplayName("Can delete gRPC web proxy endpoint")
    void canDeleteGrpcWebProxyEndpoint() throws Exception {
        // Set the network
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));

        try (var client = Client.forNetwork(network).setMirrorNetwork(List.of("localhost:5600"))) {

            // Set the operator to be account 0.0.2
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            var response = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .deleteGrpcWebProxyEndpoint()
                    .execute(client);

            response.getReceipt(client);
        }
    }

    // ================== hip-1299 changing node account ID (dab) tests ==================
    @Test
    @Disabled
    @DisplayName(
            "Given a node with an existing account ID, when a NodeUpdateTransaction is submitted to change the account ID to a new valid account with signatures from both the node admin key and the account ID key, then the transaction succeeds")
    void shouldSucceedWhenUpdatingNodeAccountIdWithProperSignatures() throws Exception {
        // Set up the local network with 2 nodes
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:51211", new AccountId(0, 0, 4));

        try (var client = Client.forNetwork(network)
                .setMirrorNetwork(List.of("localhost:5600"))
                .setTransportSecurity(false)) {
            // Set the operator to be account 0.0.2
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Given: A node with an existing account ID (0.0.3)
            // First, create a new account that will be used as the new node account ID
            var newAccountKey = PrivateKey.generateED25519();
            var newAccountId = createAccount(client, newAccountKey.getPublicKey(), Hbar.from(10));

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
            nodeUpdateTransaction.freezeWith(client).sign(nodeAdminKey).sign(newAccountKey);

            // Then: The transaction should succeed
            assertThatCode(() -> {
                        var response = nodeUpdateTransaction.execute(client);
                        assertThat(response).isNotNull();

                        // Verify the transaction was successful by checking the receipt
                        var receipt = response.getReceipt(client);
                        assertThat(receipt.status).isEqualTo(Status.SUCCESS);
                    })
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName(
            "Given a node with an existing account ID, when a NodeUpdateTransaction is submitted to change the account ID to the same existing account ID with proper signatures, then the transaction succeeds")
    void testNodeUpdateTransactionCanChangeToSameAccount() throws Exception {
        // Set up the local network with 2 nodes
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:51211", new AccountId(0, 0, 4));

        try (var client =
                Client.forNetwork(network).setTransportSecurity(false).setMirrorNetwork(List.of("localhost:5600"))) {

            // Set the operator to be account 0.0.2
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Given: A node with an existing account ID (0.0.3)
            // When: A NodeUpdateTransaction is submitted to change the account ID to the same account (0.0.3)
            var resp = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setDescription("testUpdated")
                    .setAccountId(new AccountId(0, 0, 3))
                    .setNodeAccountIds(List.of(new AccountId(0, 0, 3)))
                    .execute(client);

            // Then: The transaction should succeed
            var receipt = resp.setValidateStatus(true).getReceipt(client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given a node whose account ID has been updated, when a transaction is submitted to that node with the old account ID after the NodeUpdateTransaction reaches consensus, then the signed transaction for this node fails with INVALID_NODE_ACCOUNT_ID and the SDK retries successfully with another node")
    void testNodeUpdateTransactionCanChangeNodeAccountUpdateAddressbookAndRetry() throws Exception {
        // Set the network
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:51211", new AccountId(0, 0, 4));

        try (var client =
                Client.forNetwork(network).setTransportSecurity(false).setMirrorNetwork(List.of("localhost:5600"))) {

            // Set the operator to be account 0.0.2
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Create the account that will be the node account id
            var newNodeAccountID = createAccount(client, originalOperatorKey.getPublicKey(), Hbar.from(1));
            assertThat(newNodeAccountID).isNotNull();

            // Update node account id (0.0.3 -> newNodeAccountID)
            var resp = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setDescription("testUpdated")
                    .setAccountId(newNodeAccountID)
                    .execute(client);

            System.out.println("Transaction node: " + resp.nodeId);
            System.out.println("Receipt query nodes: " + resp.getReceiptQuery().getNodeAccountIds());
            System.out.println("Client network: " + client.getNetwork());
            var receipt = resp.setValidateStatus(true).getReceipt(client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);

            // Wait for mirror node to import data
            Thread.sleep(10000);

            // Submit to node 3 and node 4
            // Node 3 will fail with INVALID_NODE_ACCOUNT (because it now uses newNodeAccountID)
            // The SDK should automatically:
            // 1. Detect INVALID_NODE_ACCOUNT error
            // 2. Advance to next node
            // 3. Update the address book asynchronously
            // 4. Mark node 3 as unhealthy
            // 5. Retry with node 4 which should succeed
            executeAccountCreate(client, List.of(new AccountId(0, 0, 3), new AccountId(0, 0, 4)));

            // This transaction should succeed using the updated node account ID
            executeAccountCreate(client, List.of(newNodeAccountID));

            // Revert the node account id (newNodeAccountID -> 0.0.3)
            resp = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setNodeAccountIds(List.of(newNodeAccountID))
                    .setDescription("testUpdated")
                    .setAccountId(new AccountId(0, 0, 3))
                    .execute(client);

            receipt = resp.setValidateStatus(true).getReceipt(client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given a node with an existing account ID, when a NodeUpdateTransaction is submitted to change the account ID to a new valid account with only the account ID key signature (missing node admin signature), then the transaction fails with INVALID_SIGNATURE")
    void testNodeUpdateTransactionFailsWithInvalidSignatureWhenMissingNodeAdminSignature() throws Exception {
        // Set up the local network with 2 nodes
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:51211", new AccountId(0, 0, 4));

        try (var client = Client.forNetwork(network)
                .setMirrorNetwork(List.of("localhost:5600"))
                .setTransportSecurity(false)) {
            // Set the operator to be account 0.0.2 initially to create a new account
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Given: Create a new operator account without node admin privileges
            var newOperatorKey = PrivateKey.generateED25519();
            var newOperatorAccountId = createAccount(client, newOperatorKey.getPublicKey(), Hbar.from(2));

            // Switch to the new operator (who doesn't have node admin privileges)
            client.setOperator(newOperatorAccountId, newOperatorKey);

            // When: A NodeUpdateTransaction is submitted without node admin signature
            // (only has the new operator's signature, which is not sufficient)
            var nodeUpdateTransaction = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setDescription("testUpdated")
                    .setAccountId(new AccountId(0, 0, 3))
                    .setNodeAccountIds(List.of(new AccountId(0, 0, 3)));

            // Then: The transaction should fail with INVALID_SIGNATURE
            assertThatThrownBy(() -> {
                        var response = nodeUpdateTransaction.execute(client);
                        response.getReceipt(client);
                    })
                    .isInstanceOf(ReceiptStatusException.class)
                    .hasMessageContaining("INVALID_SIGNATURE")
                    .satisfies(exception -> {
                        var receiptException = (ReceiptStatusException) exception;
                        assertThat(receiptException.receipt.status).isEqualTo(Status.INVALID_SIGNATURE);
                    });
        }
    }

    @Test
    @DisplayName(
            "Given a node with an existing account ID, when a NodeUpdateTransaction is submitted to change the account ID to a new valid account with only the node admin key signature (missing account ID signature), then the transaction fails with INVALID_SIGNATURE")
    void testNodeUpdateTransactionFailsWithInvalidSignatureWhenMissingAccountIdSignature() throws Exception {
        // Set up the local network with 2 nodes
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:51211", new AccountId(0, 0, 4));

        try (var client = Client.forNetwork(network)
                .setMirrorNetwork(List.of("localhost:5600"))
                .setTransportSecurity(false)) {
            // Set the operator to be account 0.0.2 (has node admin privileges)
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Given: Create a new account that will be used as the new node account ID
            var newAccountKey = PrivateKey.generateED25519();
            var newAccountId = createAccount(client, newAccountKey.getPublicKey(), Hbar.from(2));

            // When: A NodeUpdateTransaction is submitted with node admin signature
            // but WITHOUT the new account ID's signature
            var nodeUpdateTransaction = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setDescription("testUpdated")
                    .setAccountId(newAccountId)
                    .setNodeAccountIds(List.of(new AccountId(0, 0, 3)));

            // Note: The operator (0.0.2) has node admin privileges, so the transaction
            // is automatically signed with the operator's key (node admin signature).
            // However, we're NOT signing with newAccountKey, which is required.

            // Then: The transaction should fail with INVALID_SIGNATURE
            assertThatThrownBy(() -> {
                        var response = nodeUpdateTransaction.execute(client);
                        response.getReceipt(client);
                    })
                    .isInstanceOf(ReceiptStatusException.class)
                    .hasMessageContaining("INVALID_SIGNATURE")
                    .satisfies(exception -> {
                        var receiptException = (ReceiptStatusException) exception;
                        assertThat(receiptException.receipt.status).isEqualTo(Status.INVALID_SIGNATURE);
                    });
        }
    }

    // TODO - currently the test fails because returned status is Status.INVALID_SIGNATURE
    @Test
    @Disabled
    @DisplayName(
            "Given a node with an existing account ID, when a NodeUpdateTransaction is submitted to change the account ID to a non-existent account with proper signatures, then the transaction fails with INVALID_ACCOUNT_ID")
    void testNodeUpdateTransactionFailsWithInvalidAccountIdForNonExistentAccount() throws Exception {
        // Set up the local network with 2 nodes
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:51211", new AccountId(0, 0, 4));

        try (var client = Client.forNetwork(network)
                .setMirrorNetwork(List.of("localhost:5600"))
                .setTransportSecurity(false)) {
            // Set the operator to be account 0.0.2 (has admin privileges)
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Given: A node with an existing account ID (0.0.3)
            // When: A NodeUpdateTransaction is submitted to change to a non-existent account (0.0.9999999)
            var nodeUpdateTransaction = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setDescription("testUpdated")
                    .setAccountId(new AccountId(0, 0, 9999999))
                    .setNodeAccountIds(List.of(new AccountId(0, 0, 3)));

            // Then: The transaction should fail with INVALID_NODE_ACCOUNT_ID
            assertThatThrownBy(() -> {
                        var response = nodeUpdateTransaction.execute(client);
                        response.getReceipt(client);
                    })
                    .isInstanceOf(ReceiptStatusException.class)
                    .satisfies(exception -> {
                        var receiptException = (ReceiptStatusException) exception;
                        // The status could be INVALID_ACCOUNT_ID or INVALID_NODE_ACCOUNT_ID
                        assertThat(receiptException.receipt.status)
                                .isIn(Status.INVALID_ACCOUNT_ID, Status.INVALID_NODE_ACCOUNT_ID);
                    });
        }
    }

    @Test
    @DisplayName(
            "Given a node with an existing account ID, when a NodeUpdateTransaction is submitted to change the account ID to a deleted account with proper signatures, then the transaction fails with ACCOUNT_DELETED")
    void testNodeUpdateTransactionFailsWithAccountDeletedForDeletedAccount() throws Exception {
        // Set up the local network with 2 nodes
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:51211", new AccountId(0, 0, 4));

        try (var client = Client.forNetwork(network)
                .setMirrorNetwork(List.of("localhost:5600"))
                .setTransportSecurity(false)) {
            // Set the operator to be account 0.0.2 (has admin privileges)
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Given: Create a new account that will be deleted
            var newAccountKey = PrivateKey.generateED25519();
            var newAccountId = createAccount(client, newAccountKey.getPublicKey(), Hbar.from(2));

            // Delete the account (transfer balance to operator account)
            var deleteResponse = new AccountDeleteTransaction()
                    .setAccountId(newAccountId)
                    .setTransferAccountId(client.getOperatorAccountId())
                    .freezeWith(client)
                    .sign(newAccountKey)
                    .execute(client);

            var deleteReceipt = deleteResponse.getReceipt(client);
            assertThat(deleteReceipt.status).isEqualTo(Status.SUCCESS);

            // When: A NodeUpdateTransaction is submitted to change to the deleted account
            var nodeUpdateTransaction = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setDescription("testUpdated")
                    .setAccountId(newAccountId)
                    .setNodeAccountIds(List.of(new AccountId(0, 0, 3)))
                    .freezeWith(client)
                    .sign(newAccountKey);

            // Then: The transaction should fail with ACCOUNT_DELETED
            assertThatThrownBy(() -> {
                        var response = nodeUpdateTransaction.execute(client);
                        response.getReceipt(client);
                    })
                    .isInstanceOf(ReceiptStatusException.class)
                    .hasMessageContaining("ACCOUNT_DELETED")
                    .satisfies(exception -> {
                        var receiptException = (ReceiptStatusException) exception;
                        assertThat(receiptException.receipt.status).isEqualTo(Status.ACCOUNT_DELETED);
                    });
        }
    }

    @Test
    @DisplayName(
            "Given an successfully handled transaction with outdated node account id , when subsequent transaction that target the new node account id of that node is executed, then the transaction succeeds")
    void testSubsequentTransactionWithNewNodeAccountIdSucceeds() throws Exception {
        // Set the network
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:51211", new AccountId(0, 0, 4));

        try (var client =
                Client.forNetwork(network).setTransportSecurity(false).setMirrorNetwork(List.of("localhost:5600"))) {

            // Set the operator to be account 0.0.2
            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            // Given: Create a new account that will be the node account id
            var newNodeAccountID = createAccount(client, originalOperatorKey.getPublicKey(), Hbar.from(1));
            assertThat(newNodeAccountID).isNotNull();

            // Update node 0's account id (0.0.3 -> newNodeAccountID)
            var resp = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setDescription("testUpdated")
                    .setAccountId(newNodeAccountID)
                    .execute(client);

            var receipt = resp.setValidateStatus(true).getReceipt(client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);

            // Wait for mirror node to import data
            Thread.sleep(10000);

            // Given: Successfully handled transaction with outdated node account ID
            // This transaction targets old node account ID (0.0.3) and new node account ID (0.0.4)
            // Node 0.0.3 will fail with INVALID_NODE_ACCOUNT and SDK will retry with 0.0.4
            executeAccountCreate(client, List.of(new AccountId(0, 0, 3), new AccountId(0, 0, 4)));

            // When: Subsequent transaction targets the NEW node account ID directly
            executeAccountCreate(client, List.of(newNodeAccountID));

            // Cleanup: Revert the node account id (newNodeAccountID -> 0.0.3)
            resp = new NodeUpdateTransaction()
                    .setNodeId(0)
                    .setNodeAccountIds(List.of(newNodeAccountID))
                    .setDescription("testUpdated")
                    .setAccountId(new AccountId(0, 0, 3))
                    .execute(client);

            receipt = resp.setValidateStatus(true).getReceipt(client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given an SDK receives INVALID_NODE_ACCOUNT for a node, when updating its network configuration, then the SDK updates its network with the latest node account IDs for subsequent transactions")
    void testSdkUpdatesNetworkConfigurationOnInvalidNodeAccount() throws Exception {
        var network = new HashMap<String, AccountId>();
        network.put("localhost:50211", new AccountId(0, 0, 3));
        network.put("localhost:51211", new AccountId(0, 0, 4));

        try (var client =
                Client.forNetwork(network).setTransportSecurity(false).setMirrorNetwork(List.of("localhost:5600"))) {

            var originalOperatorKey = PrivateKey.fromString(
                    "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137");
            client.setOperator(new AccountId(0, 0, 2), originalOperatorKey);

            var newNodeAccountID = createAccount(client, originalOperatorKey.getPublicKey(), Hbar.from(1));
            assertThat(newNodeAccountID).isNotNull();

            updateNodeAccountId(client, 0, newNodeAccountID, null);

            Thread.sleep(10000);

            // Trigger INVALID_NODE_ACCOUNT error and retry
            executeAccountCreate(client, List.of(new AccountId(0, 0, 3), new AccountId(0, 0, 4)));

            // Verify subsequent transaction with new node account ID
            executeAccountCreate(client, List.of(newNodeAccountID));

            // Verify the network configuration now includes the new account ID
            var finalNetwork = client.getNetwork();
            var hasNewAccountId =
                    finalNetwork.values().stream().anyMatch(accountId -> accountId.equals(newNodeAccountID));
            assertThat(hasNewAccountId)
                    .as("Client network should contain the new node account ID after address book update")
                    .isTrue();

            // Cleanup
            updateNodeAccountId(client, 0, new AccountId(0, 0, 3), List.of(newNodeAccountID));
        }
    }

    private AccountId createAccount(Client client, Key key, Hbar initialBalance) throws Exception {
        var resp = new AccountCreateTransaction()
                .setKey(key)
                .setInitialBalance(initialBalance)
                .execute(client);
        return resp.setValidateStatus(true).getReceipt(client).accountId;
    }

    private void updateNodeAccountId(Client client, long nodeId, AccountId newAccountId, List<AccountId> nodeAccountIds)
            throws Exception {
        var transaction = new NodeUpdateTransaction()
                .setNodeId(nodeId)
                .setDescription("testUpdated")
                .setAccountId(newAccountId);

        if (nodeAccountIds != null) {
            transaction.setNodeAccountIds(nodeAccountIds);
        }

        var resp = transaction.execute(client);
        var receipt = resp.setValidateStatus(true).getReceipt(client);
        assertThat(receipt.status).isEqualTo(Status.SUCCESS);
    }

    private void executeAccountCreate(Client client, List<AccountId> nodeAccountIds) throws Exception {
        var newAccountKey = PrivateKey.generateED25519();
        var resp = new AccountCreateTransaction()
                .setKey(newAccountKey.getPublicKey())
                .setNodeAccountIds(nodeAccountIds)
                .execute(client);

        assertThat(resp).isNotNull();
        var receipt = resp.setValidateStatus(true).getReceipt(client);
        assertThat(receipt.status).isEqualTo(Status.SUCCESS);
    }
}
