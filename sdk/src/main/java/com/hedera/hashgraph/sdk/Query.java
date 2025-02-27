// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.QueryHeader;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import com.hedera.hashgraph.sdk.proto.ResponseType;
import com.hedera.hashgraph.sdk.proto.Transaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Base class for all queries that can be submitted to Hedera.
 *
 * @param <O> The output type of the query.
 * @param <T> The type of the query itself. Used to enable chaining.
 */
public abstract class Query<O, T extends Query<O, T>>
        extends Executable<T, com.hedera.hashgraph.sdk.proto.Query, Response, O> {
    private final com.hedera.hashgraph.sdk.proto.Query.Builder builder;

    private final QueryHeader.Builder headerBuilder;

    /**
     * The transaction ID
     */
    @Nullable
    protected TransactionId paymentTransactionId = null;

    /**
     * List of payment transactions
     */
    @Nullable
    protected List<Transaction> paymentTransactions = null;

    @Nullable
    private Client.Operator paymentOperator = null;

    @Nullable
    private Hbar queryPayment = null;

    @Nullable
    private Hbar maxQueryPayment = null;

    @Nullable
    private Hbar chosenQueryPayment = null;

    /**
     * Constructor.
     */
    Query() {
        builder = com.hedera.hashgraph.sdk.proto.Query.newBuilder();
        headerBuilder = QueryHeader.newBuilder();
    }

    /**
     * Create a payment transaction.
     *
     * @param paymentTransactionId      the transaction id
     * @param nodeId                    the node id
     * @param operator                  the operator
     * @param paymentAmount             the amount
     * @return                          the new payment transaction
     */
    private static Transaction makePaymentTransaction(
            TransactionId paymentTransactionId, AccountId nodeId, Client.Operator operator, Hbar paymentAmount) {
        return new TransferTransaction()
                .setTransactionId(paymentTransactionId)
                .setNodeAccountIds(Collections.singletonList(nodeId))
                .setMaxTransactionFee(new Hbar(1)) // 1 Hbar
                .addHbarTransfer(operator.accountId, paymentAmount.negated())
                .addHbarTransfer(nodeId, paymentAmount)
                .freeze()
                .signWith(operator.publicKey, operator.transactionSigner)
                .makeRequest();
    }

    /**
     * Set an explicit payment amount for this query.
     * <p>
     * The client will submit exactly this amount for the payment of this query. Hedera
     * will not return any remainder.
     *
     * @param queryPayment The explicit payment amount to set
     * @return {@code this}
     */
    public T setQueryPayment(Hbar queryPayment) {
        this.queryPayment = queryPayment;

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Set the maximum payment allowable for this query.
     * <p>
     * When a query is executed without an explicit {@link Query#setQueryPayment(Hbar)} call,
     * the client will first request the cost
     * of the given query from the node it will be submitted to and attach a payment for that amount
     * from the operator account on the client.
     * <p>
     * If the returned value is greater than this value, a
     * {@link MaxQueryPaymentExceededException} will be thrown from
     * {@link Query#execute(Client)} or returned in the second callback of
     * {@link Query#executeAsync(Client, Consumer, Consumer)}.
     * <p>
     * Set to 0 to disable automatic implicit payments.
     *
     * @param maxQueryPayment The maximum payment amount to set
     * @return {@code this}
     */
    public T setMaxQueryPayment(Hbar maxQueryPayment) {
        this.maxQueryPayment = maxQueryPayment;

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Fetch the expected cost.
     *
     * @param client                    the client
     * @return                          the cost in hbar
     * @throws TimeoutException         when the transaction times out
     * @throws PrecheckStatusException  when the precheck fails
     */
    public Hbar getCost(Client client) throws TimeoutException, PrecheckStatusException {
        return getCost(client, client.getRequestTimeout());
    }

    /**
     * Fetch the expected cost.
     *
     * @param client                    the client
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @return                          the cost in hbar
     * @throws TimeoutException         when the transaction times out
     * @throws PrecheckStatusException  when the precheck fails
     */
    public Hbar getCost(Client client, Duration timeout) throws TimeoutException, PrecheckStatusException {
        initWithNodeIds(client);
        return getCostExecutable()
                .setNodeAccountIds(Objects.requireNonNull(getNodeAccountIds()))
                .execute(client, timeout);
    }

    /**
     * Fetch the expected cost asynchronously.
     *
     * @param client                    the client
     * @return                          Future result of the cost in hbar
     */
    public CompletableFuture<Hbar> getCostAsync(Client client) {
        return getCostAsync(client, client.getRequestTimeout());
    }

    /**
     * Fetch the expected cost asynchronously.
     *
     * @param client                    the client
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @return                          Future result of the cost in hbar
     */
    public CompletableFuture<Hbar> getCostAsync(Client client, Duration timeout) {
        initWithNodeIds(client);
        return getCostExecutable()
                .setNodeAccountIds(Objects.requireNonNull(getNodeAccountIds()))
                .executeAsync(client, timeout);
    }

    /**
     * Fetch the expected cost asynchronously.
     *
     * @param client                    the client
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @param callback a BiConsumer which handles the result or error.
     */
    public void getCostAsync(Client client, Duration timeout, BiConsumer<Hbar, Throwable> callback) {
        ConsumerHelper.biConsumer(getCostAsync(client, timeout), callback);
    }

    /**
     * Fetch the expected cost asynchronously.
     *
     * @param client                    the client
     * @param callback a BiConsumer which handles the result or error.
     */
    public void getCostAsync(Client client, BiConsumer<Hbar, Throwable> callback) {
        ConsumerHelper.biConsumer(getCostAsync(client), callback);
    }

    /**
     * Fetch the expected cost asynchronously.
     *
     * @param client                    the client
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void getCostAsync(Client client, Duration timeout, Consumer<Hbar> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(getCostAsync(client, timeout), onSuccess, onFailure);
    }

    /**
     * Fetch the expected cost asynchronously.
     *
     * @param client                    the client
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void getCostAsync(Client client, Consumer<Hbar> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(getCostAsync(client), onSuccess, onFailure);
    }

    /**
     * Does this query require a payment?
     *
     * @return                          does this query require a payment
     */
    boolean isPaymentRequired() {
        // nearly all queries require a payment
        return true;
    }

    /**
     * Called in {@link #makeRequest} just before the query is built. The intent is for the derived
     * class to assign their data variant to the query.
     */
    abstract void onMakeRequest(com.hedera.hashgraph.sdk.proto.Query.Builder queryBuilder, QueryHeader header);

    /**
     * The derived class should access its response header and return.
     */
    abstract ResponseHeader mapResponseHeader(Response response);

    /**
     * The derived class should access its request header and return.
     */
    abstract QueryHeader mapRequestHeader(com.hedera.hashgraph.sdk.proto.Query request);

    /**
     * Crate the new Query.
     *
     * @return                          the new Query
     */
    private Query<Hbar, QueryCostQuery> getCostExecutable() {
        return new QueryCostQuery();
    }

    /**
     * Validate the checksums.
     */
    abstract void validateChecksums(Client client) throws BadEntityIdException;

    /**
     * Retrieve the operator from the configured client.
     *
     * @param client                    the configured client
     * @return                          the operator
     */
    Client.Operator getOperatorFromClient(Client client) {
        var operator = client.getOperator();

        if (operator == null) {
            throw new IllegalStateException(
                    "`client` must have an `operator` or an explicit payment transaction must be provided");
        }

        return operator;
    }

    @Override
    void onExecute(Client client) throws TimeoutException, PrecheckStatusException {
        var grpcCostQuery = new GrpcCostQuery(client);

        if (grpcCostQuery.isNotRequired()) {
            return;
        }

        if (grpcCostQuery.getCost() == null) {
            grpcCostQuery.setCost(getCost(client));

            if (grpcCostQuery.shouldError()) {
                throw grpcCostQuery.mapError();
            }
        }

        grpcCostQuery.finish();
    }

    /**
     * <p>Note: This method requires API level 33 or higher. It will not work on devices running API versions below 31
     * because it uses features introduced in API level 31 (Android 12).</p>*
     */
    @Override
    CompletableFuture<Void> onExecuteAsync(Client client) {
        var grpcCostQuery = new GrpcCostQuery(client);

        if (grpcCostQuery.isNotRequired()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(
                        () -> {
                            if (grpcCostQuery.getCost() == null) {
                                // No payment was specified so we need to go ask
                                // This is a query in its own right so we use a nested future here
                                return getCostAsync(client).thenCompose(cost -> {
                                    grpcCostQuery.setCost(cost);

                                    if (grpcCostQuery.shouldError()) {
                                        return CompletableFuture.failedFuture(grpcCostQuery.mapError());
                                    }

                                    return CompletableFuture.completedFuture(null);
                                });
                            }

                            return CompletableFuture.completedFuture(null);
                        },
                        client.executor)
                .thenCompose(x -> x)
                .thenAccept((paymentAmount) -> {
                    grpcCostQuery.finish();
                });
    }

    private void initWithNodeIds(Client client) {
        if (client.isAutoValidateChecksumsEnabled()) {
            try {
                validateChecksums(client);
            } catch (BadEntityIdException exc) {
                throw new IllegalArgumentException(exc.getMessage());
            }
        }

        if (nodeAccountIds.size() == 0) {
            // Get a list of node AccountId's if the user has not set them manually.
            try {
                nodeAccountIds.setList(client.network.getNodeAccountIdsForExecute());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Retrieve the transaction at the given index.
     *
     * @param index                     the index
     * @return                          the transaction
     */
    Transaction getPaymentTransaction(int index) {
        paymentTransactionId = TransactionId.generate(Objects.requireNonNull(paymentOperator).accountId);

        var newPaymentTx = makePaymentTransaction(
                paymentTransactionId,
                nodeAccountIds.get(index),
                paymentOperator,
                Objects.requireNonNull(chosenQueryPayment));
        paymentTransactions.set(index, newPaymentTx);
        return newPaymentTx;
    }

    @Override
    final com.hedera.hashgraph.sdk.proto.Query makeRequest() {
        // If payment is required, set the next payment transaction on the query
        if (isPaymentRequired() && paymentTransactions != null) {
            headerBuilder.setPayment(getPaymentTransaction(nodeAccountIds.getIndex()));
        }

        // Delegate to the derived class to apply the header because the common header struct is
        // within the nested type
        onMakeRequest(
                builder, headerBuilder.setResponseType(ResponseType.ANSWER_ONLY).build());

        return builder.build();
    }

    @Override
    Status mapResponseStatus(Response response) {
        var preCheckCode = mapResponseHeader(response).getNodeTransactionPrecheckCode();

        return Status.valueOf(preCheckCode);
    }

    @Override
    @Nullable
    TransactionId getTransactionIdInternal() {
        // this is only called on an error about either the payment transaction or missing a payment transaction
        // as we make sure the latter can't happen, this will never be null
        return paymentTransactionId;
    }

    /**
     * Extract the transaction id.
     *
     * @return                          the transaction id
     */
    @Nullable
    public TransactionId getPaymentTransactionId() {
        return paymentTransactionId;
    }

    /**
     * Assign the transaction id.
     *
     * @param paymentTransactionId      the transaction id
     * @return {@code this}
     */
    @Nullable
    public T setPaymentTransactionId(TransactionId paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;

        // noinspection unchecked
        return (T) this;
    }

    @Override
    @SuppressWarnings("LiteProtoToString")
    public String toString() {
        var request = makeRequest();

        StringBuilder builder =
                new StringBuilder(request.toString().replaceAll("(?m)^# com.hedera.hashgraph.sdk.proto.Query.*", ""));

        var queryHeader = mapRequestHeader(request);
        if (queryHeader.hasPayment()) {
            builder.append("\n");

            try {
                // the replaceAll() is for removing the class name from Transaction Body
                builder.append(
                        TransactionBody.parseFrom(queryHeader.getPayment().getBodyBytes())
                                .toString()
                                .replaceAll("(?m)^# com.hedera.hashgraph.sdk.proto.TransactionBuilder.*", ""));
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }

        return builder.toString();
    }

    private class GrpcCostQuery {
        private final Hbar maxCost;
        private final boolean notRequired;

        private Client.Operator operator;
        private Hbar cost;

        GrpcCostQuery(Client client) {
            Query.this.initWithNodeIds(client);

            cost = Query.this.queryPayment;
            notRequired = (Query.this.paymentTransactions != null) || !Query.this.isPaymentRequired();
            maxCost = MoreObjects.firstNonNull(Query.this.maxQueryPayment, client.defaultMaxQueryPayment);

            if (!notRequired) {
                operator = Query.this.getOperatorFromClient(client);
            }
        }

        public Client.Operator getOperator() {
            return operator;
        }

        public Hbar getCost() {
            return cost;
        }

        public boolean isNotRequired() {
            return notRequired;
        }

        GrpcCostQuery setCost(Hbar cost) {
            this.cost = cost;
            return this;
        }

        boolean shouldError() {
            // Check if this is below our configured maximum query payment
            return cost.compareTo(maxCost) > 0;
        }

        MaxQueryPaymentExceededException mapError() {
            return new MaxQueryPaymentExceededException(Query.this, cost, maxCost);
        }

        void finish() {
            Query.this.chosenQueryPayment = cost;
            Query.this.paymentOperator = operator;
            Query.this.paymentTransactions = new ArrayList<>(Query.this.nodeAccountIds.size());

            for (int i = 0; i < Query.this.nodeAccountIds.size(); i++) {
                Query.this.paymentTransactions.add(null);
            }
        }
    }

    @SuppressWarnings("NullableDereference")
    private class QueryCostQuery extends Query<Hbar, QueryCostQuery> {
        @Override
        void validateChecksums(Client client) throws BadEntityIdException {}

        @Override
        void onMakeRequest(com.hedera.hashgraph.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
            headerBuilder.setResponseType(ResponseType.COST_ANSWER);

            // COST_ANSWER requires a payment to pass validation but doesn't actually process it
            // yes, this transaction is completely invalid
            // that is okay
            // now go back to sleep
            // without this, an error of MISSING_QUERY_HEADER is returned
            headerBuilder.setPayment(new TransferTransaction()
                    .setNodeAccountIds(Collections.singletonList(new AccountId(0, 0, 0)))
                    .setTransactionId(TransactionId.withValidStart(new AccountId(0, 0, 0), Instant.ofEpochSecond(0)))
                    .freeze()
                    .makeRequest());

            Query.this.onMakeRequest(queryBuilder, headerBuilder.build());
        }

        @Override
        ResponseHeader mapResponseHeader(Response response) {
            return Query.this.mapResponseHeader(response);
        }

        @Override
        QueryHeader mapRequestHeader(com.hedera.hashgraph.sdk.proto.Query request) {
            return Query.this.mapRequestHeader(request);
        }

        @Override
        Hbar mapResponse(Response response, AccountId nodeId, com.hedera.hashgraph.sdk.proto.Query request) {
            return Hbar.fromTinybars(mapResponseHeader(response).getCost());
        }

        @Override
        MethodDescriptor<com.hedera.hashgraph.sdk.proto.Query, Response> getMethodDescriptor() {
            return Query.this.getMethodDescriptor();
        }

        @Override
        boolean isPaymentRequired() {
            // combo breaker
            return false;
        }
    }
}
