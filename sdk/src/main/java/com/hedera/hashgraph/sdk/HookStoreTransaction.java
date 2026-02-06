// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.HookStoreTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.SmartContractServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Adds or removes key/value pairs in the storage of an EVM hook.
 */
public class HookStoreTransaction extends Transaction<HookStoreTransaction> {

    private HookId hookId;
    private List<EvmHookStorageUpdate> storageUpdates = new ArrayList<>();

    /**
     * Create a new empty HookStoreTransaction.
     */
    public HookStoreTransaction() {}

    HookStoreTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    HookStoreTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Set the id of the EVM hook whose storage is being updated.
     *
     * @param hookId the hook id
     * @return this
     */
    public HookStoreTransaction setHookId(HookId hookId) {
        requireNotFrozen();
        this.hookId = Objects.requireNonNull(hookId);
        return this;
    }

    /**
     * Get the hook id.
     *
     * @return the hook id
     */
    public HookId getHookId() {
        return hookId;
    }

    /**
     * Replace the list of storage updates.
     *
     * @param updates list of updates
     * @return this
     */
    public HookStoreTransaction setStorageUpdates(List<EvmHookStorageUpdate> updates) {
        requireNotFrozen();
        Objects.requireNonNull(updates);
        this.storageUpdates = new ArrayList<>(updates);
        return this;
    }

    /**
     * Add a storage update.
     *
     * @param update the update to add
     * @return this
     */
    public HookStoreTransaction addStorageUpdate(EvmHookStorageUpdate update) {
        requireNotFrozen();
        this.storageUpdates.add(Objects.requireNonNull(update));
        return this;
    }

    /**
     * Get the storage updates.
     *
     * @return list of updates
     */
    public List<EvmHookStorageUpdate> getStorageUpdates() {
        return storageUpdates;
    }

    HookStoreTransactionBody build() {
        var builder = HookStoreTransactionBody.newBuilder();
        if (hookId != null) {
            builder.setHookId(hookId.toProtobuf());
        }
        for (var update : storageUpdates) {
            builder.addStorageUpdates(update.toProtobuf());
        }
        return builder.build();
    }

    void initFromTransactionBody() {
        var body = sourceTransactionBody.getHookStore();
        if (body.hasHookId()) {
            this.hookId = HookId.fromProtobuf(body.getHookId());
        }
        this.storageUpdates = new ArrayList<>();
        for (var protoUpdate : body.getStorageUpdatesList()) {
            this.storageUpdates.add(EvmHookStorageUpdate.fromProtobuf(protoUpdate));
        }
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (hookId != null) {
            var entityId = hookId.getEntityId();
            if (entityId.isAccount()) {
                entityId.getAccountId().validateChecksum(client);
            } else if (entityId.isContract()) {
                entityId.getContractId().validateChecksum(client);
            }
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return SmartContractServiceGrpc.getHookStoreMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setHookStore(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException("cannot schedule HookStoreTransaction");
    }
}
