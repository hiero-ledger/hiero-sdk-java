// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

// Using fully qualified names to avoid conflicts with generated classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Definition of a lambda EVM hook.
 * <p>
 * This class represents a hook implementation that is programmed in EVM bytecode
 * and can access state or interact with external contracts. It includes the
 * hook specification and any initial storage updates.
 */
public class LambdaEvmHook {
    private final EvmHookSpec spec;
    private final List<LambdaStorageUpdate> storageUpdates;

    /**
     * Create a new LambdaEvmHook with no initial storage updates.
     *
     * @param spec the EVM hook specification
     */
    public LambdaEvmHook(EvmHookSpec spec) {
        this(spec, Collections.emptyList());
    }

    /**
     * Create a new LambdaEvmHook with initial storage updates.
     *
     * @param spec the EVM hook specification
     * @param storageUpdates the initial storage updates for the lambda
     */
    public LambdaEvmHook(EvmHookSpec spec, List<LambdaStorageUpdate> storageUpdates) {
        this.spec = Objects.requireNonNull(spec, "spec cannot be null");
        this.storageUpdates = new ArrayList<>(Objects.requireNonNull(storageUpdates, "storageUpdates cannot be null"));
    }

    /**
     * Get the EVM hook specification.
     *
     * @return the hook specification
     */
    public EvmHookSpec getSpec() {
        return spec;
    }

    /**
     * Get the initial storage updates for this lambda.
     *
     * @return an immutable list of storage updates
     */
    public List<LambdaStorageUpdate> getStorageUpdates() {
        return Collections.unmodifiableList(storageUpdates);
    }

    /**
     * Convert this LambdaEvmHook to a protobuf message.
     *
     * @return the protobuf LambdaEvmHook
     */
    public com.hedera.hapi.node.hooks.legacy.LambdaEvmHook toProtobuf() {
        var builder =
                com.hedera.hapi.node.hooks.legacy.LambdaEvmHook.newBuilder().setSpec(spec.toProtobuf());

        for (LambdaStorageUpdate update : storageUpdates) {
            builder.addStorageUpdates(update.toProtobuf());
        }

        return builder.build();
    }

    /**
     * Create a LambdaEvmHook from a protobuf message.
     *
     * @param proto the protobuf LambdaEvmHook
     * @return a new LambdaEvmHook instance
     */
    public static LambdaEvmHook fromProtobuf(com.hedera.hapi.node.hooks.legacy.LambdaEvmHook proto) {
        var storageUpdates = new ArrayList<LambdaStorageUpdate>();
        for (var protoUpdate : proto.getStorageUpdatesList()) {
            storageUpdates.add(LambdaStorageUpdate.fromProtobuf(protoUpdate));
        }

        return new LambdaEvmHook(EvmHookSpec.fromProtobuf(proto.getSpec()), storageUpdates);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaEvmHook that = (LambdaEvmHook) o;
        return spec.equals(that.spec) && storageUpdates.equals(that.storageUpdates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec, storageUpdates);
    }

    @Override
    public String toString() {
        return "LambdaEvmHook{spec=" + spec + ", storageUpdates=" + storageUpdates + "}";
    }
}
