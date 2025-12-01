// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AccountCreateTransactionHooksTest {

    @Test
    public void testAccountCreateTransactionWithHooks() {
        // Create a test contract ID
        ContractId contractId = new ContractId(100);

        // Create a test admin key
        PrivateKey adminKey = PrivateKey.generateED25519();

        // Create storage updates
        byte[] storageKey = {0x01, 0x02};
        byte[] storageValue = {0x03, 0x04};
        LambdaStorageUpdate storageUpdate = new LambdaStorageUpdate.LambdaStorageSlot(storageKey, storageValue);
        List<LambdaStorageUpdate> storageUpdates = Collections.singletonList(storageUpdate);

        // Create account create transaction with hooks
        var lambdaHookWithStorage = new LambdaEvmHook(contractId, storageUpdates);
        var hookWithAdmin = new HookCreationDetails(
                HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHookWithStorage, adminKey.getPublicKey());
        var simpleLambdaHook = new LambdaEvmHook(contractId);
        var simpleHook = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, simpleLambdaHook);

        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(100))
                .addHook(hookWithAdmin)
                .addHook(simpleHook); // Simple hook without admin key or storage

        // Verify hooks were added
        List<HookCreationDetails> hookDetails = transaction.getHooks();
        assertEquals(2, hookDetails.size());

        // Verify first hook
        HookCreationDetails firstHook = hookDetails.get(0);
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, firstHook.getExtensionPoint());
        assertEquals(1L, firstHook.getHookId());
        assertEquals(adminKey.getPublicKey(), firstHook.getAdminKey());
        assertNotNull(firstHook.getHook());
        assertEquals(1, firstHook.getHook().getStorageUpdates().size());

        // Verify second hook
        HookCreationDetails secondHook = hookDetails.get(1);
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, secondHook.getExtensionPoint());
        assertEquals(2L, secondHook.getHookId());
        assertNull(secondHook.getAdminKey());
        assertNotNull(secondHook.getHook());
        assertTrue(secondHook.getHook().getStorageUpdates().isEmpty());
    }

    @Test
    public void testAccountCreateTransactionSetHooks() {
        ContractId contractId = new ContractId(200);

        // Create hook details manually
        LambdaEvmHook lambdaEvmHook = new LambdaEvmHook(contractId);
        HookCreationDetails hookDetails =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaEvmHook);

        // Set hooks using setHookCreationDetails
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(50))
                .setHooks(Collections.singletonList(hookDetails));

        // Verify hooks were set
        List<HookCreationDetails> retrievedHooks = transaction.getHooks();
        assertEquals(1, retrievedHooks.size());
        assertEquals(hookDetails, retrievedHooks.get(0));
    }

    @Test
    public void testAccountCreateTransactionHookValidation() {
        ContractId contractId = new ContractId(300);

        // Test duplicate hook IDs
        var lambdaHook = new LambdaEvmHook(contractId);
        var hook1 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);
        var hook2 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook); // Duplicate ID

        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(25))
                .addHook(hook1)
                .addHook(hook2); // Duplicate hook ID

        // Client-side duplicate ID validation was removed; ensure build includes both entries
        var proto = transaction.build();
        assertEquals(2, proto.getHookCreationDetailsCount());
        assertEquals(1L, proto.getHookCreationDetails(0).getHookId());
        assertEquals(1L, proto.getHookCreationDetails(1).getHookId());
    }

    @Test
    public void testAccountCreateTransactionProtobufSerialization() {
        ContractId contractId = new ContractId(400);

        // Create transaction with hooks
        var lambdaHook = new LambdaEvmHook(contractId);
        var hook = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(75))
                .addHook(hook);

        // Build the protobuf
        var protoBody = transaction.build();

        // Verify hook creation details are included
        assertEquals(1, protoBody.getHookCreationDetailsCount());

        var protoHookDetails = protoBody.getHookCreationDetails(0);
        assertEquals(
                com.hedera.hashgraph.sdk.proto.HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                protoHookDetails.getExtensionPoint());
        assertEquals(1L, protoHookDetails.getHookId());
        assertTrue(protoHookDetails.hasLambdaEvmHook());
    }

    @Test
    public void testAccountCreateTransactionEmptyHooks() {
        // Test transaction without hooks
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(100));

        // Verify no hooks
        List<HookCreationDetails> hookDetails = transaction.getHooks();
        assertTrue(hookDetails.isEmpty());

        // Should build successfully
        var protoBody = transaction.build();
        assertEquals(0, protoBody.getHookCreationDetailsCount());
    }

    @Test
    public void testAccountCreateTransactionHooksPersistThroughBytesRoundTrip()
            throws com.google.protobuf.InvalidProtocolBufferException {
        // Create contract and hook details
        ContractId contractId = new ContractId(500);
        LambdaEvmHook lambdaEvmHook = new LambdaEvmHook(contractId);
        HookCreationDetails hookDetails =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 3L, lambdaEvmHook);

        // Create transaction with set hooks
        AccountCreateTransaction originalTx = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(123))
                .setHooks(Collections.singletonList(hookDetails));

        // Serialize to bytes then deserialize back
        byte[] bytes = originalTx.toBytes();
        Transaction<?> parsed = Transaction.fromBytes(bytes);
        assertTrue(parsed instanceof AccountCreateTransaction);
        AccountCreateTransaction parsedTx = (AccountCreateTransaction) parsed;

        // Verify hook information persisted
        List<HookCreationDetails> parsedHooks = parsedTx.getHooks();
        assertEquals(1, parsedHooks.size());
        HookCreationDetails parsedHook = parsedHooks.get(0);
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, parsedHook.getExtensionPoint());
        assertEquals(3L, parsedHook.getHookId());
        assertNotNull(parsedHook.getHook());
        assertTrue(parsedHook.getHook().getStorageUpdates().isEmpty());
    }
}
