// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LambdaEvmHookTest {

    @Test
    void constructorRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new LambdaEvmHook((ContractId) null));
        assertThrows(NullPointerException.class, () -> new LambdaEvmHook(new ContractId(0, 0, 1), null));
    }

    @Test
    void gettersReturnExpectedAndStorageUpdatesAreImmutable() {
        var contractId = new ContractId(0, 0, 123);
        var slot = new LambdaStorageUpdate.LambdaStorageSlot(new byte[] {0x01}, new byte[] {0x02});
        var hook = new LambdaEvmHook(contractId, List.of(slot));

        assertEquals(contractId, hook.getContractId());
        var updates = hook.getStorageUpdates();
        assertEquals(1, updates.size());
        assertEquals(slot, updates.get(0));

        // list must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> updates.add(slot));
    }

    @Test
    void protobufRoundTripPreservesData() {
        var spec = new ContractId(0, 0, 77);
        var entry = LambdaMappingEntry.ofKey(new byte[] {0x0A}, new byte[] {0x0B});
        var mappings = new LambdaStorageUpdate.LambdaMappingEntries(new byte[] {0x05}, List.of(entry));
        var slot = new LambdaStorageUpdate.LambdaStorageSlot(new byte[] {0x01}, new byte[] {0x02});
        var original = new LambdaEvmHook(spec, List.of(slot, mappings));

        var proto = original.toProtobuf();
        var restored = LambdaEvmHook.fromProtobuf(proto);

        assertEquals(original, restored);
        assertEquals(original.getContractId(), restored.getContractId());
        assertEquals(original.getStorageUpdates(), restored.getStorageUpdates());
    }

    @Test
    void equalsAndHashCodeDependOnSpecAndUpdates() {
        var spec1 = new ContractId(0, 0, 1);
        var spec2 = new ContractId(0, 0, 2);
        List<LambdaStorageUpdate> u1 =
                List.of(new LambdaStorageUpdate.LambdaStorageSlot(new byte[] {0x01}, new byte[] {0x02}));
        List<LambdaStorageUpdate> u2 =
                List.of(new LambdaStorageUpdate.LambdaStorageSlot(new byte[] {0x03}, new byte[] {0x04}));

        var a = new LambdaEvmHook(spec1, u1);
        var b = new LambdaEvmHook(spec1, new ArrayList<LambdaStorageUpdate>(u1));
        var c = new LambdaEvmHook(spec2, u1);
        var d = new LambdaEvmHook(spec1, u2);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, d);
    }

    @Test
    void toStringContainsSpecAndUpdates() {
        var spec = new ContractId(0, 0, 10);
        var hook = new LambdaEvmHook(spec);
        var s = hook.toString();
        assertTrue(s.contains("contractId"));
        assertTrue(s.contains("storageUpdates"));
    }
}
