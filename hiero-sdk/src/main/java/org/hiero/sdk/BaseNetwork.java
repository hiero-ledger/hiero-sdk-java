// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;

/**
 * Abstracts away most of the similar functionality between {@link Network} and {@link MirrorNetwork}
 *
 * @param <BaseNetworkT> - The network that is extending this class. This is used for builder pattern setter methods.
 * @param <KeyT> - The identifying type for the network.
 * @param <BaseNodeT> - The specific node type for this network.
 */
abstract class BaseNetwork<
        BaseNetworkT extends BaseNetwork<BaseNetworkT, KeyT, BaseNodeT>,
        KeyT,
        BaseNodeT extends BaseNode<BaseNodeT, KeyT>> {
    protected static final Integer DEFAULT_MAX_NODE_ATTEMPTS = -1;
    protected static final Random random = new Random();

    protected final ExecutorService executor;

    /**
     * Map of node identifiers to nodes. Used to quickly fetch node for identifier.
     */
    protected Map<KeyT, List<BaseNodeT>> network = new ConcurrentHashMap<>();

    /**
     * The list of all nodes.
     */
    protected List<BaseNodeT> nodes = new ArrayList<>();

    /**
     * The list of currently healthy nodes.
     */
    protected List<BaseNodeT> healthyNodes = new ArrayList<>();

    /**
     * The current minimum backoff for the nodes in the network. This backoff is used when nodes return a bad
     * gRPC status.
     */
    protected Duration minNodeBackoff = Client.DEFAULT_MIN_NODE_BACKOFF;

    /**
     * The current maximum backoff for the nodes in the network. This backoff is used when nodes return a bad
     * gRPC status.
     */
    protected Duration maxNodeBackoff = Client.DEFAULT_MAX_NODE_BACKOFF;

    /**
     * Timeout for closing either a single node when setting a new network, or closing the entire network.
     */
    protected Duration closeTimeout = Client.DEFAULT_CLOSE_TIMEOUT;

    /**
     * Limit for how many times we retry a node which has returned a bad gRPC status
     */
    protected int maxNodeAttempts = DEFAULT_MAX_NODE_ATTEMPTS;

    /**
     * Is the network using transport security
     */
    protected boolean transportSecurity;

    /**
     * The min time to wait before attempting to readmit nodes.
     */
    protected Duration minNodeReadmitTime = Client.DEFAULT_MIN_NODE_BACKOFF;

    /**
     * The max time to wait for readmitting nodes.
     */
    protected Duration maxNodeReadmitTime = Client.DEFAULT_MAX_NODE_BACKOFF;

    /**
     * The instant that readmission will happen after.
     */
    protected Instant earliestReadmitTime;

    /**
     * The name of the network. This corresponds to ledger ID in entity ID checksum calculations
     */
    @Nullable
    private LedgerId ledgerId;

    @VisibleForTesting
    boolean hasShutDownNow = false;

    protected BaseNetwork(ExecutorService executor) {
        this.executor = executor;
        earliestReadmitTime = Instant.now().plus(minNodeReadmitTime);
    }

    /**
     * Extract the ledger id.
     *
     * @return                          the ledger id
     */
    @Nullable
    synchronized LedgerId getLedgerId() {
        return ledgerId;
    }

    /**
     * Set the new LedgerId for this network. LedgerIds are used for TLS certificate checking and entity ID
     * checksum validation.
     *
     * @param ledgerId                  the ledger id
     * @return {@code this}
     */
    synchronized BaseNetworkT setLedgerId(@Nullable LedgerId ledgerId) {
        this.ledgerId = ledgerId;

        // noinspection unchecked
        return (BaseNetworkT) this;
    }

    /**
     * Extract the node attempts.
     *
     * @return                          maximum node attempts
     */
    synchronized int getMaxNodeAttempts() {
        return maxNodeAttempts;
    }

    /**
     * Set the max number of times a node can return a bad gRPC status before we remove it from the list.
     *
     * @param maxNodeAttempts           the max node attempts
     * @return {@code this}
     */
    synchronized BaseNetworkT setMaxNodeAttempts(int maxNodeAttempts) {
        this.maxNodeAttempts = maxNodeAttempts;

        // noinspection unchecked
        return (BaseNetworkT) this;
    }

    /**
     * Extract the minimum node backoff time.
     *
     * @return                          the minimum node backoff time
     */
    synchronized Duration getMinNodeBackoff() {
        return minNodeBackoff;
    }

    /**
     * Set the minimum backoff a node should use when receiving a bad gRPC status.
     *
     * @param minNodeBackoff            the min node backoff
     * @return {@code this}
     */
    synchronized BaseNetworkT setMinNodeBackoff(Duration minNodeBackoff) {
        this.minNodeBackoff = minNodeBackoff;

        for (var node : nodes) {
            node.setMinBackoff(minNodeBackoff);
        }

        // noinspection unchecked
        return (BaseNetworkT) this;
    }

    /**
     * Extract the maximum node backoff time.
     *
     * @return                          the maximum node backoff time
     */
    synchronized Duration getMaxNodeBackoff() {
        return maxNodeBackoff;
    }

    /**
     * Set the maximum backoff a node should use when receiving a bad gRPC status.
     *
     * @param maxNodeBackoff            the max node backoff
     * @return {@code this}
     */
    synchronized BaseNetworkT setMaxNodeBackoff(Duration maxNodeBackoff) {
        this.maxNodeBackoff = maxNodeBackoff;

        for (var node : nodes) {
            node.setMaxBackoff(maxNodeBackoff);
        }

        // noinspection unchecked
        return (BaseNetworkT) this;
    }

    /**
     * Extract the minimum node readmit time.
     *
     * @return                          the minimum node readmit time
     */
    public synchronized Duration getMinNodeReadmitTime() {
        return minNodeReadmitTime;
    }

    /**
     * Assign the minimum node readmit time.
     *
     * @param minNodeReadmitTime        the minimum node readmit time
     */
    public synchronized void setMinNodeReadmitTime(Duration minNodeReadmitTime) {
        this.minNodeReadmitTime = minNodeReadmitTime;

        for (var node : nodes) {
            node.readmitTime = Instant.now();
        }
    }

    /**
     * Extract the maximum node readmit time.
     *
     * @return                          the maximum node readmit time
     */
    public Duration getMaxNodeReadmitTime() {
        return maxNodeReadmitTime;
    }

    /**
     * Assign the maximum node readmit time.
     *
     * @param maxNodeReadmitTime        the maximum node readmit time
     */
    public void setMaxNodeReadmitTime(Duration maxNodeReadmitTime) {
        this.maxNodeReadmitTime = maxNodeReadmitTime;
    }

    /**
     * Is transport Security enabled?
     *
     * @return                          using transport security
     */
    boolean isTransportSecurity() {
        return transportSecurity;
    }

    /**
     * Extract the close timeout.
     *
     * @return                          the close timeout
     */
    synchronized Duration getCloseTimeout() {
        return closeTimeout;
    }

    /**
     * Assign the close timeout.
     *
     * @param closeTimeout              the close timeout
     * @return {@code this}
     */
    synchronized BaseNetworkT setCloseTimeout(Duration closeTimeout) {
        this.closeTimeout = closeTimeout;

        // noinspection unchecked
        return (BaseNetworkT) this;
    }

    protected abstract BaseNodeT createNodeFromNetworkEntry(Map.Entry<String, KeyT> entry);

    /**
     * Returns a list of index in descending order to remove from the current node list.
     *
     * Descending order is important here because {@link BaseNetwork#setNetwork(Map<String, KeyT>)} uses a for-each loop.
     *
     * @param network - the new network
     * @return - list of indexes in descending order
     */
    protected List<Integer> getNodesToRemove(Map<String, KeyT> network) {
        var nodes = new ArrayList<Integer>(this.nodes.size());

        for (int i = this.nodes.size() - 1; i >= 0; i--) {
            var node = this.nodes.get(i);

            if (!nodeIsInGivenNetwork(node, network)) {
                nodes.add(i);
            }
        }

        return nodes;
    }

    private boolean nodeIsInGivenNetwork(BaseNodeT node, Map<String, KeyT> network) {
        for (var entry : network.entrySet()) {
            if (node.getKey().equals(entry.getValue())
                    && node.address.equals(BaseNodeAddress.fromString(entry.getKey()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Intelligently overwrites the current network.
     *
     * Shutdown and remove any node from the current network if the new network doesn't contain it. This includes
     * checking both the URL and {@link AccountId} when the network is a {@link Network}.
     *
     * Add any nodes from the new network that don't already exist in the network.
     *
     * @param network - The new network
     * @return - {@code this}
     * @throws TimeoutException - when shutting down nodes
     * @throws InterruptedException - when acquiring the lock
     */
    synchronized BaseNetworkT setNetwork(Map<String, KeyT> network) throws TimeoutException, InterruptedException {
        var newNodes = new ArrayList<BaseNodeT>();
        var newHealthyNodes = new ArrayList<BaseNodeT>();
        var newNetwork = new HashMap<KeyT, List<BaseNodeT>>();
        var newNodeKeys = new HashSet<KeyT>();
        var newNodeAddresses = new HashSet<String>();

        // getNodesToRemove() should always return the list in reverse order
        for (var index : getNodesToRemove(network)) {
            var stopAt = Instant.now().getEpochSecond() + closeTimeout.getSeconds();
            var remainingTime = stopAt - Instant.now().getEpochSecond();
            var node = nodes.get(index);

            // Exit early if we have no time remaining
            if (remainingTime <= 0) {
                throw new TimeoutException("Failed to properly shutdown all channels");
            }

            removeNodeFromNetwork(node);
            node.close(Duration.ofSeconds(remainingTime));
            this.nodes.remove(index.intValue());
        }

        for (var node : this.nodes) {
            newNodes.add(node);
            newNodeKeys.add(node.getKey());
            newNodeAddresses.add(node.address.toString());
        }

        for (var entry : network.entrySet()) {
            var node = createNodeFromNetworkEntry(entry);

            if (newNodeKeys.contains(node.getKey())
                    && newNodeAddresses.contains(node.getAddress().toString())) {
                continue;
            }

            newNodes.add(node);
        }

        for (var node : newNodes) {
            if (newNetwork.containsKey(node.getKey())) {
                newNetwork.get(node.getKey()).add(node);
            } else {
                var list = new ArrayList<BaseNodeT>();
                list.add(node);
                newNetwork.put(node.getKey(), list);
            }

            newHealthyNodes.add(node);
        }

        // Atomically set all the variables
        nodes = newNodes;
        this.network = newNetwork;
        healthyNodes = newHealthyNodes;

        // noinspection unchecked
        return (BaseNetworkT) this;
    }

    synchronized void increaseBackoff(BaseNodeT node) {
        node.increaseBackoff();
        healthyNodes.remove(node);
    }

    synchronized void decreaseBackoff(BaseNodeT node) {
        node.decreaseBackoff();
    }

    private void removeNodeFromNetwork(BaseNodeT node) {
        var nodesForKey = this.network.get(node.getKey());
        nodesForKey.remove(node);
        if (nodesForKey.isEmpty()) {
            this.network.remove(node.getKey());
        }
    }

    private boolean addressIsInNodeList(String addressString, List<BaseNodeT> nodes) {
        var address = BaseNodeAddress.fromString(addressString);
        for (var node : nodes) {
            if (node.address.equals(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove any nodes from the network when they've exceeded the {@link BaseNetwork#maxNodeAttempts} limit
     *
     * @throws InterruptedException - when shutting down nodes
     */
    protected void removeDeadNodes() throws InterruptedException {
        if (maxNodeAttempts > 0) {
            for (int i = nodes.size() - 1; i >= 0; i--) {
                var node = Objects.requireNonNull(nodes.get(i));

                if (node.getBadGrpcStatusCount() >= maxNodeAttempts) {
                    node.close(closeTimeout);
                    removeNodeFromNetwork(node);
                    nodes.remove(i);
                }
            }
        }
    }

    /**
     * Readmits nodes from the `nodes` list into the `healthyNodes` list when the time is passed the
     * {@code earliestReadmitTime}. While readmitting nodes the `earliestReadmitTime` will be updated to
     * a new value. This value is either the value of the node with the smallest readmission time from now,
     * or `minNodeReadmitTime` or `maxNodeReadmitTime`.
     */
    synchronized void readmitNodes() {
        var now = Instant.now();

        if (now.toEpochMilli() > earliestReadmitTime.toEpochMilli()) {
            var nextEarliestReadmitTime = now.plus(maxNodeReadmitTime);

            for (var node : this.nodes) {
                if (node.readmitTime.isAfter(now) && node.readmitTime.isBefore(nextEarliestReadmitTime)) {
                    nextEarliestReadmitTime = node.readmitTime;
                }
            }

            this.earliestReadmitTime = nextEarliestReadmitTime;
            if (this.earliestReadmitTime.isBefore(now.plus(minNodeReadmitTime))) {
                this.earliestReadmitTime = now.plus(minNodeReadmitTime);
            }

            outer:
            for (var i = 0; i < this.nodes.size(); i++) {
                // Check if `healthyNodes` already contains this node
                for (var j = 0; j < this.healthyNodes.size(); j++) {
                    if (this.nodes.get(i) == this.healthyNodes.get(j)) {
                        continue outer;
                    }
                }

                // If `healthyNodes` doesn't contain the node, check the `readmitTime` on the node
                if (this.nodes.get(i).readmitTime.isBefore(now)) {
                    this.healthyNodes.add(this.nodes.get(i));
                }
            }
        }
    }

    /**
     * Get a random healthy node.
     *
     * @return                          the node
     */
    synchronized BaseNodeT getRandomNode() {
        // Attempt to readmit nodes each time a node is fetched.
        // Note: Readmitting nodes will only happen periodically so calling it each time should not harm
        // performance.
        readmitNodes();

        if (healthyNodes.isEmpty()) {
            throw new IllegalStateException("No healthy node was found");
        }

        return healthyNodes.get(random.nextInt(healthyNodes.size()));
    }

    /**
     * Get all node proxies by key
     *
     * @param key                       the desired key
     * @return                          the list of node proxies
     */
    synchronized List<BaseNodeT> getNodeProxies(KeyT key) {
        // Attempt to readmit nodes each time a node is fetched.
        // Note: Readmitting nodes will only happen periodically so calling it each time should not harm
        // performance.
        readmitNodes();

        return network.get(key);
    }

    /**
     * Returns `count` number of the most healthy nodes. Healthy-ness is determined by sort order; leftmost being most
     * healthy. This will also remove any nodes which have hit or exceeded {@link BaseNetwork#maxNodeAttempts}.
     *
     * Returns a list of nodes where each node has a unique key.
     *
     * @param count                     number of nodes to return
     * @return                          List of nodes to use
     * @throws InterruptedException     when a thread is interrupted while it's waiting, sleeping, or otherwise occupied
     */
    protected synchronized List<BaseNodeT> getNumberOfMostHealthyNodes(int count) throws InterruptedException {
        readmitNodes();
        removeDeadNodes();

        var returnNodes = new HashMap<KeyT, BaseNodeT>(count);

        for (var i = 0; i < count; i++) {
            var node = getRandomNode();

            if (!returnNodes.containsKey(node.getKey())) {
                returnNodes.put(node.getKey(), node);
            }
        }

        var returnList = new ArrayList<BaseNodeT>();
        returnList.addAll(returnNodes.values());
        return returnList;
    }

    synchronized void beginClose() {
        for (var node : nodes) {
            if (node.channel != null) {
                node.channel = node.channel.shutdown();
            }
        }
    }

    // returns null if successful, or Throwable if error occurred
    @Nullable
    synchronized Throwable awaitClose(Instant deadline, @Nullable Throwable previousError) {
        try {
            if (previousError != null) {
                throw previousError;
            }

            for (var node : nodes) {
                if (node.channel != null) {
                    var timeoutMillis =
                            Duration.between(Instant.now(), deadline).toMillis();
                    if (timeoutMillis <= 0 || !node.channel.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
                        throw new TimeoutException("Failed to properly shutdown all channels");
                    } else {
                        node.channel = null;
                    }
                }
            }

            return null;
        } catch (Throwable error) {
            for (var node : nodes) {
                if (node.channel != null) {
                    node.channel.shutdownNow();
                }
            }
            hasShutDownNow = true;

            return error;
        } finally {
            nodes.clear();
            network.clear();
        }
    }
}

