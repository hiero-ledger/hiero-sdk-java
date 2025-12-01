// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LambdaMappingEntryTest {

    @Test
    void ofKeyBuildsEntryAndCopiesArrays() {
        byte[] key = new byte[] {0x01, 0x02};
        byte[] value = new byte[] {0x03, 0x04};

        var entry = LambdaMappingEntry.ofKey(key, value);

        assertTrue(entry.hasExplicitKey());
        assertFalse(entry.hasPreimageKey());
        assertArrayEquals(key, entry.getKey());
        assertNull(entry.getPreimage());
        assertArrayEquals(value, entry.getValue());

        // Ensure defensive copies
        key[0] = 0x7F;
        value[0] = 0x7F;
        assertArrayEquals(new byte[] {0x01, 0x02}, entry.getKey());
        assertArrayEquals(new byte[] {0x03, 0x04}, entry.getValue());
    }

    @Test
    void withPreimageBuildsEntryAndCopiesArrays() {
        byte[] preimage = new byte[] {0x11, 0x22};
        byte[] value = new byte[] {0x33, 0x44};

        var entry = LambdaMappingEntry.withPreimage(preimage, value);

        assertFalse(entry.hasExplicitKey());
        assertTrue(entry.hasPreimageKey());
        assertNull(entry.getKey());
        assertArrayEquals(preimage, entry.getPreimage());
        assertArrayEquals(value, entry.getValue());

        // Ensure defensive copies
        preimage[0] = 0x7F;
        value[0] = 0x7F;
        assertArrayEquals(new byte[] {0x11, 0x22}, entry.getPreimage());
        assertArrayEquals(new byte[] {0x33, 0x44}, entry.getValue());
    }

    @Test
    void buildersRejectNullInputs() {
        assertThrows(NullPointerException.class, () -> LambdaMappingEntry.ofKey(null, new byte[] {0x01}));
        assertThrows(NullPointerException.class, () -> LambdaMappingEntry.withPreimage(null, new byte[] {0x01}));
        assertThrows(NullPointerException.class, () -> LambdaMappingEntry.ofKey(new byte[] {0x01}, null));
        assertThrows(NullPointerException.class, () -> LambdaMappingEntry.withPreimage(new byte[] {0x01}, null));
    }

    @Test
    void protobufRoundTripForKeyAndPreimage() {
        var keyEntry = LambdaMappingEntry.ofKey(new byte[] {0x01}, new byte[] {0x02});
        var keyRoundTrip = LambdaMappingEntry.fromProtobuf(keyEntry.toProtobuf());
        assertEquals(keyEntry, keyRoundTrip);

        var preimageEntry = LambdaMappingEntry.withPreimage(new byte[] {0x0A}, new byte[] {0x0B});
        var preimageRoundTrip = LambdaMappingEntry.fromProtobuf(preimageEntry.toProtobuf());
        assertEquals(preimageEntry, preimageRoundTrip);
    }

    @Test
    void fromProtobufWithoutKeyThrows() {
        var emptyProto =
                com.hedera.hashgraph.sdk.proto.LambdaMappingEntry.newBuilder().build();
        assertThrows(IllegalArgumentException.class, () -> LambdaMappingEntry.fromProtobuf(emptyProto));
    }

    @Test
    void equalsHashCodeAndToString() {
        var a = LambdaMappingEntry.ofKey(new byte[] {0x01}, new byte[] {0x02});
        var b = LambdaMappingEntry.ofKey(new byte[] {0x01}, new byte[] {0x02});
        var c = LambdaMappingEntry.ofKey(new byte[] {0x03}, new byte[] {0x04});

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);

        var s = a.toString();
        assertTrue(s.contains("key") || s.contains("preimage"));
        assertTrue(s.contains("value"));
    }
}
