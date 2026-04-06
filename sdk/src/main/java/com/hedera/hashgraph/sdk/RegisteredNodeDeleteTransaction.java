// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.AddressBookServiceGrpc;
import com.hedera.hashgraph.sdk.proto.RegisteredNodeDeleteTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;

/**
 * A transaction to delete a registered node from the network
 * address book.
 * <p>
 * This transaction, once complete, SHALL remove the identified registered
 * node from the network state.
 * This transaction MUST be signed by the existing entry `admin_key` or
 * authorized by the Hiero network governance structure.
 */
public class RegisteredNodeDeleteTransaction extends Transaction<RegisteredNodeDeleteTransaction> {
    private Long registeredNodeId;

    /**
     * Constructor.
     */
    public RegisteredNodeDeleteTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    RegisteredNodeDeleteTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    RegisteredNodeDeleteTransaction(TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Get registered node identifier in the network state.
     * @return the registered node id
     * @throws IllegalStateException when register node id is not being set
     */
    public long getRegisteredNodeId() {
        if (registeredNodeId == null) {
            throw new IllegalStateException("RegisteredNodeDeleteTransaction: 'registeredNodeId' has not been set");
        }

        return registeredNodeId;
    }

    /**
     * SET registered node identifier in the network state.
     * <p>
     * The node identified MUST exist in the registered address book.<br/>
     * The node identified MUST NOT be deleted.<br/>
     * This value is REQUIRED.
     *
     * @param registeredNodeId the registered node identifier.
     * @return {@code this}
     * @throws IllegalArgumentException if registeredNodeId is negative
     */
    public RegisteredNodeDeleteTransaction setRegisteredNodeId(long registeredNodeId) {
        this.requireNotFrozen();
        if (registeredNodeId < 0) {
            throw new IllegalArgumentException(
                    "RegisteredNodeDeleteTransaction: 'registeredNodeId' must be non-negative");
        }
        this.registeredNodeId = registeredNodeId;
        return this;
    }

    /**
     * Build the transaction body.
     * @return {@link com.hedera.hashgraph.sdk.proto.RegisteredNodeDeleteTransactionBody}
     */
    RegisteredNodeDeleteTransactionBody.Builder build() {
        var builder = RegisteredNodeDeleteTransactionBody.newBuilder();
        if (registeredNodeId != null) {
            builder.setRegisteredNodeId(registeredNodeId);
        }
        return builder;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getRegisteredNodeDelete();
        registeredNodeId = body.getRegisteredNodeId();
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        // no-op
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setRegisteredNodeDelete(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setRegisteredNodeDelete(build());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return AddressBookServiceGrpc.getDeleteRegisteredNodeMethod();
    }

    /**
     * Freeze this transaction with the given client.
     *
     * @param client the client to freeze with
     * @return this transaction
     * @throws IllegalStateException if registeredNodeId is not set
     */
    @Override
    public RegisteredNodeDeleteTransaction freezeWith(@Nullable Client client) {
        if (registeredNodeId == null) {
            throw new IllegalStateException(
                    "RegisteredNodeDeleteTransaction: 'registeredNodeId' must be explicitly set before calling freeze().");
        }
        return super.freezeWith(client);
    }
}
