// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.ContractId;
import org.hiero.sdk.EvmHook;
import org.hiero.sdk.EvmHookMappingEntry;
import org.hiero.sdk.EvmHookStorageUpdate;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.HookCreationDetails;
import org.hiero.sdk.HookExtensionPoint;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.Status;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Disabled("Temporarily disabled, EVM Hooks are not enabled in v0.73.0")
class AccountCreateTransactionHooksIntegrationTest {

    @Test
    @DisplayName("Given AccountCreateTransaction with basic EVM hook, when executed, then receipt status is SUCCESS")
    void accountCreateWithBasicLambdaHookSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Deploy a simple contract to act as the lambda hook target
            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            // Build a basic EVM hook (no admin key, no storage updates)
            var lambdaHook = new EvmHook(hookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            var response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(PrivateKey.generateED25519())
                    .setInitialBalance(new Hbar(1))
                    .setMaxTransactionFee(Hbar.from(10))
                    .addHook(hookDetails)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName("Given AccountCreateTransaction with an EVM hook and storage updates, when executed, then SUCCESS")
    void accountCreateWithLambdaHookAndStorageUpdatesSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var storageSlot = new EvmHookStorageUpdate.EvmHookStorageSlot(new byte[] {0x01}, new byte[] {0x02});
            var mappingEntries = new EvmHookStorageUpdate.EvmHookMappingEntries(
                    new byte[] {0x10},
                    java.util.List.of(EvmHookMappingEntry.ofKey(new byte[] {0x11}, new byte[] {0x12})));
            var lambdaHook = new EvmHook(hookContractId, java.util.List.of(storageSlot, mappingEntries));
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, lambdaHook);

            var response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(PrivateKey.generateED25519())
                    .setInitialBalance(new Hbar(1))
                    .addHook(hookDetails)
                    .setMaxTransactionFee(Hbar.from(10))
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given AccountCreateTransaction with duplicate hook IDs, when executed, then HOOK_ID_REPEATED_IN_CREATION_DETAILS (precheck)")
    void accountCreateWithDuplicateHookIdsFailsPrecheck() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var lambdaHook = new EvmHook(hookContractId);
            var hookDetails1 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 4L, lambdaHook);
            var hookDetails2 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 4L, lambdaHook);

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> new AccountCreateTransaction()
                            .setKeyWithoutAlias(PrivateKey.generateED25519())
                            .setInitialBalance(new Hbar(1))
                            .setMaxTransactionFee(Hbar.from(10))
                            .addHook(hookDetails1)
                            .addHook(hookDetails2)
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .withMessageContaining(Status.HOOK_ID_REPEATED_IN_CREATION_DETAILS.toString());
        }
    }

    @Test
    @DisplayName(
            "Given AccountCreateTransaction with an EVM hook and admin key, when executed with admin signature, then SUCCESS")
    void accountCreateWithLambdaHookAndAdminKeySucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            ContractId hookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var adminKey = PrivateKey.generateED25519();
            var lambdaHook = new EvmHook(hookContractId);
            var hookDetails = new HookCreationDetails(
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 5L, lambdaHook, adminKey.getPublicKey());

            var tx = new AccountCreateTransaction()
                    .setKeyWithoutAlias(PrivateKey.generateED25519())
                    .setMaxTransactionFee(Hbar.from(10))
                    .setInitialBalance(new Hbar(1))
                    .addHook(hookDetails)
                    .freezeWith(testEnv.client)
                    .sign(adminKey);

            var receipt = tx.execute(testEnv.client).getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }
}
