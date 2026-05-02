// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hiero.sdk.Key;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.PublicKey;

public final class KeyUtils {

    public enum KeyType {
        ED25519_PRIVATE_KEY("ed25519PrivateKey"),
        ED25519_PUBLIC_KEY("ed25519PublicKey"),
        ECDSA_SECP256K1_PRIVATE_KEY("ecdsaSecp256k1PrivateKey"),
        ECDSA_SECP256K1_PUBLIC_KEY("ecdsaSecp256k1PublicKey"),
        LIST_KEY("keyList"),
        THRESHOLD_KEY("thresholdKey"),
        EVM_ADDRESS_KEY("evmAddress");

        private final String keyString;

        KeyType(String keyString) {
            this.keyString = keyString;
        }

        public static KeyType fromString(String keyString) {
            for (KeyType type : KeyType.values()) {
                if (type.keyString.equals(keyString)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown key type: " + keyString);
        }
    }

    public static Key getKeyFromString(String keyString) throws InvalidProtocolBufferException {
        try {
            return PublicKey.fromStringDER(keyString);
        } catch (Exception e) {
            try {
                return PrivateKey.fromStringDER(keyString);
            } catch (Exception ex) {
                return Key.fromBytes(ByteString.fromHex(keyString).toByteArray());
            }
        }
    }
}
