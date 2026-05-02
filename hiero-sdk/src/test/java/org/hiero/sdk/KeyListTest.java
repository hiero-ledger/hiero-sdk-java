// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import java.util.List;
import org.hiero.sdk.proto.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeyListTest {

    private static final PublicKey mTestPublicKey1 = PrivateKey.fromStringED25519(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10")
            .getPublicKey();

    private static final PublicKey mTestPublicKey2 = PrivateKey.fromStringED25519(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e11")
            .getPublicKey();

    private static final PublicKey mTestPublicKey3 = PrivateKey.fromStringED25519(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e12")
            .getPublicKey();

    @Test
    @DisplayName("fromProtobuf")
    void fromProtobuf() {
        // Given
        var protoKey1 = Key.newBuilder()
                .setEd25519(ByteString.copyFrom(mTestPublicKey1.toBytes()))
                .build();
        var protoKey3 = Key.newBuilder()
                .setEd25519(ByteString.copyFrom(mTestPublicKey2.toBytes()))
                .build();
        var protoKey2 = Key.newBuilder()
                .setEd25519(ByteString.copyFrom(mTestPublicKey3.toBytes()))
                .build();
        var protoKeyList = org.hiero.sdk.proto.KeyList.newBuilder()
                .addAllKeys(List.of(protoKey1, protoKey2, protoKey3))
                .build();

        // When
        var keyList = KeyList.fromProtobuf(protoKeyList, 3);

        // Then
        assertTrue(keyList.contains(mTestPublicKey1));
        assertTrue(keyList.contains(mTestPublicKey2));
        assertTrue(keyList.contains(mTestPublicKey3));
    }

    @Test
    @DisplayName("ofKeys")
    void ofKeys() {
        // Given / When
        var keyList = KeyList.of(mTestPublicKey1, mTestPublicKey2, mTestPublicKey3);

        // Then
        assertTrue(keyList.contains(mTestPublicKey1));
        assertTrue(keyList.contains(mTestPublicKey2));
        assertTrue(keyList.contains(mTestPublicKey3));
    }

    @Test
    @DisplayName("toProtobufKey")
    void toProtobufKey() {
        // Given
        var keyList = KeyList.of(mTestPublicKey1, mTestPublicKey2, mTestPublicKey3);

        // When
        var protoKey = keyList.toProtobufKey();

        // Then
        assertThat(protoKey.getKeyList().getKeysCount()).isEqualTo(3);
        assertThat(protoKey.getKeyList().getKeys(0).getEd25519().toByteArray()).isEqualTo(mTestPublicKey1.toBytesRaw());
        assertThat(protoKey.getKeyList().getKeys(1).getEd25519().toByteArray()).isEqualTo(mTestPublicKey2.toBytesRaw());
        assertThat(protoKey.getKeyList().getKeys(2).getEd25519().toByteArray()).isEqualTo(mTestPublicKey3.toBytesRaw());
    }

    @Test
    @DisplayName("toProtobuf")
    void toProtobuf() {
        // Given
        var keyList = KeyList.of(mTestPublicKey1, mTestPublicKey2, mTestPublicKey3);

        // When
        var protoKeyList = keyList.toProtobuf();

        // Then
        assertThat(protoKeyList.getKeysCount()).isEqualTo(3);
        assertThat(protoKeyList.getKeys(0).getEd25519().toByteArray()).isEqualTo(mTestPublicKey1.toBytesRaw());
        assertThat(protoKeyList.getKeys(1).getEd25519().toByteArray()).isEqualTo(mTestPublicKey2.toBytesRaw());
        assertThat(protoKeyList.getKeys(2).getEd25519().toByteArray()).isEqualTo(mTestPublicKey3.toBytesRaw());
    }

    @Test
    @DisplayName("size")
    void size() {
        // Given / When
        var keyList = KeyList.of(mTestPublicKey1, mTestPublicKey2, mTestPublicKey3);
        var emptyKeyList = new KeyList();

        // Then
        assertThat(keyList).hasSize(3);
        assertThat(emptyKeyList).isEmpty();
    }

    @Test
    @DisplayName("contains")
    void contains() {
        // Given / When
        var keyList = KeyList.of(mTestPublicKey1, mTestPublicKey2, mTestPublicKey3);
        var emptyKeyList = new KeyList();

        // Then
        assertTrue(keyList.contains(mTestPublicKey1));
        assertTrue(keyList.contains(mTestPublicKey2));
        assertTrue(keyList.contains(mTestPublicKey3));

        assertFalse(emptyKeyList.contains(mTestPublicKey1));
        assertFalse(emptyKeyList.contains(mTestPublicKey2));
        assertFalse(emptyKeyList.contains(mTestPublicKey3));
    }

    @Test
    @DisplayName("add")
    void add() {
        // Given
        var keyList = KeyList.of(mTestPublicKey1, mTestPublicKey2);

        // When
        keyList.add(mTestPublicKey3);

        // Then
        assertThat(keyList).hasSize(3);
        assertTrue(keyList.contains(mTestPublicKey3));
    }

    @Test
    @DisplayName("remove")
    void remove() {
        // Given
        var keyList = KeyList.of(mTestPublicKey1, mTestPublicKey2, mTestPublicKey3);

        // When
        keyList.remove(mTestPublicKey1);

        // Then
        assertThat(keyList).hasSize(2);
        assertFalse(keyList.contains(mTestPublicKey1));
        assertTrue(keyList.contains(mTestPublicKey2));
        assertTrue(keyList.contains(mTestPublicKey3));
    }

    @Test
    @DisplayName("clear")
    void clear() {
        // Given
        var keyList = KeyList.of(mTestPublicKey1, mTestPublicKey2, mTestPublicKey3);

        // When
        keyList.clear();

        // Then
        assertTrue(keyList.isEmpty());
    }
}
