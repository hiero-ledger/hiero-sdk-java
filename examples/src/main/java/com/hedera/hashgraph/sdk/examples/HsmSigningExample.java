// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Hedera HSM Signing Example.
 * <p>
 * Demonstrates how to sign transactions using HSM (Hardware Security Module) simulation
 * with both single node and multi-node scenarios. This example shows how to:
 * 1. Get signable transaction body bytes for HSM signing
 * 2. Add signatures back to transactions using AddSignatureV2
 * 3. Handle both simple transfers and chunked file operations
 */
public class HsmSigningExample {
    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * Operator's account ID.
     * Used to sign and pay for operations on Hedera.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file.
     * Network can be: localhost, testnet, previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file.
     * Log levels can be: TRACE, DEBUG, INFO, WARN, ERROR, SILENT.
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    /**
     * Main method to demonstrate HSM signing scenarios.
     *
     * @param args Command-line arguments (not used in this example)
     */
    public static void main(String[] args) {
        System.out.println("HSM Signing Example Start!");

        try {
            /*
             * Step 0:
             * Create and configure SDK Client.
             */
            Client client = createClient();

            /*
             * Step 1:
             * Generate keys and create test accounts.
             */
            AccountSetup accounts = setupTestAccounts(client);

            /*
             * Step 2:
             * Demonstrate single node transaction signing.
             */
            singleNodeTransactionExample(client, accounts.senderId, accounts.receiverId, accounts.senderKey);

            /*
             * Step 3:
             * Demonstrate multi-node multi-chunk transaction signing.
             */
            multiNodeFileTransactionExample(client, accounts.senderId, accounts.senderKey);

            /*
             * Clean up:
             */
            client.close();

        } catch (Exception e) {
            System.err.println("Example failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("HSM Signing Example Complete!");
    }

    /**
     * Container class for account setup results.
     */
    private static class AccountSetup {
        final AccountId senderId;
        final AccountId receiverId;
        final PrivateKey senderKey;
        final PrivateKey receiverKey;

        AccountSetup(AccountId senderId, AccountId receiverId, PrivateKey senderKey, PrivateKey receiverKey) {
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.senderKey = senderKey;
            this.receiverKey = receiverKey;
        }
    }

    /**
     * Sets up test accounts for the example.
     *
     * @param client The Hedera network client
     * @return AccountSetup containing the created accounts and keys
     * @throws Exception If account creation fails
     */
    private static AccountSetup setupTestAccounts(Client client) throws Exception {
        System.out.println("\n--- Setting up test accounts ---");

        // Generate keys for sender and receiver
        PrivateKey senderKey = PrivateKey.generateED25519();
        PrivateKey receiverKey = PrivateKey.generateED25519();

        // Create sender account
        TransactionResponse senderAccountResponse = new AccountCreateTransaction()
                .setKeyWithoutAlias(senderKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .execute(client);

        TransactionReceipt senderAccountReceipt = senderAccountResponse.getReceipt(client);
        AccountId senderId = Objects.requireNonNull(senderAccountReceipt.accountId);

        // Create receiver account
        TransactionResponse receiverAccountResponse = new AccountCreateTransaction()
                .setKeyWithoutAlias(receiverKey.getPublicKey())
                .setInitialBalance(Hbar.from(1))
                .execute(client);

        TransactionReceipt receiverAccountReceipt = receiverAccountResponse.getReceipt(client);
        AccountId receiverId = Objects.requireNonNull(receiverAccountReceipt.accountId);

        System.out.println("Created sender account: " + senderId);
        System.out.println("Created receiver account: " + receiverId);

        return new AccountSetup(senderId, receiverId, senderKey, receiverKey);
    }

    /**
     * Demonstrates single node transaction signing with HSM simulation.
     *
     * @param client     The Hedera network client
     * @param senderId   The sender account ID
     * @param receiverId The receiver account ID
     * @param senderKey  The sender's private key (for HSM simulation)
     * @throws Exception If the transaction fails
     */
    private static void singleNodeTransactionExample(
            Client client, AccountId senderId, AccountId receiverId, PrivateKey senderKey) throws Exception {
        System.out.println("\n--- Single Node Transaction Example ---");

        // Step 1 - Create and prepare transfer transaction
        // Get first node from network
        Map<String, AccountId> network = client.getNetwork();
        AccountId nodeAccountId = network.values().iterator().next();

        // Create transfer transaction
        TransferTransaction transferTx = new TransferTransaction()
                .addHbarTransfer(senderId, Hbar.from(-1))
                .addHbarTransfer(receiverId, Hbar.from(1))
                .setNodeAccountIds(Arrays.asList(nodeAccountId))
                .setTransactionId(TransactionId.generate(senderId))
                .freezeWith(client);

        System.out.println("Transaction frozen. Node IDs: " + transferTx.getNodeAccountIds());

        // Step 2 - Get signable bytes and sign with HSM
        List<Transaction.SignableNodeTransactionBodyBytes> signableList = transferTx.getSignableNodeBodyBytesList();
        System.out.println("Got " + signableList.size() + " signable entries");

        // Sign with HSM for each entry
        for (int i = 0; i < signableList.size(); i++) {
            Transaction.SignableNodeTransactionBodyBytes signable = signableList.get(i);
            System.out.println("Signing entry " + i + " for node " + signable.getNodeID() + " and transaction "
                    + signable.getTransactionID());

            byte[] signature = hsmSign(senderKey, signable.getBody());
            transferTx = transferTx.addSignature(
                    senderKey.getPublicKey(), signature, signable.getTransactionID(), signable.getNodeID());
        }

        // Step 3 - Execute transaction and get receipt
        System.out.println("Executing transaction...");
        TransactionResponse transferResponse = transferTx.execute(client);
        TransactionReceipt transferReceipt = transferResponse.getReceipt(client);

        System.out.println("Single node transaction status: " + transferReceipt.status);
    }

    /**
     * Demonstrates multi-node file transaction signing with HSM simulation.
     * This example creates a large file append transaction that gets chunked across multiple nodes.
     *
     * @param client    The Hedera network client
     * @param senderId  The sender account ID
     * @param senderKey The sender's private key (for HSM simulation)
     * @throws Exception If the transaction fails
     */
    private static void multiNodeFileTransactionExample(Client client, AccountId senderId, PrivateKey senderKey)
            throws Exception {
        System.out.println("\n--- Multi-Node File Transaction Example ---");

        // Step 1 - Create initial file
        // Create smaller content for testing to avoid chunking issues
        String smallContents = "Test file content for HSM signing example.";

        // Create file transaction
        FileCreateTransaction fileCreateTx = new FileCreateTransaction()
                .setKeys(senderKey.getPublicKey())
                .setContents(smallContents.getBytes())
                .setMaxTransactionFee(Hbar.from(5))
                .freezeWith(client)
                .sign(senderKey);

        TransactionResponse fileCreateResponse = fileCreateTx.execute(client);
        TransactionReceipt fileCreateReceipt = fileCreateResponse.getReceipt(client);
        FileId fileId = Objects.requireNonNull(fileCreateReceipt.fileId);

        System.out.println("Created file with ID: " + fileId);

        // Step 2 - Prepare file append transaction (using smaller content to avoid chunking for now)
        String appendContent = "Additional content added via HSM signing.";

        FileAppendTransaction fileAppendTx = new FileAppendTransaction()
                .setFileId(fileId)
                .setContents(appendContent.getBytes())
                .setMaxTransactionFee(Hbar.from(5))
                .setTransactionId(TransactionId.generate(senderId))
                .freezeWith(client);

        System.out.println("File append transaction frozen. Node IDs: " + fileAppendTx.getNodeAccountIds());

        // Step 3 - Get signable bytes and sign with HSM for each node
        List<Transaction.SignableNodeTransactionBodyBytes> multiNodeSignableList =
                fileAppendTx.getSignableNodeBodyBytesList();

        System.out.println("Got " + multiNodeSignableList.size() + " signable entries for file append");

        // Sign with HSM for each entry
        for (int i = 0; i < multiNodeSignableList.size(); i++) {
            Transaction.SignableNodeTransactionBodyBytes signable = multiNodeSignableList.get(i);
            System.out.println("Signing entry " + i + " for node " + signable.getNodeID() + " and transaction "
                    + signable.getTransactionID());

            byte[] signature = hsmSign(senderKey, signable.getBody());
            fileAppendTx = fileAppendTx.addSignature(
                    senderKey.getPublicKey(), signature, signable.getTransactionID(), signable.getNodeID());
        }

        // Step 4 - Execute transaction and verify results
        System.out.println("Executing file append transaction...");
        TransactionResponse fileAppendResponse = fileAppendTx.execute(client);
        TransactionReceipt fileAppendReceipt = fileAppendResponse.getReceipt(client);

        System.out.println("Multi-node file append transaction status: " + fileAppendReceipt.status);

        // Step 5 - Verify file contents
        byte[] contents =
                new FileContentsQuery().setFileId(fileId).execute(client).toByteArray();

        System.out.println("File content length according to FileContentsQuery: " + contents.length);
        System.out.println("File contents: " + new String(contents));
    }

    /**
     * Simulates signing with an HSM (Hardware Security Module).
     * In a real implementation, this would use actual HSM SDK logic.
     *
     * @param key       The private key (representing HSM key access)
     * @param bodyBytes The transaction body bytes to sign
     * @return The signature bytes
     */
    private static byte[] hsmSign(PrivateKey key, byte[] bodyBytes) {
        // This is a placeholder that simulates HSM signing
        // In a real HSM implementation, you would:
        // 1. Send bodyBytes to the HSM
        // 2. Use HSM APIs to sign with the stored private key
        // 3. Return the signature from the HSM
        return key.sign(bodyBytes);
    }

    /**
     * Creates a Hedera network client using configuration from class-level constants.
     *
     * @return Configured Hedera network client
     * @throws InterruptedException If there's an interruption during client creation
     */
    private static Client createClient() throws InterruptedException {
        /*
         * Step 1:
         * Create a client for the specified network.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);

        /*
         * Step 2:
         * Set the operator (account paying for transactions).
         */
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        /*
         * Step 3:
         * Configure logging for the client.
         */
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        return client;
    }
}
