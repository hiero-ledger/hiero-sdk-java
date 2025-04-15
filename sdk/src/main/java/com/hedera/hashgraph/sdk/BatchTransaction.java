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

/**
 * Execute multiple transactions in a single consensus event.
 * <p>
 * ### Requirements - All transactions must be signed as required for each individual transaction. - The
 * BatchTransaction must be signed by the operator account. - Individual transaction failures do not cause the batch to
 * fail. - Fees are assessed for each inner transaction separately.
 * <p>
 * ### Block Stream Effects Each inner transaction will appear in the transaction record stream.
 */
public final class BatchTransaction extends Transaction<BatchTransaction> {
    private List<Transaction> innerTransactions = new ArrayList<>();

    /**
     * Constructor.
     */
    public BatchTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
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
     * @param txBody protobuf TransactionBody
     */
    BatchTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) throws InvalidProtocolBufferException {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Set the list of transactions to be executed as part of this BatchTransaction.
     *
     * @param transactions The list of transactions to be executed
     * @return {@code this}
     */
    public BatchTransaction setInnerTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions);
        requireNotFrozen();
        this.innerTransactions = new ArrayList<>(transactions);
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
        if (!transaction.isFrozen()) {
            throw new IllegalStateException("Inner transaction should be frozen");
        }
        this.innerTransactions.add(transaction);
        return this;
    }

    /**
     * Get the list of transactions this BatchTransaction is currently configured to execute.
     *
     * @return The list of transactions
     */
    public List<Transaction> getInnerTransactions() {
        return innerTransactions;
    }

    /**
     * Get the list of transaction IDs of each inner transaction of this BatchTransaction.
     * <p>
     * **NOTE**: this will return undefined data until the transaction IDs for each inner transaction actually get
     * generated/set (i.e. this BatchTransaction has been executed). This is provided to the user as a convenience
     * feature in case they would like to get the receipts of each individual inner transaction.
     *
     * @return The list of inner transaction IDs
     */
    public List<TransactionId> getInnerTransactionIds() {
        return this.innerTransactions.stream()
                .map(Transaction::getTransactionId)
                .toList();
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        for (Transaction<?> transaction : innerTransactions) {
            transaction.validateChecksums(client);
        }
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() throws InvalidProtocolBufferException {
        var body = sourceTransactionBody.getAtomicBatch();

        for (var atomicTransactionBytes : body.getTransactionsList()) {
            var transaction = com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                    .setSignedTransactionBytes(atomicTransactionBytes);
            innerTransactions.add(Transaction.fromBytes(transaction.build().toByteArray()));
        }
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
        for (var transaction : innerTransactions) {
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
