// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HookExtensionPointTest {

    @Test
    void roundTripAllEnumValues() {
        for (HookExtensionPoint value : HookExtensionPoint.values()) {
            var proto = value.getProtoValue();
            assertNotNull(proto);
            var restored = HookExtensionPoint.fromProtobuf(proto);
            assertEquals(value, restored, "Round-trip mismatch for " + value);
        }
    }

    @Test
    void protoValuesAreStable() {
        for (HookExtensionPoint value : HookExtensionPoint.values()) {
            var proto = value.getProtoValue();
            // basic sanity: ordinal-like mapping should not be negative
            assertTrue(proto.getNumber() >= 0);
            assertTrue(value.toString().length() > 0);
        }
    }
}
