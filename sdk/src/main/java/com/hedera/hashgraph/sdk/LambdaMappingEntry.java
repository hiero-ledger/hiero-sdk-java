// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

// Using fully qualified names to avoid conflicts with generated classes
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an entry in a Solidity mapping.
 * <p>
 * This class is used to specify updates to Solidity mapping entries in a
 * lambda EVM hook's storage. It supports both explicit key bytes and
 * preimage-based keys for variable-length mapping keys.
 */
public class LambdaMappingEntry {
    private final byte[] key;
    private final byte[] preimage;
    private final byte[] value;

    /**
     * Create a new mapping entry with an explicit key.
     *
     * @param key the explicit mapping key (max 32 bytes, minimal representation)
     * @param value the mapping value (max 32 bytes, minimal representation)
     */
    public static LambdaMappingEntry ofKey(byte[] key, byte[] value) {
        Objects.requireNonNull(key, "key cannot be null");
        return new LambdaMappingEntry(key, null, value);
    }

    /**
     * Create a new mapping entry with a preimage key.
     *
     * @param preimage the preimage bytes for the mapping key
     * @param value the mapping value (max 32 bytes, minimal representation)
     */
    public static LambdaMappingEntry withPreimage(byte[] preimage, byte[] value) {
        Objects.requireNonNull(preimage, "preimage cannot be null");
        return new LambdaMappingEntry(null, preimage, value);
    }

    private LambdaMappingEntry(byte[] key, byte[] preimage, byte[] value) {
        Objects.requireNonNull(value, "value cannot be null");
        this.key = key != null ? key.clone() : null;
        this.preimage = preimage != null ? preimage.clone() : null;
        this.value = value.clone();
    }

    /**
     * Check if this entry uses an explicit key.
     *
     * @return true if using explicit key, false if using preimage
     */
    public boolean hasExplicitKey() {
        return key != null;
    }

    /**
     * Check if this entry uses a preimage key.
     *
     * @return true if using preimage, false if using explicit key
     */
    public boolean hasPreimageKey() {
        return preimage != null;
    }

    /**
     * Get the explicit key if this entry uses one.
     *
     * @return a copy of the key bytes, or null if using preimage
     */
    public byte[] getKey() {
        return key != null ? key.clone() : null;
    }

    /**
     * Get the preimage if this entry uses one.
     *
     * @return a copy of the preimage bytes, or null if using explicit key
     */
    public byte[] getPreimage() {
        return preimage != null ? preimage.clone() : null;
    }

    /**
     * Get the mapping value.
     *
     * @return a copy of the value bytes
     */
    public byte[] getValue() {
        return value.clone();
    }

    /**
     * Convert this mapping entry to a protobuf message.
     *
     * @return the protobuf LambdaMappingEntry
     */
    com.hedera.hashgraph.sdk.proto.LambdaMappingEntry toProtobuf() {
        var builder = com.hedera.hashgraph.sdk.proto.LambdaMappingEntry.newBuilder();

        if (key != null) {
            builder.setKey(ByteString.copyFrom(key));
        } else if (preimage != null) {
            builder.setPreimage(ByteString.copyFrom(preimage));
        }

        if (value.length > 0) {
            builder.setValue(ByteString.copyFrom(value));
        }

        return builder.build();
    }

    /**
     * Create a LambdaMappingEntry from a protobuf message.
     *
     * @param proto the protobuf LambdaMappingEntry
     * @return a new LambdaMappingEntry instance
     */
    public static LambdaMappingEntry fromProtobuf(com.hedera.hashgraph.sdk.proto.LambdaMappingEntry proto) {
        return switch (proto.getEntryKeyCase()) {
            case KEY ->
                LambdaMappingEntry.ofKey(
                        proto.getKey().toByteArray(), proto.getValue().toByteArray());
            case PREIMAGE ->
                LambdaMappingEntry.withPreimage(
                        proto.getPreimage().toByteArray(), proto.getValue().toByteArray());
            case ENTRYKEY_NOT_SET ->
                throw new IllegalArgumentException("LambdaMappingEntry must have either key or preimage set");
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaMappingEntry that = (LambdaMappingEntry) o;
        return Arrays.equals(key, that.key)
                && Arrays.equals(preimage, that.preimage)
                && Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(key), Arrays.hashCode(preimage), Arrays.hashCode(value));
    }

    @Override
    public String toString() {
        if (key != null) {
            return "LambdaMappingEntry{key=" + java.util.Arrays.toString(key) + ", value="
                    + java.util.Arrays.toString(value) + "}";
        } else {
            return "LambdaMappingEntry{preimage=" + java.util.Arrays.toString(preimage) + ", value="
                    + java.util.Arrays.toString(value) + "}";
        }
    }
}
