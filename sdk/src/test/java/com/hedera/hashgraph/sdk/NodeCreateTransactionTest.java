// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.NodeCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NodeCreateTransactionTest {

    private static final PrivateKey TEST_PRIVATE_KEY = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final AccountId TEST_ACCOUNT_ID = AccountId.fromString("0.6.9");

    private static final String TEST_DESCRIPTION = "Test description";

    private static final List<Endpoint> TEST_GOSSIP_ENDPOINTS =
            List.of(spawnTestEndpoint((byte) 0), spawnTestEndpoint((byte) 1), spawnTestEndpoint((byte) 2));

    private static final List<Endpoint> TEST_SERVICE_ENDPOINTS = List.of(
            spawnTestEndpoint((byte) 3),
            spawnTestEndpoint((byte) 4),
            spawnTestEndpoint((byte) 5),
            spawnTestEndpoint((byte) 6));

    private static final Endpoint TEST_GRPC_WEB_PROXY_ENDPOINT = spawnTestEndpoint((byte) 3);

    private static final byte[] TEST_GOSSIP_CA_CERTIFICATE = new byte[] {0, 1, 2, 3, 4};

    private static final byte[] TEST_GRPC_CERTIFICATE_HASH = new byte[] {5, 6, 7, 8, 9};

    private static final PublicKey TEST_ADMIN_KEY = PrivateKey.fromString(
                    "302e020100300506032b65700422042062c4b69e9f45a554e5424fb5a6fe5e6ac1f19ead31dc7718c2d980fd1f998d4b")
            .getPublicKey();

    final Instant TEST_VALID_START = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    private static Endpoint spawnTestEndpoint(byte offset) {
        // Valid endpoint: use domainName only to comply with SDK validation
        return new Endpoint().setDomainName(offset + "unit.test.com").setPort(42 + offset);
    }

    private NodeCreateTransaction spawnTestTransaction() {
        return new NodeCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), TEST_VALID_START))
                .setAccountId(TEST_ACCOUNT_ID)
                .setDescription(TEST_DESCRIPTION)
                .setGossipEndpoints(TEST_GOSSIP_ENDPOINTS)
                .setServiceEndpoints(TEST_SERVICE_ENDPOINTS)
                .setGossipCaCertificate(TEST_GOSSIP_CA_CERTIFICATE)
                .setGrpcCertificateHash(TEST_GRPC_CERTIFICATE_HASH)
                .setAdminKey(TEST_ADMIN_KEY)
                .setMaxTransactionFee(new Hbar(1))
                .setDeclineReward(false) // accepts the reward
                .setGrpcWebProxyEndpoint(TEST_GRPC_WEB_PROXY_ENDPOINT)
                .freeze()
                .sign(TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = NodeCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setNodeCreate(NodeCreateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(NodeCreateTransaction.class);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new NodeCreateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void testUnrecognizedServicePort() throws Exception {
        var tx = new NodeCreateTransaction()
                .setServiceEndpoints(
                        List.of(new Endpoint().setDomainName("unit.test.com").setPort(50111)));
        var tx2 = NodeCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void testSetNull() {
        new NodeCreateTransaction()
                .setDescription(null)
                .setAccountId(null)
                .setGossipCaCertificate(null)
                .setGrpcCertificateHash(null)
                .setAdminKey(null);
    }

    @Test
    void setDescriptionRejectsOver100Utf8Bytes() {
        var tx = new NodeCreateTransaction();
        String tooLong = "a".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> tx.setDescription(tooLong));
    }

    @Test
    void setDescriptionAcceptsExactly100Utf8Bytes() {
        var tx = new NodeCreateTransaction();
        String exact = "a".repeat(100);
        tx.setDescription(exact);
        assertThat(tx.getDescription()).isEqualTo(exact);
    }

    @Test
    void setGossipEndpointsRejectsMoreThan10() {
        var tx = new NodeCreateTransaction();
        var endpoints = new java.util.ArrayList<Endpoint>();
        for (int i = 0; i < 11; i++) {
            endpoints.add(new Endpoint().setDomainName("gossip" + i + ".test").setPort(5000 + i));
        }
        assertThrows(IllegalArgumentException.class, () -> tx.setGossipEndpoints(endpoints));
    }

    @Test
    void addGossipEndpointRejectsMoreThan10() {
        var tx = new NodeCreateTransaction();
        for (int i = 0; i < 10; i++) {
            tx.addGossipEndpoint(
                    new Endpoint().setDomainName("gossip" + i + ".test").setPort(5000 + i));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> tx.addGossipEndpoint(
                        new Endpoint().setDomainName("gossipX.test").setPort(6000)));
    }

    @Test
    void setGossipEndpointsRejectsIpAndDomainTogether() {
        var tx = new NodeCreateTransaction();
        var invalid = new Endpoint()
                .setAddress(new byte[] {1, 2, 3, 4})
                .setDomainName("both.test")
                .setPort(5000);
        assertThrows(IllegalArgumentException.class, () -> tx.setGossipEndpoints(List.of(invalid)));
    }

    @Test
    void setServiceEndpointsRejectsMoreThan8() {
        var tx = new NodeCreateTransaction();
        var endpoints = new java.util.ArrayList<Endpoint>();
        for (int i = 0; i < 9; i++) {
            endpoints.add(new Endpoint().setDomainName("svc" + i + ".test").setPort(6000 + i));
        }
        assertThrows(IllegalArgumentException.class, () -> tx.setServiceEndpoints(endpoints));
    }

    @Test
    void addServiceEndpointRejectsMoreThan8() {
        var tx = new NodeCreateTransaction();
        for (int i = 0; i < 8; i++) {
            tx.addServiceEndpoint(
                    new Endpoint().setDomainName("svc" + i + ".test").setPort(7000 + i));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> tx.addServiceEndpoint(
                        new Endpoint().setDomainName("svcX.test").setPort(8000)));
    }

    @Test
    void setServiceEndpointsRejectsIpAndDomainTogether() {
        var tx = new NodeCreateTransaction();
        var invalid = new Endpoint()
                .setAddress(new byte[] {5, 6, 7, 8})
                .setDomainName("both.test")
                .setPort(6000);
        assertThrows(IllegalArgumentException.class, () -> tx.setServiceEndpoints(List.of(invalid)));
    }

    @Test
    void setGossipCaCertificateRejectsEmpty() {
        var tx = new NodeCreateTransaction();
        assertThrows(IllegalArgumentException.class, () -> tx.setGossipCaCertificate(new byte[] {}));
    }

    @Test
    void buildRewritesGossipFqdnWithServiceIpFallback() {
        byte[] serviceIp = new byte[] {10, 0, 0, 1};
        Endpoint gossipFqdnOnly =
                new Endpoint().setDomainName("fqdn.example.com").setPort(50211);
        Endpoint serviceIpOnly = new Endpoint().setAddress(serviceIp).setPort(50211);

        var tx = new NodeCreateTransaction()
                .setGossipEndpoints(List.of(gossipFqdnOnly))
                .setServiceEndpoints(List.of(serviceIpOnly));

        var body = tx.build().build();
        var rewritten = body.getGossipEndpoint(0);

        // gossip endpoint should now carry IP and no domain
        org.assertj.core.api.Assertions.assertThat(rewritten.getIpAddressV4().toByteArray())
                .isEqualTo(serviceIp);
        org.assertj.core.api.Assertions.assertThat(rewritten.getDomainName()).isEmpty();
        org.assertj.core.api.Assertions.assertThat(rewritten.getPort()).isEqualTo(50211);
    }

    @Test
    void buildDoesNotRewriteGossipWhenIpPresent() {
        byte[] originalIp = new byte[] {127, 0, 0, 1};
        Endpoint gossipIpOnly = new Endpoint().setAddress(originalIp).setPort(50212);
        byte[] serviceIp = new byte[] {10, 0, 0, 2};
        Endpoint serviceIpOnly = new Endpoint().setAddress(serviceIp).setPort(50211);

        var tx = new NodeCreateTransaction()
                .setGossipEndpoints(List.of(gossipIpOnly))
                .setServiceEndpoints(List.of(serviceIpOnly));

        var body = tx.build().build();
        var ge = body.getGossipEndpoint(0);
        org.assertj.core.api.Assertions.assertThat(ge.getIpAddressV4().toByteArray())
                .isEqualTo(originalIp);
        org.assertj.core.api.Assertions.assertThat(ge.getPort()).isEqualTo(50212);
    }

    @Test
    void buildDoesNotRewriteWhenNoServiceIpAvailable() {
        Endpoint gossipFqdnOnly =
                new Endpoint().setDomainName("fqdn.example.com").setPort(50213);
        Endpoint serviceFqdnOnly =
                new Endpoint().setDomainName("svc.example.com").setPort(50211);

        var tx = new NodeCreateTransaction()
                .setGossipEndpoints(List.of(gossipFqdnOnly))
                .setServiceEndpoints(List.of(serviceFqdnOnly));

        var body = tx.build().build();
        var ge = body.getGossipEndpoint(0);
        org.assertj.core.api.Assertions.assertThat(ge.getIpAddressV4().isEmpty())
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(ge.getDomainName()).isEqualTo("fqdn.example.com");
        org.assertj.core.api.Assertions.assertThat(ge.getPort()).isEqualTo(50213);
    }

    @Test
    void constructNodeCreateTransactionFromTransactionBodyProtobuf() {
        var transactionBodyBuilder = NodeCreateTransactionBody.newBuilder();

        transactionBodyBuilder.setAccountId(TEST_ACCOUNT_ID.toProtobuf());
        transactionBodyBuilder.setDescription(TEST_DESCRIPTION);

        for (Endpoint gossipEndpoint : TEST_GOSSIP_ENDPOINTS) {
            transactionBodyBuilder.addGossipEndpoint(gossipEndpoint.toProtobuf());
        }

        for (Endpoint serviceEndpoint : TEST_SERVICE_ENDPOINTS) {
            transactionBodyBuilder.addServiceEndpoint(serviceEndpoint.toProtobuf());
        }

        transactionBodyBuilder.setGossipCaCertificate(ByteString.copyFrom(TEST_GOSSIP_CA_CERTIFICATE));
        transactionBodyBuilder.setGrpcCertificateHash(ByteString.copyFrom(TEST_GRPC_CERTIFICATE_HASH));
        transactionBodyBuilder.setAdminKey(TEST_ADMIN_KEY.toProtobufKey());

        transactionBodyBuilder.setDeclineReward(true);

        var tx = TransactionBody.newBuilder()
                .setNodeCreate(transactionBodyBuilder.build())
                .build();
        var nodeCreateTransaction = new NodeCreateTransaction(tx);

        assertThat(nodeCreateTransaction.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
        assertThat(nodeCreateTransaction.getDescription()).isEqualTo(TEST_DESCRIPTION);
        assertThat(nodeCreateTransaction.getGossipEndpoints()).hasSize(TEST_GOSSIP_ENDPOINTS.size());
        assertThat(nodeCreateTransaction.getServiceEndpoints()).hasSize(TEST_SERVICE_ENDPOINTS.size());
        assertThat(nodeCreateTransaction.getGossipCaCertificate()).isEqualTo(TEST_GOSSIP_CA_CERTIFICATE);
        assertThat(nodeCreateTransaction.getGrpcCertificateHash()).isEqualTo(TEST_GRPC_CERTIFICATE_HASH);
        assertThat(nodeCreateTransaction.getAdminKey()).isEqualTo(TEST_ADMIN_KEY);
    }

    @Test
    void getSetAccountId() {
        var nodeCreateTransaction = new NodeCreateTransaction().setAccountId(TEST_ACCOUNT_ID);
        assertThat(nodeCreateTransaction.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    }

    @Test
    void getSetAccountIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAccountId(TEST_ACCOUNT_ID));
    }

    @Test
    void getSetDescription() {
        var nodeCreateTransaction = new NodeCreateTransaction().setDescription(TEST_DESCRIPTION);
        assertThat(nodeCreateTransaction.getDescription()).isEqualTo(TEST_DESCRIPTION);
    }

    @Test
    void getSetDescriptionFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setDescription(TEST_DESCRIPTION));
    }

    @Test
    void getSetGossipEndpoints() {
        var nodeCreateTransaction = new NodeCreateTransaction().setGossipEndpoints(TEST_GOSSIP_ENDPOINTS);
        assertThat(nodeCreateTransaction.getGossipEndpoints()).isEqualTo(TEST_GOSSIP_ENDPOINTS);
    }

    @Test
    void setTestGossipEndpointsFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setGossipEndpoints(TEST_GOSSIP_ENDPOINTS));
    }

    @Test
    void getSetServiceEndpoints() {
        var nodeCreateTransaction = new NodeCreateTransaction().setServiceEndpoints(TEST_SERVICE_ENDPOINTS);
        assertThat(nodeCreateTransaction.getServiceEndpoints()).isEqualTo(TEST_SERVICE_ENDPOINTS);
    }

    @Test
    void getSetServiceEndpointsFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setServiceEndpoints(TEST_SERVICE_ENDPOINTS));
    }

    @Test
    void getSetGossipCaCertificate() {
        var nodeCreateTransaction = new NodeCreateTransaction().setGossipCaCertificate(TEST_GOSSIP_CA_CERTIFICATE);
        assertThat(nodeCreateTransaction.getGossipCaCertificate()).isEqualTo(TEST_GOSSIP_CA_CERTIFICATE);
    }

    @Test
    void getSetGossipCaCertificateFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setGossipCaCertificate(TEST_GOSSIP_CA_CERTIFICATE));
    }

    @Test
    void getSetGrpcCertificateHash() {
        var nodeCreateTransaction = new NodeCreateTransaction().setGrpcCertificateHash(TEST_GRPC_CERTIFICATE_HASH);
        assertThat(nodeCreateTransaction.getGrpcCertificateHash()).isEqualTo(TEST_GRPC_CERTIFICATE_HASH);
    }

    @Test
    void getSetGrpcCertificateHashFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setGrpcCertificateHash(TEST_GRPC_CERTIFICATE_HASH));
    }

    @Test
    void getSetAdminKey() {
        var nodeCreateTransaction = new NodeCreateTransaction().setAdminKey(TEST_ADMIN_KEY);
        assertThat(nodeCreateTransaction.getAdminKey()).isEqualTo(TEST_ADMIN_KEY);
    }

    @Test
    void getSetAdminKeyFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAdminKey(TEST_ADMIN_KEY));
    }

    @Test
    void getSetDeclineReward() {
        var nodeCreateTransaction = new NodeCreateTransaction().setDeclineReward(true);
        assertThat(nodeCreateTransaction.getDeclineReward()).isTrue();
    }

    @Test
    void getSetDeclineRewardFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setDeclineReward(false));
    }

    @Test
    void getGrpcWebProxyEndpoint() {
        var nodeCreateTransaction = new NodeCreateTransaction().setGrpcWebProxyEndpoint(TEST_GRPC_WEB_PROXY_ENDPOINT);
        assertThat(nodeCreateTransaction.getGrpcWebProxyEndpoint()).isEqualTo(TEST_GRPC_WEB_PROXY_ENDPOINT);
    }

    @Test
    void setGrpcWebProxyEndpointRequiresFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setGrpcWebProxyEndpoint(TEST_GRPC_WEB_PROXY_ENDPOINT));
    }
}
