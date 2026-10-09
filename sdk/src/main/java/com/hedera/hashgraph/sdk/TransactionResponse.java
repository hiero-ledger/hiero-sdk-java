// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bouncycastle.util.encoders.Hex;
import org.jspecify.annotations.Nullable;

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
    public AccountId nodeId;

    /**
     * The transaction hash
     */
    public byte[] transactionHash;

    /**
     * The transaction ID
     */
    public TransactionId transactionId;

    /**
     * The scheduled transaction ID
     */
    @Deprecated
    public final @Nullable TransactionId scheduledTransactionId;

    @Nullable
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
            @Nullable Transaction transaction) {
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
     * Extract the node ID the transaction was submitted to.
     *
     * @return the node ID
     */
    public AccountId getNodeId() {
        return nodeId;
    }

    /**
     * Extract the hash of the transaction that was submitted.
     *
     * @return the transaction hash
     */
    public byte[] getTransactionHash() {
        return transactionHash;
    }

    /**
     * Extract the ID of the transaction that was submitted.
     *
     * @return the transaction ID
     */
    public TransactionId getTransactionId() {
        return transactionId;
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
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 1; ; attempt++) {
            try {
                // Attempt to execute the receipt query against the currently bound transaction ID
                return getReceiptQuery(client).execute(client, timeout).validateStatus(validateStatus);
            } catch (ReceiptStatusException e) {
                // Anything other than a consensus throttle, an exhausted retry budget, or a transaction we
                // must not resubmit is reported to the caller as-is
                if (e.receipt.status != Status.THROTTLED_AT_CONSENSUS
                        || attempt >= MAX_RETRY_ATTEMPTS
                        || !canResubmitThrottled(client)) {
                    throw e;
                }
            }

            try {
                // Wait with exponential backoff before resubmitting
                Thread.sleep(Math.min(backoffMs, MAX_BACKOFF_MS));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry on throttled status interrupted", ie);
            }
            backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);

            // Resubmit under a new transaction ID and rebind this response to it, so the next iteration
            // queries the receipt of the attempt that actually ran
            resubmitThrottled(client, timeout);
        }
    }

    /**
     * Whether a transaction that finalized as {@link Status#THROTTLED_AT_CONSENSUS} may be resubmitted under a
     * freshly generated transaction ID.
     *
     * @param client the client the receipt is being fetched with
     * @return whether the transaction may be resubmitted
     */
    private boolean canResubmitThrottled(Client client) {
        if (transaction == null) {
            // this response was not produced by an SDK-managed execute(), so there is nothing to resubmit
            return false;
        }

        if (Boolean.FALSE.equals(transaction.getRegenerateTransactionId())) {
            // the caller explicitly opted out of transaction ID regeneration
            return false;
        }

        if (!transaction.canRegenerateSignatures()) {
            // signatures supplied via addSignature() cannot be reproduced over a new body, so the
            // resubmission would be under-signed
            return false;
        }

        // regenerateTransactionId() always mints an operator-payer ID; resubmitting a transaction paid by
        // anyone else would silently switch the fee payer
        var operatorId = client.getOperatorAccountId();
        return operatorId != null && operatorId.equals(transactionId.accountId);
    }

    /**
     * Resubmit the transaction under a freshly generated transaction ID and rebind this response to the new
     * attempt, so {@link #getReceiptQuery(Client)}, {@link #getRecordQuery(Client)} and {@link #getRecord(Client)}
     * all follow the transaction that actually ran.
     *
     * @param client  the client to resubmit with
     * @param timeout the timeout after which the execution attempt will be cancelled
     */
    private void resubmitThrottled(Client client, Duration timeout) throws PrecheckStatusException, TimeoutException {
        var tx = Objects.requireNonNull(transaction);

        // reset the transaction body so execute() re-freezes and re-signs it under the new transaction ID
        tx.frozenBodyBuilder = null;
        // regenerate the transaction id
        tx.regenerateTransactionId(client);

        var response = (TransactionResponse) tx.execute(client, timeout);

        this.transactionId = response.transactionId;
        this.nodeId = response.nodeId;
        this.transactionHash = response.transactionHash;
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
     * Create receipt query from the {@link #transactionId} and {@link #transactionHash}
     *
     * @return {@link com.hedera.hashgraph.sdk.TransactionReceiptQuery}
     */
    public TransactionReceiptQuery getReceiptQuery(Client client) {
        List<AccountId> nodeIds = new ArrayList<>(List.of(nodeId));
        if (client != null && client.isAllowReceiptNodeFailover()) {
            nodeIds.addAll(client.getNetwork().values().stream()
                    .filter(id -> !id.equals(nodeId))
                    .toList());
        }

        return new TransactionReceiptQuery().setTransactionId(transactionId).setNodeAccountIds(nodeIds);
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
        return getReceiptQuery(client).executeAsync(client, timeout).thenCompose(receipt -> {
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
        // getReceipt() rebinds this response if the transaction had to be resubmitted, so the record query
        // below is built from the transaction that actually ran
        getReceipt(client, timeout);
        return getRecordQuery(client).execute(client, timeout).validateReceiptStatus(validateStatus);
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
     * Create record query from the {@link #transactionId} and {@link #transactionHash}
     *
     * @return {@link com.hedera.hashgraph.sdk.TransactionRecordQuery}
     */
    public TransactionRecordQuery getRecordQuery(Client client) {
        List<AccountId> nodeIds = new ArrayList<>(List.of(nodeId));
        if (client != null && client.isAllowReceiptNodeFailover()) {
            nodeIds.addAll(client.getNetwork().values().stream()
                    .filter(id -> !id.equals(nodeId))
                    .toList());
        }

        return new TransactionRecordQuery().setTransactionId(transactionId).setNodeAccountIds(nodeIds);
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
        return getReceiptAsync(client, timeout).thenCompose((receipt) -> getRecordQuery(client)
                .executeAsync(client, timeout)
                .thenCompose(record -> {
                    try {
                        return CompletableFuture.completedFuture(record.validateReceiptStatus(validateStatus));
                    } catch (ReceiptStatusException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                }));
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

    /**
     * Build a JSON representation of this response
     *
     * @return the JSON object
     */
    JsonObject toJsonObject() {
        var json = new JsonObject();
        json.addProperty("nodeId", nodeId.toString());
        json.addProperty("transactionHash", Hex.toHexString(transactionHash));
        json.addProperty("transactionId", transactionId.toString());
        return json;
    }

    /**
     * Serialize this response to a JSON string, matching the JS SDK's
     * {@code JSON.stringify(response.toJSON())} so all SDKs produce identical JSON.
     *
     * @return the JSON string
     */
    public String toJson() {
        return new GsonBuilder().disableHtmlEscaping().create().toJson(toJsonObject());
    }
}
