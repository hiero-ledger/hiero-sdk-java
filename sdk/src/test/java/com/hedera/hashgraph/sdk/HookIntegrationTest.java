// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

/**
 * Integration test demonstrating the complete hook workflow.
 * <p>
 * This test shows how to:
 * 1. Create an account with hooks
 * 2. Update hook storage using LambdaSStoreTransaction
 * 3. Verify the complete workflow
 */
public class HookIntegrationTest {

    @Test
    public void testCompleteHookWorkflow() {
        // Step 1: Create a contract that will implement the hook
        ContractId hookContract = new ContractId(1000);
        
        // Step 2: Create an account with an allowance hook
        AccountCreateTransaction accountCreateTx = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(100))
                .addAccountAllowanceHook(1L, hookContract);
        
        // Verify the account creation transaction has hooks
        List<HookCreationDetails> hookDetails = accountCreateTx.getHookCreationDetails();
        assertEquals(1, hookDetails.size());
        
        HookCreationDetails hookDetail = hookDetails.get(0);
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, hookDetail.getExtensionPoint());
        assertEquals(1L, hookDetail.getHookId());
        assertEquals(hookContract, hookDetail.getHook().getSpec().getContractId());
        
        // Step 3: Create a LambdaSStoreTransaction to update the hook's storage
        AccountId accountId = new AccountId(2000);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Create storage updates for the hook
        byte[] storageKey1 = {0x01, 0x02, 0x03};
        byte[] storageValue1 = {0x04, 0x05, 0x06};
        byte[] storageKey2 = {0x07, 0x08, 0x09};
        byte[] storageValue2 = {0x0A, 0x0B, 0x0C};
        
        LambdaSStoreTransaction storageUpdateTx = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageSlot(storageKey1, storageValue1)
                .addStorageSlot(storageKey2, storageValue2);
        
        // Verify the storage update transaction
        List<LambdaStorageUpdate> storageUpdates = storageUpdateTx.getStorageUpdates();
        assertEquals(2, storageUpdates.size());
        
        // Verify the first storage slot
        LambdaStorageUpdate.LambdaStorageSlot slot1 = (LambdaStorageUpdate.LambdaStorageSlot) storageUpdates.get(0);
        assertArrayEquals(storageKey1, slot1.getKey());
        assertArrayEquals(storageValue1, slot1.getValue());
        
        // Verify the second storage slot
        LambdaStorageUpdate.LambdaStorageSlot slot2 = (LambdaStorageUpdate.LambdaStorageSlot) storageUpdates.get(1);
        assertArrayEquals(storageKey2, slot2.getKey());
        assertArrayEquals(storageValue2, slot2.getValue());
        
        // Step 4: Test mapping entries update
        byte[] mappingSlot = {0x10, 0x11, 0x12};
        LambdaMappingEntry entry1 = LambdaMappingEntry.ofKey(new byte[]{0x13}, new byte[]{0x14});
        LambdaMappingEntry entry2 = LambdaMappingEntry.withPreimage(new byte[]{0x15, 0x16}, new byte[]{0x17});
        List<LambdaMappingEntry> mappingEntries = List.of(entry1, entry2);
        
        LambdaSStoreTransaction mappingUpdateTx = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addMappingEntries(mappingSlot, mappingEntries);
        
        // Verify the mapping update transaction
        List<LambdaStorageUpdate> mappingUpdates = mappingUpdateTx.getStorageUpdates();
        assertEquals(1, mappingUpdates.size());
        
        LambdaStorageUpdate.LambdaMappingEntries mappingUpdate = (LambdaStorageUpdate.LambdaMappingEntries) mappingUpdates.get(0);
        assertArrayEquals(mappingSlot, mappingUpdate.getMappingSlot());
        assertEquals(mappingEntries, mappingUpdate.getEntries());
        
        // Step 5: Test protobuf serialization
        var accountCreateProto = accountCreateTx.build();
        assertTrue(accountCreateProto.getHookCreationDetailsCount() > 0);
        
        var storageUpdateProto = storageUpdateTx.build();
        assertTrue(storageUpdateProto.getStorageUpdatesCount() > 0);
        
        // Step 6: Test validation
        // Test missing hook ID
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaSStoreTransaction()
                    .addStorageSlot(new byte[]{0x01}, new byte[]{0x02})
                    .build();
        });
        
        // Test empty storage updates
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaSStoreTransaction()
                    .setHookId(hookId)
                    .build();
        });
        
        // Test duplicate hook IDs in account creation
        assertThrows(IllegalArgumentException.class, () -> {
            new AccountCreateTransaction()
                    .setKey(PrivateKey.generateED25519().getPublicKey())
                    .setInitialBalance(Hbar.from(50))
                    .addAccountAllowanceHook(1L, hookContract)
                    .addAccountAllowanceHook(1L, hookContract) // Duplicate hook ID
                    .build();
        });
    }

    @Test
    public void testHookTypesSerialization() {
        // Test all hook types can be serialized to protobuf and back
        
        // Test HookExtensionPoint
        HookExtensionPoint extensionPoint = HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK;
        assertEquals(extensionPoint, HookExtensionPoint.fromProtobuf(extensionPoint.getProtoValue()));
        
        // Test HookEntityId
        AccountId accountId = new AccountId(3000);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        assertEquals(entityId, HookEntityId.fromProtobuf(entityId.toProtobuf()));
        
        ContractId contractId = new ContractId(4000);
        HookEntityId contractEntityId = HookEntityId.ofContract(contractId);
        assertEquals(contractEntityId, HookEntityId.fromProtobuf(contractEntityId.toProtobuf()));
        
        // Test HookId
        HookId hookId = new HookId(entityId, 5L);
        assertEquals(hookId, HookId.fromProtobuf(hookId.toProtobuf()));
        
        // Test EvmHookSpec
        EvmHookSpec evmHookSpec = new EvmHookSpec(contractId);
        assertEquals(evmHookSpec, EvmHookSpec.fromProtobuf(evmHookSpec.toProtobuf()));
        
        // Test LambdaStorageUpdate
        LambdaStorageUpdate storageSlot = new LambdaStorageUpdate.LambdaStorageSlot(
                new byte[]{0x20}, new byte[]{0x21});
        assertEquals(storageSlot, LambdaStorageUpdate.fromProtobuf(storageSlot.toProtobuf()));
        
        // Test LambdaMappingEntry
        LambdaMappingEntry mappingEntry = LambdaMappingEntry.ofKey(
                new byte[]{0x22}, new byte[]{0x23});
        assertEquals(mappingEntry, LambdaMappingEntry.fromProtobuf(mappingEntry.toProtobuf()));
        
        // Test LambdaEvmHook
        LambdaEvmHook lambdaEvmHook = new LambdaEvmHook(evmHookSpec, Collections.singletonList(storageSlot));
        assertEquals(lambdaEvmHook, LambdaEvmHook.fromProtobuf(lambdaEvmHook.toProtobuf()));
        
        // Test HookCreationDetails
        HookCreationDetails hookCreationDetails = new HookCreationDetails(
                extensionPoint, 6L, lambdaEvmHook, PrivateKey.generateED25519().getPublicKey());
        assertEquals(hookCreationDetails, HookCreationDetails.fromProtobuf(hookCreationDetails.toProtobuf()));
    }

    @Test
    public void testHookValidation() {
        // Test various validation scenarios
        
        // Test LambdaStorageSlot validation
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaStorageUpdate.LambdaStorageSlot(new byte[33], new byte[]{0x01});
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaStorageUpdate.LambdaStorageSlot(new byte[]{0x00, 0x01}, new byte[]{0x01});
        });
        
        // Test LambdaMappingEntry validation
        assertThrows(IllegalArgumentException.class, () -> {
            LambdaMappingEntry.ofKey(new byte[33], new byte[]{0x01});
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            LambdaMappingEntry.ofKey(new byte[]{0x00, 0x01}, new byte[]{0x01});
        });
        
        // Test LambdaSStoreTransaction validation
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaSStoreTransaction()
                    .addStorageSlot(new byte[]{0x01}, new byte[]{0x02})
                    .build();
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaSStoreTransaction()
                    .setHookId(new HookId(HookEntityId.ofAccount(new AccountId(1)), 1L))
                    .build();
        });
    }
}
