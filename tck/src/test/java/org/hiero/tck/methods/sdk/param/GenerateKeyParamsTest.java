// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.hiero.tck.util.KeyUtils.KeyType;
import org.junit.jupiter.api.Test;

class GenerateKeyParamsTest {
    @Test
    void testParseWithAllFields() throws Exception {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("type", "ed25519PublicKey");
        jrpcParams.put("fromKey", "someFromKey");
        jrpcParams.put("threshold", 2L);

        JSONArray jsonArray = new JSONArray();
        Map<String, Object> nestedParamsMap = new HashMap<>();
        nestedParamsMap.put("type", "ecdsaSecp256k1PublicKey");
        jsonArray.add(new JSONObject(nestedParamsMap));
        jrpcParams.put("keys", jsonArray);

        GenerateKeyParams params = new GenerateKeyParams().parse(jrpcParams);

        assertEquals(KeyType.ED25519_PUBLIC_KEY, params.getType());
        assertEquals(Optional.of("someFromKey"), params.getFromKey());
        assertEquals(Optional.of(2L), params.getThreshold());
        assertTrue(params.getKeys().isPresent());
        assertEquals(1, params.getKeys().get().size());
        assertEquals(
                KeyType.ECDSA_SECP256K1_PUBLIC_KEY,
                params.getKeys().get().get(0).getType());
    }

    @Test
    void testParseWithOptionalFieldsAbsent() throws Exception {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("type", "ed25519PublicKey");

        GenerateKeyParams params = new GenerateKeyParams().parse(jrpcParams);

        assertEquals(KeyType.ED25519_PUBLIC_KEY, params.getType());
        assertEquals(Optional.empty(), params.getFromKey());
        assertEquals(Optional.empty(), params.getThreshold());
        assertEquals(Optional.empty(), params.getKeys());
    }

    @Test
    void testParseWithInvalidFieldTypes() {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("type", 123); // Invalid type

        assertThrows(ClassCastException.class, () -> {
            new GenerateKeyParams().parse(jrpcParams);
        });
    }

    @Test
    void testParseWithEmptyParams() throws Exception {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("type", "keyList");

        GenerateKeyParams params = new GenerateKeyParams().parse(jrpcParams);

        assertEquals(Optional.empty(), params.getFromKey());
        assertEquals(Optional.empty(), params.getThreshold());
        assertEquals(Optional.empty(), params.getKeys());
    }

    @Test
    void testParseWithNestedKeys() throws Exception {
        Map<String, Object> jrpcParams = new HashMap<>();
        jrpcParams.put("type", "keyList");

        JSONArray jsonArray = new JSONArray();
        Map<String, Object> nestedParamsMap1 = new HashMap<>();
        nestedParamsMap1.put("type", "ed25519PublicKey");
        Map<String, Object> nestedParamsMap2 = new HashMap<>();
        nestedParamsMap2.put("type", "ecdsaSecp256k1PublicKey");
        jsonArray.add(new JSONObject(nestedParamsMap1));
        jsonArray.add(new JSONObject(nestedParamsMap2));
        jrpcParams.put("keys", jsonArray);

        GenerateKeyParams params = new GenerateKeyParams().parse(jrpcParams);

        assertEquals(KeyType.LIST_KEY, params.getType());
        assertTrue(params.getKeys().isPresent());
        assertEquals(2, params.getKeys().get().size());
        assertEquals(KeyType.ED25519_PUBLIC_KEY, params.getKeys().get().get(0).getType());
        assertEquals(
                KeyType.ECDSA_SECP256K1_PUBLIC_KEY,
                params.getKeys().get().get(1).getType());
    }

    @Test
    void testKeyTypeFromString() {
        assertEquals(KeyType.ED25519_PRIVATE_KEY, KeyType.fromString("ed25519PrivateKey"));
        assertEquals(KeyType.ED25519_PUBLIC_KEY, KeyType.fromString("ed25519PublicKey"));
        assertEquals(KeyType.ECDSA_SECP256K1_PRIVATE_KEY, KeyType.fromString("ecdsaSecp256k1PrivateKey"));
        assertEquals(KeyType.ECDSA_SECP256K1_PUBLIC_KEY, KeyType.fromString("ecdsaSecp256k1PublicKey"));
        assertEquals(KeyType.LIST_KEY, KeyType.fromString("keyList"));
        assertEquals(KeyType.THRESHOLD_KEY, KeyType.fromString("thresholdKey"));
        assertEquals(KeyType.EVM_ADDRESS_KEY, KeyType.fromString("evmAddress"));
    }
}
