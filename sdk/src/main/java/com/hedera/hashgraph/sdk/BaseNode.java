// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ConnectivityState;
import io.grpc.ForwardingClientCall;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.TlsChannelCredentials;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * Internal utility class.
 *
 * @param <N>                           the n type
 * @param <KeyT>                        the key t type
 */
abstract class BaseNode<N extends BaseNode<N, KeyT>, KeyT> {
    private static final int GET_STATE_INTERVAL_MILLIS = 50;
    private static final int GET_STATE_TIMEOUT_MILLIS = 10000;
    private static final int GET_STATE_MAX_ATTEMPTS = GET_STATE_TIMEOUT_MILLIS / GET_STATE_INTERVAL_MILLIS;
    private boolean hasConnected = false;

    protected final ExecutorService executor;

    /**
     * Address of this node
     */
    protected final BaseNodeAddress address;

    /**
     * Timestamp of when this node will be considered healthy again
     */
    protected Instant readmitTime;

    /**
     * The current backoff duration. Uses exponential backoff so think 1s, 2s, 4s, 8s, etc until maxBackoff is hit
     */
    protected Duration currentBackoff;

    /**
     * Minimum backoff used by node when receiving a bad gRPC status
     */
    protected Duration minBackoff;

    /**
     * Maximum backoff used by node when receiving a bad gRPC status
     */
    protected Duration maxBackoff;

    /**
     * Number of times this node has received a bad gRPC status
     */
    protected long badGrpcStatusCount;

    @Nullable
    protected ManagedChannel channel = null;

    /**
     * Constructor.
     *
     * @param address                   the node address
     * @param executor                  the client
     */
    protected BaseNode(BaseNodeAddress address, ExecutorService executor) {
        this.executor = executor;
        this.address = address;
        this.currentBackoff = Client.DEFAULT_MIN_NODE_BACKOFF;
        this.minBackoff = Client.DEFAULT_MIN_NODE_BACKOFF;
        this.maxBackoff = Client.DEFAULT_MAX_NODE_BACKOFF;
        this.readmitTime = Instant.EPOCH;
    }

    /**
     * Constructor.
     *
     * @param node                      the node object
     * @param address                   the address to assign
     */
    protected BaseNode(N node, BaseNodeAddress address) {
        this.address = address;

        this.executor = node.executor;
        this.minBackoff = node.minBackoff;
        this.maxBackoff = node.maxBackoff;
        this.readmitTime = node.readmitTime;
        this.currentBackoff = node.currentBackoff;
        this.badGrpcStatusCount = node.badGrpcStatusCount;
    }

    /**
     * Return the local host ip address
     *
     * @return                          the authority address
     */
    protected String getAuthority() {
        return "127.0.0.1";
    }

    /**
     * Extract the key list
     *
     * @return                          the key list
     */
    abstract KeyT getKey();

    /**
     * Get the address of this node
     *
     * @return                          the address for the node
     */
    BaseNodeAddress getAddress() {
        return address;
    }

    /**
     * Get the minimum backoff time
     *
     * @return                          the minimum backoff time
     */
    synchronized Duration getMinBackoff() {
        return minBackoff;
    }

    /**
     * Set the minimum backoff tim
     *
     * @param minBackoff                the minimum backoff time
     * @return {@code this}
     */
    synchronized N setMinBackoff(Duration minBackoff) {
        if (currentBackoff == this.minBackoff) {
            currentBackoff = minBackoff;
        }
        this.minBackoff = minBackoff;

        // noinspection unchecked
        return (N) this;
    }

    /**
     * Get the maximum backoff time
     *
     * @return                          the maximum backoff time
     */
    Duration getMaxBackoff() {
        return maxBackoff;
    }

    /**
     * Set the maximum backoff time
     *
     * @param maxBackoff                the max backoff time
     * @return {@code this}
     */
    N setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;

        // noinspection unchecked
        return (N) this;
    }

    /**
     * Get the number of times this node has received a bad gRPC status
     *
     * @return                          the count of bad grpc status
     */
    long getBadGrpcStatusCount() {
        return badGrpcStatusCount;
    }

    /**
     * Extract the unhealthy backoff time remaining.
     *
     * @return                          the unhealthy backoff time remaining
     */
    long unhealthyBackoffRemaining() {
        return Math.max(0, readmitTime.toEpochMilli() - System.currentTimeMillis());
    }

    /**
     * Determines if this is node is healthy.
     * Healthy means the node has either not received any bad gRPC statuses, or if it has received bad gRPC status then
     * the node backed off for a period of time.
     *
     * @return                          is the node healthy
     */
    boolean isHealthy() {
        return readmitTime.toEpochMilli() < Instant.now().toEpochMilli();
    }

    /**
     * Used when a node has received a bad gRPC status
     */
    synchronized void increaseBackoff() {
        this.badGrpcStatusCount++;
        this.readmitTime = Instant.now().plus(this.currentBackoff);
        this.currentBackoff = currentBackoff.multipliedBy(2);
        this.currentBackoff = currentBackoff.compareTo(maxBackoff) < 0 ? currentBackoff : maxBackoff;
    }

    /**
     * Used when a node has not received a bad gRPC status.
     * This means on each request that doesn't get a bad gRPC status the current backoff will be lowered. The point of
     * this is to allow a node which has been performing poorly (receiving several bad gRPC status) to become used again
     * once it stops receiving bad gRPC statuses.
     */
    synchronized void decreaseBackoff() {
        this.currentBackoff = currentBackoff.dividedBy(2);
        this.currentBackoff = currentBackoff.compareTo(minBackoff) > 0 ? currentBackoff : minBackoff;
    }

    /**
     * Get the amount of time the node has to wait until it's healthy again
     *
     * @return                          remaining back off time
     */
    long getRemainingTimeForBackoff() {
        return readmitTime.toEpochMilli() - System.currentTimeMillis();
    }

    /**
     * Create TLS credentials when transport security is enabled
     *
     * @return                          the channel credentials
     */
    ChannelCredentials getChannelCredentials() {
        return TlsChannelCredentials.create();
    }

    /**
     * Get the gRPC channel for this node
     *
     * @return                          the channel
     */
    synchronized ManagedChannel getChannel() {
        if (channel != null) {
            return channel;
        }

        ManagedChannelBuilder<?> channelBuilder;

        if (address.isInProcess()) {
            channelBuilder = InProcessChannelBuilder.forName(Objects.requireNonNull(address.getName()));
        } else if (address.isTransportSecurity()) {
            channelBuilder = Grpc.newChannelBuilder(address.toString(), getChannelCredentials());

            String authority = getAuthority();
            if (authority != null) {
                channelBuilder = channelBuilder.overrideAuthority(authority);
            }
        } else {
            channelBuilder = ManagedChannelBuilder.forTarget(address.toString()).usePlaintext();
        }

        channel = channelBuilder
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .intercept(new MetadataInterceptor())
                .enableRetry()
                .executor(executor)
                .build();

        return channel;
    }

    /**
     * Did we fail to connect?
     *
     * @return                          did we fail to connect
     */
    boolean channelFailedToConnect() {
        return channelFailedToConnect(Instant.MAX);
    }

    boolean channelFailedToConnect(Instant timeoutTime) {
        if (hasConnected) {
            return false;
        }
        hasConnected = (getChannel().getState(true) == ConnectivityState.READY);
        try {
            for (int i = 0; i < GET_STATE_MAX_ATTEMPTS && !hasConnected; i++) {
                Duration currentTimeout = Duration.between(Instant.now(), timeoutTime);
                if (currentTimeout.isNegative() || currentTimeout.isZero()) {
                    return false;
                }
                TimeUnit.MILLISECONDS.sleep(GET_STATE_INTERVAL_MILLIS);
                hasConnected = (getChannel().getState(true) == ConnectivityState.READY);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return !hasConnected;
    }

    private CompletableFuture<Boolean> channelFailedToConnectAsync(int i, ConnectivityState state) {
        hasConnected = (state == ConnectivityState.READY);
        if (i >= GET_STATE_MAX_ATTEMPTS || hasConnected) {
            return CompletableFuture.completedFuture(!hasConnected);
        }
        return Delayer.delayFor(GET_STATE_INTERVAL_MILLIS, executor).thenCompose(ignored -> {
            return channelFailedToConnectAsync(i + 1, getChannel().getState(true));
        });
    }

    /**
     * Asynchronously determine if the channel failed to connect.
     *
     * @return                          did we fail to connect
     */
    CompletableFuture<Boolean> channelFailedToConnectAsync() {
        if (hasConnected) {
            return CompletableFuture.completedFuture(false);
        }
        return channelFailedToConnectAsync(0, getChannel().getState(true));
    }

    /**
     * Close the current nodes channel
     *
     * @param timeout                   the timeout value
     * @throws InterruptedException     thrown when a thread is interrupted while it's waiting, sleeping, or otherwise occupied
     */
    synchronized void close(Duration timeout) throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
            channel = null;
        }
    }

    /**
     * Extract the user agent. This information is used to gather usage metrics.
     * If the version is not available, the user agent will be set to "hiero-sdk-java/DEV".
     */
    private String getUserAgent() {
        var thePackage = getClass().getPackage();
        var implementationVersion = thePackage != null ? thePackage.getImplementationVersion() : null;
        return "hiero-sdk-java/" + ((implementationVersion != null) ? (implementationVersion) : "DEV");
    }

    class MetadataInterceptor implements ClientInterceptor {
        private final Metadata metadata;

        public MetadataInterceptor() {
            metadata = new Metadata();
            Metadata.Key<String> authKey = Metadata.Key.of("x-user-agent", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(authKey, getUserAgent()); // TODO optimise this to be called only once
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
            return new ForwardingClientCall.SimpleForwardingClientCall<>(call) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    headers.merge(metadata);
                    super.start(responseListener, headers);
                }
            };
        }
    }
}
