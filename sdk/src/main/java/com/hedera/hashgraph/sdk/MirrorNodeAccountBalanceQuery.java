// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.io.BaseEncoding;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.Nullable;

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
 * <p>An account the mirror node does not know fails with a {@link PrecheckStatusException} carrying
 * {@link Status#INVALID_ACCOUNT_ID}, the same error {@link AccountBalanceQuery} reported. Note that the
 * balances endpoint answers with an empty result rather than a 404, so this is the SDK's mapping of that
 * empty result — not a status the mirror node itself returned.
 *
 * <p><b>Eventual consistency:</b> the mirror node reflects network state with a small lag, typically
 * a few seconds. Applications that need an immediate post-transaction balance must allow for it. The lag
 * applies to the account's existence as well as to its balance: a <i>freshly created</i> account can
 * transiently fail with {@link Status#INVALID_ACCOUNT_ID} until the mirror node has ingested it, so code
 * that queries an account right after creating it should retry rather than treat the first failure as
 * final.
 *
 * <p>This query is free.
 */
public final class MirrorNodeAccountBalanceQuery {
    private @Nullable AccountId accountId = null;

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
    public @Nullable AccountId getAccountId() {
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

    /**
     * Executes the query with the user supplied client.
     *
     * @param client the client with which this will be executed
     * @return the retrieved {@link MirrorNodeAccountBalance}
     * @throws PrecheckStatusException with {@link Status#INVALID_ACCOUNT_ID} if the mirror node knows no
     *         such account
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public MirrorNodeAccountBalance execute(Client client)
            throws PrecheckStatusException, ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        return execute(client, client.getRequestTimeout());
    }

    /**
     * Executes the query with the user supplied client and timeout.
     *
     * @param client the client with which this will be executed
     * @param timeout the maximum duration for each individual HTTP request
     * @return the retrieved {@link MirrorNodeAccountBalance}
     * @throws PrecheckStatusException with {@link Status#INVALID_ACCOUNT_ID} if the mirror node knows no
     *         such account
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public MirrorNodeAccountBalance execute(Client client, Duration timeout)
            throws PrecheckStatusException, ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");

        // The future is built outside the try so that the validation errors executeAsync raises
        // synchronously are not mistaken for an execution failure below.
        var future = executeAsync(client, timeout);

        try {
            return future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof PrecheckStatusException precheckStatusException) {
                throw precheckStatusException;
            }
            throw e;
        }
    }

    /**
     * Executes the query asynchronously with the user supplied client.
     *
     * <p>If the mirror node knows no such account the future completes exceptionally with a
     * {@link PrecheckStatusException} carrying {@link Status#INVALID_ACCOUNT_ID} — surfaced as the cause
     * of a {@code CompletionException} from {@code join()}, or of an {@code ExecutionException} from
     * {@code get()}.
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
     * <p>If the mirror node knows no such account the future completes exceptionally with a
     * {@link PrecheckStatusException} carrying {@link Status#INVALID_ACCOUNT_ID} — surfaced as the cause
     * of a {@code CompletionException} from {@code join()}, or of an {@code ExecutionException} from
     * {@code get()}.
     *
     * @param client the client with which this will be executed
     * @param timeout the maximum duration for each individual HTTP request
     * @return a future representing the retrieved {@link MirrorNodeAccountBalance}
     */
    public CompletableFuture<MirrorNodeAccountBalance> executeAsync(Client client, Duration timeout) {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");

        // Validate before scheduling any work so a misconfigured query never reaches the network.
        var path = buildPath(client);
        var policy = client.getMirrorNodeHttpConfig()
                .getRetryPolicy()
                .withMaxAttempts(this.maxAttempts)
                .withMaxBackoff(this.maxBackoff);
        var total = MirrorNodeHttpClient.resolveTotalDeadline(policy, client.getRequestTimeout(), timeout);
        var httpClient = client.newMirrorNodeHttpClient(policy);

        return CompletableFuture.supplyAsync(
                () -> {
                    var json = fetch(httpClient, path, total);
                    var balance = MirrorNodeAccountBalance.fromJson(json);

                    if (balance == null) {
                        // The mirror node answers HTTP 200 with an empty `balances` array for an account
                        // it does not know. Report it the way the consensus node did.
                        throw new CompletionException(new PrecheckStatusException(Status.INVALID_ACCOUNT_ID, null));
                    }

                    return balance;
                },
                client.executor);
    }

    private JsonObject fetch(MirrorNodeHttpClient httpClient, MirrorNodeRestPath path, Duration total) {
        HttpResponse response;

        try {
            response = httpClient.get(path, Cancellation.none(), MirrorNodeHttpClient.CallDeadline.startingNow(total));
        } catch (HttpTransportException e) {
            var lastResponse = e instanceof MirrorNodeHttpException mirrorNodeHttpException
                    ? mirrorNodeHttpException.getLastResponse()
                    : null;
            throw new CompletionException(new RuntimeException(
                    "Failed to fetch account balance: " + e.getKind().getId()
                            + (lastResponse == null ? "" : "(" + MirrorNodeErrorMessage.describe(lastResponse) + ")"),
                    e));
        }

        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Mirror Node error: HTTP " + response.getStatusCode() + ": "
                    + MirrorNodeErrorMessage.describe(response));
        }

        try {
            return JsonParser.parseString(new String(response.getBody(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Mirror Node returned a malformed JSON response", e);
        }
    }

    private MirrorNodeRestPath buildPath(Client client) {
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

        return MirrorNodeRestPath.of(
                "/balances?account.id=" + URLEncoder.encode(toAccountIdParam(accountId), StandardCharsets.UTF_8));
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

    @Override
    public String toString() {
        return "MirrorNodeAccountBalanceQuery{accountId=" + accountId + "}";
    }
}
