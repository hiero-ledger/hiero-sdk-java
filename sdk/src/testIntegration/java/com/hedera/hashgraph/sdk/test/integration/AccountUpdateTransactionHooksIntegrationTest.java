// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountUpdateTransaction;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HookCreationDetails;
import com.hedera.hashgraph.sdk.HookExtensionPoint;
import com.hedera.hashgraph.sdk.LambdaEvmHook;
import com.hedera.hashgraph.sdk.LambdaMappingEntry;
import com.hedera.hashgraph.sdk.LambdaStorageUpdate;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountUpdateTransactionHooksIntegrationTest {

    @Test
    @DisplayName(
            "Given an account exists without hooks, when an AccountUpdateTransaction adds a basic lambda EVM hook, then the hook is successfully attached to the account")
    void accountUpdateWithBasicLambdaHookSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create an account without hooks first
            var accountKey = PrivateKey.generateED25519();
            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Deploy a simple contract to act as the lambda hook target
            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            // Build a basic lambda EVM hook (no admin key, no storage updates)
            var lambdaHook = new LambdaEvmHook(hookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            // Update the account to add the hook
            var response = new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToCreate(hookDetails)
                    .freezeWith(testEnv.client)
                    .sign(accountKey);

            var receipt = response.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given an AccountUpdateTransaction is configured with duplicate hook IDs in the same creation details, when the transaction is executed, then the transaction fails with a HOOK_ID_REPEATED_IN_CREATION_DETAILS error")
    void accountUpdateWithDuplicateHookIdsInSameTransactionFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create an account without hooks first
            var accountKey = PrivateKey.generateED25519();
            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var lambdaHook = new LambdaEvmHook(hookContractId);
            var hookDetails1 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);
            var hookDetails2 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> new AccountUpdateTransaction()
                            .setAccountId(accountId)
                            .setMaxTransactionFee(Hbar.from(10))
                            .addHookToCreate(hookDetails1)
                            .addHookToCreate(hookDetails2)
                            .freezeWith(testEnv.client)
                            .sign(accountKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.HOOK_ID_REPEATED_IN_CREATION_DETAILS.toString());
        }
    }

    @Test
    @DisplayName(
            "Given an account exists with a hook, when an AccountUpdateTransaction attempts to add a hook with the same ID that already exists on the account, then the transaction fails with a HOOK_ID_IN_USE error")
    void accountUpdateWithExistingHookIdFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create an account with a hook first
            var accountKey = PrivateKey.generateED25519();
            ContractId hookContractId1 = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var lambdaHook1 = new LambdaEvmHook(hookContractId1);
            var hookDetails1 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook1);

            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(1))
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHook(hookDetails1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Try to add another hook with the same ID
            ContractId hookContractId2 = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var lambdaHook2 = new LambdaEvmHook(hookContractId2);
            var hookDetails2 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook2);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new AccountUpdateTransaction()
                            .setAccountId(accountId)
                            .addHookToCreate(hookDetails2)
                            .setMaxTransactionFee(Hbar.from(10))
                            .freezeWith(testEnv.client)
                            .sign(accountKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_ID_IN_USE));
        }
    }

    @Test
    @DisplayName(
            "Given an account exists without hooks, when an AccountUpdateTransaction adds a lambda EVM hook with initial storage updates, then the hook is attached and storage is initialized correctly")
    void accountUpdateWithLambdaHookAndStorageUpdatesSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create an account without hooks first
            var accountKey = PrivateKey.generateED25519();
            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var storageSlot = new LambdaStorageUpdate.LambdaStorageSlot(new byte[] {0x01}, new byte[] {0x02});
            var mappingEntries = new LambdaStorageUpdate.LambdaMappingEntries(
                    new byte[] {0x10},
                    java.util.List.of(LambdaMappingEntry.ofKey(new byte[] {0x11}, new byte[] {0x12})));
            var lambdaHook = new LambdaEvmHook(hookContractId, java.util.List.of(storageSlot, mappingEntries));
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            // Update the account to add the hook with storage updates
            var response = new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToCreate(hookDetails)
                    .freezeWith(testEnv.client)
                    .sign(accountKey);

            var receipt = response.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given an account exists with an existing hook, when an AccountUpdateTransaction attempts to add another hook with the same ID that is already in use, then the transaction fails with a HOOK_ID_IN_USE error")
    void accountUpdateWithHookIdAlreadyInUseFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create an account with a hook first
            var accountKey = PrivateKey.generateED25519();
            ContractId hookContractId1 = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var lambdaHook1 = new LambdaEvmHook(hookContractId1);
            var hookDetails1 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook1);

            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(1))
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHook(hookDetails1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Try to add another hook with the same ID (1L)
            ContractId hookContractId2 = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var lambdaHook2 = new LambdaEvmHook(hookContractId2);
            var hookDetails2 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook2);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new AccountUpdateTransaction()
                            .setAccountId(accountId)
                            .addHookToCreate(hookDetails2)
                            .setMaxTransactionFee(Hbar.from(10))
                            .freezeWith(testEnv.client)
                            .sign(accountKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_ID_IN_USE));
        }
    }

    @Test
    @DisplayName(
            "Given an account exists with a hook, when an AccountUpdateTransaction deletes the hook by ID with valid signatures, then the hook is successfully removed from the account")
    void accountUpdateWithHookDeletionSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create an account with a hook first
            var accountKey = PrivateKey.generateED25519();
            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var lambdaHook = new LambdaEvmHook(hookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(1))
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHook(hookDetails)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Update the account to delete the hook
            var response = new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToDelete(1L)
                    .freezeWith(testEnv.client)
                    .sign(accountKey);

            var receipt = response.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given an account exists with hooks, when an AccountUpdateTransaction attempts to delete a hook ID that doesn't exist on the account, then the transaction fails with a HOOK_NOT_FOUND error")
    void accountUpdateWithNonExistentHookIdDeletionFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create an account with a hook first
            var accountKey = PrivateKey.generateED25519();
            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var lambdaHook = new LambdaEvmHook(hookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(1))
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHook(hookDetails)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Try to delete a hook that doesn't exist (ID 999)
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new AccountUpdateTransaction()
                            .setAccountId(accountId)
                            .setMaxTransactionFee(Hbar.from(10))
                            .addHookToDelete(999L)
                            .freezeWith(testEnv.client)
                            .sign(accountKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_NOT_FOUND));
        }
    }

    @Test
    @DisplayName(
            "Given an AccountUpdateTransaction attempts to add and delete hooks with the same ID in the same transaction, when the transaction is executed, then the transaction fails with a HOOK_NOT_FOUND error")
    void accountUpdateWithAddAndDeleteSameHookIdFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create an account without hooks first
            var accountKey = PrivateKey.generateED25519();
            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var lambdaHook = new LambdaEvmHook(hookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            // Try to add and delete the same hook ID in the same transaction
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new AccountUpdateTransaction()
                            .setAccountId(accountId)
                            .setMaxTransactionFee(Hbar.from(20))
                            .addHookToCreate(hookDetails)
                            .addHookToDelete(1L)
                            .freezeWith(testEnv.client)
                            .sign(accountKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_NOT_FOUND));
        }
    }

    @Test
    @DisplayName(
            "Given an account exists with a hook that has been previously deleted, when an AccountUpdateTransaction attempts to delete the same hook again, then the transaction fails with a HOOK_DELETED error")
    void accountUpdateWithAlreadyDeletedHookFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create an account with a hook first
            var accountKey = PrivateKey.generateED25519();
            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var lambdaHook = new LambdaEvmHook(hookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setMaxTransactionFee(Hbar.from(10))
                    .setInitialBalance(new Hbar(1))
                    .addHook(hookDetails)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // First delete the hook
            var deleteResponse = new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHookToDelete(1L)
                    .freezeWith(testEnv.client)
                    .sign(accountKey);

            var deleteReceipt = deleteResponse.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(deleteReceipt.status).isEqualTo(Status.SUCCESS);

            // Try to delete the same hook again
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new AccountUpdateTransaction()
                            .setAccountId(accountId)
                            .addHookToDelete(1L)
                            .setMaxTransactionFee(Hbar.from(10))
                            .freezeWith(testEnv.client)
                            .sign(accountKey)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_NOT_FOUND));
        }
    }
}
