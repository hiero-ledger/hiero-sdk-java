// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * How to work with LambdaSStoreTransaction.
 * <p>
 * This example demonstrates how to update storage slots of existing lambda hooks using LambdaSStoreTransaction.
 * The example includes prerequisite setup (creating a hook contract and account with lambda hook)
 * to demonstrate the LambdaSStoreTransaction functionality.
 */
class LambdaSStoreExample {

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
        System.out.println("Lambda SStore Example Start!");

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
         * Set up prerequisites: Create hook contract and account with lambda hook.
         * Note: This is not part of LambdaSStoreTransaction itself, but required for the example.
         */
        System.out.println("Setting up prerequisites...");
        ContractId contractId = createContractId(client);
        AccountWithKey accountWithKey = createAccountWithLambdaHook(client, contractId);
        AccountId accountId = accountWithKey.accountId;
        PrivateKey accountKey = accountWithKey.privateKey;

        /*
         * Step 2:
         * Demonstrate LambdaSStoreTransaction - the core functionality.
         */
        System.out.println("\n=== LambdaSStoreTransaction Example ===");

        // Create storage update (equivalent to TypeScript sample)
        byte[] storageKey = new byte[32];
        Arrays.fill(storageKey, (byte) 1);
        byte[] storageValue = new byte[32];
        Arrays.fill(storageValue, (byte) 200);

        LambdaStorageUpdate storageUpdate = new LambdaStorageUpdate.LambdaStorageSlot(storageKey, storageValue);

        // Create HookId for the existing hook (accountId with hook ID 1)
        HookId hookId = new HookId(new HookEntityId(accountId), 1L);

        // Execute LambdaSStoreTransaction (matches TypeScript pattern)
        TransactionResponse lambdaStoreResponse = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageUpdate(storageUpdate)
                .freezeWith(client)
                .sign(accountKey)
                .execute(client);

        lambdaStoreResponse.getReceipt(client);
        System.out.println("Successfully updated lambda hook storage!");

        client.close();
        System.out.println("Lambda SStore Example Complete!");
    }

    /**
     * Simple class to hold both account ID and private key.
     */
    private static class AccountWithKey {
        final AccountId accountId;
        final PrivateKey privateKey;

        AccountWithKey(AccountId accountId, PrivateKey privateKey) {
            this.accountId = accountId;
            this.privateKey = privateKey;
        }
    }

    /**
     * Creates an account with a lambda hook that has initial storage.
     * This is a prerequisite for the LambdaSStoreTransaction example.
     */
    private static AccountWithKey createAccountWithLambdaHook(Client client, ContractId contractId) throws Exception {
        System.out.println("Creating account with lambda hook");
        // Create lambda hook with initial storage updates
        LambdaEvmHook lambdaHook = new LambdaEvmHook(contractId);

        // Create hook creation details
        Key adminKey = OPERATOR_KEY.getPublicKey();
        HookCreationDetails hookDetails =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook, adminKey);

        // Create account with lambda hook
        PrivateKey accountKey = PrivateKey.generateED25519();
        PublicKey accountPublicKey = accountKey.getPublicKey();

        try {
            TransactionResponse accountCreateResponse = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountPublicKey)
                    .setInitialBalance(Hbar.from(1))
                    .addHook(hookDetails)
                    .freezeWith(client)
                    .sign(accountKey)
                    .execute(client);

            TransactionReceipt accountCreateReceipt = accountCreateResponse.getReceipt(client);
            AccountId accountId = accountCreateReceipt.accountId;
            Objects.requireNonNull(accountId);
            System.out.println("Created account with ID: " + accountId);
            System.out.println("Successfully created account with lambda hook and initial storage!");

            return new AccountWithKey(accountId, accountKey);
        } catch (Exception e) {
            System.err.println("Failed to execute account creation with hook: " + e.getMessage());
            throw e;
        }
    }

    private static FileId createBytecodeFile(Client client) throws Exception {
        String contractBytecodeHex = ContractHelper.getBytecodeHex("contracts/hiero_hook/hiero_hook.json");

        var response = new FileCreateTransaction()
                .setKeys(OPERATOR_KEY)
                .setContents(contractBytecodeHex.getBytes(StandardCharsets.UTF_8))
                .setMaxTransactionFee(Hbar.from(2))
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
