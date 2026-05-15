// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Reject undesired token(s) and dissociate in a single flow.
 */
public class TokenRejectFlow {
    /**
     * An account holding the tokens to be rejected.
     */
    @Nullable
    private AccountId ownerId = null;

    /**
     * A list of one or more token rejections (a fungible/common token type).
     */
    private List<TokenId> tokenIds = new ArrayList<>();

    /**
     * A list of one or more token rejections (a single specific serialized non-fungible/unique token).
     */
    private List<NftId> nftIds = new ArrayList<>();

    @Nullable
    private List<AccountId> nodeAccountIds = null;

    @Nullable
    private Client freezeWithClient = null;

    @Nullable
    private PrivateKey signPrivateKey = null;

    @Nullable
    private PublicKey signPublicKey = null;

    @Nullable
    private UnaryOperator<byte[]> transactionSigner = null;

    public TokenRejectFlow() {}

    /**
     * Extract the Account ID of the Owner.
     * @return the Account ID of the Owner.
     */
    public AccountId getOwnerId() {
        return ownerId;
    }

    /**
     * Assign the Account ID of the Owner.
     * @param ownerId the Account ID of the Owner.
     * @return {@code this}
     */
    public TokenRejectFlow setOwnerId(AccountId ownerId) {
        Objects.requireNonNull(ownerId);
        this.ownerId = ownerId;
        return this;
    }

    /**
     * Extract the list of tokenIds.
     * @return the list of tokenIds.
     */
    public List<TokenId> getTokenIds() {
        return tokenIds;
    }

    /**
     * Assign the list of tokenIds.
     * @param tokenIds the list of tokenIds.
     * @return {@code this}
     */
    public TokenRejectFlow setTokenIds(List<TokenId> tokenIds) {
        Objects.requireNonNull(tokenIds);
        this.tokenIds = new ArrayList<>(tokenIds);
        return this;
    }

    /**
     * Add a token to the list of tokens.
     * @param tokenId token to add.
     * @return {@code this}
     */
    public TokenRejectFlow addTokenId(TokenId tokenId) {
        tokenIds.add(tokenId);
        return this;
    }

    /**
     * Extract the list of nftIds.
     * @return the list of nftIds.
     */
    public List<NftId> getNftIds() {
        return nftIds;
    }

    /**
     * Assign the list of nftIds.
     * @param nftIds the list of nftIds.
     * @return {@code this}
     */
    public TokenRejectFlow setNftIds(List<NftId> nftIds) {
        Objects.requireNonNull(nftIds);
        this.nftIds = new ArrayList<>(nftIds);
        return this;
    }

    /**
     * Add a nft to the list of nfts.
     * @param nftId nft to add.
     * @return {@code this}
     */
    public TokenRejectFlow addNftId(NftId nftId) {
        nftIds.add(nftId);
        return this;
    }

    /**
     * Set the account IDs of the nodes that this transaction will be submitted to.
     * <p>
     * Providing an explicit node account ID interferes with client-side load balancing of the network. By default, the
     * SDK will pre-generate a transaction for 1/3 of the nodes on the network. If a node is down, busy, or otherwise
     * reports a fatal error, the SDK will try again with a different node.
     *
     * @param nodeAccountIds The list of node AccountIds to be set
     * @return {@code this}
     */
    public TokenRejectFlow setNodeAccountIds(List<AccountId> nodeAccountIds) {
        Objects.requireNonNull(nodeAccountIds);
        this.nodeAccountIds = new ArrayList(nodeAccountIds);
        return this;
    }

    /**
     * Set the client that this transaction will be frozen with.
     *
     * @param client the client with the transaction to execute
     * @return {@code this}
     */
    public TokenRejectFlow freezeWith(Client client) {
        this.freezeWithClient = client;
        return this;
    }

    /**
     * Set the private key that this transaction will be signed with.
     *
     * @param privateKey the private key used for signing
     * @return {@code this}
     */
    public TokenRejectFlow sign(PrivateKey privateKey) {
        this.signPrivateKey = privateKey;
        this.signPublicKey = null;
        this.transactionSigner = null;
        return this;
    }

    /**
     * Set the public key and key list that this transaction will be signed with.
     *
     * @param publicKey         the public key
     * @param transactionSigner the key list
     * @return {@code this}
     */
    public TokenRejectFlow signWith(PublicKey publicKey, UnaryOperator<byte[]> transactionSigner) {
        this.signPublicKey = publicKey;
        this.transactionSigner = transactionSigner;
        this.signPrivateKey = null;
        return this;
    }

    /**
     * Set the operator that this transaction will be signed with.
     *
     * @param client the client with the transaction to execute
     * @return {@code this}
     */
    public TokenRejectFlow signWithOperator(Client client) {
        var operator = Objects.requireNonNull(client.getOperator());
        this.signPublicKey = operator.publicKey;
        this.transactionSigner = operator.transactionSigner;
        this.signPrivateKey = null;
        return this;
    }

    private void fillOutTransaction(final Transaction<?> transaction) {
        if (nodeAccountIds != null) {
            transaction.setNodeAccountIds(nodeAccountIds);
        }
        if (freezeWithClient != null) {
            transaction.freezeWith(freezeWithClient);
        }
        if (signPrivateKey != null) {
            transaction.sign(signPrivateKey);
        } else if (signPublicKey != null && transactionSigner != null) {
            transaction.signWith(signPublicKey, transactionSigner);
        }
    }

    private TokenRejectTransaction createTokenRejectTransaction() {
        var tokenRejectTransaction = new TokenRejectTransaction()
                .setOwnerId(ownerId)
                .setTokenIds(tokenIds)
                .setNftIds(nftIds);

        fillOutTransaction(tokenRejectTransaction);

        return tokenRejectTransaction;
    }

    private TokenDissociateTransaction createTokenDissociateTransaction() {
        List<TokenId> tokenIdsToReject = Stream.concat(
                        tokenIds.stream(), nftIds.stream().map(nftId -> nftId.tokenId))
                .distinct()
                .collect(Collectors.toList());

        var tokenDissociateTransaction =
                new TokenDissociateTransaction().setAccountId(ownerId).setTokenIds(tokenIdsToReject);

        fillOutTransaction(tokenDissociateTransaction);

        return tokenDissociateTransaction;
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
     * @return the response of TokenRejectTransaction
     * @throws PrecheckStatusException when the precheck fails
     * @throws TimeoutException        when the transaction times out
     */
    public TransactionResponse execute(Client client, Duration timeoutPerTransaction)
            throws PrecheckStatusException, TimeoutException {
        try {
            var tokenRejectTxResponse = createTokenRejectTransaction().execute(client, timeoutPerTransaction);
            tokenRejectTxResponse.getReceipt(client, timeoutPerTransaction);

            var tokenDissociateTxResponse = createTokenDissociateTransaction().execute(client, timeoutPerTransaction);
            tokenDissociateTxResponse.getReceipt(client, timeoutPerTransaction);

            return tokenRejectTxResponse;
        } catch (ReceiptStatusException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute the transactions in the flow with the passed in client asynchronously.
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
     * @param client                the client with the transaction to execute
     * @param timeoutPerTransaction The timeout after which each transaction's execution attempt will be cancelled.
     * @return the response
     */
    public CompletableFuture<TransactionResponse> executeAsync(Client client, Duration timeoutPerTransaction) {
        return createTokenRejectTransaction()
                .executeAsync(client, timeoutPerTransaction)
                .thenCompose(tokenRejectResponse ->
                        tokenRejectResponse.getReceiptQuery(client).executeAsync(client, timeoutPerTransaction))
                .thenCompose(receipt -> createTokenDissociateTransaction().executeAsync(client, timeoutPerTransaction));
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
