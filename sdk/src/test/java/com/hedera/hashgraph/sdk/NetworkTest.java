// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NetworkTest {

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("When maxNodesPerRequest is NOT set, return full network size")
    void getNumberOfNodesForRequestReturnsFullNetworkSizeWhenNotSet() {
        Network network = createNetwork(3);

        // When maxNodesPerRequest is not set, should return full network size
        int numberOfNodes = network.getNumberOfNodesForRequest();
        assertThat(numberOfNodes).isEqualTo(3);
    }

    @Test
    @DisplayName("When maxNodesPerRequest IS set, return the limit if network is larger")
    void getNumberOfNodesForRequestReturnsMaxWhenSetAndLessThanNetworkSize() {
        Network network = createNetwork(5);

        // Set maxNodesPerRequest to 2
        network.setMaxNodesPerRequest(2);

        // Should return 2
        int numberOfNodes = network.getNumberOfNodesForRequest();
        assertThat(numberOfNodes).isEqualTo(2);
    }

    @Test
    @DisplayName("When maxNodesPerRequest IS set, return actual size if network is smaller than limit")
    void getNumberOfNodesForRequestReturnsNetworkSizeWhenMaxIsGreater() {
        Network network = createNetwork(2);

        // Set maxNodesPerRequest to 10 (greater than network size)
        network.setMaxNodesPerRequest(10);

        // Should return 2 (the network size, not 10)
        int numberOfNodes = network.getNumberOfNodesForRequest();
        assertThat(numberOfNodes).isEqualTo(2);
    }

    @Test
    @DisplayName("When maxNodesPerRequest equals network size, return network size")
    void getNumberOfNodesForRequestReturnsNetworkSizeWhenMaxEqualsNetworkSize() {
        Network network = createNetwork(4);

        // Set maxNodesPerRequest to 4 (equals network size)
        network.setMaxNodesPerRequest(4);

        // Should return 4
        int numberOfNodes = network.getNumberOfNodesForRequest();
        assertThat(numberOfNodes).isEqualTo(4);
    }

    @Test
    @DisplayName("Single node network returns 1")
    void getNumberOfNodesForRequestReturnsOneForSingleNodeNetwork() {
        Network network = createNetwork(1);

        // Should return 1
        int numberOfNodes = network.getNumberOfNodesForRequest();
        assertThat(numberOfNodes).isEqualTo(1);
    }

    @Test
    @DisplayName("Empty network returns 0")
    void getNumberOfNodesForRequestReturnsZeroForEmptyNetwork() {
        Network network = createNetwork(0);

        // Should return 0
        int numberOfNodes = network.getNumberOfNodesForRequest();
        assertThat(numberOfNodes).isEqualTo(0);
    }

    /**
     * Helper method to generate a network of a specific size.
     */
    private Network createNetwork(int nodeCount) {
        Map<String, AccountId> networkMap = new HashMap<>();
        for (int i = 0; i < nodeCount; i++) {
            // Generate dummy node addresses and IDs
            networkMap.put(i + ".testnet.hedera.com:50211", new AccountId(0, 0, 3 + i));
        }
        return Network.forNetwork(executor, networkMap);
    }
}
