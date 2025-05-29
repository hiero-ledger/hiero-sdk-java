// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Hex;

/**
 * When the client sends the node a transaction of any kind, the node replies with this, which simply says that the
 * transaction passed the pre-check (so the node will submit it to the network) or it failed (so it won't). To learn the
 * consensus result, the client should later obtain a receipt (free), or can buy a more detailed record (not free).
 * <br>
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/miscellaneous/transactionresponse">Hedera
 * Documentation</a>
 */
public final class TransactionResponse {

    /**
     * The maximum number of retry attempts for throttled transactions
     */
    private static final int MAX_RETRY_ATTEMPTS = 5;

    /**
     * The initial backoff delay in milliseconds
     */
    private static final long INITIAL_BACKOFF_MS = 250;

    /**
     * The maximum backoff delay in milliseconds
     */
    private static final long MAX_BACKOFF_MS = 8000;

    /**
     * The node ID
     */
    public final AccountId nodeId;

    /**
     * The transaction hash
     */
    public final byte[] transactionHash;

    /**
     * The transaction ID
     */
    public final TransactionId transactionId;

    /**
     * The scheduled transaction ID
     */
    @Nullable
    @Deprecated
    public final TransactionId scheduledTransactionId;

    private final Transaction transaction;

    private boolean validateStatus = true;

    /**
     * Constructor.
     *
     * @param nodeId                 the node id
     * @param transactionId          the transaction id
     * @param transactionHash        the transaction hash
     * @param scheduledTransactionId the scheduled transaction id
     */
    TransactionResponse(
            AccountId nodeId,
            TransactionId transactionId,
            byte[] transactionHash,
            @Nullable TransactionId scheduledTransactionId,
            Transaction transaction) {
        this.nodeId = nodeId;
        this.transactionId = transactionId;
        this.transactionHash = transactionHash;
        this.scheduledTransactionId = scheduledTransactionId;
        this.transaction = transaction;
    }

    /**
     * @return whether getReceipt() or getRecord() will throw an exception if the receipt status is not SUCCESS
     */
    public boolean getValidateStatus() {
        return validateStatus;
    }

    /**
     * @param validateStatus whether getReceipt() or getRecord() will throw an exception if the receipt status is not
     *                       SUCCESS
     * @return {@code this}
     */
    public TransactionResponse setValidateStatus(boolean validateStatus) {
        this.validateStatus = validateStatus;
        return this;
    }

    /**
     * Fetch the receipt of the transaction.
     *
     * @param client The client with which this will be executed.
     * @return the transaction receipt
     * @throws TimeoutException        when the transaction times out
     * @throws PrecheckStatusException when the precheck fails
     * @throws ReceiptStatusException  when there is an issue with the receipt
     */
    public TransactionReceipt getReceipt(Client client)
            throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        return getReceipt(client, client.getRequestTimeout());
    }

    /**
     * Fetch the receipt of the transaction.
     *
     * @param client  The client with which this will be executed.
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @return the transaction receipt
     * @throws TimeoutException        when the transaction times out
     * @throws PrecheckStatusException when the precheck fails
     * @throws ReceiptStatusException  when there is an issue with the receipt
     */
    public TransactionReceipt getReceipt(Client client, Duration timeout)
            throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        int attempts = 0;
        ReceiptStatusException lastException = null;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                // Attempt to execute the receipt query
                return getReceiptQuery().execute(client, timeout).validateStatus(validateStatus);
            } catch (ReceiptStatusException e) {
                // Check if the exception status indicates throttling or inner transaction throttling
                if (e.receipt.status == Status.THROTTLED_AT_CONSENSUS) {
                    lastException = e;
                    attempts++;

                    if (attempts < MAX_RETRY_ATTEMPTS) {
                        try {
                            // Wait with exponential backoff before retrying
                            Thread.sleep(Math.min(backoffMs, MAX_BACKOFF_MS));
                            // Double the backoff for next attempt
                            backoffMs *= 2;

                            // Retry the transaction
                            return retryTransaction(client);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Retry on throttled status interrupted", ie);
                        } catch (ReceiptStatusException retryException) {
                            // Store the exception and continue with the next attempt
                            lastException = retryException;
                        }
                    }
                } else {
                    // If not throttled, rethrow the exception immediately
                    throw e;
                }
            }
        }

        // If we've exhausted all retries, throw the last exception
        throw lastException;
    }

    private TransactionReceipt retryTransaction(Client client)
            throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        // reset the transaction body
        transaction.frozenBodyBuilder = null;
        // regenerate the transaction id
        transaction.regenerateTransactionId(client);
        TransactionResponse transactionResponse = (TransactionResponse) this.transaction.execute(client);
        return new TransactionReceiptQuery()
                .setTransactionId(transactionResponse.transactionId)
                .setNodeAccountIds(List.of(transactionResponse.nodeId))
                .execute(client)
                .validateStatus(validateStatus);
    }

    /**
     * Create receipt query from the {@link #transactionId} and {@link #transactionHash}
     *
     * @return {@link com.hedera.hashgraph.sdk.TransactionReceiptQuery}
     */
    public TransactionReceiptQuery getReceiptQuery() {
        return new TransactionReceiptQuery()
                .setTransactionId(transactionId)
                .setNodeAccountIds(Collections.singletonList(nodeId));
    }

    /**
     * Fetch the receipt of the transaction asynchronously.
     *
     * @param client The client with which this will be executed.
     * @return future result of the transaction receipt
     */
    public CompletableFuture<TransactionReceipt> getReceiptAsync(Client client) {
        return getReceiptAsync(client, client.getRequestTimeout());
    }

    /**
     * Fetch the receipt of the transaction asynchronously.
     *
     * @param client  The client with which this will be executed.
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @return the transaction receipt
     */
    public CompletableFuture<TransactionReceipt> getReceiptAsync(Client client, Duration timeout) {
        return getReceiptQuery().executeAsync(client, timeout).thenCompose(receipt -> {
            try {
                return CompletableFuture.completedFuture(receipt.validateStatus(validateStatus));
            } catch (ReceiptStatusException e) {
                return CompletableFuture.failedFuture(e);
            }
        });
    }

    /**
     * Fetch the receipt of the transaction asynchronously.
     *
     * @param client   The client with which this will be executed.
     * @param callback a BiConsumer which handles the result or error.
     */
    public void getReceiptAsync(Client client, BiConsumer<TransactionReceipt, Throwable> callback) {
        ConsumerHelper.biConsumer(getReceiptAsync(client), callback);
    }

    /**
     * Fetch the receipt of the transaction asynchronously.
     *
     * @param client   The client with which this will be executed.
     * @param timeout  The timeout after which the execution attempt will be cancelled.
     * @param callback a BiConsumer which handles the result or error.
     */
    public void getReceiptAsync(Client client, Duration timeout, BiConsumer<TransactionReceipt, Throwable> callback) {
        ConsumerHelper.biConsumer(getReceiptAsync(client, timeout), callback);
    }

    /**
     * Fetch the receipt of the transaction asynchronously.
     *
     * @param client    The client with which this will be executed.
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void getReceiptAsync(Client client, Consumer<TransactionReceipt> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(getReceiptAsync(client), onSuccess, onFailure);
    }

    /**
     * Fetch the receipt of the transaction asynchronously.
     *
     * @param client    The client with which this will be executed.
     * @param timeout   The timeout after which the execution attempt will be cancelled.
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void getReceiptAsync(
            Client client, Duration timeout, Consumer<TransactionReceipt> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(getReceiptAsync(client, timeout), onSuccess, onFailure);
    }

    /**
     * Fetch the record of the transaction.
     *
     * @param client The client with which this will be executed.
     * @return the transaction record
     * @throws TimeoutException        when the transaction times out
     * @throws PrecheckStatusException when the precheck fails
     * @throws ReceiptStatusException  when there is an issue with the receipt
     */
    public TransactionRecord getRecord(Client client)
            throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        return getRecord(client, client.getRequestTimeout());
    }

    /**
     * Fetch the record of the transaction.
     *
     * @param client  The client with which this will be executed.
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @return the transaction record
     * @throws TimeoutException        when the transaction times out
     * @throws PrecheckStatusException when the precheck fails
     * @throws ReceiptStatusException  when there is an issue with the receipt
     */
    public TransactionRecord getRecord(Client client, Duration timeout)
            throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        getReceipt(client, timeout);
        return getRecordQuery().execute(client, timeout);
    }

    /**
     * Create record query from the {@link #transactionId} and {@link #transactionHash}
     *
     * @return {@link com.hedera.hashgraph.sdk.TransactionRecordQuery}
     */
    public TransactionRecordQuery getRecordQuery() {
        return new TransactionRecordQuery()
                .setTransactionId(transactionId)
                .setNodeAccountIds(Collections.singletonList(nodeId));
    }

    /**
     * Fetch the record of the transaction asynchronously.
     *
     * @param client The client with which this will be executed.
     * @return future result of the transaction record
     */
    public CompletableFuture<TransactionRecord> getRecordAsync(Client client) {
        return getRecordAsync(client, client.getRequestTimeout());
    }

    /**
     * Fetch the record of the transaction asynchronously.
     *
     * @param client  The client with which this will be executed.
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @return future result of the transaction record
     */
    public CompletableFuture<TransactionRecord> getRecordAsync(Client client, Duration timeout) {
        return getReceiptAsync(client, timeout)
                .thenCompose((receipt) -> getRecordQuery().executeAsync(client, timeout));
    }

    /**
     * Fetch the record of the transaction asynchronously.
     *
     * @param client   The client with which this will be executed.
     * @param callback a BiConsumer which handles the result or error.
     */
    public void getRecordAsync(Client client, BiConsumer<TransactionRecord, Throwable> callback) {
        ConsumerHelper.biConsumer(getRecordAsync(client), callback);
    }

    /**
     * Fetch the record of the transaction asynchronously.
     *
     * @param client   The client with which this will be executed.
     * @param timeout  The timeout after which the execution attempt will be cancelled.
     * @param callback a BiConsumer which handles the result or error.
     */
    public void getRecordAsync(Client client, Duration timeout, BiConsumer<TransactionRecord, Throwable> callback) {
        ConsumerHelper.biConsumer(getRecordAsync(client, timeout), callback);
    }

    /**
     * Fetch the record of the transaction asynchronously.
     *
     * @param client    The client with which this will be executed.
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void getRecordAsync(Client client, Consumer<TransactionRecord> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(getRecordAsync(client), onSuccess, onFailure);
    }

    /**
     * Fetch the record of the transaction asynchronously.
     *
     * @param client    The client with which this will be executed.
     * @param timeout   The timeout after which the execution attempt will be cancelled.
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void getRecordAsync(
            Client client, Duration timeout, Consumer<TransactionRecord> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(getRecordAsync(client, timeout), onSuccess, onFailure);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("nodeId", nodeId)
                .add("transactionHash", Hex.toHexString(transactionHash))
                .add("transactionId", transactionId)
                .toString();
    }
}
