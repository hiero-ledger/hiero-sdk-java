// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

// Using fully qualified names to avoid conflicts with generated classes
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Objects;

/**
 * Abstract base class for lambda storage updates.
 * <p>
 * Storage updates define how to modify the storage of a lambda EVM hook.
 * This can be done either by directly specifying storage slots or by
 * updating Solidity mapping entries.
 */
public abstract class LambdaStorageUpdate {

    /**
     * Convert this storage update to a protobuf message.
     *
     * @return the protobuf LambdaStorageUpdate
     */
    abstract com.hedera.hashgraph.sdk.proto.LambdaStorageUpdate toProtobuf();

    /**
     * Create a LambdaStorageUpdate from a protobuf message.
     *
     * @param proto the protobuf LambdaStorageUpdate
     * @return a new LambdaStorageUpdate instance
     */
    static LambdaStorageUpdate fromProtobuf(com.hedera.hashgraph.sdk.proto.LambdaStorageUpdate proto) {
        return switch (proto.getUpdateCase()) {
            case STORAGE_SLOT -> LambdaStorageSlot.fromProtobuf(proto.getStorageSlot());
            case MAPPING_ENTRIES -> LambdaMappingEntries.fromProtobuf(proto.getMappingEntries());
            case UPDATE_NOT_SET ->
                throw new IllegalArgumentException(
                        "LambdaStorageUpdate must have either storage_slot or mapping_entries set");
        };
    }

    /**
     * Represents a direct storage slot update.
     * <p>
     * This class allows direct manipulation of storage slots in the lambda's storage.
     */
    public static class LambdaStorageSlot extends LambdaStorageUpdate {
        private final byte[] key;
        private final byte[] value;

        /**
         * Create a new storage slot update.
         *
         * @param key the storage slot key (max 32 bytes, minimal representation)
         * @param value the storage slot value (max 32 bytes, minimal representation)
         */
        public LambdaStorageSlot(byte[] key, byte[] value) {
            this.key = Objects.requireNonNull(key, "key cannot be null").clone();
            this.value = value != null ? value.clone() : new byte[0];
        }

        /**
         * Get the storage slot key.
         *
         * @return a copy of the key bytes
         */
        public byte[] getKey() {
            return key.clone();
        }

        /**
         * Get the storage slot value.
         *
         * @return a copy of the value bytes
         */
        public byte[] getValue() {
            return value.clone();
        }

        @Override
        com.hedera.hashgraph.sdk.proto.LambdaStorageUpdate toProtobuf() {
            return com.hedera.hashgraph.sdk.proto.LambdaStorageUpdate.newBuilder()
                    .setStorageSlot(com.hedera.hashgraph.sdk.proto.LambdaStorageSlot.newBuilder()
                            .setKey(ByteString.copyFrom(key))
                            .setValue(ByteString.copyFrom(value))
                            .build())
                    .build();
        }

        public static LambdaStorageSlot fromProtobuf(com.hedera.hashgraph.sdk.proto.LambdaStorageSlot proto) {
            return new LambdaStorageSlot(
                    proto.getKey().toByteArray(), proto.getValue().toByteArray());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LambdaStorageSlot that = (LambdaStorageSlot) o;
            return Arrays.equals(key, that.key) && Arrays.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(key), Arrays.hashCode(value));
        }

        @Override
        public String toString() {
            return "LambdaStorageSlot{key=" + java.util.Arrays.toString(key) + ", value="
                    + java.util.Arrays.toString(value) + "}";
        }
    }

    /**
     * Represents storage updates via Solidity mapping entries.
     * <p>
     * This class allows updates to be specified in terms of Solidity mapping
     * entries rather than raw storage slots, making it easier to work with
     * high-level data structures.
     */
    public static class LambdaMappingEntries extends LambdaStorageUpdate {
        private final byte[] mappingSlot;
        private final java.util.List<LambdaMappingEntry> entries;

        /**
         * Create a new mapping entries update.
         *
         * @param mappingSlot the slot that corresponds to the Solidity mapping (minimal representation)
         * @param entries the entries to update in the mapping
         */
        public LambdaMappingEntries(byte[] mappingSlot, java.util.List<LambdaMappingEntry> entries) {
            this.mappingSlot = Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null")
                    .clone();
            this.entries = new java.util.ArrayList<>(Objects.requireNonNull(entries, "entries cannot be null"));
        }

        /**
         * Get the mapping slot.
         *
         * @return a copy of the mapping slot bytes
         */
        public byte[] getMappingSlot() {
            return mappingSlot.clone();
        }

        /**
         * Get the mapping entries.
         *
         * @return a copy of the entries list
         */
        public java.util.List<LambdaMappingEntry> getEntries() {
            return new java.util.ArrayList<>(entries);
        }

        @Override
        com.hedera.hashgraph.sdk.proto.LambdaStorageUpdate toProtobuf() {
            var builder = com.hedera.hashgraph.sdk.proto.LambdaMappingEntries.newBuilder()
                    .setMappingSlot(ByteString.copyFrom(mappingSlot));

            for (LambdaMappingEntry entry : entries) {
                builder.addEntries(entry.toProtobuf());
            }

            return com.hedera.hashgraph.sdk.proto.LambdaStorageUpdate.newBuilder()
                    .setMappingEntries(builder.build())
                    .build();
        }

        static LambdaMappingEntries fromProtobuf(com.hedera.hashgraph.sdk.proto.LambdaMappingEntries proto) {
            var entries = new java.util.ArrayList<LambdaMappingEntry>();
            for (var protoEntry : proto.getEntriesList()) {
                entries.add(LambdaMappingEntry.fromProtobuf(protoEntry));
            }

            return new LambdaMappingEntries(proto.getMappingSlot().toByteArray(), entries);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LambdaMappingEntries that = (LambdaMappingEntries) o;
            return Arrays.equals(mappingSlot, that.mappingSlot) && entries.equals(that.entries);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(mappingSlot), entries);
        }

        @Override
        public String toString() {
            return "LambdaMappingEntries{mappingSlot=" + java.util.Arrays.toString(mappingSlot) + ", entries=" + entries
                    + "}";
        }
    }
}
