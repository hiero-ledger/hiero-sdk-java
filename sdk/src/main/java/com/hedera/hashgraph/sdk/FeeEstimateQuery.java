// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.mirror.NetworkServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Deadline;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query the mirror node for fee estimates for a transaction.
 * <p>
 * This query allows users, SDKs, and tools to estimate expected fees without
 * submitting transactions to the network.
 */
public class FeeEstimateQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeeEstimateQuery.class);

    @Nullable
    private FeeEstimateMode mode = null;

    @Nullable
    private com.hedera.hashgraph.sdk.proto.Transaction transaction = null;

    private int maxAttempts = 10;
    private Duration maxBackoff = Duration.ofSeconds(8L);

    /**
     * Constructor.
     */
    public FeeEstimateQuery() {}

    private static boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException statusRuntimeException) {
            var code = statusRuntimeException.getStatus().getCode();
            var description = statusRuntimeException.getStatus().getDescription();

            return (code == io.grpc.Status.Code.UNAVAILABLE)
                    || (code == io.grpc.Status.Code.DEADLINE_EXCEEDED)
                    || (code == io.grpc.Status.Code.RESOURCE_EXHAUSTED)
                    || (code == Status.Code.INTERNAL
                            && description != null
                            && Executable.RST_STREAM.matcher(description).matches());
        }

        return false;
    }

    /**
     * Extract the fee estimate mode.
     *
     * @return the fee estimate mode that was set, or null if not set
     */
    @Nullable
    public FeeEstimateMode getMode() {
        return mode;
    }

    /**
     * Set the mode for fee estimation.
     * <p>
     * Defaults to {@link FeeEstimateMode#STATE} if not set.
     *
     * @param mode the fee estimate mode
     * @return {@code this}
     */
    public FeeEstimateQuery setMode(FeeEstimateMode mode) {
        Objects.requireNonNull(mode);
        this.mode = mode;
        return this;
    }

    /**
     * Extract the transaction to estimate fees for.
     *
     * @return the transaction that was set, or null if not set
     */
    @Nullable
    public com.hedera.hashgraph.sdk.proto.Transaction getTransaction() {
        return transaction;
    }

    /**
     * Set the transaction to estimate fees for.
     * <p>
     * This should be the raw HAPI transaction that will be estimated.
     *
     * @param transaction the transaction proto
     * @return {@code this}
     */
    public FeeEstimateQuery setTransaction(com.hedera.hashgraph.sdk.proto.Transaction transaction) {
        Objects.requireNonNull(transaction);
        this.transaction = transaction;
        return this;
    }

    /**
     * Set the transaction to estimate fees for from a Transaction object.
     *
     * @param transaction the transaction to estimate
     * @return {@code this}
     */
    public <T extends com.hedera.hashgraph.sdk.Transaction<T>> FeeEstimateQuery setTransaction(
            com.hedera.hashgraph.sdk.Transaction<T> transaction) {
        Objects.requireNonNull(transaction);
        this.transaction = transaction.makeRequest();
        return this;
    }

    /**
     * Extract the maximum number of attempts.
     *
     * @return the maximum number of attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Set the maximum number of attempts for the query.
     *
     * @param maxAttempts the maximum number of attempts
     * @return {@code this}
     */
    public FeeEstimateQuery setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    /**
     * Extract the maximum backoff duration.
     *
     * @return the maximum backoff duration
     */
    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    /**
     * Set the maximum backoff duration for retry attempts.
     *
     * @param maxBackoff the maximum backoff duration
     * @return {@code this}
     */
    public FeeEstimateQuery setMaxBackoff(Duration maxBackoff) {
        Objects.requireNonNull(maxBackoff);
        if (maxBackoff.toMillis() < 500L) {
            throw new IllegalArgumentException("maxBackoff must be at least 500 ms");
        }
        this.maxBackoff = maxBackoff;
        return this;
    }

    /**
     * Execute the query with preset timeout.
     *
     * @param client the client object
     * @return the fee estimate response
     */
    public FeeEstimateResponse execute(Client client) {
        return execute(client, client.getRequestTimeout());
    }

    /**
     * Execute the query with user supplied timeout.
     *
     * @param client  the client object
     * @param timeout the user supplied timeout
     * @return the fee estimate response
     */
    public FeeEstimateResponse execute(Client client, Duration timeout) {
        var deadline = Deadline.after(timeout.toMillis(), TimeUnit.MILLISECONDS);
        for (int attempt = 1; true; attempt++) {
            try {
                var responseProto = ClientCalls.blockingUnaryCall(buildCall(client, deadline), buildQuery());
                return FeeEstimateResponse.fromProtobuf(responseProto);
            } catch (Throwable error) {
                if (!shouldRetry(error) || attempt >= maxAttempts) {
                    LOGGER.error("Error attempting to get fee estimate", error);
                    throw error;
                }
                warnAndDelay(attempt, error);
            }
        }
    }

    /**
     * Execute the query with preset timeout asynchronously.
     *
     * @param client the client object
     * @return the fee estimate response
     */
    public CompletableFuture<FeeEstimateResponse> executeAsync(Client client) {
        return executeAsync(client, client.getRequestTimeout());
    }

    /**
     * Execute the query with user supplied timeout asynchronously.
     *
     * @param client  the client object
     * @param timeout the user supplied timeout
     * @return the fee estimate response
     */
    public CompletableFuture<FeeEstimateResponse> executeAsync(Client client, Duration timeout) {
        var deadline = Deadline.after(timeout.toMillis(), TimeUnit.MILLISECONDS);
        CompletableFuture<FeeEstimateResponse> returnFuture = new CompletableFuture<>();
        executeAsync(client, deadline, returnFuture, 1);
        return returnFuture;
    }

    /**
     * Execute the query asynchronously (internal implementation).
     *
     * @param client       the client object
     * @param deadline     the deadline for the call
     * @param returnFuture the future to complete with the result
     * @param attempt      the current attempt number
     */
    void executeAsync(
            Client client, Deadline deadline, CompletableFuture<FeeEstimateResponse> returnFuture, int attempt) {
        ClientCalls.asyncUnaryCall(
                buildCall(client, deadline),
                buildQuery(),
                new io.grpc.stub.StreamObserver<com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse>() {
                    @Override
                    public void onNext(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse response) {
                        returnFuture.complete(FeeEstimateResponse.fromProtobuf(response));
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (attempt >= maxAttempts || !shouldRetry(error)) {
                            LOGGER.error("Error attempting to get fee estimate", error);
                            returnFuture.completeExceptionally(error);
                            return;
                        }
                        warnAndDelay(attempt, error);
                        executeAsync(client, deadline, returnFuture, attempt + 1);
                    }

                    @Override
                    public void onCompleted() {
                        // Response already handled in onNext
                    }
                });
    }

    /**
     * Build the FeeEstimateQuery protobuf message.
     *
     * @return the protobuf query
     */
    com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery buildQuery() {
        var builder = com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery.newBuilder();

        if (mode != null) {
            builder.setModeValue(mode.code);
        } else {
            // Default to STATE mode
            builder.setModeValue(FeeEstimateMode.STATE.code);
        }

        if (transaction != null) {
            builder.setTransaction(transaction);
        }

        return builder.build();
    }

    private ClientCall<
                    com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateQuery,
                    com.hedera.hashgraph.sdk.proto.mirror.FeeEstimateResponse>
            buildCall(Client client, Deadline deadline) {
        try {
            return client.mirrorNetwork
                    .getNextMirrorNode()
                    .getChannel()
                    .newCall(NetworkServiceGrpc.getGetFeeEstimateMethod(), CallOptions.DEFAULT.withDeadline(deadline));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void warnAndDelay(int attempt, Throwable error) {
        var delay = Math.min(500 * (long) Math.pow(2, attempt), maxBackoff.toMillis());
        LOGGER.warn(
                "Error fetching fee estimate during attempt #{}. Waiting {} ms before next attempt: {}",
                attempt,
                delay,
                error.getMessage());

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
