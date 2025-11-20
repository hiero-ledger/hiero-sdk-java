// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to work with contract hooks.
 * <p>
 * This example demonstrates how to create contracts with hooks and add hooks to existing contracts.
 * It shows different types of hook configurations including basic lambda hooks and hooks with storage updates.
 */
class ContractHooksExample {
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
     * HEDERA_NETWORK defaults to localhost if not specified in dotenv file.
     * Network can be: localhost, testnet, previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "localhost");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file.
     * Log levels can be: TRACE, DEBUG, INFO, WARN, ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL,
     * for example via VM options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        System.out.println("Contract Hooks Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Create the hook contract.
         */
        System.out.println("Creating hook contract...");
        ContractId contractId = createContractId(client);
        System.out.println("Hook contract created with ID: " + contractId);

        /*
         * Step 2:
         * Demonstrate creating a contract with hooks.
         * Note: This may not work on all networks, so we'll show the concept
         * and then demonstrate adding hooks to existing contracts.
         */
        System.out.println("\n=== Creating Contract with Hooks ===");
        ContractId contractWithHooksId = createContractWithHooks(client, contractId);

        /*
         * Step 3:
         * Demonstrate adding hooks to an existing contract.
         */
        System.out.println("\n=== Adding Hooks to Existing Contract ===");
        addHooksToContract(client, contractId, contractWithHooksId);

        /*
         * Step 4:
         * Demonstrate hook deletion.
         */
        System.out.println("\n=== Deleting Hooks from Contract ===");
        deleteHooksFromContract(client, contractWithHooksId);

        client.close();
        System.out.println("Contract Hooks Example Complete!");
    }

    /**
     * Creates a contract with hooks from the start.
     * Based on ContractCreateTransactionHooksIntegrationTest patterns.
     */
    private static ContractId createContractWithHooks(Client client, ContractId hookContractId) throws Exception {
        System.out.println("Creating contract with lambda EVM hook...");

        // Build a basic lambda EVM hook (no admin key, no storage updates) - like the integration test
        var lambdaHook = new LambdaEvmHook(hookContractId);
        var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

        var response = new ContractCreateTransaction()
                .setAdminKey(OPERATOR_KEY)
                .setGas(400_000)
                .setBytecodeFileId(createBytecodeFile(client))
                .addHook(hookDetails)
                .execute(client);

        var receipt = response.getReceipt(client);
        ContractId contractId = receipt.contractId;
        Objects.requireNonNull(contractId);
        System.out.println("Created contract with ID: " + contractId);
        System.out.println("Successfully created contract with basic lambda hook!");

        return contractId;
    }

    /**
     * Adds hooks to an existing contract.
     */
    private static void addHooksToContract(Client client, ContractId hookContractId, ContractId targetContractId) {
        System.out.println("Adding hooks to existing contract...");

        Key adminKey = OPERATOR_KEY.getPublicKey();

        // Hook 3: Basic lambda hook with no storage updates (using ID 3 to avoid conflict with existing hook 1)
        LambdaEvmHook basicHook = new LambdaEvmHook(hookContractId);
        HookCreationDetails hook3 =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 3L, basicHook, adminKey);
        try {
            TransactionResponse contractUpdateResponse = new ContractUpdateTransaction()
                    .setContractId(targetContractId)
                    .addHookToCreate(hook3)
                    .freezeWith(client)
                    .sign(OPERATOR_KEY)
                    .execute(client);

            contractUpdateResponse.getReceipt(client);

            // Throws on failure; success if we reached here
            System.out.println("Successfully added hooks to contract!");
        } catch (Exception e) {
            System.err.println("Failed to execute hook transaction: " + e.getMessage());
        }
    }

    /**
     * Deletes hooks from a contract.
     */
    private static void deleteHooksFromContract(Client client, ContractId contractId) throws Exception {
        System.out.println("Deleting hooks from contract...");

        // Delete both hooks we created
        try {
            TransactionResponse deleteHookResponse = new ContractUpdateTransaction()
                    .setContractId(contractId)
                    .addHookToDelete(1L) // Delete hook created during contract creation
                    .addHookToDelete(3L) // Delete hook added via contract update
                    .freezeWith(client)
                    .sign(OPERATOR_KEY)
                    .execute(client);

            deleteHookResponse.getReceipt(client);

            // Throws on failure; success if we reached here
            System.out.println("Successfully deleted hooks with IDs: 1 and 3");
        } catch (Exception e) {
            System.err.println("Failed to execute hook deletion: " + e.getMessage());
        }
    }

    private static FileId createBytecodeFile(Client client) throws Exception {
        String contractBytecodeHex = ContractHelper.getBytecodeHex("contracts/hello_world/hello_world.json");

        var response = new FileCreateTransaction()
                .setKeys(OPERATOR_KEY)
                .setContents(contractBytecodeHex)
                .execute(client);
        return Objects.requireNonNull(response.getReceipt(client).fileId);
    }

    private static ContractId createContractId(Client client) throws Exception {
        var fileId = createBytecodeFile(client);

        var response = new ContractCreateTransaction()
                .setAdminKey(OPERATOR_KEY)
                .setGas(500_000)
                .setBytecodeFileId(fileId)
                .execute(client);

        var receipt = response.getReceipt(client);
        return receipt.contractId;
    }
}
