// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.Nullable;

/**
 * Query the mirror node for fee estimates for a transaction.
 * <p>
 * This query allows users, SDKs, and tools to estimate expected fees without
 * submitting transactions to the network.
 */
public class FeeEstimateQuery {
    private @Nullable FeeEstimateMode mode = null;

    private com.hedera.hashgraph.sdk.proto.@Nullable Transaction transaction = null;

    private int highVolumeThrottle = 0;
    private int maxAttempts = 10;
    private Duration maxBackoff = Duration.ofSeconds(8L);

    /**
     * Constructor.
     */
    public FeeEstimateQuery() {}

    /**
     * Extract the fee estimate mode.
     *
     * @return the fee estimate mode that was set, or null if not set
     */
    public @Nullable FeeEstimateMode getMode() {
        return mode;
    }

    /**
     * Set the mode for fee estimation.
     * <p>
     * Defaults to {@link FeeEstimateMode#INTRINSIC} if not set.
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
     * Extract the high-volume throttle utilization in basis points.
     *
     * @return the high-volume throttle value (0–10000)
     */
    public int getHighVolumeThrottle() {
        return highVolumeThrottle;
    }

    /**
     * Set the high-volume throttle utilization in basis points (0–10000, where 10000 = 100%).
     * <p>
     * When non-zero, the mirror node returns a high-volume pricing multiplier
     * in the response.
     *
     * @param highVolumeThrottle the throttle utilization in basis points
     * @return {@code this}
     */
    public FeeEstimateQuery setHighVolumeThrottle(int highVolumeThrottle) {
        if (highVolumeThrottle < 0 || highVolumeThrottle > 10000) {
            throw new IllegalArgumentException("highVolumeThrottle must be between 0 and 10000");
        }
        this.highVolumeThrottle = highVolumeThrottle;
        return this;
    }

    /**
     * Extract the transaction to estimate fees for.
     *
     * @return the transaction that was set, or null if not set
     */
    public com.hedera.hashgraph.sdk.proto.@Nullable Transaction getTransaction() {
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
        try {
            return executeAsync(client, timeout).get();
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new RuntimeException(cause);
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
        var resolvedMode = mode != null ? mode : FeeEstimateMode.INTRINSIC;
        return executeAsync(client, timeout, resolvedMode);
    }

    /**
     * Execute the query asynchronously (internal implementation).
     *
     * @param client       the client object
     */
    CompletableFuture<FeeEstimateResponse> executeAsync(Client client, Duration timeout, FeeEstimateMode resolvedMode) {
        var baseUrl = resolveBaseUrl(client);
        var path = buildPath(resolvedMode);
        var policy = client.getMirrorNodeHttpConfig()
                .getRetryPolicy()
                .withMaxAttempts(this.maxAttempts)
                .withMaxBackoff(this.maxBackoff);
        var total = MirrorNodeHttpClient.resolveTotalDeadline(policy, client.getRequestTimeout(), timeout);
        var httpClient = client.newMirrorNodeHttpClient(baseUrl, policy);
        var requestBody = getRequestPayload();

        return CompletableFuture.supplyAsync(
                () -> {
                    HttpResponse response;

                    try {
                        response = httpClient.post(
                                path,
                                "application/protobuf",
                                requestBody,
                                Cancellation.none(),
                                MirrorNodeHttpClient.CallDeadline.startingNow(total));
                    } catch (HttpTransportException e) {
                        var lastResponse = e instanceof MirrorNodeHttpException mirrorNodeHttpException
                                ? mirrorNodeHttpException.getLastResponse()
                                : null;
                        throw new CompletionException(new RuntimeException(
                                "Failed to fetch fee estimation response: "
                                        + e.getKind().getId()
                                        + (lastResponse == null
                                                ? ""
                                                : "(" + MirrorNodeErrorMessage.describe(lastResponse) + ")"),
                                e));
                    }

                    if (response.getStatusCode() != 200) {
                        throw new IllegalStateException("Mirror Node error: HTTP status " + response.getStatusCode()
                                + ": " + MirrorNodeErrorMessage.describe(response));
                    }

                    try {
                        var json = new String(response.getBody(), StandardCharsets.UTF_8);
                        return FeeEstimateResponse.fromJson(json);
                    } catch (RuntimeException e) {
                        throw new IllegalStateException("Mirror Node returned a malformed JSON response", e);
                    }
                },
                client.executor);
    }

    private byte[] getRequestPayload() {
        if (transaction == null) {
            throw new IllegalStateException("transaction must be set before executing fee estimate");
        }

        return transaction.toByteArray();
    }

    private MirrorNodeRestPath buildPath(FeeEstimateMode resolvedMode) {
        // Keep mode casing consistent with JS SDK (uppercase)
        StringBuilder url = new StringBuilder("/network/fees?mode=")
                .append(URLEncoder.encode(resolvedMode.toString(), StandardCharsets.UTF_8));

        if (highVolumeThrottle > 0) {
            url.append("&high_volume_throttle=").append(highVolumeThrottle);
        }

        return MirrorNodeRestPath.of(url.toString());
    }

    private String resolveBaseUrl(Client client) {
        String baseUrl = client.getMirrorRestBaseUrl();

        // For localhost use the java-rest api port
        if (baseUrl.contains("localhost:38081") || baseUrl.contains("127.0.0.1:38081")) {
            baseUrl = baseUrl.replace(":38081", ":8084");
        }
        return baseUrl;
    }
}
