// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.io.BaseEncoding;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get the HBAR balance of an account from the mirror node REST API
 * ({@code GET /api/v1/balances?account.id=...}).
 *
 * <p>This is the replacement for {@link AccountBalanceQuery}, which relies on the consensus node
 * {@code CryptoService/cryptoGetBalance} endpoint that the network is retiring.
 *
 * <p>The account may be identified by {@code shard.realm.num}, by an EVM address, or by a public key
 * alias — the mirror node resolves all three. Contract IDs are also accepted; pass them through
 * {@link #setAccountId(AccountId)}, as the balances endpoint supports them directly and no separate
 * setter is needed.
 *
 * <p>Only the HBAR balance is returned. For token balances use {@link MirrorNodeTokenBalanceQuery},
 * which reads one token at a time.
 *
 * <p><b>Eventual consistency:</b> the mirror node reflects network state with a small lag, typically
 * a few seconds. Applications that need an immediate post-transaction balance must allow for it.
 *
 * <p>This query is free.
 */
public final class MirrorNodeAccountBalanceQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(MirrorNodeAccountBalanceQuery.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Nullable
    private AccountId accountId = null;

    private int maxAttempts = 10;
    private Duration maxBackoff = Duration.ofSeconds(8L);

    /**
     * Constructor.
     */
    public MirrorNodeAccountBalanceQuery() {}

    /**
     * Return the account's id.
     *
     * @return {@code accountId}
     */
    @Nullable
    public AccountId getAccountId() {
        return accountId;
    }

    /**
     * The ID of the account for which the balance is being requested.
     *
     * <p>Accepts {@code shard.realm.num}, an EVM address, or a public key alias. Contract IDs are
     * also accepted — the balances endpoint resolves them, so no separate setter is needed; convert
     * with {@code new AccountId(contractId.shard, contractId.realm, contractId.num)}.
     *
     * @param accountId the account id to set
     * @return {@code this}
     */
    public MirrorNodeAccountBalanceQuery setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        this.accountId = accountId;
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
    public MirrorNodeAccountBalanceQuery setMaxAttempts(int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
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
    public MirrorNodeAccountBalanceQuery setMaxBackoff(Duration maxBackoff) {
        Objects.requireNonNull(maxBackoff);
        if (maxBackoff.toMillis() < 500L) {
            throw new IllegalArgumentException("maxBackoff must be at least 500 ms");
        }
        this.maxBackoff = maxBackoff;
        return this;
    }

    private static boolean shouldRetry(Throwable throwable) {
        return throwable instanceof HttpTimeoutException || throwable instanceof IOException;
    }

    private static boolean shouldRetry(int statusCode) {
        return statusCode == 408 || statusCode == 429 || (statusCode >= 500 && statusCode < 600);
    }

    /**
     * Executes the query with the user supplied client.
     *
     * @param client the client with which this will be executed
     * @return the retrieved {@link MirrorNodeAccountBalance}
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public MirrorNodeAccountBalance execute(Client client) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        return execute(client, client.getRequestTimeout());
    }

    /**
     * Executes the query with the user supplied client and timeout.
     *
     * @param client the client with which this will be executed
     * @param timeout the maximum duration for each individual HTTP request
     * @return the retrieved {@link MirrorNodeAccountBalance}
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public MirrorNodeAccountBalance execute(Client client, Duration timeout)
            throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        return executeAsync(client, timeout).get();
    }

    /**
     * Executes the query asynchronously with the user supplied client.
     *
     * @param client the client with which this will be executed
     * @return a future representing the retrieved {@link MirrorNodeAccountBalance}
     */
    public CompletableFuture<MirrorNodeAccountBalance> executeAsync(Client client) {
        Objects.requireNonNull(client, "client must not be null");
        return executeAsync(client, client.getRequestTimeout());
    }

    /**
     * Executes the query asynchronously with the user supplied client and timeout.
     *
     * @param client the client with which this will be executed
     * @param timeout the maximum duration for each individual HTTP request
     * @return a future representing the retrieved {@link MirrorNodeAccountBalance}
     */
    public CompletableFuture<MirrorNodeAccountBalance> executeAsync(Client client, Duration timeout) {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");

        // Validate before scheduling any work so a misconfigured query never reaches the network.
        String url = buildUrl(client);

        return CompletableFuture.supplyAsync(
                () -> MirrorNodeAccountBalance.fromJson(fetch(url, timeout)), client.executor);
    }

    private JsonObject fetch(String url, Duration timeout) {
        String body = fetchBody(url, timeout);

        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Mirror Node returned a malformed JSON response", e);
        }
    }

    private String fetchBody(String url, Duration timeout) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                HttpResponse<String> response =
                        HTTP_CLIENT.send(buildHttpRequest(url, timeout), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return response.body();
                }

                if (!shouldRetry(response.statusCode()) || attempt >= maxAttempts) {
                    throw new IllegalStateException("Mirror Node error: HTTP " + response.statusCode());
                }

                lastException = new RuntimeException("HTTP " + response.statusCode());
                warnAndDelay(attempt, lastException);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                if (attempt >= maxAttempts || !shouldRetry(e)) {
                    throw new RuntimeException("Failed to fetch account balance after " + attempt + " attempts", e);
                }
                warnAndDelay(attempt, lastException);
            }
        }

        throw new RuntimeException("Failed to fetch account balance after " + maxAttempts + " attempts", lastException);
    }

    private String buildUrl(Client client) {
        if (accountId == null) {
            throw new IllegalStateException("accountId must be set before executing MirrorNodeAccountBalanceQuery");
        }

        if (client.isAutoValidateChecksumsEnabled()) {
            try {
                accountId.validateChecksum(client);
            } catch (BadEntityIdException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        return client.getMirrorRestBaseUrl() + "/balances?account.id="
                + URLEncoder.encode(toAccountIdParam(accountId), StandardCharsets.UTF_8);
    }

    /**
     * Render the account id in the form the mirror node's {@code account.id} parameter expects.
     *
     * <p>EVM addresses are sent in their {@code 0x}-prefixed hex form, and plain IDs as
     * {@code shard.realm.num}. A public key alias is sent as the unpadded base32 encoding of the
     * protobuf {@code Key} bytes with no {@code shard.realm.} prefix — the only alias form the mirror
     * node accepts. {@link AccountId#toString()} renders an alias as {@code shard.realm.<DER hex>}
     * instead, which the endpoint rejects with HTTP 400, so it cannot be used here.
     */
    private static String toAccountIdParam(AccountId accountId) {
        if (accountId.evmAddress != null) {
            return "0x" + accountId.evmAddress;
        }

        if (accountId.aliasKey != null) {
            return BaseEncoding.base32()
                    .omitPadding()
                    .encode(accountId.aliasKey.toProtobufKey().toByteArray());
        }

        return accountId.toString();
    }

    private HttpRequest buildHttpRequest(String url, Duration timeout) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .GET()
                .build();
    }

    private void warnAndDelay(int attempt, Throwable error) {
        var delay = Math.min(500 * (long) Math.pow(2, attempt), maxBackoff.toMillis());
        LOGGER.warn(
                "Error fetching account balance during attempt #{}. Waiting {} ms before next attempt: {}",
                attempt,
                delay,
                error.getMessage());

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return "MirrorNodeAccountBalanceQuery{accountId=" + accountId + "}";
    }
}
