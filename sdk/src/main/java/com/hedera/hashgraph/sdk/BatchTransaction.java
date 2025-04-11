// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.AtomicBatchTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import com.hedera.hashgraph.sdk.proto.UtilServiceGrpc;
import io.grpc.MethodDescriptor;
import java.util.*;
import javax.annotation.Nullable;

/**
 * Execute multiple transactions in a single consensus event.
 *
 * ### Requirements
 * - All transactions must be signed as required for each individual transaction.
 * - The BatchTransaction must be signed by the operator account.
 * - Individual transaction failures do not cause the batch to fail.
 * - Fees are assessed for each inner transaction separately.
 *
 * ### Block Stream Effects
 * Each inner transaction will appear in the transaction record stream.
 */
public final class BatchTransaction extends Transaction<BatchTransaction> {
    private List<Transaction> transactions = new ArrayList<>();

    @Nullable
    private List<TransactionId> innerTransactionIds = new ArrayList<>();

    /**
     * Constructor.
     */
    public BatchTransaction() {}

    /**
     * Constructor.
     *
     * @param txs                       Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException  when there is an issue with the protobuf
     */
    BatchTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody                    protobuf TransactionBody
     */
    BatchTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Set the list of transactions to be executed as part of this BatchTransaction.
     *
     * @param transactions The list of transactions to be executed
     * @return {@code this}
     */
    public BatchTransaction setInnerTransactions(List<Transaction<?>> transactions) {
        Objects.requireNonNull(transactions);
        requireNotFrozen();
        this.transactions.clear();
        this.transactions.addAll(transactions);
        return this;
    }

    /**
     * Append a transaction to the list of transactions this BatchTransaction will execute.
     *
     * @param transaction The transaction to be added
     * @return {@code this}
     */
    public BatchTransaction addInnerTransaction(Transaction<?> transaction) {
        Objects.requireNonNull(transaction);
        requireNotFrozen();
        if (transaction.isFrozen() && transaction.getTransactionId() != null) {
            innerTransactionIds.add(transaction.getTransactionId());
        }
        this.transactions.add(transaction);
        return this;
    }

    /**
     * Get the list of transactions this BatchTransaction is currently configured to execute.
     *
     * @return The list of transactions
     */
    public List<Transaction> getInnerTransactions() {
        return transactions;
    }

    /**
     * Get the list of transaction IDs of each inner transaction of this BatchTransaction.
     *
     * **NOTE**: this will return undefined data until the transaction IDs for each inner
     * transaction actually get generated/set (i.e. this BatchTransaction has been executed).
     * This is provided to the user as a convenience feature in case they would like to get
     * the receipts of each individual inner transaction.
     *
     * @return The list of inner transaction IDs
     */
    public List<TransactionId> getInnerTransactionIds() {
        if (innerTransactionIds == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(innerTransactionIds);
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        for (Transaction<?> transaction : transactions) {
            transaction.validateChecksums(client);
        }
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        throw new UnsupportedOperationException("Cannot construct BatchTransaction from bytes");
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return UtilServiceGrpc.getAtomicBatchMethod();
    }

    /**
     * Create the builder.
     *
     * @return the transaction builder
     */
    AtomicBatchTransactionBody build() {
        var builder = AtomicBatchTransactionBody.newBuilder();
        for (var transaction : transactions) {
            builder.addTransactions(transaction.makeRequest().getSignedTransactionBytes());
        }
        return builder.build();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setAtomicBatch(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException("Cannot schedule Atomic Batch");
    }
}
