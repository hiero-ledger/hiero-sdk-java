// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

// Using fully qualified names to avoid conflicts with generated classes
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Updates the storage of a lambda EVM hook.
 * <p>
 * This transaction allows the owner of a lambda EVM hook to update its storage slots,
 * either by direct storage slot updates or by updating Solidity mapping entries.
 * <p>
 * The transaction must be signed by the hook's admin key.
 */
public final class LambdaSStoreTransaction extends Transaction<LambdaSStoreTransaction> {
    private HookId hookId;
    private List<LambdaStorageUpdate> storageUpdates = new ArrayList<>();

    /**
     * Constructor.
     */
    public LambdaSStoreTransaction() {
        defaultMaxTransactionFee = Hbar.from(1);
    }

    /**
     * Get the ID of the lambda EVM hook whose storage is being updated.
     *
     * @return the {@link HookId}
     */
    @Nullable
    public HookId getHookId() {
        return hookId;
    }

    /**
     * Set the ID of the lambda EVM hook whose storage is being updated.
     *
     * @param hookId the {@link HookId}
     * @return {@code this}
     */
    public LambdaSStoreTransaction setHookId(HookId hookId) {
        requireNotFrozen();
        Objects.requireNonNull(hookId, "hookId cannot be null");
        this.hookId = hookId;
        return this;
    }

    /**
     * Get the storage updates for the lambda.
     *
     * @return a copy of the storage updates list
     */
    public List<LambdaStorageUpdate> getStorageUpdates() {
        return new ArrayList<>(storageUpdates);
    }

    /**
     * Add a storage update to the lambda.
     *
     * @param storageUpdate the storage update to add
     * @return {@code this}
     */
    public LambdaSStoreTransaction addStorageUpdate(LambdaStorageUpdate storageUpdate) {
        requireNotFrozen();
        Objects.requireNonNull(storageUpdate, "storageUpdate cannot be null");
        this.storageUpdates.add(storageUpdate);
        return this;
    }

    /**
     * Set the storage updates for the lambda.
     *
     * @param storageUpdates the list of storage updates
     * @return {@code this}
     */
    public LambdaSStoreTransaction setStorageUpdates(List<LambdaStorageUpdate> storageUpdates) {
        requireNotFrozen();
        Objects.requireNonNull(storageUpdates, "storageUpdates cannot be null");
        this.storageUpdates = new ArrayList<>(storageUpdates);
        return this;
    }

    /**
     * Add a direct storage slot update.
     * <p>
     * This is a convenience method for adding a {@link LambdaStorageUpdate.LambdaStorageSlot}.
     *
     * @param key the storage slot key (max 32 bytes, minimal representation)
     * @param value the storage slot value (max 32 bytes, minimal representation)
     * @return {@code this}
     */
    public LambdaSStoreTransaction addStorageSlot(byte[] key, byte[] value) {
        requireNotFrozen();
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        return addStorageUpdate(new LambdaStorageUpdate.LambdaStorageSlot(key, value));
    }

    /**
     * Add a Solidity mapping entry update.
     * <p>
     * This is a convenience method for adding a {@link LambdaStorageUpdate.LambdaMappingEntries}.
     *
     * @param mappingSlot the mapping slot (max 32 bytes, minimal representation)
     * @param entries the mapping entries to update
     * @return {@code this}
     */
    public LambdaSStoreTransaction addMappingEntries(byte[] mappingSlot, List<LambdaMappingEntry> entries) {
        requireNotFrozen();
        Objects.requireNonNull(mappingSlot, "mappingSlot cannot be null");
        Objects.requireNonNull(entries, "entries cannot be null");
        return addStorageUpdate(new LambdaStorageUpdate.LambdaMappingEntries(mappingSlot, entries));
    }

    /**
     * Validate the transaction parameters.
     *
     * @throws IllegalArgumentException if validation fails
     */
    private void validate() {
        if (hookId == null) {
            throw new IllegalArgumentException("hookId must be set");
        }
        if (storageUpdates.isEmpty()) {
            throw new IllegalArgumentException("at least one storage update must be specified");
        }
        // Note: Additional validation for storage update limits would be done server-side
    }

    /**
     * Build the transaction body.
     *
     * @return the protobuf LambdaSStoreTransactionBody builder
     */
    com.hedera.hapi.node.hooks.legacy.LambdaSStoreTransactionBody.Builder build() {
        validate();
        
        var builder = com.hedera.hapi.node.hooks.legacy.LambdaSStoreTransactionBody.newBuilder()
                .setHookId(hookId.toProtobuf());

        for (LambdaStorageUpdate update : storageUpdates) {
            builder.addStorageUpdates(update.toProtobuf());
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (hookId != null) {
            hookId.getEntityId().getAccountId().validateChecksum(client);
            hookId.getEntityId().getContractId().validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        // TODO: Hook services are not yet defined in the protobuf files
        // This will need to be updated once the hook service is available
        throw new UnsupportedOperationException("Hook services are not yet available in the current SDK version");
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        throw new UnsupportedOperationException("LambdaSStore not wired into TransactionBody in this branch");
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduledBuilder) {
        throw new UnsupportedOperationException("LambdaSStore transactions are not schedulable");
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        // No-op: LambdaSStore not available on this branch
    }

}
