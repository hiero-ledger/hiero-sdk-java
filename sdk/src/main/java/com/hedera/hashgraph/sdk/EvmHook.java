// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

// Using fully qualified names to avoid conflicts with generated classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Definition of an EVM hook.
 * <p>
 * This class represents a hook implementation that is programmed in EVM bytecode
 * and can access state or interact with external contracts. It includes the
 * hook specification and any initial storage updates.
 */
public class EvmHook extends EvmHookSpec {
    private final List<EvmHookStorageUpdate> storageUpdates;

    /**
     * Create a new EvmHook with no initial storage updates.
     *
     * @param contractId underlying contract of the hook
     */
    public EvmHook(ContractId contractId) {
        this(contractId, Collections.emptyList());
    }

    /**
     * Create a new EvmHook with initial storage updates.
     *
     * @param contractId underlying contract of the hook
     * @param storageUpdates the initial storage updates for the EVM hook
     */
    public EvmHook(ContractId contractId, List<EvmHookStorageUpdate> storageUpdates) {
        super(Objects.requireNonNull(contractId, "contractId cannot be null"));
        this.storageUpdates = new ArrayList<>(Objects.requireNonNull(storageUpdates, "storageUpdates cannot be null"));
    }

    /**
     * Get the initial storage updates for this EVM hook.
     *
     * @return an immutable list of storage updates
     */
    public List<EvmHookStorageUpdate> getStorageUpdates() {
        return Collections.unmodifiableList(storageUpdates);
    }

    /**
     * Convert this EvmHook to a protobuf message.
     *
     * @return the protobuf EvmHook
     */
    com.hedera.hashgraph.sdk.proto.EvmHook toProtobuf() {
        var specProto = com.hedera.hashgraph.sdk.proto.EvmHookSpec.newBuilder()
                .setContractId(getContractId().toProtobuf())
                .build();
        var builder = com.hedera.hashgraph.sdk.proto.EvmHook.newBuilder().setSpec(specProto);

        for (EvmHookStorageUpdate update : storageUpdates) {
            builder.addStorageUpdates(update.toProtobuf());
        }

        return builder.build();
    }

    /**
     * Create an EvmHook from a protobuf message.
     *
     * @param proto the protobuf EvmHook
     * @return a new EvmHook instance
     */
    public static EvmHook fromProtobuf(com.hedera.hashgraph.sdk.proto.EvmHook proto) {
        var storageUpdates = new ArrayList<EvmHookStorageUpdate>();
        for (var protoUpdate : proto.getStorageUpdatesList()) {
            storageUpdates.add(EvmHookStorageUpdate.fromProtobuf(protoUpdate));
        }

        return new EvmHook(ContractId.fromProtobuf(proto.getSpec().getContractId()), storageUpdates);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EvmHook that = (EvmHook) o;
        return super.equals(o) && storageUpdates.equals(that.storageUpdates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), storageUpdates);
    }

    @Override
    public String toString() {
        return "EvmHook{contractId=" + getContractId() + ", storageUpdates=" + storageUpdates + "}";
    }
}
