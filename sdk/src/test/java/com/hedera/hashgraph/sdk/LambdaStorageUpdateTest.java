// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LambdaStorageUpdateTest {

    @Test
    void lambdaStorageSlotConstructsAndDefensiveCopies() {
        byte[] key = new byte[] {0x01, 0x02};
        byte[] value = new byte[] {0x03, 0x04};

        var slot = new LambdaStorageUpdate.LambdaStorageSlot(key, value);

        assertArrayEquals(key, slot.getKey());
        assertArrayEquals(value, slot.getValue());

        // Defensive copies on construction/getters
        key[0] = 0x7F;
        value[0] = 0x7F;
        assertArrayEquals(new byte[] {0x01, 0x02}, slot.getKey());
        assertArrayEquals(new byte[] {0x03, 0x04}, slot.getValue());
    }

    @Test
    void lambdaStorageSlotProtobufRoundTrip() {
        var original = new LambdaStorageUpdate.LambdaStorageSlot(new byte[] {0x0A}, new byte[] {0x0B});
        var proto = original.toProtobuf();
        var restored = LambdaStorageUpdate.fromProtobuf(proto);
        assertEquals(original, restored);
        assertTrue(restored instanceof LambdaStorageUpdate.LambdaStorageSlot);
        var restoredSlot = (LambdaStorageUpdate.LambdaStorageSlot) restored;
        assertArrayEquals(new byte[] {0x0A}, restoredSlot.getKey());
        assertArrayEquals(new byte[] {0x0B}, restoredSlot.getValue());
        assertTrue(original.toString().contains("key"));
    }

    @Test
    void lambdaMappingEntriesConstructsValidatesAndCopies() {
        byte[] mappingSlot = new byte[] {0x05};
        var entry = LambdaMappingEntry.ofKey(new byte[] {0x10}, new byte[] {0x20});
        var updates = new LambdaStorageUpdate.LambdaMappingEntries(mappingSlot, List.of(entry));

        assertArrayEquals(mappingSlot, updates.getMappingSlot());
        assertEquals(1, updates.getEntries().size());
        assertEquals(entry, updates.getEntries().get(0));

        // Defensive copy of mappingSlot
        mappingSlot[0] = 0x7F;
        assertArrayEquals(new byte[] {0x05}, updates.getMappingSlot());

        // Returned entries list is a copy
        var list1 = updates.getEntries();
        var list2 = updates.getEntries();
        assertNotSame(list1, list2);
        assertEquals(list1, list2);
    }

    @Test
    void lambdaMappingEntriesValidation() {
        // mappingSlot cannot be null
        assertThrows(NullPointerException.class, () -> new LambdaStorageUpdate.LambdaMappingEntries(null, List.of()));
        // entries cannot be null
        assertThrows(
                NullPointerException.class,
                () -> new LambdaStorageUpdate.LambdaMappingEntries(new byte[] {0x01}, null));
        // current behavior: length > 32 is allowed
        assertDoesNotThrow(() -> new LambdaStorageUpdate.LambdaMappingEntries(new byte[33], List.of()));
        // current behavior: leading zeros are allowed
        assertDoesNotThrow(() -> new LambdaStorageUpdate.LambdaMappingEntries(new byte[] {0x00, 0x01}, List.of()));
    }

    @Test
    void lambdaMappingEntriesProtobufRoundTrip() {
        var entry1 = LambdaMappingEntry.ofKey(new byte[] {0x11}, new byte[] {0x22});
        var entry2 = LambdaMappingEntry.withPreimage(new byte[] {0x33}, new byte[] {0x44});
        var original = new LambdaStorageUpdate.LambdaMappingEntries(new byte[] {0x09}, List.of(entry1, entry2));
        var proto = original.toProtobuf();
        var restored = LambdaStorageUpdate.fromProtobuf(proto);
        assertEquals(original, restored);
        assertTrue(restored instanceof LambdaStorageUpdate.LambdaMappingEntries);
        var restoredME = (LambdaStorageUpdate.LambdaMappingEntries) restored;
        assertArrayEquals(new byte[] {0x09}, restoredME.getMappingSlot());
        assertEquals(List.of(entry1, entry2), restoredME.getEntries());
        assertTrue(original.toString().contains("mappingSlot"));
    }

    @Test
    void fromProtobufWithoutUpdateThrows() {
        var emptyProto =
                com.hedera.hashgraph.sdk.proto.LambdaStorageUpdate.newBuilder().build();
        assertThrows(IllegalArgumentException.class, () -> LambdaStorageUpdate.fromProtobuf(emptyProto));
    }
}
