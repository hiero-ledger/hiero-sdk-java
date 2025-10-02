// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for working with hook storage operations.
 * <p>
 * This class provides methods for common storage operations,
 * including Solidity mapping calculations, data encoding, and
 * storage slot management.
 */
public final class StorageUtils {

    private StorageUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Calculate the storage slot for a Solidity mapping entry.
     * <p>
     * The storage slot for a mapping entry is calculated as:
     * keccak256(abi.encodePacked(mappingSlot, key))
     *
     * @param mappingSlot the mapping slot (32 bytes)
     * @param key the mapping key
     * @return the calculated storage slot (32 bytes)
     */
    public static byte[] calculateMappingStorageSlot(byte[] mappingSlot, byte[] key) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        
        if (mappingSlot.length != 32) {
            throw new IllegalArgumentException("Mapping slot must be 32 bytes");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            
            // Concatenate mapping slot and key
            byte[] data = new byte[mappingSlot.length + key.length];
            System.arraycopy(mappingSlot, 0, data, 0, mappingSlot.length);
            System.arraycopy(key, 0, data, mappingSlot.length, key.length);
            
            // Calculate keccak256
            byte[] hash = digest.digest(data);
            
            // Ensure result is 32 bytes
            if (hash.length != 32) {
                throw new IllegalStateException("Keccak256 should produce 32 bytes");
            }
            
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA3-256 not available", e);
        }
    }

    /**
     * Calculate the storage slot for a Solidity mapping entry with preimage.
     * <p>
     * This is equivalent to calculateMappingStorageSlot but takes a preimage
     * that will be hashed to produce the key.
     *
     * @param mappingSlot the mapping slot (32 bytes)
     * @param preimage the preimage bytes
     * @return the calculated storage slot (32 bytes)
     */
    public static byte[] calculateMappingStorageSlotWithPreimage(byte[] mappingSlot, byte[] preimage) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(preimage, "preimage cannot be null");
        
        byte[] key = keccak256(preimage);
        return calculateMappingStorageSlot(mappingSlot, key);
    }

    /**
     * Calculate keccak256 hash of the input bytes.
     *
     * @param input the input bytes
     * @return the keccak256 hash (32 bytes)
     */
    public static byte[] keccak256(byte[] input) {
        Objects.requireNonNull(input, "input cannot be null");
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA3-256 not available", e);
        }
    }

    /**
     * Encode a string as bytes for storage.
     *
     * @param value the string value
     * @return the encoded bytes
     */
    public static byte[] encodeString(String value) {
        Objects.requireNonNull(value, "value cannot be null");
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encode a number as bytes for storage.
     *
     * @param value the number value
     * @return the encoded bytes (32 bytes, big-endian)
     */
    public static byte[] encodeNumber(long value) {
        return BigInteger.valueOf(value).toByteArray();
    }

    /**
     * Encode a number as bytes for storage with specific bit length.
     *
     * @param value the number value
     * @param bits the number of bits (must be multiple of 8)
     * @return the encoded bytes
     */
    public static byte[] encodeNumber(long value, int bits) {
        if (bits % 8 != 0) {
            throw new IllegalArgumentException("Bits must be multiple of 8");
        }
        
        int bytes = bits / 8;
        if (bytes > 32) {
            throw new IllegalArgumentException("Cannot encode more than 32 bytes");
        }
        
        byte[] result = new byte[bytes];
        for (int i = bytes - 1; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        
        return result;
    }

    /**
     * Encode an address as bytes for storage.
     * <p>
     * Addresses are typically 20 bytes and should be padded to 32 bytes.

     * @param address the address bytes (20 bytes)
     * @return the encoded bytes (32 bytes, left-padded with zeros)
     */
    public static byte[] encodeAddress(byte[] address) {
        Objects.requireNonNull(address, "address cannot be null");
        
        if (address.length != 20) {
            throw new IllegalArgumentException("Address must be 20 bytes");
        }
        
        return HookUtils.padToLength(address, 32);
    }

    /**
     * Create a storage slot update for a mapping entry.
     *
     * @param mappingSlot the mapping slot (32 bytes)
     * @param key the mapping key
     * @param value the mapping value
     * @return a LambdaStorageUpdate for the mapping entry
     */
    public static LambdaStorageUpdate createMappingStorageUpdate(byte[] mappingSlot, byte[] key, byte[] value) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        
        LambdaMappingEntry entry = LambdaMappingEntry.ofKey(key, value);
        return new LambdaStorageUpdate.LambdaMappingEntries(mappingSlot, List.of(entry));
    }

    /**
     * Create a storage slot update for a mapping entry with preimage.
     *
     * @param mappingSlot the mapping slot (32 bytes)
     * @param preimage the preimage bytes
     * @param value the mapping value
     * @return a LambdaStorageUpdate for the mapping entry
     */
    public static LambdaStorageUpdate createMappingStorageUpdateWithPreimage(byte[] mappingSlot, byte[] preimage, byte[] value) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(preimage, "preimage cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        
        LambdaMappingEntry entry = LambdaMappingEntry.withPreimage(preimage, value);
        return new LambdaStorageUpdate.LambdaMappingEntries(mappingSlot, List.of(entry));
    }

    /**
     * Create a storage slot update for a direct storage slot.
     *
     * @param slot the storage slot (32 bytes)
     * @param value the storage value
     * @return a LambdaStorageUpdate for the storage slot
     */
    public static LambdaStorageUpdate createDirectStorageUpdate(byte[] slot, byte[] value) {
        Objects.requireNonNull(slot, "slot cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        
        return new LambdaStorageUpdate.LambdaStorageSlot(slot, value);
    }

    /**
     * Create a storage slot update for a number value.
     *
     * @param slot the storage slot (32 bytes)
     * @param value the number value
     * @return a LambdaStorageUpdate for the storage slot
     */
    public static LambdaStorageUpdate createNumberStorageUpdate(byte[] slot, long value) {
        Objects.requireNonNull(slot, "slot cannot be null");
        
        byte[] encodedValue = encodeNumber(value);
        return createDirectStorageUpdate(slot, encodedValue);
    }

    /**
     * Create a storage slot update for a string value.
     *
     * @param slot the storage slot (32 bytes)
     * @param value the string value
     * @return a LambdaStorageUpdate for the storage slot
     */
    public static LambdaStorageUpdate createStringStorageUpdate(byte[] slot, String value) {
        Objects.requireNonNull(slot, "slot cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        
        byte[] encodedValue = encodeString(value);
        return createDirectStorageUpdate(slot, encodedValue);
    }

    /**
     * Create a storage slot update for an address value.
     *
     * @param slot the storage slot (32 bytes)
     * @param address the address (20 bytes)
     * @return a LambdaStorageUpdate for the storage slot
     */
    public static LambdaStorageUpdate createAddressStorageUpdate(byte[] slot, byte[] address) {
        Objects.requireNonNull(slot, "slot cannot be null");
        Objects.requireNonNull(address, "address cannot be null");
        
        byte[] encodedValue = encodeAddress(address);
        return createDirectStorageUpdate(slot, encodedValue);
    }

    /**
     * Create multiple storage updates for a mapping with different keys.
     *
     * @param mappingSlot the mapping slot (32 bytes)
     * @param keyValuePairs alternating key and value arrays
     * @return a LambdaStorageUpdate for the mapping entries
     */
    public static LambdaStorageUpdate createMappingStorageUpdates(byte[] mappingSlot, byte[]... keyValuePairs) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(keyValuePairs, "keyValuePairs cannot be null");
        
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Number of arrays must be even (key-value pairs)");
        }
        
        List<LambdaMappingEntry> entries = new ArrayList<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            byte[] key = keyValuePairs[i];
            byte[] value = keyValuePairs[i + 1];
            entries.add(LambdaMappingEntry.ofKey(key, value));
        }
        
        return new LambdaStorageUpdate.LambdaMappingEntries(mappingSlot, entries);
    }

    /**
     * Create a storage slot update that clears a storage slot.
     * <p>
     * This is done by setting the value to an empty byte array.

     * @param slot the storage slot to clear
     * @return a LambdaStorageUpdate that clears the slot
     */
    public static LambdaStorageUpdate createClearStorageUpdate(byte[] slot) {
        Objects.requireNonNull(slot, "slot cannot be null");
        
        return new LambdaStorageUpdate.LambdaStorageSlot(slot, new byte[0]);
    }

    /**
     * Create a storage slot update that clears a mapping entry.
     *
     * @param mappingSlot the mapping slot (32 bytes)
     * @param key the mapping key to clear
     * @return a LambdaStorageUpdate that clears the mapping entry
     */
    public static LambdaStorageUpdate createClearMappingUpdate(byte[] mappingSlot, byte[] key) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        
        LambdaMappingEntry entry = LambdaMappingEntry.ofKey(key, new byte[0]);
        return new LambdaStorageUpdate.LambdaMappingEntries(mappingSlot, List.of(entry));
    }

    /**
     * Create a storage slot update that clears a mapping entry with preimage.
     *
     * @param mappingSlot the mapping slot (32 bytes)
     * @param preimage the preimage bytes
     * @return a LambdaStorageUpdate that clears the mapping entry
     */
    public static LambdaStorageUpdate createClearMappingUpdateWithPreimage(byte[] mappingSlot, byte[] preimage) {
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(preimage, "preimage cannot be null");
        
        LambdaMappingEntry entry = LambdaMappingEntry.withPreimage(preimage, new byte[0]);
        return new LambdaStorageUpdate.LambdaMappingEntries(mappingSlot, List.of(entry));
    }
}
