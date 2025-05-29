// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.FutureConverter.toCompletableFuture;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.MessageLite;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Hex;

/**
 * Abstract base utility class.
 *
 * @param <SdkRequestT>   the sdk request
 * @param <ProtoRequestT> the proto request
 * @param <ResponseT>     the response
 * @param <O>             the O type
 */
abstract class Executable<SdkRequestT, ProtoRequestT extends MessageLite, ResponseT extends MessageLite, O> {
    @SuppressWarnings("java:S2245")
    protected static final Random random = new Random();

    static final Pattern RST_STREAM =
            Pattern.compile(".*\\brst[^0-9a-zA-Z]stream\\b.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /**
     * The maximum times execution will be attempted
     */
    @Nullable
    protected Integer maxAttempts = null;

    /**
     * The maximum amount of time to wait between retries
     */
    @Nullable
    protected Duration maxBackoff = null;

    /**
     * The minimum amount of time to wait between retries
     */
    @Nullable
    protected Duration minBackoff = null;

    /**
     * List of account IDs for nodes with which execution will be attempted.
     */
    protected LockableList<AccountId> nodeAccountIds = new LockableList<>();

    /**
     * List of healthy and unhealthy nodes with which execution will be attempted.
     */
    protected LockableList<Node> nodes = new LockableList<>();

    /**
     * Indicates if the request has been attempted to be sent to all nodes
     */
    protected boolean attemptedAllNodes = false;
    /**
     * The timeout for each execution attempt
     */
    protected Duration grpcDeadline;

    protected Logger logger;
    private java.util.function.Function<ProtoRequestT, ProtoRequestT> requestListener;
    // Lambda responsible for executing synchronous gRPC requests. Pluggable for unit testing.
    @VisibleForTesting
    Function<GrpcRequest, ResponseT> blockingUnaryCall =
            (grpcRequest) -> ClientCalls.blockingUnaryCall(grpcRequest.createCall(), grpcRequest.getRequest());

    private java.util.function.Function<ResponseT, ResponseT> responseListener;

    Executable() {
        requestListener = request -> {
            if (logger.isEnabledForLevel(LogLevel.TRACE)) {
                logger.trace("Sent protobuf {}", Hex.toHexString(request.toByteArray()));
            }
            return request;
        };
        responseListener = response -> {
            if (logger.isEnabledForLevel(LogLevel.TRACE)) {
                logger.trace("Received protobuf {}", Hex.toHexString(response.toByteArray()));
            }
            return response;
        };
    }

    /**
     * When execution is attempted, a single attempt will time out when this deadline is reached. (The SDK may
     * subsequently retry the execution.)
     *
     * @return The timeout for each execution attempt
     */
    public final Duration grpcDeadline() {
        return grpcDeadline;
    }

    /**
     * When execution is attempted, a single attempt will timeout when this deadline is reached. (The SDK may
     * subsequently retry the execution.)
     *
     * @param grpcDeadline The timeout for each execution attempt
     * @return {@code this}
     */
    public final SdkRequestT setGrpcDeadline(Duration grpcDeadline) {
        this.grpcDeadline = Objects.requireNonNull(grpcDeadline);

        // noinspection unchecked
        return (SdkRequestT) this;
    }

    /**
     * The maximum amount of time to wait between retries
     *
     * @return maxBackoff
     */
    public final Duration getMaxBackoff() {
        return maxBackoff != null ? maxBackoff : Client.DEFAULT_MAX_BACKOFF;
    }

    /**
     * The maximum amount of time to wait between retries. Every retry attempt will increase the wait time exponentially
     * until it reaches this time.
     *
     * @param maxBackoff The maximum amount of time to wait between retries
     * @return {@code this}
     */
    public final SdkRequestT setMaxBackoff(Duration maxBackoff) {
        if (maxBackoff == null || maxBackoff.toNanos() < 0) {
            throw new IllegalArgumentException("maxBackoff must be a positive duration");
        } else if (maxBackoff.compareTo(getMinBackoff()) < 0) {
            throw new IllegalArgumentException("maxBackoff must be greater than or equal to minBackoff");
        }
        this.maxBackoff = maxBackoff;
        // noinspection unchecked
        return (SdkRequestT) this;
    }

    /**
     * The minimum amount of time to wait between retries
     *
     * @return minBackoff
     */
    public final Duration getMinBackoff() {
        return minBackoff != null ? minBackoff : Client.DEFAULT_MIN_BACKOFF;
    }

    /**
     * The minimum amount of time to wait between retries. When retrying, the delay will start at this time and increase
     * exponentially until it reaches the maxBackoff.
     *
     * @param minBackoff The minimum amount of time to wait between retries
     * @return {@code this}
     */
    public final SdkRequestT setMinBackoff(Duration minBackoff) {
        if (minBackoff == null || minBackoff.toNanos() < 0) {
            throw new IllegalArgumentException("minBackoff must be a positive duration");
        } else if (minBackoff.compareTo(getMaxBackoff()) > 0) {
            throw new IllegalArgumentException("minBackoff must be less than or equal to maxBackoff");
        }
        this.minBackoff = minBackoff;
        // noinspection unchecked
        return (SdkRequestT) this;
    }

    /**
     * @return Number of errors before execution will fail.
     * @deprecated Use {@link #getMaxAttempts()} instead.
     */
    @java.lang.Deprecated
    public final int getMaxRetry() {
        return getMaxAttempts();
    }

    /**
     * @param count Number of errors before execution will fail
     * @return {@code this}
     * @deprecated Use {@link #setMaxAttempts(int)} instead.
     */
    @java.lang.Deprecated
    public final SdkRequestT setMaxRetry(int count) {
        return setMaxAttempts(count);
    }

    /**
     * Get the maximum times execution will be attempted.
     *
     * @return Number of errors before execution will fail.
     */
    public final int getMaxAttempts() {
        return maxAttempts != null ? maxAttempts : Client.DEFAULT_MAX_ATTEMPTS;
    }

    /**
     * Set the maximum times execution will be attempted.
     *
     * @param maxAttempts Execution will fail after this many errors.
     * @return {@code this}
     */
    public final SdkRequestT setMaxAttempts(int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
        this.maxAttempts = maxAttempts;
        // noinspection unchecked
        return (SdkRequestT) this;
    }

    /**
     * Get the list of account IDs for nodes with which execution will be attempted.
     *
     * @return the list of account IDs
     */
    @Nullable
    public final List<AccountId> getNodeAccountIds() {
        if (!nodeAccountIds.isEmpty()) {
            return new ArrayList<>(nodeAccountIds.getList());
        }

        return null;
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
    public SdkRequestT setNodeAccountIds(List<AccountId> nodeAccountIds) {
        this.nodeAccountIds.setList(nodeAccountIds).setLocked(true);

        // noinspection unchecked
        return (SdkRequestT) this;
    }

    /**
     * Set a callback that will be called right before the request is sent. As input, the callback will receive the
     * protobuf of the request, and the callback should return the request protobuf.  This means the callback has an
     * opportunity to read, copy, or modify the request that will be sent.
     *
     * @param requestListener The callback to use
     * @return {@code this}
     */
    public final SdkRequestT setRequestListener(UnaryOperator<ProtoRequestT> requestListener) {
        this.requestListener = Objects.requireNonNull(requestListener);
        return (SdkRequestT) this;
    }

    /**
     * Set a callback that will be called right before the response is returned. As input, the callback will receive the
     * protobuf of the response, and the callback should return the response protobuf.  This means the callback has an
     * opportunity to read, copy, or modify the response that will be read.
     *
     * @param responseListener The callback to use
     * @return {@code this}
     */
    public final SdkRequestT setResponseListener(UnaryOperator<ResponseT> responseListener) {
        this.responseListener = Objects.requireNonNull(responseListener);
        return (SdkRequestT) this;
    }

    /**
     * Set the logger
     *
     * @param logger the new logger
     * @return {@code this}
     */
    public SdkRequestT setLogger(Logger logger) {
        this.logger = logger;
        return (SdkRequestT) this;
    }

    void checkNodeAccountIds() {
        if (nodeAccountIds.isEmpty()) {
            throw new IllegalStateException("Request node account IDs were not set before executing");
        }
    }

    abstract void onExecute(Client client) throws TimeoutException, PrecheckStatusException;

    abstract CompletableFuture<Void> onExecuteAsync(Client client);

    void mergeFromClient(Client client) {
        if (maxAttempts == null) {
            maxAttempts = client.getMaxAttempts();
        }

        if (maxBackoff == null) {
            maxBackoff = client.getMaxBackoff();
        }

        if (minBackoff == null) {
            minBackoff = client.getMinBackoff();
        }

        if (grpcDeadline == null) {
            grpcDeadline = client.getGrpcDeadline();
        }
    }

    private void delay(long delay) {
        if (delay <= 0) {
            return;
        }
        try {
            if (delay > 0) {
                if (logger.isEnabledForLevel(LogLevel.DEBUG)) {
                    logger.debug("Sleeping for: " + delay + " | Thread name: "
                            + Thread.currentThread().getName());
                }
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute this transaction or query
     *
     * @param client The client with which this will be executed.
     * @return Result of execution
     * @throws TimeoutException        when the transaction times out
     * @throws PrecheckStatusException when the precheck fails
     */
    public O execute(Client client) throws TimeoutException, PrecheckStatusException {
        return execute(client, client.getRequestTimeout());
    }

    /**
     * Execute this transaction or query with a timeout
     *
     * @param client  The client with which this will be executed.
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @return Result of execution
     * @throws TimeoutException        when the transaction times out
     * @throws PrecheckStatusException when the precheck fails
     */
    public O execute(Client client, Duration timeout) throws TimeoutException, PrecheckStatusException {
        Throwable lastException = null;
        if (isBatchedAndNotBatchTransaction()) {
            throw new IllegalArgumentException("Cannot execute batchified transaction outside of BatchTransaction");
        }
        // If the logger on the request is not set, use the logger in client
        // (if set, otherwise do not use logger)
        if (this.logger == null) {
            this.logger = client.getLogger();
        }

        mergeFromClient(client);
        onExecute(client);
        checkNodeAccountIds();
        setNodesFromNodeAccountIds(client);

        var timeoutTime = Instant.now().plus(timeout);

        for (int attempt = 1; /* condition is done within loop */ ; attempt++) {
            if (attempt > maxAttempts) {
                throw new MaxAttemptsExceededException(lastException);
            }

            Duration currentTimeout = Duration.between(Instant.now(), timeoutTime);
            if (currentTimeout.isNegative() || currentTimeout.isZero()) {
                throw new TimeoutException();
            }

            GrpcRequest grpcRequest = new GrpcRequest(client.network, attempt, currentTimeout);
            Node node = grpcRequest.getNode();
            ResponseT response = null;

            // If we get an unhealthy node here, we've cycled through all the "good" nodes that have failed
            // and have no choice but to try a bad one.
            if (!node.isHealthy()) {
                delay(node.getRemainingTimeForBackoff());
            }

            if (node.channelFailedToConnect(timeoutTime)) {
                logger.trace("Failed to connect channel for node {} for request #{}", node.getAccountId(), attempt);
                lastException = grpcRequest.reactToConnectionFailure();
                continue;
            }

            currentTimeout = Duration.between(Instant.now(), timeoutTime);
            grpcRequest.setGrpcDeadline(currentTimeout);

            try {
                response = blockingUnaryCall.apply(grpcRequest);
                logTransaction(this.getTransactionIdInternal(), client, node, false, attempt, response, null);
            } catch (Throwable e) {
                if (e instanceof StatusRuntimeException) {
                    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) e;
                    if (statusRuntimeException.getStatus().getCode().equals(Code.DEADLINE_EXCEEDED)) {
                        throw new TimeoutException();
                    }
                }
                lastException = e;
                logTransaction(this.getTransactionIdInternal(), client, node, false, attempt, null, e);
            }

            if (response == null) {
                if (grpcRequest.shouldRetryExceptionally(lastException)) {
                    continue;
                } else {
                    throw new RuntimeException(lastException);
                }
            }

            var status = mapResponseStatus(response);
            var executionState = getExecutionState(status, response);
            grpcRequest.handleResponse(response, status, executionState);

            switch (executionState) {
                case SERVER_ERROR:
                    lastException = grpcRequest.mapStatusException();
                    continue;
                case RETRY:
                    // Response is not ready yet from server, need to wait.
                    lastException = grpcRequest.mapStatusException();
                    if (attempt < maxAttempts) {
                        currentTimeout = Duration.between(Instant.now(), timeoutTime);
                        delay(Math.min(currentTimeout.toMillis(), grpcRequest.getDelay()));
                    }
                    continue;
                case REQUEST_ERROR:
                    throw grpcRequest.mapStatusException();
                case SUCCESS:
                default:
                    return grpcRequest.mapResponse();
            }
        }
    }

    protected boolean isBatchedAndNotBatchTransaction() {
        return false;
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * <p>Note: This method requires API level 33 or higher. It will not work on devices running API versions below 31
     * because it uses features introduced in API level 31 (Android 12).</p>*
     *
     * @param client The client with which this will be executed.
     * @return Future result of execution
     */
    public CompletableFuture<O> executeAsync(Client client) {
        return executeAsync(client, client.getRequestTimeout());
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * <p>Note: This method requires API level 33 or higher. It will not work on devices running API versions below 31
     * because it uses features introduced in API level 31 (Android 12).</p>*
     *
     * @param client  The client with which this will be executed.
     * @param timeout The timeout after which the execution attempt will be cancelled.
     * @return Future result of execution
     */
    public CompletableFuture<O> executeAsync(Client client, Duration timeout) {
        var retval = new CompletableFuture<O>().orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);

        mergeFromClient(client);

        onExecuteAsync(client)
                .thenRun(() -> {
                    checkNodeAccountIds();
                    setNodesFromNodeAccountIds(client);

                    executeAsyncInternal(client, 1, null, retval, timeout);
                })
                .exceptionally(error -> {
                    retval.completeExceptionally(error);
                    return null;
                });
        return retval;
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client   The client with which this will be executed.
     * @param callback a BiConsumer which handles the result or error.
     */
    public void executeAsync(Client client, BiConsumer<O, Throwable> callback) {
        ConsumerHelper.biConsumer(executeAsync(client), callback);
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client   The client with which this will be executed.
     * @param timeout  The timeout after which the execution attempt will be cancelled.
     * @param callback a BiConsumer which handles the result or error.
     */
    public void executeAsync(Client client, Duration timeout, BiConsumer<O, Throwable> callback) {
        ConsumerHelper.biConsumer(executeAsync(client, timeout), callback);
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client    The client with which this will be executed.
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void executeAsync(Client client, Consumer<O> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(executeAsync(client), onSuccess, onFailure);
    }

    /**
     * Execute this transaction or query asynchronously.
     *
     * @param client    The client with which this will be executed.
     * @param timeout   The timeout after which the execution attempt will be cancelled.
     * @param onSuccess a Consumer which consumes the result on success.
     * @param onFailure a Consumer which consumes the error on failure.
     */
    public void executeAsync(Client client, Duration timeout, Consumer<O> onSuccess, Consumer<Throwable> onFailure) {
        ConsumerHelper.twoConsumers(executeAsync(client, timeout), onSuccess, onFailure);
    }

    /**
     * Logs the transaction's parameters
     *
     * @param transactionId the transaction's id
     * @param client        the client that executed the transaction
     * @param node          the node the transaction was sent to
     * @param isAsync       whether the transaction was executed asynchronously
     * @param attempt       the attempt number
     * @param response      the transaction response if the transaction was successful
     * @param error         the error if the transaction was not successful
     */
    protected void logTransaction(
            TransactionId transactionId,
            Client client,
            Node node,
            boolean isAsync,
            int attempt,
            @Nullable ResponseT response,
            @Nullable Throwable error) {

        if (!logger.isEnabledForLevel(LogLevel.TRACE)) {
            return;
        }

        logger.trace(
                "Execute{} Transaction ID: {}, submit to {}, node: {}, attempt: {}",
                isAsync ? "Async" : "",
                transactionId,
                client.network,
                node.getAccountId(),
                attempt);

        if (response != null) {
            logger.trace(" - Response: {}", response);
        }

        if (error != null) {
            logger.trace(" - Error: {}", error.getMessage());
        }
    }

    @SuppressWarnings("java:S2245")
    @VisibleForTesting
    void setNodesFromNodeAccountIds(Client client) {
        nodes.clear();

        // When a single node is explicitly set we get all of its proxies so in case of
        // failure the system can retry with different proxy on each attempt
        if (nodeAccountIds.size() == 1) {
            var nodeProxies = client.network.getNodeProxies(nodeAccountIds.get(0));
            if (nodeProxies == null || nodeProxies.isEmpty()) {
                throw new IllegalStateException("Account ID did not map to valid node in the client's network");
            }

            nodes.addAll(nodeProxies).shuffle();

            return;
        }

        // When multiple nodes are available the system retries with different node on each attempt
        // instead of different proxy of the same node
        for (var accountId : nodeAccountIds) {
            @Nullable var nodeProxies = client.network.getNodeProxies(accountId);
            if (nodeProxies == null || nodeProxies.isEmpty()) {
                logger.warn(
                        "Attempting to fetch node {} proxy which is not included in the Client's network. Please review your Client config.",
                        accountId.toString());
                continue;
            }

            var node = nodeProxies.get(random.nextInt(nodeProxies.size()));

            nodes.add(Objects.requireNonNull(node));
        }
        if (nodes.isEmpty()) {
            throw new IllegalStateException("All node account IDs did not map to valid nodes in the client's network");
        }
    }

    /**
     * Return the next node for execution. Will select the first node that is deemed healthy. If we cannot find such a
     * node and have tried n nodes (n being the size of the node list), we will select the node with the smallest
     * remaining delay. All delays MUST be executed in calling layer as this method will be called for sync + async
     * scenarios.
     */
    @VisibleForTesting
    Node getNodeForExecute(int attempt) {
        Node node = null;
        Node candidate = null;
        long smallestDelay = Long.MAX_VALUE;

        for (int _i = 0; _i < nodes.size(); _i++) {
            // NOTE: _i is NOT the index into this.nodes, it is just keeping track of how many times we've iterated.
            // In the event of ServerErrors, this method depends on the nodes list to have advanced to
            // the next node.
            node = nodes.getCurrent();

            if (!node.isHealthy()) {
                // Keep track of the node with the smallest delay seen thus far. If we go through the entire list
                // (meaning all nodes are unhealthy) then we will select the node with the smallest delay.
                long backoff = node.getRemainingTimeForBackoff();
                if (backoff < smallestDelay) {
                    candidate = node;
                    smallestDelay = backoff;
                }

                node = null;
                advanceRequest();
            } else {
                break; // got a good node, use it
            }
        }

        if (node == null) {
            node = candidate;

            // If we've tried all nodes, index will be +1 too far. Index increment happens outside
            // this method so try to be consistent with happy path.
            nodeAccountIds.setIndex(Math.max(0, nodeAccountIds.getIndex()));
        }

        // node won't be null at this point because execute() validates before this method is called.
        // Add null check here to work around sonar NPE detection.
        if (node != null && logger != null) {
            logger.trace("Using node {} for request #{}: {}", node.getAccountId(), attempt, this);
        }

        return node;
    }

    private ProtoRequestT getRequestForExecute() {
        var request = makeRequest();

        // advance the internal index
        // non-free queries and transactions map to more than 1 actual transaction and this will cause
        // the next invocation of makeRequest to return the _next_ transaction
        advanceRequest();

        return request;
    }

    private void executeAsyncInternal(
            Client client,
            int attempt,
            @Nullable Throwable lastException,
            CompletableFuture<O> returnFuture,
            Duration timeout) {
        // If the logger on the request is not set, use the logger in client
        // (if set, otherwise do not use logger)
        if (this.logger == null && client.getLogger() != null) {
            this.logger = client.getLogger();
        }

        if (returnFuture.isCancelled() || returnFuture.isCompletedExceptionally() || returnFuture.isDone()) {
            return;
        }

        if (attempt > maxAttempts) {
            returnFuture.completeExceptionally(
                    new CompletionException(new MaxAttemptsExceededException(lastException)));
            return;
        }

        var timeoutTime = Instant.now().plus(timeout);

        GrpcRequest grpcRequest =
                new GrpcRequest(client.network, attempt, Duration.between(Instant.now(), timeoutTime));

        Supplier<CompletableFuture<Void>> afterUnhealthyDelay = () -> {
            return grpcRequest.getNode().isHealthy()
                    ? CompletableFuture.completedFuture((Void) null)
                    : Delayer.delayFor(grpcRequest.getNode().getRemainingTimeForBackoff(), client.executor);
        };

        afterUnhealthyDelay.get().thenRun(() -> {
            grpcRequest
                    .getNode()
                    .channelFailedToConnectAsync()
                    .thenAccept(connectionFailed -> {
                        if (connectionFailed) {
                            var connectionException = grpcRequest.reactToConnectionFailure();
                            executeAsyncInternal(
                                    client,
                                    attempt + 1,
                                    connectionException,
                                    returnFuture,
                                    Duration.between(Instant.now(), timeoutTime));
                            return;
                        }

                        toCompletableFuture(
                                        ClientCalls.futureUnaryCall(grpcRequest.createCall(), grpcRequest.getRequest()))
                                .handle((response, error) -> {
                                    logTransaction(
                                            this.getTransactionIdInternal(),
                                            client,
                                            grpcRequest.getNode(),
                                            true,
                                            attempt,
                                            response,
                                            error);

                                    if (grpcRequest.shouldRetryExceptionally(error)) {
                                        // the transaction had a network failure reaching Hedera
                                        executeAsyncInternal(
                                                client,
                                                attempt + 1,
                                                error,
                                                returnFuture,
                                                Duration.between(Instant.now(), timeoutTime));
                                        return null;
                                    }

                                    if (error != null) {
                                        // not a network failure, some other weirdness going on; just fail fast
                                        returnFuture.completeExceptionally(new CompletionException(error));
                                        return null;
                                    }

                                    var status = mapResponseStatus(response);
                                    var executionState = getExecutionState(status, response);
                                    grpcRequest.handleResponse(response, status, executionState);

                                    switch (executionState) {
                                        case SERVER_ERROR:
                                            executeAsyncInternal(
                                                    client,
                                                    attempt + 1,
                                                    grpcRequest.mapStatusException(),
                                                    returnFuture,
                                                    Duration.between(Instant.now(), timeoutTime));
                                            break;
                                        case RETRY:
                                            Delayer.delayFor(
                                                            (attempt < maxAttempts) ? grpcRequest.getDelay() : 0,
                                                            client.executor)
                                                    .thenRun(() -> executeAsyncInternal(
                                                            client,
                                                            attempt + 1,
                                                            grpcRequest.mapStatusException(),
                                                            returnFuture,
                                                            Duration.between(Instant.now(), timeoutTime)));
                                            break;
                                        case REQUEST_ERROR:
                                            returnFuture.completeExceptionally(
                                                    new CompletionException(grpcRequest.mapStatusException()));
                                            break;
                                        case SUCCESS:
                                        default:
                                            returnFuture.complete(grpcRequest.mapResponse());
                                    }
                                    return null;
                                })
                                .exceptionally(error -> {
                                    returnFuture.completeExceptionally(error);
                                    return null;
                                });
                    })
                    .exceptionally(error -> {
                        returnFuture.completeExceptionally(error);
                        return null;
                    });
        });
    }

    abstract ProtoRequestT makeRequest();

    GrpcRequest getGrpcRequest(int attempt) {
        return new GrpcRequest(null, attempt, this.grpcDeadline);
    }

    void advanceRequest() {
        if (nodeAccountIds.getIndex() + 1 == nodes.size() - 1) {
            attemptedAllNodes = true;
        }

        nodes.advance();
        if (nodeAccountIds.size() > 1) {
            nodeAccountIds.advance();
        }
    }

    /**
     * Called after receiving the query response from Hedera. The derived class should map into its output type.
     */
    abstract O mapResponse(ResponseT response, AccountId nodeId, ProtoRequestT request);

    abstract Status mapResponseStatus(ResponseT response);

    /**
     * Called to direct the invocation of the query to the appropriate gRPC service.
     */
    abstract MethodDescriptor<ProtoRequestT, ResponseT> getMethodDescriptor();

    @Nullable
    abstract TransactionId getTransactionIdInternal();

    boolean shouldRetryExceptionally(@Nullable Throwable error) {
        if (error instanceof StatusRuntimeException statusException) {
            var status = statusException.getStatus().getCode();
            var description = statusException.getStatus().getDescription();

            return (status == Code.UNAVAILABLE)
                    || (status == Code.RESOURCE_EXHAUSTED)
                    || (status == Code.INTERNAL
                            && description != null
                            && RST_STREAM.matcher(description).matches());
        }

        return false;
    }

    /**
     * Default implementation, may be overridden in subclasses (especially for query case). Called just after receiving
     * the query response from Hedera. By default it triggers a retry when the pre-check status is {@code BUSY}.
     */
    ExecutionState getExecutionState(Status status, ResponseT response) {
        switch (status) {
            case PLATFORM_TRANSACTION_NOT_CREATED:
            case PLATFORM_NOT_ACTIVE:
                return ExecutionState.SERVER_ERROR;
            case BUSY:
                return ExecutionState.RETRY;
            case OK:
                return ExecutionState.SUCCESS;
            default:
                return ExecutionState.REQUEST_ERROR; // user error
        }
    }

    @VisibleForTesting
    class GrpcRequest {
        @Nullable
        private final Network network;

        private final Node node;
        private final int attempt;
        // private final ClientCall<ProtoRequestT, ResponseT> call;
        private final ProtoRequestT request;
        private final long startAt;
        private final long delay;
        private Duration grpcDeadline;
        private ResponseT response;
        private double latency;
        private Status responseStatus;

        GrpcRequest(@Nullable Network network, int attempt, Duration grpcDeadline) {
            this.network = network;
            this.attempt = attempt;
            this.grpcDeadline = grpcDeadline;
            this.node = getNodeForExecute(attempt);
            this.request = getRequestForExecute(); // node index gets incremented here
            this.startAt = System.nanoTime();

            // Exponential back-off for Delayer: 250ms, 500ms, 1s, 2s, 4s, 8s, ... 8s
            delay = (long) Math.min(
                    Objects.requireNonNull(minBackoff).toMillis() * Math.pow(2, attempt - 1.0),
                    Objects.requireNonNull(maxBackoff).toMillis());
        }

        public CallOptions getCallOptions() {
            long deadline = Math.min(this.grpcDeadline.toMillis(), Executable.this.grpcDeadline.toMillis());

            return CallOptions.DEFAULT.withDeadlineAfter(deadline, TimeUnit.MILLISECONDS);
        }

        public void setGrpcDeadline(Duration grpcDeadline) {
            this.grpcDeadline = grpcDeadline;
        }

        public Node getNode() {
            return node;
        }

        public ClientCall<ProtoRequestT, ResponseT> createCall() {
            verboseLog(node);
            return this.node.getChannel().newCall(Executable.this.getMethodDescriptor(), getCallOptions());
        }

        public ProtoRequestT getRequest() {
            return Executable.this.requestListener.apply(request);
        }

        public long getDelay() {
            return delay;
        }

        Throwable reactToConnectionFailure() {
            Objects.requireNonNull(network).increaseBackoff(node);
            logger.warn(
                    "Retrying in {} ms after channel connection failure with node {} during attempt #{}",
                    node.getRemainingTimeForBackoff(),
                    node.getAccountId(),
                    attempt);
            verboseLog(node);
            return new IllegalStateException("Failed to connect to node " + node.getAccountId());
        }

        boolean shouldRetryExceptionally(@Nullable Throwable e) {
            latency = (double) (System.nanoTime() - startAt) / 1000000000.0;

            var retry = Executable.this.shouldRetryExceptionally(e);

            if (retry) {
                Objects.requireNonNull(network).increaseBackoff(node);
                logger.warn(
                        "Retrying in {} ms after failure with node {} during attempt #{}: {}",
                        node.getRemainingTimeForBackoff(),
                        node.getAccountId(),
                        attempt,
                        e != null ? e.getMessage() : "NULL");
                verboseLog(node);
            }

            return retry;
        }

        PrecheckStatusException mapStatusException() {
            // request to hedera failed in a non-recoverable way
            return new PrecheckStatusException(responseStatus, Executable.this.getTransactionIdInternal());
        }

        O mapResponse() {
            // successful response from Hedera
            return Executable.this.mapResponse(response, node.getAccountId(), request);
        }

        void handleResponse(ResponseT response, Status status, ExecutionState executionState) {
            node.decreaseBackoff();

            this.response = Executable.this.responseListener.apply(response);
            this.responseStatus = status;

            logger.trace(
                    "Received {} response in {} s from node {} during attempt #{}: {}",
                    responseStatus,
                    latency,
                    node.getAccountId(),
                    attempt,
                    response);

            if (executionState == ExecutionState.SERVER_ERROR && attemptedAllNodes) {
                executionState = ExecutionState.RETRY;
                attemptedAllNodes = false;
            }
            switch (executionState) {
                case RETRY -> {
                    logger.warn(
                            "Retrying in {} ms after failure with node {} during attempt #{}: {}",
                            delay,
                            node.getAccountId(),
                            attempt,
                            responseStatus);
                    verboseLog(node);
                }
                case SERVER_ERROR -> logger.warn(
                        "Problem submitting request to node {} for attempt #{}, retry with new node: {}",
                        node.getAccountId(),
                        attempt,
                        responseStatus);
                default -> {}
            }
        }

        void verboseLog(Node node) {
            String ipAddress;
            if (node.address == null) {
                ipAddress = "NULL";
            } else if (node.address.getAddress() == null) {
                ipAddress = "NULL";
            } else {
                ipAddress = node.address.getAddress();
            }
            logger.trace(
                    "Node IP {} Timestamp {} Transaction Type {}",
                    ipAddress,
                    System.currentTimeMillis(),
                    this.getClass().getSimpleName());
        }
    }
}
