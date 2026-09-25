// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class.
 */
class MirrorNetwork extends BaseNetwork<MirrorNetwork, BaseNodeAddress, MirrorNode> {
    /**
     * The cursor behind {@link #getNextRestBaseUrlRoundRobin()}. It is the only shared mutable state on
     * the mirror REST hot path, and a client is explicitly concurrent, so it advances atomically.
     */
    private final AtomicInteger restBaseUrlCursor = new AtomicInteger();

    private MirrorNetwork(ExecutorService executor, List<String> addresses) {
        super(executor);
        this.transportSecurity = true;
        try {
            setNetwork(addresses);
        } catch (InterruptedException | TimeoutException e) {
            // This should never occur. The network is empty.
        }
    }

    /**
     * Create an arbitrary mirror network.
     *
     * @param executor  the executor service
     * @param addresses the arbitrary address for the network
     * @return the new mirror network object
     */
    static MirrorNetwork forNetwork(ExecutorService executor, List<String> addresses) {
        return new MirrorNetwork(executor, addresses);
    }

    /**
     * Create a mirror network for mainnet.
     *
     * @param executor the executor service
     * @return the new mirror network for mainnet
     */
    static MirrorNetwork forMainnet(ExecutorService executor) {
        return new MirrorNetwork(executor, List.of("mainnet-public.mirrornode.hedera.com:443"));
    }

    /**
     * Create a mirror network for testnet.
     *
     * @param executor the executor service
     * @return the new mirror network for testnet
     */
    static MirrorNetwork forTestnet(ExecutorService executor) {
        return new MirrorNetwork(executor, List.of("testnet.mirrornode.hedera.com:443"));
    }

    /**
     * Create a mirror network for previewnet.
     *
     * @param executor the executor service
     * @return the new mirror network for previewnet
     */
    static MirrorNetwork forPreviewnet(ExecutorService executor) {
        return new MirrorNetwork(executor, List.of("previewnet.mirrornode.hedera.com:443"));
    }

    /**
     * Extract the network names.
     *
     * @return the network names
     */
    synchronized List<String> getNetwork() {
        List<String> retval = new ArrayList<>(network.size());
        for (var address : network.keySet()) {
            retval.add(address.toString());
        }
        return retval;
    }

    /**
     * Assign the desired network.
     *
     * @param network the desired network
     * @return the mirror network
     * @throws TimeoutException     when the transaction times out
     * @throws InterruptedException when a thread is interrupted while it's waiting, sleeping, or otherwise occupied
     */
    synchronized MirrorNetwork setNetwork(List<String> network) throws TimeoutException, InterruptedException {
        var map = new HashMap<String, BaseNodeAddress>(network.size());
        for (var address : network) {
            map.put(address, BaseNodeAddress.fromString(address));
        }
        return super.setNetwork(map);
    }

    @Override
    protected MirrorNode createNodeFromNetworkEntry(Map.Entry<String, BaseNodeAddress> entry) {
        return new MirrorNode(entry.getKey(), executor);
    }

    /**
     * Extract the next healthy mirror node on the list.
     *
     * @return the next healthy mirror node on the list
     * @throws InterruptedException when a thread is interrupted while it's waiting, sleeping, or otherwise occupied
     */
    synchronized MirrorNode getNextMirrorNode() throws InterruptedException {
        return getNumberOfMostHealthyNodes(1).get(0);
    }

    /**
     * Convenience to get the REST base URL from the next healthy mirror node.
     */
    String getRestBaseUrl() throws InterruptedException {
        return getNextMirrorNode().getRestBaseUrl();
    }

    /**
     * The REST base URL for one mirror node REST call, chosen by round-robin.
     *
     * <p>Distinct from {@link #getRestBaseUrl()}, which picks by health order and is what the
     * consensus-era mirror REST call sites still use. Round-robin is what the HTTP transport proposal
     * specifies: drawing at random from a list nothing ever demotes makes a two-node network with one
     * node down a coin flip that retrying cannot escape, re-flipped on the next call.
     *
     * <p>A transport failure does not mark a mirror node unhealthy. Demotion on failure is the right end
     * state and a different design; it is excluded deliberately rather than forgotten.
     *
     * @return the base URL to pin for the whole call
     */
    synchronized String getNextRestBaseUrlRoundRobin() {
        readmitNodes();

        if (healthyNodes.isEmpty()) {
            throw new IllegalStateException("this client has no mirror network configured");
        }

        var index = Math.floorMod(restBaseUrlCursor.getAndIncrement(), healthyNodes.size());
        return healthyNodes.get(index).getRestBaseUrl();
    }
}
