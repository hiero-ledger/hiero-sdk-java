// SPDX-License-Identifier:  Apache-2.0
package com.hedera.hashgraph.sdk;

import java.io.IOException;
import java. net.URI;
import java.net.http.HttpClient;
import java. net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java. util.Objects;
import java. util.concurrent.CompletableFuture;
import javax.annotation. Nullable;
import org. slf4j.Logger;
import org. slf4j.LoggerFactory;

/**
 * Query the mirror node for fee estimates for a transaction.
 * <p>
 * This query allows users, SDKs, and tools to estimate expected fees without
 * submitting transactions to the network.
 */
public class FeeEstimateQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeeEstimateQuery.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Nullable
    private FeeEstimateMode mode = null;

    @Nullable
    private com.hedera.hashgraph.sdk. proto.Transaction transaction = null;

    private int maxAttempts = 10;
    private Duration maxBackoff = Duration. ofSeconds(8L);

    /**
     * Constructor.
     */
    public FeeEstimateQuery() {}

    private static boolean shouldRetry(Throwable throwable) {
        return throwable instanceof HttpTimeoutException || throwable instanceof IOException;
    }

    private static boolean shouldRetry(int statusCode) {
        // Retry on common transient HTTP statuses
        return statusCode == 408 || statusCode == 429 || (statusCode >= 500 && statusCode < 600);
    }

    private static boolean isSuccessfulResponse(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
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
    public com. hedera.hashgraph.sdk.proto.Transaction getTransaction() {
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
    public FeeEstimateQuery setTransaction(com.hedera. hashgraph.sdk.proto.Transaction transaction) {
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
        com.hedera.hashgraph.sdk. Transaction<T> transaction) {
        Objects.requireNonNull(transaction);
        this.transaction = transaction. makeRequest();
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
        if (maxBackoff. toMillis() < 500L) {
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
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public FeeEstimateResponse execute(Client client) throws IOException, InterruptedException {
        return execute(client, client.getRequestTimeout());
    }

    /**
     * Execute the query with user supplied timeout.
     *
     * @param client  the client object
     * @param timeout the user supplied timeout
     * @return the fee estimate response
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public FeeEstimateResponse execute(Client client, Duration timeout) throws IOException, InterruptedException {
        var resolvedMode = mode != null ? mode :  FeeEstimateMode.STATE;
        var requestPayload = getRequestPayload();
        var url = buildUrl(client, resolvedMode);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                var response = HTTP_CLIENT.send(
                    buildHttpRequest(url, timeout, requestPayload), HttpResponse.BodyHandlers. ofString());

                var result = handleResponse(response, resolvedMode, attempt);
                if (result != null) {
                    return result;
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception error) {
                handleError(error, attempt);
            }
        }
        throw new RuntimeException("Failed to fetch fee estimate after " + maxAttempts + " attempts");
    }

    /**
     * Handle the HTTP response and return the result or null if retry is needed.
     */
    private FeeEstimateResponse handleResponse(
        HttpResponse<String> response, FeeEstimateMode resolvedMode, int attempt) {
        if (isSuccessfulResponse(response.statusCode())) {
            return FeeEstimateResponse.fromJson(response.body(), resolvedMode);
        }

        if (! shouldRetry(response.statusCode()) || attempt >= maxAttempts) {
            throw new RuntimeException("Failed to fetch fee estimate. HTTP status: " + response.statusCode()
                + " body: " + response.body());
        }

        warnAndDelay(attempt, new RuntimeException("HTTP status: " + response.statusCode()));
        return null;
    }

    /**
     * Handle errors during execution.
     */
    private void handleError(Exception error, int attempt) throws IOException, InterruptedException {
        if (! shouldRetry(error) || attempt >= maxAttempts) {
            LOGGER.error("Error attempting to get fee estimate", error);
            if (error instanceof IOException ioException) {
                throw ioException;
            }
            if (error instanceof InterruptedException interruptedException) {
                throw interruptedException;
            }
            throw new RuntimeException(error);
        }
        warnAndDelay(attempt, error);
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
        var resolvedMode = mode != null ? mode :  FeeEstimateMode.STATE;
        CompletableFuture<FeeEstimateResponse> returnFuture = new CompletableFuture<>();
        executeAsync(client, timeout, resolvedMode, returnFuture, 1);
        return returnFuture;
    }

    /**
     * Execute the query asynchronously (internal implementation).
     *
     * @param client       the client object
     * @param returnFuture the future to complete with the result
     * @param attempt      the current attempt number
     */
    void executeAsync(
        Client client,
        Duration timeout,
        FeeEstimateMode resolvedMode,
        CompletableFuture<FeeEstimateResponse> returnFuture,
        int attempt) {
        var requestPayload = getRequestPayload();
        var url = buildUrl(client, resolvedMode);

        HTTP_CLIENT
            .sendAsync(buildHttpRequest(url, timeout, requestPayload), HttpResponse.BodyHandlers.ofString())
            .whenComplete((response, error) -> {
                if (error != null) {
                    handleAsyncError(client, timeout, resolvedMode, returnFuture, attempt, error);
                    return;
                }
                handleAsyncResponse(client, timeout, resolvedMode, returnFuture, attempt, response);
            });
    }

    /**
     * Handle async error response.
     */
    private void handleAsyncError(
        Client client,
        Duration timeout,
        FeeEstimateMode resolvedMode,
        CompletableFuture<FeeEstimateResponse> returnFuture,
        int attempt,
        Throwable error) {
        if (attempt >= maxAttempts || !shouldRetry(error)) {
            LOGGER.error("Error attempting to get fee estimate", error);
            returnFuture.completeExceptionally(error);
            return;
        }
        warnAndDelay(attempt, error);
        executeAsync(client, timeout, resolvedMode, returnFuture, attempt + 1);
    }

    /**
     * Handle async success response.
     */
    private void handleAsyncResponse(
        Client client,
        Duration timeout,
        FeeEstimateMode resolvedMode,
        CompletableFuture<FeeEstimateResponse> returnFuture,
        int attempt,
        HttpResponse<String> response) {
        if (isSuccessfulResponse(response.statusCode())) {
            returnFuture.complete(FeeEstimateResponse.fromJson(response.body(), resolvedMode));
            return;
        }

        if (attempt >= maxAttempts || !shouldRetry(response.statusCode())) {
            LOGGER.error(
                "Failed to fetch fee estimate. HTTP status: {} body: {}",
                response.statusCode(),
                response.body());
            returnFuture.completeExceptionally(
                new RuntimeException("Failed to fetch fee estimate, status " + response. statusCode()));
            return;
        }

        warnAndDelay(attempt, new RuntimeException("Transient HTTP status: " + response.statusCode()));
        executeAsync(client, timeout, resolvedMode, returnFuture, attempt + 1);
    }

    /**
     * Build the FeeEstimateQuery protobuf message.
     *
     * @return the protobuf query
     */
    HttpRequest buildRequest(Client client, Duration timeout, FeeEstimateMode resolvedMode) {
        String url = buildUrl(client, resolvedMode);
        return buildHttpRequest(url, timeout, getRequestPayload());
    }

    private byte[] getRequestPayload() {
        if (transaction == null) {
            throw new IllegalStateException("transaction must be set before executing fee estimate");
        }

        return transaction.toByteArray();
    }

    private String buildUrl(Client client, FeeEstimateMode resolvedMode) {
        // Keep mode casing consistent with JS SDK (uppercase)
        return client.getMirrorRestBaseUrl() + "/network/fees?mode=" + resolvedMode.toString();
    }

    private HttpRequest buildHttpRequest(String url, Duration timeout, byte[] payload) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout)
            .header("Content-Type", "application/protobuf")
            .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
            .build();
    }

    private void warnAndDelay(int attempt, Throwable error) {
        var delay = Math.min(500 * (long) Math.pow(2, attempt), maxBackoff.toMillis());
        LOGGER.warn(
            "Error fetching fee estimate during attempt #{}. Waiting {} ms before next attempt: {}",
            attempt,
            delay,
            error. getMessage());

        try {
            Thread. sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
