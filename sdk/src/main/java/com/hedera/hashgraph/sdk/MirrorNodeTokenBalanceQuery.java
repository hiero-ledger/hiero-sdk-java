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
import javax.annotation.Nullable;

/**
 * Get the balance an account holds in a single token from the mirror node REST API
 * ({@code GET /api/v1/accounts/{id}/tokens?token.id=...}).
 *
 * <p>This is the token-balance counterpart to {@link MirrorNodeAccountBalanceQuery}, which returns HBAR.
 * Together they replace {@link AccountBalanceQuery}.
 *
 * <p>Both {@link #setAccountId(AccountId)} and {@link #setTokenId(TokenId)} are required, so the query
 * is always bounded to a single relationship and never paginates. Reading several tokens for one
 * account means issuing several queries -- this is deliberate: enumerating a whole token portfolio
 * would require unbounded pagination against an account with an arbitrarily large number of tokens.
 *
 * <p>The returned {@link MirrorNodeTokenBalance#balance} is the amount in the token's smallest
 * denomination for {@code FUNGIBLE_COMMON} tokens, and the number of NFTs held for
 * {@code NON_FUNGIBLE_UNIQUE} tokens. If the entity holds no relationship to the token, the balance is
 * zero and {@link MirrorNodeTokenBalance#isAssociated()} is false.
 *
 * <p>The account may be identified by {@code shard.realm.num}, by an EVM address, or by a public key
 * alias -- the mirror node resolves all three. Contract IDs are also accepted; pass them through
 * {@link #setAccountId(AccountId)}, converting with
 * {@code new AccountId(contractId.shard, contractId.realm, contractId.num)}.
 *
 * <p>An entity the mirror node does not know -- because it does not exist, or because the mirror node
 * has not ingested it yet -- reports {@code isAssociated() == false} with a zero balance rather than
 * raising an error, matching how {@link MirrorNodeAccountBalanceQuery} reports zero for an unknown
 * account. A wrong account id therefore reads as "no relationship" rather than failing.
 *
 * <p><b>Eventual consistency:</b> the mirror node reflects network state with a small lag, typically
 * a few seconds. An entity queried immediately after it is created will report as not associated until
 * the mirror node catches up, so applications that need a definitive answer must allow for the lag
 * rather than treating the first response as final.
 *
 * <p>HTTP is performed through the client's {@link HttpTransport}, so the retry budget, the timeout
 * bounds and the connection pool are the ones on {@link Client#getMirrorNodeHttpConfig()}. The setters
 * below override a single field of that budget each, and nothing else.
 *
 * <p>This query is free.
 */
public final class MirrorNodeTokenBalanceQuery {
    @Nullable
    private AccountId accountId = null;

    @Nullable
    private TokenId tokenId = null;

    @Nullable
    private Integer maxAttempts = null;

    @Nullable
    private Duration maxBackoff = null;

    /**
     * Constructor.
     */
    public MirrorNodeTokenBalanceQuery() {}

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
     * The ID of the account whose token balance is being requested. Required.
     *
     * <p>Accepts {@code shard.realm.num}, an EVM address, or a public key alias. Contract IDs are also
     * accepted -- convert with {@code new AccountId(contractId.shard, contractId.realm, contractId.num)}.
     *
     * @param accountId the account id to set
     * @return {@code this}
     */
    public MirrorNodeTokenBalanceQuery setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        this.accountId = accountId;
        return this;
    }

    /**
     * Return the token's id.
     *
     * @return {@code tokenId}
     */
    @Nullable
    public TokenId getTokenId() {
        return tokenId;
    }

    /**
     * The ID of the token whose balance is being requested. Required.
     *
     * @param tokenId the token id to set
     * @return {@code this}
     */
    public MirrorNodeTokenBalanceQuery setTokenId(TokenId tokenId) {
        Objects.requireNonNull(tokenId);
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Extract the maximum number of attempts.
     *
     * @return the maximum number of attempts, per HTTP request
     */
    public int getMaxAttempts() {
        return maxAttempts != null
                ? maxAttempts
                : MirrorNodeHttpRetryPolicy.defaults().getMaxAttempts();
    }

    /**
     * Set the maximum number of attempts for the query.
     *
     * <p>Overrides only this field of the client's retry policy; every other field is left at the
     * client's value.
     *
     * @param maxAttempts the maximum number of attempts
     * @return {@code this}
     */
    public MirrorNodeTokenBalanceQuery setMaxAttempts(int maxAttempts) {
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
        return maxBackoff != null
                ? maxBackoff
                : MirrorNodeHttpRetryPolicy.defaults().getMaxBackoff();
    }

    /**
     * Set the maximum backoff duration for retry attempts.
     *
     * <p>Overrides only this field of the client's retry policy; every other field is left at the
     * client's value.
     *
     * @param maxBackoff the maximum backoff duration
     * @return {@code this}
     */
    public MirrorNodeTokenBalanceQuery setMaxBackoff(Duration maxBackoff) {
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
     * @return the retrieved {@link MirrorNodeTokenBalance}
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public MirrorNodeTokenBalance execute(Client client) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        return executeAsyncInternal(client, null).get();
    }

    /**
     * Executes the query with the user supplied client and timeout.
     *
     * @param client the client with which this will be executed
     * @param timeout the budget for the whole call, retries and backoff included
     * @return the retrieved {@link MirrorNodeTokenBalance}
     * @throws ExecutionException if the query fails
     * @throws InterruptedException if the thread is interrupted
     */
    public MirrorNodeTokenBalance execute(Client client, Duration timeout)
            throws ExecutionException, InterruptedException {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        return executeAsyncInternal(client, timeout).get();
    }

    /**
     * Executes the query asynchronously with the user supplied client.
     *
     * @param client the client with which this will be executed
     * @return a future representing the retrieved {@link MirrorNodeTokenBalance}
     */
    public CompletableFuture<MirrorNodeTokenBalance> executeAsync(Client client) {
        Objects.requireNonNull(client, "client must not be null");
        return executeAsyncInternal(client, null);
    }

    /**
     * Executes the query asynchronously with the user supplied client and timeout.
     *
     * @param client the client with which this will be executed
     * @param timeout the budget for the whole call, retries and backoff included
     * @return a future representing the retrieved {@link MirrorNodeTokenBalance}
     */
    public CompletableFuture<MirrorNodeTokenBalance> executeAsync(Client client, Duration timeout) {
        Objects.requireNonNull(client, "client must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        return executeAsyncInternal(client, timeout);
    }

    private CompletableFuture<MirrorNodeTokenBalance> executeAsyncInternal(Client client, @Nullable Duration timeout) {
        // Validate and resolve before scheduling any work, so a misconfigured query never reaches the
        // network and the base URL for the call is pinned up front.
        var path = buildPath(client);
        var requestedTokenId = Objects.requireNonNull(tokenId);
        var policy = resolvePolicy(client);
        var total = MirrorNodeHttpClient.resolveTotalDeadline(policy, client.getRequestTimeout(), timeout);
        var httpClient = client.newMirrorNodeHttpClient(policy);

        return CompletableFuture.supplyAsync(
                () -> {
                    var json = fetch(httpClient, path, total);
                    return json == null
                            ? MirrorNodeTokenBalance.notAssociated(requestedTokenId)
                            : MirrorNodeTokenBalance.fromJson(json, requestedTokenId);
                },
                client.executor);
    }

    /**
     * Fetch the relationship JSON, or {@code null} if the mirror node does not know the entity.
     */
    @Nullable
    private static JsonObject fetch(MirrorNodeHttpClient httpClient, MirrorNodeRestPath path, Duration total) {
        HttpResponse response;

        try {
            response = httpClient.get(path, Cancellation.none(), MirrorNodeHttpClient.CallDeadline.startingNow(total));
        } catch (HttpTransportException e) {
            var lastResponse = e instanceof MirrorNodeHttpException mirrorNodeHttpException
                    ? mirrorNodeHttpException.getLastResponse()
                    : null;

            throw new CompletionException(new RuntimeException(
                    "Failed to fetch token balance: " + e.getKind().getId()
                            + (lastResponse == null ? "" : " (" + MirrorNodeErrorMessage.describe(lastResponse) + ")"),
                    e));
        }

        if (response.getStatusCode() == 404) {
            return null;
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

    private MirrorNodeHttpRetryPolicy resolvePolicy(Client client) {
        var policy = client.getMirrorNodeHttpConfig().getRetryPolicy();

        // A query setter overrides only the field it names, leaving the others at the client value.
        if (maxAttempts != null) {
            policy = policy.withMaxAttempts(maxAttempts);
        }

        if (maxBackoff != null) {
            policy = policy.withMaxBackoff(maxBackoff);
        }

        return policy;
    }

    private MirrorNodeRestPath buildPath(Client client) {
        if (accountId == null) {
            throw new IllegalStateException("accountId must be set before executing MirrorNodeTokenBalanceQuery");
        }

        if (tokenId == null) {
            throw new IllegalStateException("tokenId must be set before executing MirrorNodeTokenBalanceQuery");
        }

        if (client.isAutoValidateChecksumsEnabled()) {
            try {
                accountId.validateChecksum(client);
                tokenId.validateChecksum(client);
            } catch (BadEntityIdException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        // limit=1 because token.id pins the result to at most one relationship; there is nothing to page.
        return MirrorNodeRestPath.of("/accounts/"
                + URLEncoder.encode(toAccountPathParam(accountId), StandardCharsets.UTF_8) + "/tokens?token.id="
                + URLEncoder.encode(tokenId.toString(), StandardCharsets.UTF_8) + "&limit=1");
    }

    /**
     * Render the account id in the form the mirror node's {@code idOrAliasOrEvmAddress} path segment
     * expects.
     *
     * <p>EVM addresses are sent in their {@code 0x}-prefixed hex form, and plain IDs as
     * {@code shard.realm.num}. A public key alias is sent as the unpadded base32 encoding of the
     * protobuf {@code Key} bytes -- the only alias form the mirror node accepts.
     * {@link AccountId#toString()} renders an alias as {@code shard.realm.<DER hex>} instead, which the
     * endpoint rejects with HTTP 400, so it cannot be used here.
     */
    private static String toAccountPathParam(AccountId accountId) {
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
        return "MirrorNodeTokenBalanceQuery{accountId=" + accountId + ", tokenId=" + tokenId + "}";
    }
}
