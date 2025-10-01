// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

/**
 * Test class for core hook classes.
 */
public class HookClassesTest {

    @Test
    public void testHookExtensionPoint() {
        // Test enum values
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 
                    HookExtensionPoint.fromProtobuf(com.hedera.hapi.node.hooks.legacy.HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK));
        
        // Test protobuf conversion
        assertEquals(com.hedera.hapi.node.hooks.legacy.HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                    HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK.getProtoValue());
    }

    @Test
    public void testHookEntityId() {
        // Test account entity
        var accountId = new AccountId(1, 2, 3);
        var accountEntity = HookEntityId.ofAccount(accountId);
        
        assertTrue(accountEntity.isAccount());
        assertFalse(accountEntity.isContract());
        assertEquals(accountId, accountEntity.getAccountId());
        assertNull(accountEntity.getContractId());
        
        // Test contract entity
        var contractId = new ContractId(1, 2, 4);
        var contractEntity = HookEntityId.ofContract(contractId);
        
        assertFalse(contractEntity.isAccount());
        assertTrue(contractEntity.isContract());
        assertNull(contractEntity.getAccountId());
        assertEquals(contractId, contractEntity.getContractId());
        
        // Test protobuf round trip
        assertEquals(accountEntity, HookEntityId.fromProtobuf(accountEntity.toProtobuf()));
        assertEquals(contractEntity, HookEntityId.fromProtobuf(contractEntity.toProtobuf()));
    }

    @Test
    public void testHookId() {
        var accountId = new AccountId(1, 2, 3);
        var entityId = HookEntityId.ofAccount(accountId);
        var hookId = new HookId(entityId, 123L);
        
        assertEquals(entityId, hookId.getEntityId());
        assertEquals(123L, hookId.getHookId());
        
        // Test protobuf round trip
        assertEquals(hookId, HookId.fromProtobuf(hookId.toProtobuf()));
    }

    @Test
    public void testEvmHookSpec() {
        var contractId = new ContractId(1, 2, 4);
        var spec = new EvmHookSpec(contractId);
        
        assertEquals(contractId, spec.getContractId());
        
        // Test protobuf round trip
        assertEquals(spec, EvmHookSpec.fromProtobuf(spec.toProtobuf()));
    }

    @Test
    public void testLambdaStorageSlot() {
        var key = new byte[]{1, 2, 3};
        var value = new byte[]{4, 5, 6};
        var slot = new LambdaStorageUpdate.LambdaStorageSlot(key, value);
        
        assertArrayEquals(key, slot.getKey());
        assertArrayEquals(value, slot.getValue());
        
        // Test protobuf round trip
        var roundTrip = (LambdaStorageUpdate.LambdaStorageSlot) LambdaStorageUpdate.fromProtobuf(slot.toProtobuf());
        assertArrayEquals(key, roundTrip.getKey());
        assertArrayEquals(value, roundTrip.getValue());
    }

    @Test
    public void testLambdaMappingEntry() {
        var key = new byte[]{1, 2, 3};
        var value = new byte[]{4, 5, 6};
        var entry = new LambdaMappingEntry(key, value);
        
        assertTrue(entry.hasExplicitKey());
        assertFalse(entry.hasPreimageKey());
        assertArrayEquals(key, entry.getKey());
        assertArrayEquals(value, entry.getValue());
        
        // Test preimage entry
        var preimage = new byte[]{7, 8, 9};
        var preimageEntry = LambdaMappingEntry.withPreimage(preimage, value);
        
        assertFalse(preimageEntry.hasExplicitKey());
        assertTrue(preimageEntry.hasPreimageKey());
        assertArrayEquals(preimage, preimageEntry.getPreimage());
        assertArrayEquals(value, preimageEntry.getValue());
        
        // Test protobuf round trip
        assertEquals(entry, LambdaMappingEntry.fromProtobuf(entry.toProtobuf()));
        assertEquals(preimageEntry, LambdaMappingEntry.fromProtobuf(preimageEntry.toProtobuf()));
    }

    @Test
    public void testLambdaEvmHook() {
        var contractId = new ContractId(1, 2, 4);
        var spec = new EvmHookSpec(contractId);
        var hook = new LambdaEvmHook(spec);
        
        assertEquals(spec, hook.getSpec());
        assertTrue(hook.getStorageUpdates().isEmpty());
        
        // Test with storage updates
        var key = new byte[]{1, 2, 3};
        var value = new byte[]{4, 5, 6};
        var slot = new LambdaStorageUpdate.LambdaStorageSlot(key, value);
        var hookWithUpdates = new LambdaEvmHook(spec, Collections.singletonList(slot));
        
        assertEquals(1, hookWithUpdates.getStorageUpdates().size());
        assertEquals(slot, hookWithUpdates.getStorageUpdates().get(0));
        
        // Test protobuf round trip
        assertEquals(hook, LambdaEvmHook.fromProtobuf(hook.toProtobuf()));
        assertEquals(hookWithUpdates, LambdaEvmHook.fromProtobuf(hookWithUpdates.toProtobuf()));
    }

    @Test
    public void testHookCreationDetails() {
        var contractId = new ContractId(1, 2, 4);
        var spec = new EvmHookSpec(contractId);
        var hook = new LambdaEvmHook(spec);
        var adminKey = PrivateKey.generate().getPublicKey();
        
        var details = new HookCreationDetails(
                HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                123L,
                hook,
                adminKey
        );
        
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, details.getExtensionPoint());
        assertEquals(123L, details.getHookId());
        assertEquals(hook, details.getHook());
        assertEquals(adminKey, details.getAdminKey());
        assertTrue(details.hasAdminKey());
        
        // Test without admin key
        var detailsNoAdmin = new HookCreationDetails(
                HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                456L,
                hook
        );
        
        assertNull(detailsNoAdmin.getAdminKey());
        assertFalse(detailsNoAdmin.hasAdminKey());
        
        // Test protobuf round trip
        assertEquals(details, HookCreationDetails.fromProtobuf(details.toProtobuf()));
        assertEquals(detailsNoAdmin, HookCreationDetails.fromProtobuf(detailsNoAdmin.toProtobuf()));
    }

    @Test
    public void testValidation() {
        // Test invalid storage slot key (too long)
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaStorageUpdate.LambdaStorageSlot(new byte[33], new byte[]{1, 2, 3});
        });
        
        // Test invalid storage slot key (leading zeros)
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaStorageUpdate.LambdaStorageSlot(new byte[]{0, 1, 2}, new byte[]{1, 2, 3});
        });
        
        // Test invalid mapping entry key (too long)
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaMappingEntry(new byte[33], new byte[]{1, 2, 3});
        });
        
        // Test invalid mapping entry key (leading zeros)
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaMappingEntry(new byte[]{0, 1, 2}, new byte[]{1, 2, 3});
        });
    }
}
