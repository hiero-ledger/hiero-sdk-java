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
        return new Endpoint()
                .setAddress(new byte[] {0x00, 0x01, 0x02, 0x03})
                .setDomainName(offset + "unit.test.com")
                .setPort(42 + offset);
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
                .setServiceEndpoints(List.of(new Endpoint()
                        .setAddress(new byte[] {0x00, 0x01, 0x02, 0x03})
                        .setDomainName("unit.test.com")
                        .setPort(50111)));
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
