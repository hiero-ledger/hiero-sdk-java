// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.hiero.sdk.Key.fromBytes;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.hiero.sdk.proto.Key;
import org.hiero.sdk.proto.KeyList;
import org.hiero.sdk.proto.ThresholdKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeyTest {
    @Test
    @DisplayName("can sign and verify a message")
    void signatureVerified() {
        var message = "Hello, World".getBytes(UTF_8);
        var privateKey = PrivateKey.generateED25519();
        var publicKey = privateKey.getPublicKey();
        var signature = privateKey.sign(message);

        assertThat(signature.length).isEqualTo(64);
        assertThat(publicKey.verify(message, signature)).isTrue();
    }

    @Test
    @DisplayName("can sign and verify a message with ECDSA")
    void signatureVerifiedECDSA() {
        var message = "Hello, World".getBytes(UTF_8);
        var privateKey = PrivateKey.generateECDSA();
        var publicKey = privateKey.getPublicKey();
        var signature = privateKey.sign(message);
        assertThat(signature.length).isEqualTo(64);
        assertThat(publicKey.verify(message, signature)).isTrue();
        // muck with the signature a little and make sure it breaks
        signature[5] += 1;
        assertThat(publicKey.verify(message, signature)).isFalse();
    }

    @Test
    @DisplayName("Calculated recId is either 0 or 1 for ECDSA secp256k1 curve")
    void calculateRecoveryIdECDSA() {
        var message = "Hello, World".getBytes(UTF_8);
        var privateKey = PrivateKey.generateECDSA();
        var signature = privateKey.sign(message);
        // wrap in signature object
        final byte[] r = new byte[32];
        System.arraycopy(signature, 0, r, 0, 32);
        final byte[] s = new byte[32];
        System.arraycopy(signature, 32, s, 0, 32);
        var recId = ((PrivateKeyECDSA) privateKey).getRecoveryId(r, s, message);
        assertThat(recId).isBetween(0, 1);
    }

    @Test
    @DisplayName("Fail to calculate recId for ECDSA with illegal inputs")
    void failToCalculateRecoveryIdWithIllegalInputDataECDSA() {
        // create signature
        var message = "Hello, World".getBytes(UTF_8);
        var privateKey = PrivateKey.generateECDSA();
        var signature = privateKey.sign(message);
        final byte[] r = new byte[32];
        System.arraycopy(signature, 0, r, 0, 32);
        final byte[] s = new byte[32];
        System.arraycopy(signature, 32, s, 0, 32);
        // recover public key with recId > 1
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Crypto.recoverPublicKeyECDSAFromSignature(
                        2, BigInteger.ONE, BigInteger.ONE, Crypto.calcKeccak256(message)));
        // recover public key with negative 'r' or 's'
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Crypto.recoverPublicKeyECDSAFromSignature(
                        0, BigInteger.valueOf(-1), BigInteger.ONE, Crypto.calcKeccak256(message)));
        // calculate recId with wrong message
        var wrongMessage = "Hello".getBytes(UTF_8);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> ((PrivateKeyECDSA) privateKey).getRecoveryId(r, s, wrongMessage));
    }

    @Test
    @DisplayName("can convert from protobuf ED25519 key to PublicKey")
    void fromProtoKeyEd25519() {
        var keyBytes = Hex.decode("0011223344556677889900112233445566778899001122334455667788990011");
        var protoKey =
                Key.newBuilder().setEd25519(ByteString.copyFrom(keyBytes)).build();

        var cut = PublicKey.fromProtobufKey(protoKey);

        assertThat(cut.getClass()).isEqualTo(PublicKeyED25519.class);
        assertThat(cut.toBytes()).containsExactly(keyBytes);
    }

    @Test
    @DisplayName("can convert from protobuf ECDSA key to PublicKey")
    void fromProtoKeyECDSA() throws InvalidProtocolBufferException {
        var keyProtobufBytes = Hex.decode("3a21034e0441201f2bf9c7d9873c2a9dc3fd451f64b7c05e17e4d781d916e3a11dfd99");
        var protoKey = Key.parseFrom(keyProtobufBytes);

        var cut = PublicKey.fromProtobufKey(protoKey);

        assertThat(cut.getClass()).isEqualTo(PublicKeyECDSA.class);
        assertThat(((PublicKey) cut).toProtobufKey().toByteArray()).containsExactly(keyProtobufBytes);
    }

    @Test
    @DisplayName("can convert from protobuf key list to PublicKey")
    void fromProtoKeyKeyList() {
        // given
        var keyBytes = new byte[][] {
            Hex.decode("0011223344556677889900112233445566778899001122334455667788990011"),
            Hex.decode("aa11223344556677889900112233445566778899001122334455667788990011")
        };

        var protoKeyList = KeyList.newBuilder();

        for (byte[] kb : keyBytes) {
            protoKeyList.addKeys(Key.newBuilder().setEd25519(ByteString.copyFrom(kb)));
        }

        var protoKey = Key.newBuilder().setKeyList(protoKeyList).build();

        // when
        var cut = org.hiero.sdk.Key.fromProtobufKey(protoKey);

        // then
        assertThat(cut.getClass()).isEqualTo(org.hiero.sdk.KeyList.class);

        var keyList = (org.hiero.sdk.KeyList) cut;
        var actual = keyList.toProtobufKey().getKeyList();

        assertThat(actual.getKeysCount()).isEqualTo(2);
        assertThat(actual.getKeys(0).getEd25519().toByteArray()).containsExactly(keyBytes[0]);
        assertThat(actual.getKeys(1).getEd25519().toByteArray()).containsExactly(keyBytes[1]);
    }

    @Test
    @DisplayName("can convert from protobuf threshold key to PublicKey")
    void fromProtoKeyThresholdKey() {
        // given
        var keyBytes = new byte[][] {
            Hex.decode("0011223344556677889900112233445566778899001122334455667788990011"),
            Hex.decode("aa11223344556677889900112233445566778899001122334455667788990011")
        };

        var protoKeyList = KeyList.newBuilder();

        for (byte[] kb : keyBytes) {
            protoKeyList.addKeys(Key.newBuilder().setEd25519(ByteString.copyFrom(kb)));
        }

        var protoThresholdKey = ThresholdKey.newBuilder().setThreshold(1).setKeys(protoKeyList);
        var protoKey = Key.newBuilder().setThresholdKey(protoThresholdKey).build();

        // when
        var cut = org.hiero.sdk.Key.fromProtobufKey(protoKey);

        // then
        assertThat(cut.getClass()).isEqualTo(org.hiero.sdk.KeyList.class);

        var thresholdKey = (org.hiero.sdk.KeyList) cut;
        var actual = thresholdKey.toProtobufKey().getThresholdKey();

        assertThat(actual.getThreshold()).isEqualTo(1);
        assertThat(actual.getKeys().getKeysCount()).isEqualTo(2);
        assertThat(actual.getKeys().getKeys(0).getEd25519().toByteArray()).containsExactly(keyBytes[0]);
        assertThat(actual.getKeys().getKeys(1).getEd25519().toByteArray()).containsExactly(keyBytes[1]);
    }

    @Test
    @DisplayName("Throws given unsupported key")
    void throwsUnsupportedKey() {
        byte[] keyBytes = {0, 1, 2};
        var protoKey =
                Key.newBuilder().setRSA3072(ByteString.copyFrom(keyBytes)).build();
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> org.hiero.sdk.Key.fromProtobufKey(protoKey));
    }

    @Test
    @DisplayName("Key equals")
    void keyEquals() {
        var key1 = PrivateKey.fromString(
                "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

        var key2 = PrivateKey.fromString(
                "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

        assertThat(key2.toString()).isEqualTo(key1.toString());
        assertThat(key2.getPublicKey()).isEqualTo(key1.getPublicKey());
        assertThat(key1.getPublicKey()).isNotEqualTo("random string");
    }

    @Test
    @DisplayName("Key has hash")
    void keyHash() {
        var key = PrivateKey.fromString(
                "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

        assertThatNoException().isThrownBy(key::hashCode);
    }

    @Test
    @DisplayName("KeyList methods")
    void keyListMethods() {
        var key1 = PrivateKey.fromString(
                "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

        var key2 = PrivateKey.fromString(
                "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e11");

        var key3 = PrivateKey.fromString(
                "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e12");

        var keyList = org.hiero.sdk.KeyList.withThreshold(1);
        keyList.add(key1);
        keyList.addAll(List.of(key2, key3));

        assertThat(keyList.isEmpty()).isFalse();
        assertThat(keyList.size()).isEqualTo(3);
        assertThat(keyList).contains(key1);
        assertThat(keyList).contains(key2);
        assertThat(keyList).contains(key3);

        var arr = keyList.toArray();
        assertThat(arr[0]).isEqualTo(key1);
        assertThat(arr[1]).isEqualTo(key2);
        assertThat(arr[2]).isEqualTo(key3);

        arr = new org.hiero.sdk.Key[] {null, null, null};
        keyList.toArray(arr);
        assertThat(arr[0]).isEqualTo(key1);
        assertThat(arr[1]).isEqualTo(key2);
        assertThat(arr[2]).isEqualTo(key3);

        keyList.remove(key2);
        assertThat(keyList.size()).isEqualTo(2);

        keyList.clear();

        keyList.addAll(List.of(key1, key2, key3));
        assertThat(keyList.size()).isEqualTo(3);

        keyList.retainAll(List.of(key2, key3));

        assertThat(keyList.size()).isEqualTo(2);
        assertThat(keyList).containsAll(List.of(key2, key3));

        keyList.removeAll(List.of(key2, key3));
        assertThat(keyList).isEmpty();
    }

    @Test
    @DisplayName("can convert from bytes ED25519 key to Key")
    void fromBytesEd25519() throws InvalidProtocolBufferException {
        var keyBytes = Hex.decode("0011223344556677889900112233445566778899001122334455667788990011");
        var protoKey =
                Key.newBuilder().setEd25519(ByteString.copyFrom(keyBytes)).build();
        var bytes = protoKey.toByteArray();

        var cut = fromBytes(bytes);

        assertThat(cut.getClass()).isEqualTo(PublicKeyED25519.class);
        assertThat(cut.toBytes()).containsExactly(keyBytes);
    }

    @Test
    @DisplayName("can convert from bytes ECDSA key to Key")
    void fromBytesECDSA() throws InvalidProtocolBufferException {
        var keyBytes = Hex.decode("3a21034e0441201f2bf9c7d9873c2a9dc3fd451f64b7c05e17e4d781d916e3a11dfd99");

        var cut = fromBytes(keyBytes);

        assertThat(cut.getClass()).isEqualTo(PublicKeyECDSA.class);
        assertThat(cut.toProtobufKey().toByteArray()).containsExactly(keyBytes);
    }

    @Test
    @DisplayName("can convert from bytes key list to Key")
    void fromBytesKeyList() throws InvalidProtocolBufferException {
        var keyBytes = new byte[][] {
            Hex.decode("0011223344556677889900112233445566778899001122334455667788990011"),
            Hex.decode("aa11223344556677889900112233445566778899001122334455667788990011")
        };

        var protoKeyList = KeyList.newBuilder();

        for (byte[] kb : keyBytes) {
            protoKeyList.addKeys(Key.newBuilder().setEd25519(ByteString.copyFrom(kb)));
        }

        var protoKey = Key.newBuilder().setKeyList(protoKeyList).build();
        var bytes = protoKey.toByteArray();

        var cut = fromBytes(bytes);

        assertThat(cut.getClass()).isEqualTo(org.hiero.sdk.KeyList.class);

        var keyList = (org.hiero.sdk.KeyList) cut;
        var actual = keyList.toProtobufKey().getKeyList();

        assertThat(actual.getKeysCount()).isEqualTo(2);
        assertThat(actual.getKeys(0).getEd25519().toByteArray()).containsExactly(keyBytes[0]);
        assertThat(actual.getKeys(1).getEd25519().toByteArray()).containsExactly(keyBytes[1]);
    }

    @Test
    @DisplayName("can convert from bytes threshold key to Key")
    void fromBytesThresholdKey() throws InvalidProtocolBufferException {
        var keyBytes = new byte[][] {
            Hex.decode("0011223344556677889900112233445566778899001122334455667788990011"),
            Hex.decode("aa11223344556677889900112233445566778899001122334455667788990011")
        };

        var protoKeyList = KeyList.newBuilder();

        for (byte[] kb : keyBytes) {
            protoKeyList.addKeys(Key.newBuilder().setEd25519(ByteString.copyFrom(kb)));
        }

        var protoThresholdKey = ThresholdKey.newBuilder().setThreshold(1).setKeys(protoKeyList);
        var protoKey = Key.newBuilder().setThresholdKey(protoThresholdKey).build();
        var bytes = protoKey.toByteArray();

        var cut = fromBytes(bytes);

        assertThat(cut.getClass()).isEqualTo(org.hiero.sdk.KeyList.class);

        var thresholdKey = (org.hiero.sdk.KeyList) cut;
        var actual = thresholdKey.toProtobufKey().getThresholdKey();

        assertThat(actual.getThreshold()).isEqualTo(1);
        assertThat(actual.getKeys().getKeysCount()).isEqualTo(2);
        assertThat(actual.getKeys().getKeys(0).getEd25519().toByteArray()).containsExactly(keyBytes[0]);
        assertThat(actual.getKeys().getKeys(1).getEd25519().toByteArray()).containsExactly(keyBytes[1]);
    }

    @Test
    @DisplayName("Throws given unsupported key")
    void throwsUnsupportedKeyFromBytes() {
        byte[] keyBytes = {0, 1, 2};
        var protoKey =
                Key.newBuilder().setRSA3072(ByteString.copyFrom(keyBytes)).build();
        var bytes = protoKey.toByteArray();

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> fromBytes(bytes));
    }
}
