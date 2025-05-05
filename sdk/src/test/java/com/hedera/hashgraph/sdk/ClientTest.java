// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.BaseNodeAddress.PORT_NODE_PLAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClientTest {

    @Test
    @DisplayName("Can construct mainnet client")
    void forMainnet() throws TimeoutException {
        Client.forMainnet().close();
    }

    @Test
    @DisplayName("Can construct mainnet client with executor")
    void forMainnetWithExecutor() throws TimeoutException {
        var executor = new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Client.forMainnet(executor).close();
    }

    @Test
    @DisplayName("Can construct testnet client with executor")
    void forTestnetWithExecutor() throws TimeoutException {
        var executor = new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Client.forTestnet(executor).close();
    }

    @Test
    @DisplayName("Can construct previewnet client with executor")
    void forPreviewnetWithWithExecutor() throws TimeoutException {
        var executor = new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Client.forPreviewnet(executor).close();
    }

    @Test
    @DisplayName("Client.setMaxQueryPayment() negative")
    void setMaxQueryPaymentNegative() throws TimeoutException {
        var client = Client.forTestnet();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            client.setMaxQueryPayment(Hbar.MIN);
        });
        client.close();
    }

    @ValueSource(ints = {-1, 0})
    @ParameterizedTest(name = "Invalid maxAttempts {0}")
    void setMaxAttempts(int maxAttempts) throws TimeoutException {
        var client = Client.forNetwork(Map.of());
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            client.setMaxAttempts(maxAttempts);
        });
        client.close();
    }

    @NullSource
    @ValueSource(longs = {-1, 0, 249})
    @ParameterizedTest(name = "Invalid maxBackoff {0}")
    @SuppressWarnings("NullAway")
    void setMaxBackoffInvalid(@Nullable Long maxBackoffMillis) throws TimeoutException {
        @Nullable Duration maxBackoff = maxBackoffMillis != null ? Duration.ofMillis(maxBackoffMillis) : null;
        var client = Client.forNetwork(Map.of());
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            client.setMaxBackoff(maxBackoff);
        });
        client.close();
    }

    @ValueSource(longs = {250, 8000})
    @ParameterizedTest(name = "Valid maxBackoff {0}")
    void setMaxBackoffValid(long maxBackoff) throws TimeoutException {
        Client.forNetwork(Map.of()).setMaxBackoff(Duration.ofMillis(maxBackoff)).close();
    }

    @NullSource
    @ValueSource(longs = {-1, 8001})
    @ParameterizedTest(name = "Invalid minBackoff {0}")
    @SuppressWarnings("NullAway")
    void setMinBackoffInvalid(@Nullable Long minBackoffMillis) throws TimeoutException {
        @Nullable Duration minBackoff = minBackoffMillis != null ? Duration.ofMillis(minBackoffMillis) : null;
        var client = Client.forNetwork(Map.of());
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            client.setMinBackoff(minBackoff);
        });
        client.close();
    }

    @ValueSource(longs = {0, 250, 8000})
    @ParameterizedTest(name = "Valid minBackoff {0}")
    void setMinBackoffValid(long minBackoff) throws TimeoutException {
        Client.forNetwork(Map.of()).setMinBackoff(Duration.ofMillis(minBackoff)).close();
    }

    @Test
    @DisplayName("Client.setMaxTransactionFee() negative")
    void setMaxTransactionFeeNegative() throws TimeoutException {
        var client = Client.forTestnet();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            client.setDefaultMaxTransactionFee(Hbar.MIN);
        });
        client.close();
    }

    @Test
    @DisplayName("fromJsonFile() functions correctly")
    void fromJsonFile() throws Exception {
        Client.fromConfigFile(new File("./src/test/resources/client-config.json"))
                .close();
        Client.fromConfigFile(new File("./src/test/resources/client-config-with-operator.json"))
                .close();
        Client.fromConfigFile("./src/test/resources/client-config.json").close();
        Client.fromConfigFile("./src/test/resources/client-config-with-operator.json")
                .close();
    }

    @Test
    @DisplayName("fromJson() functions correctly")
    void testFromJson() throws Exception {
        // Copied content of `client-config-with-operator.json`
        var client = Client.fromConfig("{\n" + "    \"network\":\"mainnet\",\n"
                + "    \"operator\": {\n"
                + "        \"accountId\": \"0.0.36\",\n"
                + "        \"privateKey\": \"302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10\"\n"
                + "    }\n"
                + "}\n");

        // put it in a file for nicer formatting
        InputStream clientConfig = ClientTest.class.getClassLoader().getResourceAsStream("client-config.json");

        assertThat(clientConfig).isNotNull();

        Client.fromConfig(new InputStreamReader(clientConfig, StandardCharsets.UTF_8))
                .close();

        // put it in a file for nicer formatting
        InputStream clientConfigWithOperator =
                ClientTest.class.getClassLoader().getResourceAsStream("client-config-with-operator.json");

        assertThat(clientConfigWithOperator).isNotNull();

        client.close();
    }

    @Test
    @DisplayName("setNetwork() functions correctly")
    void setNetworkWorks() throws Exception {
        var defaultNetwork = Map.of(
                "0.testnet.hedera.com:50211", new AccountId(0, 0, 3),
                "1.testnet.hedera.com:50211", new AccountId(0, 0, 4));

        Client client = Client.forNetwork(defaultNetwork);
        assertThat(client.getNetwork()).containsExactlyInAnyOrderEntriesOf(defaultNetwork);

        client.setNetwork(defaultNetwork);
        assertThat(client.getNetwork()).containsExactlyInAnyOrderEntriesOf(defaultNetwork);

        var defaultNetworkWithExtraNode = Map.of(
                "0.testnet.hedera.com:50211", new AccountId(0, 0, 3),
                "1.testnet.hedera.com:50211", new AccountId(0, 0, 4),
                "2.testnet.hedera.com:50211", new AccountId(0, 0, 5));

        client.setNetwork(defaultNetworkWithExtraNode);
        assertThat(client.getNetwork()).containsExactlyInAnyOrderEntriesOf(defaultNetworkWithExtraNode);

        var singleNodeNetwork = Map.of("2.testnet.hedera.com:50211", new AccountId(0, 0, 5));

        client.setNetwork(singleNodeNetwork);
        assertThat(client.getNetwork()).containsExactlyInAnyOrderEntriesOf(singleNodeNetwork);

        var singleNodeNetworkWithDifferentAccountId = Map.of("2.testnet.hedera.com:50211", new AccountId(0, 0, 6));

        client.setNetwork(singleNodeNetworkWithDifferentAccountId);
        assertThat(client.getNetwork()).containsExactlyInAnyOrderEntriesOf(singleNodeNetworkWithDifferentAccountId);

        var multiAddressNetwork = Map.of(
                "0.testnet.hedera.com:50211", new AccountId(0, 0, 3),
                "34.94.106.61:50211", new AccountId(0, 0, 3),
                "50.18.132.211:50211", new AccountId(0, 0, 3),
                "138.91.142.219:50211", new AccountId(0, 0, 3),
                "1.testnet.hedera.com:50211", new AccountId(0, 0, 4),
                "35.237.119.55:50211", new AccountId(0, 0, 4),
                "3.212.6.13:50211", new AccountId(0, 0, 4),
                "52.168.76.241:50211", new AccountId(0, 0, 4));

        client.setNetwork(multiAddressNetwork);
        assertThat(client.getNetwork()).containsExactlyInAnyOrderEntriesOf(multiAddressNetwork);

        client.close();
    }

    @Test
    @DisplayName("setMirrorNetwork() functions correctly")
    void setMirrorNetworkWorks() throws Exception {
        var defaultNetwork = List.of("testnet.mirrornode.hedera.com:443");

        Client client = Client.forNetwork(new HashMap<>()).setMirrorNetwork(defaultNetwork);
        assertThat(client.getMirrorNetwork()).containsExactlyInAnyOrderElementsOf(defaultNetwork);

        client.setMirrorNetwork(defaultNetwork);
        assertThat(client.getMirrorNetwork()).containsExactlyInAnyOrderElementsOf(defaultNetwork);

        var defaultNetworkWithExtraNode =
                List.of("testnet.mirrornode.hedera.com:443", "testnet1.mirrornode.hedera.com:443");

        client.setMirrorNetwork(defaultNetworkWithExtraNode);
        assertThat(client.getMirrorNetwork()).containsExactlyInAnyOrderElementsOf(defaultNetworkWithExtraNode);

        var singleNodeNetwork = List.of("testnet1.mirrornode.hedera.com:443");

        client.setMirrorNetwork(singleNodeNetwork);
        assertThat(client.getMirrorNetwork()).containsExactlyInAnyOrderElementsOf(singleNodeNetwork);

        var singleNodeNetworkWithDifferentNode = List.of("testnet.mirrornode.hedera.com:443");

        client.setMirrorNetwork(singleNodeNetworkWithDifferentNode);
        assertThat(client.getMirrorNetwork()).containsExactlyInAnyOrderElementsOf(singleNodeNetworkWithDifferentNode);

        client.close();
    }

    @Test
    @DisplayName("setMirrorNetwork() throws exception if there is no time to remove the old nodes")
    void setMirrorNetworkFails() throws Exception {
        var defaultNetwork = List.of("testnet.mirrornode.hedera.com:443", "testnet.mirrornode2.hedera.com:443");

        Client client = Client.forNetwork(new HashMap<>()).setMirrorNetwork(defaultNetwork);
        assertThat(client.getMirrorNetwork()).containsExactlyInAnyOrderElementsOf(defaultNetwork);

        client.setCloseTimeout(Duration.ZERO);
        final List<String> updatedNetwork = List.of("testnet.mirrornode.hedera.com:443");

        assertThatThrownBy(() -> client.setMirrorNetwork(updatedNetwork))
                .hasMessageEndingWith("Failed to properly shutdown all channels");
    }

    @Test
    @DisplayName("forName() sets the correct network")
    void forNameReturnsCorrectNetwork() {
        Client mainnetClient = Client.forName("mainnet");
        assertThat(mainnetClient.getLedgerId()).isEqualTo(LedgerId.MAINNET);

        Client testnetClient = Client.forName("testnet");
        assertThat(testnetClient.getLedgerId()).isEqualTo(LedgerId.TESTNET);

        Client previewnetClient = Client.forName("previewnet");
        assertThat(previewnetClient.getLedgerId()).isEqualTo(LedgerId.PREVIEWNET);

        assertThatThrownBy(() -> Client.forName("unknown"))
                .hasMessageEndingWith("Name must be one-of `mainnet`, `testnet`, or `previewnet`");
    }

    @ParameterizedTest
    @CsvSource({"onClient", "onQuery"})
    void testExecuteAsyncTimeout(String timeoutSite) throws Exception {
        AccountId accountId = AccountId.fromString("0.0.1");
        Duration timeout = Duration.ofSeconds(5);

        Client client = Client.forNetwork(Map.of("1.1.1.1:50211", accountId))
                .setNodeMinBackoff(Duration.ofMillis(0))
                .setNodeMaxBackoff(Duration.ofMillis(0))
                .setMinNodeReadmitTime(Duration.ofMillis(0))
                .setMaxNodeReadmitTime(Duration.ofMillis(0));
        AccountBalanceQuery query =
                new AccountBalanceQuery().setAccountId(accountId).setMaxAttempts(3);
        Instant start = Instant.now();

        try {
            if (timeoutSite.equals("onClient")) {
                client.setRequestTimeout(timeout);
                query.executeAsync(client).get();
            } else {
                query.executeAsync(client, timeout).get();
            }
        } catch (ExecutionException e) {
            // fine...
        }
        long secondsTaken = java.time.Duration.between(start, Instant.now()).toSeconds();

        // 20 seconds would indicate we tried 2 times to connect
        assertThat(secondsTaken).isLessThan(7);

        client.close();
    }

    @ParameterizedTest
    @CsvSource({"onClient", "onQuery"})
    void testExecuteSyncTimeout(String timeoutSite) throws Exception {
        AccountId accountId = AccountId.fromString("0.0.1");
        // Executing requests in sync mode will require at most 10 seconds to connect
        // to a gRPC node. If we're not able to connect to a gRPC node within 10 seconds
        // we fail that request attempt. This means setting at timeout on a request
        // which hits non-connecting gRPC nodes will fail within ~10s of the set timeout
        // e.g. setting a timeout of 15 seconds, the request could fail within the range
        // of [5 seconds, 25 seconds]. The 10 second timeout for connecting to gRPC nodes
        // is not configurable.
        Duration timeout = Duration.ofSeconds(5);

        Client client = Client.forNetwork(Map.of("1.1.1.1:50211", accountId))
                .setNodeMinBackoff(Duration.ofMillis(0))
                .setNodeMaxBackoff(Duration.ofMillis(0))
                .setMinNodeReadmitTime(Duration.ofMillis(0))
                .setMaxNodeReadmitTime(Duration.ofMillis(0));

        AccountBalanceQuery query = new AccountBalanceQuery()
                .setAccountId(accountId)
                .setMaxAttempts(3)
                .setGrpcDeadline(Duration.ofSeconds(5));
        Instant start = Instant.now();

        try {
            if (timeoutSite.equals("onClient")) {
                client.setRequestTimeout(timeout);
                query.execute(client);
            } else {
                query.execute(client, timeout);
            }
        } catch (TimeoutException e) {
            // fine...
        }
        long secondsTaken = java.time.Duration.between(start, Instant.now()).toSeconds();

        // 20 seconds would indicate we tried 2 times to connect
        assertThat(secondsTaken).isLessThan(15);

        client.close();
    }

    com.hedera.hashgraph.sdk.proto.NodeAddress nodeAddress(
            long accountNum, String rsaPubKeyHex, byte[] certHash, byte[] ipv4) {
        com.hedera.hashgraph.sdk.proto.NodeAddress.Builder builder =
                com.hedera.hashgraph.sdk.proto.NodeAddress.newBuilder()
                        .setNodeAccountId(com.hedera.hashgraph.sdk.proto.AccountID.newBuilder()
                                .setAccountNum(accountNum)
                                .build())
                        .addServiceEndpoint(com.hedera.hashgraph.sdk.proto.ServiceEndpoint.newBuilder()
                                .setIpAddressV4(ByteString.copyFrom(ipv4))
                                .setPort(PORT_NODE_PLAIN)
                                .build())
                        .setRSAPubKey(rsaPubKeyHex);
        if (certHash != null) {
            builder.setNodeCertHash(ByteString.copyFrom(certHash));
        }
        return builder.build();
    }

    @Test
    @DisplayName("setNetworkFromAddressBook() updates security parameters in the client")
    void setNetworkFromAddressBook() throws Exception {
        try (Client client = Client.forNetwork(Map.of())) {
            Function<Integer, NodeAddress> nodeAddress = accountNum -> client.network
                    .network
                    .get(new AccountId(0, 0, accountNum))
                    .get(0)
                    .getAddressBookEntry();

            // reconfigure client network from addressbook (add new nodes)
            client.setNetworkFromAddressBook(
                    NodeAddressBook.fromBytes(com.hedera.hashgraph.sdk.proto.NodeAddressBook.newBuilder()
                            .addNodeAddress(nodeAddress(10001, "10001", new byte[] {1, 0, 1}, new byte[] {10, 0, 0, 1}))
                            .addNodeAddress(nodeAddress(10002, "10002", new byte[] {1, 0, 2}, new byte[] {10, 0, 0, 2}))
                            .build()
                            .toByteString()));

            // verify security parameters in client
            assertThat(nodeAddress.apply(10001).certHash).isEqualTo(ByteString.copyFrom(new byte[] {1, 0, 1}));
            assertThat(nodeAddress.apply(10001).publicKey).isEqualTo("10001");
            assertThat(nodeAddress.apply(10002).certHash).isEqualTo(ByteString.copyFrom(new byte[] {1, 0, 2}));
            assertThat(nodeAddress.apply(10002).publicKey).isEqualTo("10002");

            // reconfigure client network from addressbook without `certHash`
            client.setNetworkFromAddressBook(
                    NodeAddressBook.fromBytes(com.hedera.hashgraph.sdk.proto.NodeAddressBook.newBuilder()
                            .addNodeAddress(nodeAddress(10001, "10001", null, new byte[] {10, 0, 0, 1}))
                            .addNodeAddress(nodeAddress(10002, "10002", null, new byte[] {10, 0, 0, 2}))
                            .build()
                            .toByteString()));

            // verify security parameters in client (unchanged)
            assertThat(nodeAddress.apply(10001).certHash).isEqualTo(ByteString.copyFrom(new byte[] {1, 0, 1}));
            assertThat(nodeAddress.apply(10001).publicKey).isEqualTo("10001");
            assertThat(nodeAddress.apply(10002).certHash).isEqualTo(ByteString.copyFrom(new byte[] {1, 0, 2}));
            assertThat(nodeAddress.apply(10002).publicKey).isEqualTo("10002");

            // reconfigure client network from addressbook (update existing nodes)
            client.setNetworkFromAddressBook(
                    NodeAddressBook.fromBytes(com.hedera.hashgraph.sdk.proto.NodeAddressBook.newBuilder()
                            .addNodeAddress(
                                    nodeAddress(10001, "810001", new byte[] {8, 1, 0, 1}, new byte[] {10, 0, 0, 1}))
                            .addNodeAddress(
                                    nodeAddress(10002, "810002", new byte[] {8, 1, 0, 2}, new byte[] {10, 0, 0, 2}))
                            .build()
                            .toByteString()));

            // verify security parameters in client
            assertThat(nodeAddress.apply(10001).certHash).isEqualTo(ByteString.copyFrom(new byte[] {8, 1, 0, 1}));
            assertThat(nodeAddress.apply(10001).publicKey).isEqualTo("810001");
            assertThat(nodeAddress.apply(10002).certHash).isEqualTo(ByteString.copyFrom(new byte[] {8, 1, 0, 2}));
            assertThat(nodeAddress.apply(10002).publicKey).isEqualTo("810002");
        }
    }

    @Test
    @DisplayName("Is TLS present when node is created by network entry")
    void assignAddressBookOnNodeCreationWhenAddressBookPresentShouldHaveTLSParametersPresent()
            throws TimeoutException, InterruptedException {
        var client = Client.forTestnet();
        client.setNetwork(Map.of("1.2.3.4:50211", AccountId.fromString("0.0.3")));

        assertThat(client.network.nodes.get(0).getChannelCredentials()).isNotNull();

        var addressBookEntry = client.network.nodes.get(0).getAddressBookEntry();

        assertThat(addressBookEntry).isNotNull();
        assertThat(addressBookEntry.certHash).isNotNull();
        assertThat(addressBookEntry.addresses).isNotNull();
        assertThat(addressBookEntry.accountId).isNotNull();
        assertThat(addressBookEntry.description).isNotNull();
        client.close();
    }
}
