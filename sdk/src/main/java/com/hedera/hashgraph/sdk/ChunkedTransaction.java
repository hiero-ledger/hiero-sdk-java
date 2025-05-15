// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SignatureMap;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionID;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * A common base for file and topic message transactions.
 */
abstract class ChunkedTransaction<T extends ChunkedTransaction<T>> extends Transaction<T> {
    private int chunkSize = 1024;

    /**
     * The transaction data
     */
    protected ByteString data = ByteString.EMPTY;

    /**
     * Maximum number of chunks this message will get broken up into when
     * it's frozen.
     */
    private int maxChunks = 20;

    /**
     * Constructor.
     *
     * @param txs                       Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    ChunkedTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
    }

    /**
     * Constructor.
     *
     * @param txBody                    protobuf TransactionBody
     */
    ChunkedTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
    }

    /**
     * Constructor.
     */
    ChunkedTransaction() {
        super();
    }

    /**
     * Extract the data.
     *
     * @return                          the data
     */
    ByteString getData() {
        return data;
    }

    /**
     * Assign the data via a byte array.
     *
     * @param data                      the byte array
     * @return {@code this}
     */
    T setData(byte[] data) {
        requireNotFrozen();
        this.data = ByteString.copyFrom(data);

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Assign the data via a byte string.
     *
     * @param data                      the byte string
     * @return {@code this}
     */
    T setData(ByteString data) {
        requireNotFrozen();
        this.data = data;

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Assign the data via a string.
     *
     * @param text                      the byte array
     * @return {@code this}
     */
    T setData(String text) {
        requireNotFrozen();
        this.data = ByteString.copyFromUtf8(text);

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieve the maximum number of chunks.
     *
     * @return                          the number of chunks
     */
    public int getMaxChunks() {
        return maxChunks;
    }

    /**
     * Assign the max number of chunks.
     *
     * @param maxChunks                 the number of chunks
     * @return {@code this}
     */
    public T setMaxChunks(int maxChunks) {
        requireNotFrozen();
        this.maxChunks = maxChunks;

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Retrieve the chunk size.
     *
     * @return                          the chunk size
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Assign the chunk size.
     *
     * @param chunkSize                 the chunk size
     * @return {@code this}
     */
    public T setChunkSize(int chunkSize) {
        requireNotFrozen();
        this.chunkSize = chunkSize;

        // noinspection unchecked
        return (T) this;
    }

    @Override
    public byte[] getTransactionHash() {
        if (outerTransactions.size() > nodeAccountIds.size()) {
            throw new IllegalStateException(
                    "a single transaction hash can not be calculated for a chunked transaction, try calling `getAllTransactionHashesPerNode`");
        }

        return super.getTransactionHash();
    }

    @Override
    public Map<AccountId, byte[]> getTransactionHashPerNode() {
        if (outerTransactions.size() > nodeAccountIds.size()) {
            throw new IllegalStateException(
                    "a single transaction hash can not be calculated for a chunked transaction, try calling `getAllTransactionHashesPerNode`");
        }

        return super.getTransactionHashPerNode();
    }

    /**
     * Extract the list of transaction hashes.
     *
     * @return                          the list of transaction hashes
     */
    public final List<Map<AccountId, byte[]>> getAllTransactionHashesPerNode() {
        if (!this.isFrozen()) {
            throw new IllegalStateException(
                    "transaction must have been frozen before calculating the hash will be stable, try calling `freeze`");
        }

        transactionIds.setLocked(true);
        nodeAccountIds.setLocked(true);

        buildAllTransactions();

        var txCount = transactionIds.size();
        var nodeCount = nodeAccountIds.size();
        var transactionHashes = new ArrayList<Map<AccountId, byte[]>>(txCount);

        for (var txIndex = 0; txIndex < txCount; ++txIndex) {
            var hashes = new HashMap<AccountId, byte[]>();
            var offset = txIndex * nodeCount;

            for (var nodeIndex = 0; nodeIndex < nodeCount; ++nodeIndex) {
                hashes.put(
                        nodeAccountIds.get(nodeIndex),
                        hash(outerTransactions
                                .get(offset + nodeIndex)
                                .getSignedTransactionBytes()
                                .toByteArray()));
            }

            transactionHashes.add(hashes);
        }

        return transactionHashes;
    }

    @Override
    public T addSignature(PublicKey publicKey, byte[] signature) {
        if (data.size() > chunkSize) {
            throw new IllegalStateException(
                    "Cannot manually add signature to chunked transaction with length greater than " + chunkSize);
        }
        return super.addSignature(publicKey, signature);
    }

    @Override
    public Map<AccountId, Map<PublicKey, byte[]>> getSignatures() {
        if (data.size() > chunkSize) {
            throw new IllegalStateException(
                    "Cannot call getSignatures() on a chunked transaction with length greater than " + chunkSize);
        }
        return super.getSignatures();
    }

    /**
     * Extract the list of all signers.
     *
     * @return                          the list of all signatures
     */
    public List<Map<AccountId, Map<PublicKey, byte[]>>> getAllSignatures() {
        if (publicKeys.isEmpty()) {
            return new ArrayList<>();
        }

        buildAllTransactions();

        var txCount = transactionIds.size();
        var nodeCount = nodeAccountIds.size();

        var retval = new ArrayList<Map<AccountId, Map<PublicKey, byte[]>>>(txCount);

        for (int i = 0; i < txCount; i++) {
            retval.add(getSignaturesAtOffset(i * nodeCount));
        }

        return retval;
    }

    private void freezeAndSign(Client client) {
        if (!isFrozen()) {
            freezeWith(client);
        }

        var operatorId = client.getOperatorAccountId();

        if (operatorId != null && operatorId.equals(Objects.requireNonNull(getTransactionIdInternal().accountId))) {
            // on execute, sign each transaction with the operator, if present
            // and we are signing a transaction that used the default transaction ID
            signWithOperator(client);
        }
    }

    @Override
    public TransactionResponse execute(Client client, Duration timeoutPerChunk)
            throws TimeoutException, PrecheckStatusException {
        return executeAll(client, timeoutPerChunk).get(0);
    }

    /**
     * Execute this transaction or query
     *
     * @param client The client with which this will be executed.
     * @return Result of execution for each chunk
     * @throws TimeoutException         when the transaction times out
     * @throws PrecheckStatusException  when the precheck fails
     */
    public List<TransactionResponse> executeAll(Client client) throws PrecheckStatusException, TimeoutException {
        return executeAll(client, client.getRequestTimeout());
    }

    /**
     * Execute this transaction or query
     *
     * @param client The client with which this will be executed.
     * @param timeoutPerChunk The timeout after which the execution attempt will be cancelled.
     * @return Result of execution for each chunk
     * @throws TimeoutException         when the transaction times out
     * @throws PrecheckStatusException  when the precheck fails
     */
    public List<TransactionResponse> executeAll(Client client, Duration timeoutPerChunk)
            throws PrecheckStatusException, TimeoutException {
        freezeAndSign(client);

        var responses = new ArrayList<TransactionResponse>(transactionIds.size());

        for (var i = 0; i < transactionIds.size(); i++) {
            var response = super.execute(client, timeoutPerChunk);

            if (shouldGetReceipt()) {
                new TransactionReceiptQuery()
                        .setNodeAccountIds(Collections.singletonList(response.nodeId))
                        .setTransactionId(response.transactionId)
                        .execute(client, timeoutPerChunk);
            }

            responses.add(response);
        }

        return responses;
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client The client with which this will be executed.
     * @return Future result of execution for each chunk
     */
    public CompletableFuture<List<TransactionResponse>> executeAllAsync(Client client) {
        return executeAllAsync(client, client.getRequestTimeout());
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client The client with which this will be executed.
     * @param timeoutPerChunk The timeout after which the execution attempt will be cancelled.
     * @return Future result of execution for each chunk
     */
    public CompletableFuture<List<TransactionResponse>> executeAllAsync(Client client, Duration timeoutPerChunk) {
        freezeAndSign(client);

        CompletableFuture<List<com.hedera.hashgraph.sdk.TransactionResponse>> future =
                CompletableFuture.supplyAsync(() -> new ArrayList<>(transactionIds.size()));

        for (var i = 0; i < transactionIds.size(); i++) {
            future = future.thenCompose(list -> {
                var responseFuture = super.executeAsync(client, timeoutPerChunk);

                Function<TransactionResponse, ? extends CompletionStage<TransactionResponse>> receiptFuture =
                        (TransactionResponse response) -> response.getReceiptAsync(client, timeoutPerChunk)
                                .thenApply(receipt -> response);

                Function<TransactionResponse, List<TransactionResponse>> addToList = (response) -> {
                    list.add(response);
                    return list;
                };

                if (shouldGetReceipt()) {
                    return responseFuture.thenCompose(receiptFuture).thenApply(addToList);
                } else {
                    return responseFuture.thenApply(addToList);
                }
            });
        }

        return future;
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client The client with which this will be executed.
     * @param callback a BiConsumer which handles the result or error.
     */
    public void executeAllAsync(Client client, BiConsumer<List<TransactionResponse>, Throwable> callback) {
        ConsumerHelper.biConsumer(executeAllAsync(client), callback);
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client The client with which this will be executed.
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @param callback a BiConsumer which handles the result or error.
     */
    public void executeAllAsync(
            Client client, Duration timeout, BiConsumer<List<TransactionResponse>, Throwable> callback) {
        ConsumerHelper.biConsumer(executeAllAsync(client, timeout), callback);
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client The client with which this will be executed.
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void executeAllAsync(
            Client client, Consumer<List<TransactionResponse>> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(executeAllAsync(client), onSuccess, onFailure);
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client The client with which this will be executed.
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void executeAllAsync(
            Client client,
            Duration timeout,
            Consumer<List<TransactionResponse>> onSuccess,
            Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(executeAllAsync(client, timeout), onSuccess, onFailure);
    }

    @Override
    public CompletableFuture<com.hedera.hashgraph.sdk.TransactionResponse> executeAsync(
            Client client, Duration timeoutPerChunk) {
        return executeAllAsync(client, timeoutPerChunk).thenApply(responses -> responses.get(0));
    }

    @Override
    public ScheduleCreateTransaction schedule() {
        requireNotFrozen();
        if (!nodeAccountIds.isEmpty()) {
            throw new IllegalStateException(
                    "The underlying transaction for a scheduled transaction cannot have node account IDs set");
        }
        if (data.size() > chunkSize) {
            throw new IllegalStateException(
                    "Cannot schedule a chunked transaction with length greater than " + chunkSize);
        }

        var bodyBuilder = spawnBodyBuilder(null);

        onFreeze(bodyBuilder);

        onFreezeChunk(bodyBuilder, null, 0, data.size(), 1, 1);

        return doSchedule(bodyBuilder);
    }

    @Override
    int getRequiredChunks() {
        var requiredChunks = (this.data.size() + (chunkSize - 1)) / chunkSize;

        if (requiredChunks == 0) {
            requiredChunks = 1;
        }

        if (requiredChunks > maxChunks) {
            throw new IllegalArgumentException("message of " + this.data.size() + " bytes requires " + requiredChunks
                    + " chunks but the maximum allowed chunks is " + maxChunks + ", try using setMaxChunks");
        }
        return requiredChunks;
    }

    @Override
    void wipeTransactionLists(int requiredChunks) {
        sigPairLists = new ArrayList<>(requiredChunks * nodeAccountIds.size());
        outerTransactions = new ArrayList<>(requiredChunks * nodeAccountIds.size());
        innerSignedTransactions = new ArrayList<>(requiredChunks * nodeAccountIds.size());

        for (int i = 0; i < requiredChunks; i++) {
            if (!transactionIds.isEmpty()) {
                var startIndex = i * chunkSize;
                var endIndex = startIndex + chunkSize;

                if (endIndex > this.data.size()) {
                    endIndex = this.data.size();
                }

                onFreezeChunk(
                        Objects.requireNonNull(frozenBodyBuilder)
                                .setTransactionID(transactionIds.get(i).toProtobuf()),
                        transactionIds.get(0).toProtobuf(),
                        startIndex,
                        endIndex,
                        i,
                        requiredChunks);
            }

            // For each node we add a transaction with that node
            for (var nodeId : nodeAccountIds) {
                sigPairLists.add(SignatureMap.newBuilder());
                innerSignedTransactions.add(SignedTransaction.newBuilder()
                        .setBodyBytes(frozenBodyBuilder
                                .setNodeAccountID(nodeId.toProtobuf())
                                .build()
                                .toByteString()));
                outerTransactions.add(null);
            }
        }
    }

    /**
     * A common base for file and topic message transactions.
     */
    abstract void onFreezeChunk(
            TransactionBody.Builder body,
            @Nullable TransactionID initialTransactionId,
            int startIndex,
            int endIndex,
            int chunk,
            int total);

    /**
     * Should the receipt be retrieved?
     *
     * @return                          by default do not get a receipt
     */
    boolean shouldGetReceipt() {
        return false;
    }

    /**
     * Get the body sizes for all chunks in a FileAppendTransaction.
     * For transactions with multiple chunks (like large file appends),
     * this returns an array containing the size of each chunk's transaction body.
     * The size is calculated by encoding the transaction body to protobuf format.
     *
     * @return List of integers that represent the size of each chunk
     */
    public List<Integer> bodySizeAllChunks() {
        List<Integer> list = new ArrayList<>();

        int originalIndex = this.transactionIds.getIndex();

        try {
            // Calculate size for each chunk
            for (int i = 0; i < getRequiredChunks(); i++) {
                this.transactionIds.setIndex(i);
                list.add(getTransactionBodySize());
            }
        } finally {
            this.transactionIds.setIndex(originalIndex);
        }

        return list;
    }
}
