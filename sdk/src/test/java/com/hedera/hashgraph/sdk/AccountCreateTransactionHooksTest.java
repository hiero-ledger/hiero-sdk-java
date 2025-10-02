// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

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
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(100))
                .addAccountAllowanceHook(1L, contractId, adminKey.getPublicKey(), storageUpdates)
                .addAccountAllowanceHook(2L, contractId); // Simple hook without admin key or storage
        
        // Verify hooks were added
        List<HookCreationDetails> hookDetails = transaction.getHookCreationDetails();
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
        EvmHookSpec evmHookSpec = new EvmHookSpec(contractId);
        LambdaEvmHook lambdaEvmHook = new LambdaEvmHook(evmHookSpec);
        HookCreationDetails hookDetails = new HookCreationDetails(
                HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 
                1L, 
                lambdaEvmHook
        );
        
        // Set hooks using setHookCreationDetails
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(50))
                .setHookCreationDetails(Collections.singletonList(hookDetails));
        
        // Verify hooks were set
        List<HookCreationDetails> retrievedHooks = transaction.getHookCreationDetails();
        assertEquals(1, retrievedHooks.size());
        assertEquals(hookDetails, retrievedHooks.get(0));
    }

    @Test
    public void testAccountCreateTransactionHookValidation() {
        ContractId contractId = new ContractId(300);
        
        // Test duplicate hook IDs
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(25))
                .addAccountAllowanceHook(1L, contractId)
                .addAccountAllowanceHook(1L, contractId); // Duplicate hook ID
        
        // This should throw an exception when building
        assertThrows(IllegalArgumentException.class, () -> {
            transaction.build();
        });
    }

    @Test
    public void testAccountCreateTransactionProtobufSerialization() {
        ContractId contractId = new ContractId(400);
        
        // Create transaction with hooks
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(75))
                .addAccountAllowanceHook(1L, contractId);
        
        // Build the protobuf
        var protoBody = transaction.build();
        
        // Verify hook creation details are included
        assertEquals(1, protoBody.getHookCreationDetailsCount());
        
        var protoHookDetails = protoBody.getHookCreationDetails(0);
        assertEquals(com.hedera.hapi.node.hooks.legacy.HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 
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
        List<HookCreationDetails> hookDetails = transaction.getHookCreationDetails();
        assertTrue(hookDetails.isEmpty());
        
        // Should build successfully
        var protoBody = transaction.build();
        assertEquals(0, protoBody.getHookCreationDetailsCount());
    }
}
