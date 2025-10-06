// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EvmHookSpecTest {

    @Test
    void constructorRejectsNullContractId() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new EvmHookSpec(null));
        assertTrue(ex.getMessage().contains("contractId cannot be null"));
    }

    @Test
    void getContractIdReturnsProvidedValue() {
        var cid = new ContractId(0, 0, 1234);
        var spec = new EvmHookSpec(cid);
        assertEquals(cid, spec.getContractId());
    }

    @Test
    void protobufRoundTripPreservesValue() {
        var cid = new ContractId(0, 0, 999);
        var original = new EvmHookSpec(cid);

        var proto = original.toProtobuf();
        var restored = EvmHookSpec.fromProtobuf(proto);

        assertEquals(original, restored);
        assertEquals(cid, restored.getContractId());
    }

    @Test
    void equalsAndHashCodeDependOnContractId() {
        var a = new EvmHookSpec(new ContractId(0, 0, 1));
        var b = new EvmHookSpec(new ContractId(0, 0, 1));
        var c = new EvmHookSpec(new ContractId(0, 0, 2));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void toStringContainsContractId() {
        var cid = new ContractId(0, 0, 42);
        var spec = new EvmHookSpec(cid);
        var s = spec.toString();
        assertTrue(s.contains("contractId"));
        assertTrue(s.contains("0.0.42"));
    }
}


