// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.hiero.sdk.BaseNodeAddress.PORT_NODE_PLAIN;

import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import org.hiero.sdk.proto.mirror.NetworkServiceGrpc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AddressBookQueryMockTest {

    private Client client;
    private final AddressBookQueryStub addressBookServiceStub = new AddressBookQueryStub();
    private Server server;
    private AddressBookQuery addressBookQuery;

    @BeforeEach
    void setup() throws Exception {
        client = Client.forNetwork(Collections.emptyMap());
        client.setMirrorNetwork(List.of("in-process:test"));
        server = InProcessServerBuilder.forName("test")
                .addService(addressBookServiceStub)
                .directExecutor()
                .build()
                .start();
        addressBookQuery = new AddressBookQuery();
        addressBookQuery.setFileId(FileId.ADDRESS_BOOK);
    }

    @AfterEach
    void teardown() throws Exception {
        addressBookServiceStub.verify();
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.shutdown();
            server.awaitTermination();
        }
    }

    @ParameterizedTest(name = "[{0}] AddressBookQuery works")
    @CsvSource({"sync", "async"})
    void addressBookQueryWorks(String executeVersion) throws Throwable {
        addressBookServiceStub.requests.add(org.hiero.sdk.proto.mirror.AddressBookQuery.newBuilder()
                .setFileId(FileId.ADDRESS_BOOK.toProtobuf())
                .setLimit(3)
                .build());
        addressBookServiceStub.responses.add(new org.hiero.sdk.NodeAddress()
                .setAccountId(AccountId.fromString("0.0.3"))
                .toProtobuf());

        addressBookQuery.setLimit(3);

        var nodes = executeVersion.equals("sync")
                ? addressBookQuery.execute(client)
                : addressBookQuery.executeAsync(client).get();
        assertThat(nodes.nodeAddresses).hasSize(1);
        assertThat(nodes.nodeAddresses.get(0).accountId).isEqualTo(AccountId.fromString("0.0.3"));
    }

    Endpoint spawnEndpoint() {
        return new Endpoint()
                .setAddress(new byte[] {0x00, 0x01, 0x02, 0x03})
                .setDomainName("unit.test.com")
                .setPort(PORT_NODE_PLAIN);
    }

    @Test
    void networkUpdatePeriodWorks() throws Throwable {
        addressBookServiceStub.requests.add(org.hiero.sdk.proto.mirror.AddressBookQuery.newBuilder()
                .setFileId(FileId.ADDRESS_BOOK.toProtobuf())
                .build());
        addressBookServiceStub.responses.add(new org.hiero.sdk.NodeAddress()
                .setAccountId(AccountId.fromString("0.0.3"))
                .setAddresses(Collections.singletonList(spawnEndpoint()))
                .toProtobuf());

        client.setNetworkUpdatePeriod(Duration.ofSeconds(1));
        Thread.sleep(1400);

        var clientNetwork = client.getNetwork();
        assertThat(clientNetwork).hasSize(1);
        assertThat(clientNetwork.values()).contains(AccountId.fromString("0.0.3"));
    }

    @ParameterizedTest(name = "[{0}] Retry recovers w/ status {1} and description {2}")
    @CsvSource({
        "sync, INTERNAL, internal RST_STREAM error",
        "sync, INTERNAL, rst stream",
        "sync, RESOURCE_EXHAUSTED, ",
        "sync, UNAVAILABLE, ",
        "async, INTERNAL, internal RST_STREAM error",
        "async, INTERNAL, rst stream",
        "async, RESOURCE_EXHAUSTED, ",
        "async, UNAVAILABLE, "
    })
    void addressBookQueryRetries(String executeVersion, Status.Code code, String description) throws Throwable {
        addressBookServiceStub.requests.add(org.hiero.sdk.proto.mirror.AddressBookQuery.newBuilder()
                .setFileId(FileId.ADDRESS_BOOK.toProtobuf())
                .build());
        addressBookServiceStub.requests.add(org.hiero.sdk.proto.mirror.AddressBookQuery.newBuilder()
                .setFileId(FileId.ADDRESS_BOOK.toProtobuf())
                .build());
        addressBookServiceStub.responses.add(
                code.toStatus().withDescription(description).asRuntimeException());
        addressBookServiceStub.responses.add(new org.hiero.sdk.NodeAddress()
                .setAccountId(AccountId.fromString("0.0.3"))
                .toProtobuf());

        var nodes = executeVersion.equals("sync")
                ? addressBookQuery.execute(client)
                : addressBookQuery.executeAsync(client).get();
        assertThat(nodes.nodeAddresses).hasSize(1);
        assertThat(nodes.nodeAddresses.get(0).accountId).isEqualTo(AccountId.fromString("0.0.3"));
    }

    @ParameterizedTest(name = "No retry w/ status {0} and description {1}")
    @CsvSource({
        "sync, INTERNAL, internal first_stream error",
        "sync, INTERNAL, internal error",
        "sync, INTERNAL, ",
        "sync, INVALID_ARGUMENT, ",
        "async, INTERNAL, internal first_stream error",
        "async, INTERNAL, internal error",
        "async, INTERNAL, ",
        "async, INVALID_ARGUMENT, "
    })
    void addressBookQueryFails(String executeVersion, Status.Code code, String description) {
        addressBookServiceStub.requests.add(org.hiero.sdk.proto.mirror.AddressBookQuery.newBuilder()
                .setFileId(FileId.ADDRESS_BOOK.toProtobuf())
                .build());
        addressBookServiceStub.responses.add(
                code.toStatus().withDescription(description).asRuntimeException());

        assertThatException().isThrownBy(() -> {
            var result = executeVersion.equals("sync")
                    ? addressBookQuery.execute(client)
                    : addressBookQuery.executeAsync(client).get();
        });
    }

    @ParameterizedTest(name = "[{0}] address book query stops at max attempts w/ status {1} and description {2}")
    @CsvSource({
        "sync, INTERNAL, internal RST_STREAM error",
        "sync, INTERNAL, rst stream",
        "sync, RESOURCE_EXHAUSTED, ",
        "sync, UNAVAILABLE, ",
        "async, INTERNAL, internal RST_STREAM error",
        "async, INTERNAL, rst stream",
        "async, RESOURCE_EXHAUSTED, ",
        "async, UNAVAILABLE, "
    })
    void addressBookQueryStopsAtMaxAttempts(String executeVersion, Status.Code code, String description)
            throws Throwable {
        addressBookQuery.setMaxAttempts(2);

        addressBookServiceStub.requests.add(org.hiero.sdk.proto.mirror.AddressBookQuery.newBuilder()
                .setFileId(FileId.ADDRESS_BOOK.toProtobuf())
                .build());
        addressBookServiceStub.requests.add(org.hiero.sdk.proto.mirror.AddressBookQuery.newBuilder()
                .setFileId(FileId.ADDRESS_BOOK.toProtobuf())
                .build());
        addressBookServiceStub.responses.add(
                code.toStatus().withDescription(description).asRuntimeException());
        addressBookServiceStub.responses.add(
                code.toStatus().withDescription(description).asRuntimeException());

        assertThatException().isThrownBy(() -> {
            var result = executeVersion.equals("sync")
                    ? addressBookQuery.execute(client)
                    : addressBookQuery.executeAsync(client).get();
        });
    }

    private static class AddressBookQueryStub extends NetworkServiceGrpc.NetworkServiceImplBase {

        private final Queue<org.hiero.sdk.proto.mirror.AddressBookQuery> requests = new ArrayDeque<>();
        private final Queue<Object> responses = new ArrayDeque<>();

        @Override
        public void getNodes(
                org.hiero.sdk.proto.mirror.AddressBookQuery addressBookQuery,
                StreamObserver<org.hiero.sdk.proto.NodeAddress> streamObserver) {
            var request = requests.poll();
            assertThat(request).isNotNull();
            assertThat(addressBookQuery).isEqualTo(request);

            while (!responses.isEmpty()) {
                var response = responses.poll();
                assertThat(response).isNotNull();

                if (response instanceof Throwable) {
                    streamObserver.onError((Throwable) response);
                    return;
                }

                streamObserver.onNext((org.hiero.sdk.proto.NodeAddress) response);
            }
            streamObserver.onCompleted();
        }

        public void verify() {
            assertThat(requests).isEmpty();
            assertThat(responses).isEmpty();
        }
    }
}
