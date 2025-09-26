// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MirrorNodeUrlBuilderTest {

    private ExecutorService executor;
    private Client client;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    void testBuildApiUrlWithHostPortFormat() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor,
            List.of("mirror.example.com:8080"));
        client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);
        client.setLedgerId(LedgerId.TESTNET); // Set ledger ID to avoid local network behavior

        String result = MirrorNodeUrlBuilder.buildApiUrl(client, "/accounts/0x123", false);

        assertThat(result).isEqualTo("https://mirror.example.com:8080/api/v1/accounts/0x123");
    }

    @Test
    void testBuildApiUrlWithDefaultHttpsPort() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor,
            List.of("mirror.example.com:443"));
        client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);
        client.setLedgerId(LedgerId.TESTNET); // Set ledger ID to avoid local network behavior

        String result = MirrorNodeUrlBuilder.buildApiUrl(client, "/accounts/0x123", false);

        assertThat(result).isEqualTo("https://mirror.example.com/api/v1/accounts/0x123");
    }

    @Test
    void testBuildApiUrlForLocalNetworkWithContractCall() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor,
            List.of("mirror.example.com:8080"));
        client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);

        String result = MirrorNodeUrlBuilder.buildApiUrl(client, "/contracts/call", true);

        assertThat(result).isEqualTo("http://mirror.example.com:8545/api/v1/contracts/call");
    }

    @Test
    void testBuildApiUrlForLocalNetworkWithoutContractCall() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor,
            List.of("mirror.example.com:8080"));
        client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);

        String result = MirrorNodeUrlBuilder.buildApiUrl(client, "/accounts/0x123", false);

        assertThat(result).isEqualTo("http://mirror.example.com:5551/api/v1/accounts/0x123");
    }

    @Test
    void testBuildApiUrlWithEmptyMirrorNetwork() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor, List.of());
        client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);

        assertThatThrownBy(() -> MirrorNodeUrlBuilder.buildApiUrl(client, "/accounts/0x123", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Mirror URL not found");
    }
}
