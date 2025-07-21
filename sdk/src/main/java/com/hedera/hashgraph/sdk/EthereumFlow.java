// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Hex;

/**
 * Execute an Ethereum transaction on Hedera
 */
@Deprecated
// With the introduction of jumbo transactions, it should always be less cost and more efficient to use
// EthereumTransaction instead.
public class EthereumFlow {
    /**
     * 128,000 bytes - jumbo transaction limit
     * Indicates when we should splice out the call data from an ethereum transaction data
     */
    static int MAX_ETHEREUM_DATA_SIZE = 128_000;

    @Nullable
    private EthereumTransactionData ethereumData;

    @Nullable
    private FileId callDataFileId;

    @Nullable
    private Hbar maxGasAllowance;

    /**
     * Constructor
     */
    public EthereumFlow() {}

    private static FileId createFile(
            byte[] callData, Client client, Duration timeoutPerTransaction, Transaction<?> ethereumTransaction)
            throws PrecheckStatusException, TimeoutException {
        try {
            // Hex encode the call data
            byte[] callDataHex = Hex.encode(callData);

            var transaction = new FileCreateTransaction()
                    .setKeys(Objects.requireNonNull(client.getOperatorPublicKey()))
                    .setContents(Arrays.copyOfRange(
                            callDataHex, 0, Math.min(FileAppendTransaction.DEFAULT_CHUNK_SIZE, callDataHex.length)))
                    .execute(client, timeoutPerTransaction);
            var fileId = transaction.getReceipt(client, timeoutPerTransaction).fileId;
            var nodeId = transaction.nodeId;
            if (callDataHex.length > FileAppendTransaction.DEFAULT_CHUNK_SIZE) {
                new FileAppendTransaction()
                        .setFileId(fileId)
                        .setMaxChunks(1000)
                        .setNodeAccountIds(Collections.singletonList(nodeId))
                        .setContents(Arrays.copyOfRange(
                                callDataHex, FileAppendTransaction.DEFAULT_CHUNK_SIZE, callDataHex.length))
                        .execute(client, timeoutPerTransaction)
                        .getReceipt(client);
            }

            ethereumTransaction.setNodeAccountIds(Collections.singletonList(nodeId));

            return fileId;
        } catch (ReceiptStatusException e) {
            throw new RuntimeException(e);
        }
    }

    private static CompletableFuture<FileId> createFileAsync(
            byte[] callData, Client client, Duration timeoutPerTransaction, Transaction<?> ethereumTransaction) {
        // Hex encode the call data
        byte[] callDataHex = Hex.encode(callData);

        return new FileCreateTransaction()
                .setKeys(Objects.requireNonNull(client.getOperatorPublicKey()))
                .setContents(Arrays.copyOfRange(
                        callDataHex, 0, Math.min(FileAppendTransaction.DEFAULT_CHUNK_SIZE, callDataHex.length)))
                .executeAsync(client, timeoutPerTransaction)
                .thenCompose((response) -> {
                    var nodeId = response.nodeId;
                    ethereumTransaction.setNodeAccountIds(Collections.singletonList(nodeId));

                    return response.getReceiptAsync(client, timeoutPerTransaction)
                            .thenCompose((receipt) -> {
                                if (callDataHex.length > FileAppendTransaction.DEFAULT_CHUNK_SIZE) {
                                    return new FileAppendTransaction()
                                            .setFileId(receipt.fileId)
                                            .setNodeAccountIds(Collections.singletonList(nodeId))
                                            .setMaxChunks(1000)
                                            .setContents(Arrays.copyOfRange(
                                                    callDataHex,
                                                    FileAppendTransaction.DEFAULT_CHUNK_SIZE,
                                                    callDataHex.length))
                                            .executeAsync(client, timeoutPerTransaction)
                                            .thenCompose((appendResponse) ->
                                                    appendResponse.getReceiptAsync(client, timeoutPerTransaction))
                                            .thenApply((r) -> receipt.fileId);
                                } else {
                                    return CompletableFuture.completedFuture(receipt.fileId);
                                }
                            });
                });
    }

    /**
     * Gets the data of the Ethereum transaction
     *
     * @return the data of the Ethereum transaction
     */
    @Nullable
    public EthereumTransactionData getEthereumData() {
        return ethereumData;
    }

    /**
     * Sets the raw Ethereum transaction (RLP encoded type 0, 1, and 2). Complete
     * unless the callDataFileId is set.
     *
     * @param ethereumData raw ethereum transaction bytes
     * @return {@code this}
     */
    public EthereumFlow setEthereumData(byte[] ethereumData) {
        this.ethereumData = EthereumTransactionData.fromBytes(ethereumData);
        return this;
    }

    /**
     * Gets the maximum amount that the payer of the hedera transaction
     * is willing to pay to complete the transaction.
     *
     * @return the max gas allowance
     */
    @Nullable
    public Hbar getMaxGasAllowance() {
        return maxGasAllowance;
    }

    /**
     * Sets the maximum amount that the payer of the hedera transaction
     * is willing to pay to complete the transaction.
     * <br>
     * Ordinarily the account with the ECDSA alias corresponding to the public
     * key that is extracted from the ethereum_data signature is responsible for
     * fees that result from the execution of the transaction. If that amount of
     * authorized fees is not sufficient then the payer of the transaction can be
     * charged, up to but not exceeding this amount. If the ethereum_data
     * transaction authorized an amount that was insufficient then the payer will
     * only be charged the amount needed to make up the difference. If the gas
     * price in the transaction was set to zero then the payer will be assessed
     * the entire fee.
     *
     * @param maxGasAllowance the maximum gas allowance
     * @return {@code this}
     */
    public EthereumFlow setMaxGasAllowance(Hbar maxGasAllowance) {
        this.maxGasAllowance = maxGasAllowance;
        return this;
    }

    /**
     * Execute the transactions in the flow with the passed in client.
     *
     * @param client the client with the transaction to execute
     * @return the response
     * @throws PrecheckStatusException when the precheck fails
     * @throws TimeoutException        when the transaction times out
     */
    public TransactionResponse execute(Client client) throws PrecheckStatusException, TimeoutException {
        return execute(client, client.getRequestTimeout());
    }

    /**
     * Execute the transactions in the flow with the passed in client.
     *
     * @param client                the client with the transaction to execute
     * @param timeoutPerTransaction The timeout after which each transaction's execution attempt will be cancelled.
     * @return the response
     * @throws PrecheckStatusException when the precheck fails
     * @throws TimeoutException        when the transaction times out
     */
    public TransactionResponse execute(Client client, Duration timeoutPerTransaction)
            throws PrecheckStatusException, TimeoutException {
        if (ethereumData == null) {
            throw new IllegalStateException("Cannot execute a ethereum flow when ethereum data was not provided");
        }

        var ethereumTransaction = new EthereumTransaction();
        var ethereumDataBytes = ethereumData.toBytes();

        if (maxGasAllowance != null) {
            ethereumTransaction.setMaxGasAllowanceHbar(maxGasAllowance);
        }

        if (ethereumDataBytes.length <= MAX_ETHEREUM_DATA_SIZE) {
            ethereumTransaction.setEthereumData(ethereumDataBytes);
        } else {
            var callDataFileId = createFile(ethereumData.callData, client, timeoutPerTransaction, ethereumTransaction);
            ethereumData.callData = new byte[] {};
            ethereumTransaction.setEthereumData(ethereumData.toBytes()).setCallDataFileId(callDataFileId);
        }
        return ethereumTransaction.execute(client, timeoutPerTransaction);
    }

    /**
     * Execute the transactions in the flow with the passed in client asynchronously.
     *
     * <p>Note: This method requires API level 33 or higher. It will not work on devices running API versions below 31
     * because it uses features introduced in API level 31 (Android 12).</p>*
     *
     * @param client the client with the transaction to execute
     * @return the response
     */
    public CompletableFuture<TransactionResponse> executeAsync(Client client) {
        return executeAsync(client, client.getRequestTimeout());
    }

    /**
     * Execute the transactions in the flow with the passed in client asynchronously.
     *
     * <p>Note: This method requires API level 33 or higher. It will not work on devices running API versions below 31
     * because it uses features introduced in API level 31 (Android 12).</p>*
     *
     * @param client                the client with the transaction to execute
     * @param timeoutPerTransaction The timeout after which each transaction's execution attempt will be cancelled.
     * @return the response
     */
    public CompletableFuture<TransactionResponse> executeAsync(Client client, Duration timeoutPerTransaction) {
        if (ethereumData == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Cannot execute a ethereum flow when ethereum data was not provided"));
        }

        var ethereumTransaction = new EthereumTransaction();
        var ethereumDataBytes = ethereumData.toBytes();

        if (maxGasAllowance != null) {
            ethereumTransaction.setMaxGasAllowanceHbar(maxGasAllowance);
        }

        if (ethereumDataBytes.length <= MAX_ETHEREUM_DATA_SIZE) {
            return ethereumTransaction.setEthereumData(ethereumDataBytes).executeAsync(client);
        } else {
            return createFileAsync(ethereumData.callData, client, timeoutPerTransaction, ethereumTransaction)
                    .thenCompose((callDataFileId) -> ethereumTransaction
                            .setEthereumData(ethereumData.toBytes())
                            .setCallDataFileId(callDataFileId)
                            .executeAsync(client, timeoutPerTransaction));
        }
    }

    /**
     * Execute the transactions in the flow with the passed in client asynchronously.
     *
     * @param client   the client with the transaction to execute
     * @param callback a BiConsumer which handles the result or error.
     */
    public void executeAsync(Client client, BiConsumer<TransactionResponse, Throwable> callback) {
        ConsumerHelper.biConsumer(executeAsync(client), callback);
    }

    /**
     * Execute the transactions in the flow with the passed in client asynchronously.
     *
     * @param client                the client with the transaction to execute
     * @param timeoutPerTransaction The timeout after which each transaction's execution attempt will be cancelled.
     * @param callback              a BiConsumer which handles the result or error.
     */
    public void executeAsync(
            Client client, Duration timeoutPerTransaction, BiConsumer<TransactionResponse, Throwable> callback) {
        ConsumerHelper.biConsumer(executeAsync(client, timeoutPerTransaction), callback);
    }

    /**
     * Execute the transactions in the flow with the passed in client asynchronously.
     *
     * @param client    the client with the transaction to execute
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void executeAsync(Client client, Consumer<TransactionResponse> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(executeAsync(client), onSuccess, onFailure);
    }

    /**
     * Execute the transactions in the flow with the passed in client asynchronously.
     *
     * @param client                the client with the transaction to execute
     * @param timeoutPerTransaction The timeout after which each transaction's execution attempt will be cancelled.
     * @param onSuccess             a Consumer which consumes the result on success.
     * @param onFailure             a Consumer which consumes the error on failure.
     */
    public void executeAsync(
            Client client,
            Duration timeoutPerTransaction,
            Consumer<TransactionResponse> onSuccess,
            Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(executeAsync(client, timeoutPerTransaction), onSuccess, onFailure);
    }
}
