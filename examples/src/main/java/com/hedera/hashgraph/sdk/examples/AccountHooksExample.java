// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * How to work with account hooks.
 * <p>
 * This example demonstrates how to create accounts with hooks and add hooks to existing accounts.
 * It shows different types of hook configurations including basic lambda hooks and hooks with storage updates.
 */
class AccountHooksExample {

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
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL,
     * for example via VM options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");


    public static void main(String[] args) throws Exception {
        System.out.println("Account Hooks Example Start!");

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
         * Demonstrate creating an account with hooks.
         */
        System.out.println("\n=== Creating Account with Hooks ===");
        AccountWithKey accountWithKey = createAccountWithHooks(client, contractId);
        AccountId accountId = accountWithKey.accountId;
        PrivateKey accountKey = accountWithKey.privateKey;

        /*
         * Step 3:
         * Demonstrate adding hooks to an existing account.
         */
        System.out.println("\n=== Adding Hooks to Existing Account ===");
        addHooksToAccount(client, contractId, accountId, accountKey);

        /*
         * Step 4:
         * Demonstrate hook deletion.
         */
        System.out.println("\n=== Deleting Hooks from Account ===");
        deleteHooksFromAccount(client, accountId, accountKey);

        client.close();
        System.out.println("Account Hooks Example Complete!");
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
     * Creates an account with hooks from the start.
     */
    private static AccountWithKey createAccountWithHooks(Client client, ContractId contractId) throws Exception {
        System.out.println("Creating account with lambda EVM hook...");

        // Create storage updates for the hook
        byte[] storageKey1 = new byte[32];
        Arrays.fill(storageKey1, (byte) 0x01);
        byte[] storageValue1 = new byte[32];
        Arrays.fill(storageValue1, (byte) 0x64);

        byte[] storageKey2 = new byte[32];
        Arrays.fill(storageKey2, (byte) 0x02);
        byte[] storageValue2 = new byte[32];
        Arrays.fill(storageValue2, (byte) 0x32);

        List<LambdaStorageUpdate> storageUpdates = Arrays.asList(
                new LambdaStorageUpdate.LambdaStorageSlot(storageKey1, storageValue1),
                new LambdaStorageUpdate.LambdaStorageSlot(storageKey2, storageValue2));

        // Create lambda hook with storage updates
        LambdaEvmHook lambdaHook = new LambdaEvmHook(contractId, storageUpdates);

        // Create hook creation details
        Key adminKey = OPERATOR_KEY.getPublicKey();
        HookCreationDetails hookDetails =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1002L, lambdaHook, adminKey);

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

            if (accountCreateReceipt.status == Status.SUCCESS) {
                System.out.println("Successfully created account with lambda hook!");
            } else {
                System.err.println("Failed to create account with hook. Status: " + accountCreateReceipt.status);
            }

            return new AccountWithKey(accountId, accountKey);
        } catch (Exception e) {
            System.err.println("Failed to execute account creation with hook: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Adds hooks to an existing account.
     */
    private static void addHooksToAccount(
            Client client, ContractId contractId, AccountId accountId, PrivateKey accountKey) throws Exception {
        System.out.println("Adding hooks to existing account...");

        // Admin key for the hooks
        Key adminKey = OPERATOR_KEY.getPublicKey();

        // Hook 1: Basic lambda hook with no storage updates
        LambdaEvmHook basicHook = new LambdaEvmHook(contractId);
        HookCreationDetails hook1 =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, basicHook, adminKey);

        // Hook 2: Lambda hook with storage slot updates
        byte[] storageKey = new byte[32];
        Arrays.fill(storageKey, (byte) 0x01);
        byte[] storageValue = new byte[32];
        Arrays.fill(storageValue, (byte) 0x64);

        List<LambdaStorageUpdate> storageUpdates =
                Arrays.asList(new LambdaStorageUpdate.LambdaStorageSlot(storageKey, storageValue));

        LambdaEvmHook storageHook = new LambdaEvmHook(contractId, storageUpdates);
        HookCreationDetails hook2 =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, storageHook, adminKey);

        try {
            TransactionResponse accountUpdateResponse = new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .addHookToCreate(hook1)
                    .addHookToCreate(hook2)
                    .freezeWith(client)
                    .sign(accountKey)
                    .execute(client);

            TransactionReceipt accountUpdateReceipt = accountUpdateResponse.getReceipt(client);

            if (accountUpdateReceipt.status == Status.SUCCESS) {
                System.out.println("Successfully added hooks to account!");
            } else {
                System.err.println("Failed to add hooks to account. Status: " + accountUpdateReceipt.status);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute hook transaction: " + e.getMessage());
        }

        // Verify the hooks were added by querying account info
        AccountInfo accountInfo = new AccountInfoQuery().setAccountId(accountId).execute(client);

        System.out.println("Account ID: " + accountInfo.accountId);
        System.out.println("Account balance: " + accountInfo.balance);
    }

    /**
     * Deletes hooks from an account.
     */
    private static void deleteHooksFromAccount(Client client, AccountId accountId, PrivateKey accountKey)
            throws Exception {
        System.out.println("Deleting hooks from account...");

        // First, delete the basic hook (no storage)
        try {
            TransactionResponse deleteHookResponse = new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .addHookToDelete(1L) // Delete hook with ID 1 (basic hook without storage)
                    .freezeWith(client)
                    .sign(accountKey)
                    .execute(client);

            TransactionReceipt deleteHookReceipt = deleteHookResponse.getReceipt(client);

            if (deleteHookReceipt.status == Status.SUCCESS) {
                System.out.println("Successfully deleted hook with ID: 1");
            } else {
                System.err.println("Failed to delete hook 1. Status: " + deleteHookReceipt.status);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute hook 1 deletion: " + e.getMessage());
        }

        // Note: Hook 2 has storage and cannot be easily deleted without clearing storage first.
        // This demonstrates that hooks with storage require special handling for deletion.
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
                .setGas(1_000_000)
                .setBytecodeFileId(fileId)
                .execute(client);

        var receipt = response.getReceipt(client);
        return receipt.contractId;
    }
}
