// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility class providing general helper methods for working with hooks.
 * <p>
 * This class contains static methods for common hook operations,
 * conversions, and helper functions that don't fit into other utility classes.
 */
public final class HookUtils {

    private HookUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Create a simple account allowance hook with minimal configuration.
     *
     * @param hookId the hook ID
     * @param contractId the contract that implements the hook
     * @return a HookCreationDetails for the hook
     */
    public static HookCreationDetails createSimpleAccountAllowanceHook(long hookId, ContractId contractId) {
        Objects.requireNonNull(contractId, "contractId cannot be null");
        
        EvmHookSpec evmHookSpec = new EvmHookSpec(contractId);
        LambdaEvmHook lambdaEvmHook = new LambdaEvmHook(evmHookSpec);
        
        return new HookCreationDetails(
                HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                hookId,
                lambdaEvmHook
        );
    }

    /**
     * Create an account allowance hook with admin key.
     *
     * @param hookId the hook ID
     * @param contractId the contract that implements the hook
     * @param adminKey the admin key for managing the hook
     * @return a HookCreationDetails for the hook
     */
    public static HookCreationDetails createAccountAllowanceHookWithAdmin(long hookId, ContractId contractId, Key adminKey) {
        Objects.requireNonNull(contractId, "contractId cannot be null");
        Objects.requireNonNull(adminKey, "adminKey cannot be null");
        
        EvmHookSpec evmHookSpec = new EvmHookSpec(contractId);
        LambdaEvmHook lambdaEvmHook = new LambdaEvmHook(evmHookSpec);
        
        return new HookCreationDetails(
                HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                hookId,
                lambdaEvmHook,
                adminKey
        );
    }

    /**
     * Create a hook with initial storage configuration.
     *
     * @param hookId the hook ID
     * @param contractId the contract that implements the hook
     * @param storageUpdates the initial storage updates
     * @return a HookCreationDetails for the hook
     */
    public static HookCreationDetails createHookWithStorage(long hookId, ContractId contractId, List<LambdaStorageUpdate> storageUpdates) {
        Objects.requireNonNull(contractId, "contractId cannot be null");
        Objects.requireNonNull(storageUpdates, "storageUpdates cannot be null");
        
        EvmHookSpec evmHookSpec = new EvmHookSpec(contractId);
        LambdaEvmHook lambdaEvmHook = new LambdaEvmHook(evmHookSpec, storageUpdates);
        
        return new HookCreationDetails(
                HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                hookId,
                lambdaEvmHook
        );
    }

    /**
     * Create a storage slot update from hex strings.
     *
     * @param keyHex the storage key as a hex string (without 0x prefix)
     * @param valueHex the storage value as a hex string (without 0x prefix)
     * @return a LambdaStorageUpdate for the storage slot
     * @throws IllegalArgumentException if the hex strings are invalid
     */
    public static LambdaStorageUpdate createStorageSlotFromHex(String keyHex, String valueHex) {
        Objects.requireNonNull(keyHex, "keyHex cannot be null");
        Objects.requireNonNull(valueHex, "valueHex cannot be null");
        
        byte[] key = hexStringToBytes(keyHex);
        byte[] value = hexStringToBytes(valueHex);
        
        return new LambdaStorageUpdate.LambdaStorageSlot(key, value);
    }

    /**
     * Create a mapping entry from hex strings.
     *
     * @param mappingSlotHex the mapping slot as a hex string (without 0x prefix)
     * @param keyHex the mapping key as a hex string (without 0x prefix)
     * @param valueHex the mapping value as a hex string (without 0x prefix)
     * @return a LambdaStorageUpdate for the mapping entries
     * @throws IllegalArgumentException if the hex strings are invalid
     */
    public static LambdaStorageUpdate createMappingEntryFromHex(String mappingSlotHex, String keyHex, String valueHex) {
        Objects.requireNonNull(mappingSlotHex, "mappingSlotHex cannot be null");
        Objects.requireNonNull(keyHex, "keyHex cannot be null");
        Objects.requireNonNull(valueHex, "valueHex cannot be null");
        
        byte[] mappingSlot = hexStringToBytes(mappingSlotHex);
        byte[] key = hexStringToBytes(keyHex);
        byte[] value = hexStringToBytes(valueHex);
        
        LambdaMappingEntry entry = LambdaMappingEntry.ofKey(key, value);
        return new LambdaStorageUpdate.LambdaMappingEntries(mappingSlot, List.of(entry));
    }

    /**
     * Convert a hex string to bytes.
     *
     * @param hexString the hex string (without 0x prefix)
     * @return the bytes
     * @throws IllegalArgumentException if the hex string is invalid
     */
    public static byte[] hexStringToBytes(String hexString) {
        Objects.requireNonNull(hexString, "hexString cannot be null");
        
        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        
        if (hexString.length() == 0) {
            return new byte[0];
        }
        
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            String hexByte = hexString.substring(i * 2, i * 2 + 2);
            try {
                bytes[i] = (byte) Integer.parseInt(hexByte, 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid hex string: " + hexString, e);
            }
        }
        
        return bytes;
    }

    /**
     * Convert bytes to a hex string.
     *
     * @param bytes the bytes to convert
     * @return the hex string (without 0x prefix)
     */
    public static String bytesToHexString(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes cannot be null");
        
        if (bytes.length == 0) {
            return "";
        }
        
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b & 0xFF));
        }
        return hex.toString();
    }

    /**
     * Create a minimal byte representation of a number.
     * <p>
     * Removes leading zeros to create the minimal representation
     * required by the Hedera network for storage slots and values.
     *
     * @param bytes the bytes to minimize
     * @return the minimal representation
     */
    public static byte[] toMinimalRepresentation(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes cannot be null");
        
        if (bytes.length == 0) {
            return new byte[0];
        }
        
        // Find the first non-zero byte
        int startIndex = 0;
        while (startIndex < bytes.length && bytes[startIndex] == 0) {
            startIndex++;
        }
        
        // If all bytes are zero, return a single zero byte
        if (startIndex == bytes.length) {
            return new byte[]{0};
        }
        
        // Copy from the first non-zero byte
        byte[] result = new byte[bytes.length - startIndex];
        System.arraycopy(bytes, startIndex, result, 0, result.length);
        return result;
    }

    /**
     * Pad bytes to a specific length with leading zeros.
     *
     * @param bytes the bytes to pad
     * @param targetLength the target length
     * @return the padded bytes
     * @throws IllegalArgumentException if bytes is longer than target length
     */
    public static byte[] padToLength(byte[] bytes, int targetLength) {
        Objects.requireNonNull(bytes, "bytes cannot be null");
        
        if (bytes.length > targetLength) {
            throw new IllegalArgumentException("Bytes length (" + bytes.length + ") exceeds target length (" + targetLength + ")");
        }
        
        if (bytes.length == targetLength) {
            return bytes.clone();
        }
        
        byte[] result = new byte[targetLength];
        System.arraycopy(bytes, 0, result, targetLength - bytes.length, bytes.length);
        return result;
    }

    /**
     * Create a list of storage updates from a list of key-value pairs.
     *
     * @param keyValuePairs the key-value pairs as alternating key, value arrays
     * @return a list of LambdaStorageUpdate objects
     * @throws IllegalArgumentException if the number of arrays is not even
     */
    public static List<LambdaStorageUpdate> createStorageUpdatesFromPairs(byte[]... keyValuePairs) {
        Objects.requireNonNull(keyValuePairs, "keyValuePairs cannot be null");
        
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Number of arrays must be even (key-value pairs)");
        }
        
        List<LambdaStorageUpdate> updates = new ArrayList<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            byte[] key = keyValuePairs[i];
            byte[] value = keyValuePairs[i + 1];
            updates.add(new LambdaStorageUpdate.LambdaStorageSlot(key, value));
        }
        
        return updates;
    }

    /**
     * Check if a hook ID is valid for a given entity.
     *
     * @param hookId the hook ID to check
     * @param entityId the entity ID to check against
     * @return true if the hook ID belongs to the entity
     */
    public static boolean isHookForEntity(HookId hookId, HookEntityId entityId) {
        Objects.requireNonNull(hookId, "hookId cannot be null");
        Objects.requireNonNull(entityId, "entityId cannot be null");
        
        return hookId.getEntityId().equals(entityId);
    }

    /**
     * Get all hook IDs from a list of hook creation details.
     *
     * @param hookDetails the list of hook creation details
     * @return a list of hook IDs
     */
    public static List<Long> extractHookIds(List<HookCreationDetails> hookDetails) {
        Objects.requireNonNull(hookDetails, "hookDetails cannot be null");
        
        List<Long> hookIds = new ArrayList<>();
        for (HookCreationDetails details : hookDetails) {
            hookIds.add(details.getHookId());
        }
        return hookIds;
    }

    /**
     * Filter hook creation details by extension point.
     *
     * @param hookDetails the list of hook creation details
     * @param extensionPoint the extension point to filter by
     * @return a list of hook creation details matching the extension point
     */
    public static List<HookCreationDetails> filterByExtensionPoint(List<HookCreationDetails> hookDetails, HookExtensionPoint extensionPoint) {
        Objects.requireNonNull(hookDetails, "hookDetails cannot be null");
        Objects.requireNonNull(extensionPoint, "extensionPoint cannot be null");
        
        List<HookCreationDetails> filtered = new ArrayList<>();
        for (HookCreationDetails details : hookDetails) {
            if (details.getExtensionPoint() == extensionPoint) {
                filtered.add(details);
            }
        }
        return filtered;
    }

    /**
     * Create an empty storage update list.
     *
     * @return an empty immutable list
     */
    public static List<LambdaStorageUpdate> emptyStorageUpdates() {
        return Collections.emptyList();
    }

    /**
     * Create a single storage update list.
     *
     * @param storageUpdate the storage update
     * @return a list containing the single storage update
     */
    public static List<LambdaStorageUpdate> singleStorageUpdate(LambdaStorageUpdate storageUpdate) {
        Objects.requireNonNull(storageUpdate, "storageUpdate cannot be null");
        return Collections.singletonList(storageUpdate);
    }
}
