// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

public class LambdaSStoreTransactionTest {

    @Test
    public void testLambdaSStoreTransactionBasic() {
        // Create a test hook ID
        AccountId accountId = new AccountId(100);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Create storage updates
        byte[] key1 = {0x01, 0x02};
        byte[] value1 = {0x03, 0x04};
        byte[] key2 = {0x05, 0x06};
        byte[] value2 = {0x07, 0x08};
        
        LambdaStorageUpdate storageSlot1 = new LambdaStorageUpdate.LambdaStorageSlot(key1, value1);
        LambdaStorageUpdate storageSlot2 = new LambdaStorageUpdate.LambdaStorageSlot(key2, value2);
        
        // Create transaction
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageUpdate(storageSlot1)
                .addStorageUpdate(storageSlot2);
        
        // Verify transaction properties
        assertEquals(hookId, transaction.getHookId());
        List<LambdaStorageUpdate> storageUpdates = transaction.getStorageUpdates();
        assertEquals(2, storageUpdates.size());
        assertEquals(storageSlot1, storageUpdates.get(0));
        assertEquals(storageSlot2, storageUpdates.get(1));
    }

    @Test
    public void testLambdaSStoreTransactionConvenienceMethods() {
        AccountId accountId = new AccountId(200);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 2L);
        
        // Test addStorageSlot convenience method
        byte[] key = {0x10, 0x11};
        byte[] value = {0x12, 0x13};
        
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageSlot(key, value);
        
        List<LambdaStorageUpdate> storageUpdates = transaction.getStorageUpdates();
        assertEquals(1, storageUpdates.size());
        assertTrue(storageUpdates.get(0) instanceof LambdaStorageUpdate.LambdaStorageSlot);
        
        LambdaStorageUpdate.LambdaStorageSlot slot = (LambdaStorageUpdate.LambdaStorageSlot) storageUpdates.get(0);
        assertArrayEquals(key, slot.getKey());
        assertArrayEquals(value, slot.getValue());
    }

    @Test
    public void testLambdaSStoreTransactionMappingEntries() {
        AccountId accountId = new AccountId(300);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 3L);
        
        // Create mapping entries
        byte[] mappingSlot = {0x20, 0x21};
        LambdaMappingEntry entry1 = LambdaMappingEntry.ofKey(new byte[]{0x01}, new byte[]{0x02});
        LambdaMappingEntry entry2 = LambdaMappingEntry.withPreimage(new byte[]{0x03, 0x04}, new byte[]{0x05});
        List<LambdaMappingEntry> entries = List.of(entry1, entry2);
        
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addMappingEntries(mappingSlot, entries);
        
        List<LambdaStorageUpdate> storageUpdates = transaction.getStorageUpdates();
        assertEquals(1, storageUpdates.size());
        assertTrue(storageUpdates.get(0) instanceof LambdaStorageUpdate.LambdaMappingEntries);
        
        LambdaStorageUpdate.LambdaMappingEntries mappingEntries = (LambdaStorageUpdate.LambdaMappingEntries) storageUpdates.get(0);
        assertArrayEquals(mappingSlot, mappingEntries.getMappingSlot());
        assertEquals(entries, mappingEntries.getEntries());
    }

    @Test
    public void testLambdaSStoreTransactionSetStorageUpdates() {
        AccountId accountId = new AccountId(400);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 4L);
        
        // Create multiple storage updates
        LambdaStorageUpdate slot1 = new LambdaStorageUpdate.LambdaStorageSlot(new byte[]{0x01}, new byte[]{0x02});
        LambdaStorageUpdate slot2 = new LambdaStorageUpdate.LambdaStorageSlot(new byte[]{0x03}, new byte[]{0x04});
        List<LambdaStorageUpdate> updates = List.of(slot1, slot2);
        
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .setStorageUpdates(updates);
        
        List<LambdaStorageUpdate> retrievedUpdates = transaction.getStorageUpdates();
        assertEquals(updates, retrievedUpdates);
    }

    @Test
    public void testLambdaSStoreTransactionValidation() {
        // Test missing hook ID
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .addStorageSlot(new byte[]{0x01}, new byte[]{0x02});
        
        assertThrows(IllegalArgumentException.class, () -> {
            transaction.build();
        });
        
        // Test empty storage updates
        AccountId accountId = new AccountId(500);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 5L);
        
        LambdaSStoreTransaction emptyTransaction = new LambdaSStoreTransaction()
                .setHookId(hookId);
        
        assertThrows(IllegalArgumentException.class, () -> {
            emptyTransaction.build();
        });
    }

    @Test
    public void testLambdaSStoreTransactionProtobufSerialization() {
        AccountId accountId = new AccountId(600);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 6L);
        
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageSlot(new byte[]{0x01}, new byte[]{0x02});
        
        // Build the protobuf
        var protoBody = transaction.build();
        
        // Verify protobuf content
        assertTrue(protoBody.hasHookId());
        assertEquals(1, protoBody.getStorageUpdatesCount());
        
        var protoHookId = protoBody.getHookId();
        assertEquals(accountId.toProtobuf(), protoHookId.getEntityId().getAccountId());
        assertEquals(6L, protoHookId.getHookId());
        
        var protoStorageUpdate = protoBody.getStorageUpdates(0);
        assertTrue(protoStorageUpdate.hasStorageSlot());
    }

    @Test
    public void testLambdaSStoreTransactionScheduling() {
        // Test that scheduling is not supported
        AccountId accountId = new AccountId(700);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 7L);
        
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageSlot(new byte[]{0x01}, new byte[]{0x02});
        
        // Test that scheduling throws an exception
        assertThrows(UnsupportedOperationException.class, () -> {
            transaction.onScheduled(com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody.newBuilder());
        });
    }

    @Test
    public void testLambdaSStoreTransactionMethodDescriptor() {
        // Test that the method descriptor throws the expected exception
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            transaction.getMethodDescriptor();
        });
    }
}
