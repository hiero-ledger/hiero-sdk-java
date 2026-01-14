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

/**
 * Replacement for MirrorNodeUrlBuilderTest validating base URL generation via Client/MirrorNode.
 */
public class ClientMirrorBaseUrlTest {

    private ExecutorService executor;

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
    void hostPort_customPort_preserved_https_whenLedgerSet() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor, List.of("mirror.example.com:8080"));
        var client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);
        client.setLedgerId(LedgerId.TESTNET);

        String base = client.getMirrorRestBaseUrl();
        assertThat(base).isEqualTo("https://mirror.example.com:8080/api/v1");
    }

    @Test
    void hostPort_defaultHttpsPort_omitted() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor, List.of("mirror.example.com:443"));
        var client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);
        client.setLedgerId(LedgerId.TESTNET);

        String base = client.getMirrorRestBaseUrl();
        assertThat(base).isEqualTo("https://mirror.example.com/api/v1");
    }

    @Test
    void localNetwork_regularQuery_swaps5600to5551() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor, List.of("localhost:5600"));
        var client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);

        String base = client.getMirrorRestBaseUrl();
        assertThat(base).isEqualTo("http://localhost:5551/api/v1");
    }

    @Test
    void localNetwork_contractCall_swaps5600to5551() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor, List.of("127.0.0.1:5600"));
        var client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);

        String base = client.getMirrorRestBaseUrl();
        assertThat(base).isEqualTo("http://127.0.0.1:5551/api/v1");
    }

    @Test
    void emptyMirrorNetwork_throws_whenAccessingBase() {
        var network = Network.forNetwork(executor, new HashMap<>());
        var mirrorNetwork = MirrorNetwork.forNetwork(executor, List.of());
        var client = new Client(executor, network, mirrorNetwork, null, true, null, 0, 0);

        assertThatThrownBy(() -> client.getMirrorRestBaseUrl()).isInstanceOf(RuntimeException.class);
    }
}
