// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ClientIntegrationTest {

    @Test
    @DisplayName("fails when all the manually set nodes are not matching the address book")
    void failsWhenNoNodesAreMatching() throws Exception {
        var client = Client.forTestnet().setTransportSecurity(true);

        var nodes = new ArrayList<AccountId>();
        nodes.add(new AccountId(0, 0, 1000));
        nodes.add(new AccountId(0, 0, 1001));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new AccountBalanceQuery()
                        .setNodeAccountIds(nodes)
                        .setAccountId(new AccountId(0, 0, 7))
                        .execute(client))
                .withMessageContaining("All node account IDs did not map to valid nodes in the client's network");
        client.close();
    }

    @Test
    @DisplayName("can skip invalid nodes")
    void canSkipNodes() throws Exception {
        var client = Client.forTestnet().setTransportSecurity(true);

        var nodes = new ArrayList<>(client.getNetwork().values().stream().toList());
        nodes.add(new AccountId(0, 0, 1000));
        new AccountBalanceQuery()
                .setNodeAccountIds(nodes)
                .setAccountId(new AccountId(0, 0, 7))
                .execute(client);

        client.close();
    }

    @Test
    @DisplayName("setNetwork() functions correctly")
    void testReplaceNodes() throws Exception {
        Map<String, AccountId> network = new HashMap<>();
        network.put("0.testnet.hedera.com:50211", new AccountId(0, 0, 3));
        network.put("1.testnet.hedera.com:50211", new AccountId(0, 0, 4));

        try (var testEnv = new IntegrationTestEnv(1)) {

            testEnv.client
                    .setMaxQueryPayment(new Hbar(2))
                    .setRequestTimeout(Duration.ofMinutes(2))
                    .setNetwork(network);

            assertThat(testEnv.operatorId).isNotNull();

            // Execute two simple queries so we create a channel for each network node.
            new AccountBalanceQuery().setAccountId(new AccountId(0, 0, 3)).execute(testEnv.client);

            new AccountBalanceQuery().setAccountId(new AccountId(0, 0, 3)).execute(testEnv.client);

            network = new HashMap<>();
            network.put("1.testnet.hedera.com:50211", new AccountId(0, 0, 4));
            network.put("2.testnet.hedera.com:50211", new AccountId(0, 0, 5));

            testEnv.client.setNetwork(network);

            network = new HashMap<>();
            network.put("35.186.191.247:50211", new AccountId(0, 0, 4));
            network.put("35.192.2.25:50211", new AccountId(0, 0, 5));

            testEnv.client.setNetwork(network);
        }
    }

    @Test
    void transactionIdNetworkIsVerified() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            var client = Client.forPreviewnet();
            client.setAutoValidateChecksums(true);

            new AccountCreateTransaction()
                    .setTransactionId(TransactionId.generate(AccountId.fromString("0.0.123-esxsf")))
                    .execute(client);
            client.close();
        });
    }

    @Test
    @DisplayName("`setMaxNodesPerTransaction()`")
    void testMaxNodesPerTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            testEnv.client.setMaxNodesPerTransaction(1);

            var transaction = new AccountDeleteTransaction()
                    .setAccountId(testEnv.operatorId)
                    .freezeWith(testEnv.client);

            assertThat(transaction.getNodeAccountIds()).isNotNull();
            assertThat(transaction.getNodeAccountIds().size()).isEqualTo(1);
        }
    }

    @Test
    void ping() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var network = testEnv.client.getNetwork();
            var nodes = new ArrayList<>(network.values());

            assertThat(nodes.isEmpty()).isFalse();

            var node = nodes.get(0);

            testEnv.client.setMaxNodeAttempts(1);
            testEnv.client.ping(node);
        }
    }

    @Test
    void pingAll() throws Exception {
        try (var testEnv = new IntegrationTestEnv()) {

            testEnv.client.setMaxNodeAttempts(1);
            testEnv.client.pingAll();

            var network = testEnv.client.getNetwork();
            var nodes = new ArrayList<>(network.values());

            assertThat(nodes.isEmpty()).isFalse();

            var node = nodes.get(0);

            new AccountBalanceQuery().setAccountId(node).execute(testEnv.client);
        }
    }

    @Test
    void pingAllBadNetwork() throws Exception {
        try (var testEnv = new IntegrationTestEnv(3)) {

            // Skip if using local node.
            // Note: this check should be removed once the local node is supporting multiple nodes.
            testEnv.assumeNotLocalNode();

            testEnv.client.setMaxNodeAttempts(1);
            testEnv.client.setMaxAttempts(1);
            testEnv.client.setMaxNodesPerTransaction(2);

            var network = testEnv.client.getNetwork();

            var entries = new ArrayList<>(network.entrySet());
            assertThat(entries.size()).isGreaterThan(1);

            network.clear();
            network.put("in-process:name", entries.get(0).getValue());
            network.put(entries.get(1).getKey(), entries.get(1).getValue());

            testEnv.client.setNetwork(network);

            assertThatExceptionOfType(MaxAttemptsExceededException.class)
                    .isThrownBy(() -> {
                        testEnv.client.pingAll();
                    })
                    .withMessageContaining("exceeded maximum attempts");

            var nodes = new ArrayList<>(testEnv.client.getNetwork().values());
            assertThat(nodes.isEmpty()).isFalse();

            var node = nodes.get(0);

            new AccountBalanceQuery().setAccountId(node).execute(testEnv.client);

            assertThat(testEnv.client.getNetwork().values().size()).isEqualTo(1);
        }
    }

    @Test
    void pingAsync() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var network = testEnv.client.getNetwork();
            var nodes = new ArrayList<>(network.values());

            assertThat(nodes.isEmpty()).isFalse();

            var node = nodes.get(0);

            testEnv.client.setMaxNodeAttempts(1);
            testEnv.client.pingAsync(node).get();
        }
    }

    @Test
    void pingAllAsync() throws Exception {
        try (var testEnv = new IntegrationTestEnv()) {

            testEnv.client.setMaxNodeAttempts(1);
            testEnv.client.pingAllAsync().get();

            var network = testEnv.client.getNetwork();
            var nodes = new ArrayList<>(network.values());

            assertThat(nodes.isEmpty()).isFalse();

            var node = nodes.get(0);

            new AccountBalanceQuery().setAccountId(node).execute(testEnv.client);
        }
    }

    @Test
    @DisplayName("`forMirrorNetwork()`")
    void testClientInitWithMirrorNetwork() throws Exception {
        var mirrorNetworkString = "testnet.mirrornode.hedera.com:443";
        var client = Client.forMirrorNetwork(List.of(mirrorNetworkString));
        var mirrorNetwork = client.getMirrorNetwork();

        assertThat(mirrorNetwork).hasSize(1);
        assertThat(mirrorNetwork.get(0)).isEqualTo(mirrorNetworkString);
        assertThat(client.getNetwork()).isNotNull();
        assertThat(client.getNetwork()).isNotEmpty();
        client.close();
    }

    @Test
    @DisplayName("`forMirrorNetwork with custom realm and shard")
    void testClientInitWithMirrorNetworkAnCustomRealmAndShard() throws Exception {
        var mirrorNetworkString = "testnet.mirrornode.hedera.com:443";
        var client = Client.forMirrorNetwork(List.of(mirrorNetworkString), 0, 0);
        var mirrorNetwork = client.getMirrorNetwork();

        assertThat(mirrorNetwork).hasSize(1);
        assertThat(mirrorNetwork.get(0)).isEqualTo(mirrorNetworkString);
        assertThat(client.getNetwork()).isNotNull();
        assertThat(client.getNetwork()).isNotEmpty();
    }
}
