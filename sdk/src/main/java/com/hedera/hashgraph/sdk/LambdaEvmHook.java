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
public class LambdaEvmHook extends EvmHookSpec {
    private final List<LambdaStorageUpdate> storageUpdates;

    /**
     * Create a new LambdaEvmHook with no initial storage updates.
     *
     * @param spec the EVM hook specification
     */
    public LambdaEvmHook(ContractId contractId) {
        this(contractId, Collections.emptyList());
    }

    /**
     * Create a new LambdaEvmHook with initial storage updates.
     *
     * @param spec the EVM hook specification
     * @param storageUpdates the initial storage updates for the lambda
     */
    public LambdaEvmHook(ContractId contractId, List<LambdaStorageUpdate> storageUpdates) {
        super(Objects.requireNonNull(contractId, "contractId cannot be null"));
        this.storageUpdates = new ArrayList<>(Objects.requireNonNull(storageUpdates, "storageUpdates cannot be null"));
    }

    /**
     * Get the EVM hook specification.
     *
     * @return the hook specification
     */
    public ContractId getSpec() {
        return getContractId();
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
        var specProto = com.hedera.hapi.node.hooks.legacy.EvmHookSpec.newBuilder()
                .setContractId(getContractId().toProtobuf())
                .build();
        var builder =
                com.hedera.hapi.node.hooks.legacy.LambdaEvmHook.newBuilder().setSpec(specProto);

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

        return new LambdaEvmHook(ContractId.fromProtobuf(proto.getSpec().getContractId()), storageUpdates);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaEvmHook that = (LambdaEvmHook) o;
        return super.equals(o) && storageUpdates.equals(that.storageUpdates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), storageUpdates);
    }

    @Override
    public String toString() {
        return "LambdaEvmHook{spec=" + getSpec() + ", storageUpdates=" + storageUpdates + "}";
    }
}
