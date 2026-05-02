// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk;

import static org.hiero.tck.util.KeyUtils.KeyType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Optional;
import org.hiero.tck.exception.InvalidJSONRPC2RequestException;
import org.hiero.tck.methods.sdk.param.*;
import org.hiero.tck.methods.sdk.response.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class KeyServiceTest {

    private final KeyService keyService = new KeyService();

    @Test
    void testGenerateKeyWithInvalidFromKey() {
        GenerateKeyParams params =
                new GenerateKeyParams(ED25519_PRIVATE_KEY, Optional.of("someKey"), Optional.empty(), Optional.empty());

        Executable executable = () -> keyService.generateKey(params);

        Assertions.assertThrows(
                InvalidJSONRPC2RequestException.class,
                executable,
                "invalid parameters: fromKey should only be provided for ed25519PublicKey, ecdsaSecp256k1PublicKey, or evmAddress types.");
    }

    @Test
    void testGenerateKeyWithInvalidThreshold() {
        GenerateKeyParams params =
                new GenerateKeyParams(ED25519_PUBLIC_KEY, Optional.empty(), Optional.of(1L), Optional.empty());

        Executable executable = () -> keyService.generateKey(params);

        assertThrows(
                InvalidJSONRPC2RequestException.class,
                executable,
                "invalid parameters: threshold should only be provided for thresholdKey types.");
    }

    @Test
    void testGenerateKeyWithInvalidKeys() {
        GenerateKeyParams params = new GenerateKeyParams(
                ED25519_PUBLIC_KEY, Optional.empty(), Optional.empty(), Optional.of(Collections.emptyList()));

        Executable executable = () -> keyService.generateKey(params);

        assertThrows(
                InvalidJSONRPC2RequestException.class,
                executable,
                "invalid parameters: keys should only be provided for keyList or thresholdKey types.");
    }

    @Test
    void testGenerateKeyWithMissingKeysForKeyList() {
        GenerateKeyParams params =
                new GenerateKeyParams(LIST_KEY, Optional.empty(), Optional.empty(), Optional.empty());

        Executable executable = () -> keyService.generateKey(params);

        assertThrows(
                InvalidJSONRPC2RequestException.class,
                executable,
                "invalid request: keys list is required for generating a KeyList type.");
    }

    @Test
    void testGenerateKeyWithMissingThresholdForThresholdKey() {
        GenerateKeyParams params = new GenerateKeyParams(
                THRESHOLD_KEY, Optional.empty(), Optional.empty(), Optional.of(Collections.emptyList()));

        Executable executable = () -> keyService.generateKey(params);

        assertThrows(
                InvalidJSONRPC2RequestException.class,
                executable,
                "invalid request: threshold is required for generating a ThresholdKey type.");
    }

    @Test
    void testGenerateKeyWithValidEd25519PrivateKey() throws Exception {
        GenerateKeyParams params =
                new GenerateKeyParams(ED25519_PRIVATE_KEY, Optional.empty(), Optional.empty(), Optional.empty());

        GenerateKeyResponse response = keyService.generateKey(params);

        assertNotNull(response.getKey());
        assertTrue(response.getKey().contains("302e020100300506032b657004220420"));
    }

    @Test
    void testGenerateKeyWithValidEd25519PublicKey() throws Exception {
        GenerateKeyParams params =
                new GenerateKeyParams(ED25519_PUBLIC_KEY, Optional.empty(), Optional.empty(), Optional.empty());

        GenerateKeyResponse response = keyService.generateKey(params);

        assertNotNull(response.getKey());
        assertTrue(response.getKey().contains("302a300506032b6570032100"));
    }

    @Test
    void testGenerateKeyWithValidThresholdKey() throws Exception {
        GenerateKeyParams params = new GenerateKeyParams(
                THRESHOLD_KEY,
                Optional.empty(),
                Optional.of(2L),
                Optional.of(Collections.singletonList(new GenerateKeyParams(
                        ED25519_PUBLIC_KEY, Optional.empty(), Optional.empty(), Optional.empty()))));

        GenerateKeyResponse response = keyService.generateKey(params);

        assertNotNull(response.getKey());
        assertFalse(response.getPrivateKeys().isEmpty());
    }

    @Test
    void testGenerateKeyWithValidListKey() throws Exception {
        GenerateKeyParams params = new GenerateKeyParams(
                LIST_KEY,
                Optional.empty(),
                Optional.empty(),
                Optional.of(Collections.singletonList(new GenerateKeyParams(
                        ED25519_PUBLIC_KEY, Optional.empty(), Optional.empty(), Optional.empty()))));

        GenerateKeyResponse response = keyService.generateKey(params);

        assertNotNull(response.getKey());
        assertFalse(response.getPrivateKeys().isEmpty());
    }

    @Test
    void testGenerateKeyWithValidEvmAddressKey() throws Exception {
        GenerateKeyParams params = new GenerateKeyParams(
                EVM_ADDRESS_KEY,
                Optional.of(
                        "3054020101042056b071002a75ab207a44bb2c18320286062bc26969fcb98240301e4afbe9ee2ea00706052b8104000aa124032200038ef0b62d60b1415f8cfb460303c498fbf09cb2ef2d2ff19fad33982228ef86fd"),
                Optional.empty(),
                Optional.empty());

        GenerateKeyResponse response = keyService.generateKey(params);

        assertNotNull(response.getKey());
    }
}
