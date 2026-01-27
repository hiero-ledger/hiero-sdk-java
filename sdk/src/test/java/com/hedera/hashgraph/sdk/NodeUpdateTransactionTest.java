// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.StringValue;
import com.hedera.hashgraph.sdk.proto.NodeUpdateTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NodeUpdateTransactionTest {

    private static final PrivateKey TEST_PRIVATE_KEY = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final long TEST_NODE_ID = 420;

    private static final AccountId TEST_ACCOUNT_ID = AccountId.fromString("0.6.9");

    private static final String TEST_DESCRIPTION = "Test description";

    private static final List<Endpoint> TEST_GOSSIP_ENDPOINTS = List.of(
            spawnTestEndpointIpOnly((byte) 0), spawnTestEndpointIpOnly((byte) 1), spawnTestEndpointIpOnly((byte) 2));

    private static final List<Endpoint> TEST_SERVICE_ENDPOINTS = List.of(
            spawnTestEndpointIpOnly((byte) 3),
            spawnTestEndpointIpOnly((byte) 4),
            spawnTestEndpointIpOnly((byte) 5),
            spawnTestEndpointIpOnly((byte) 6));

    private static final Endpoint TEST_GRPC_WEB_PROXY_ENDPOINT = spawnTestEndpointDomainOnly((byte) 3);

    private static final byte[] TEST_GOSSIP_CA_CERTIFICATE = new byte[] {0, 1, 2, 3, 4};

    private static final byte[] TEST_GRPC_CERTIFICATE_HASH = new byte[48]; // SHA-384 hash (48 bytes)

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

    private static Endpoint spawnTestEndpointIpOnly(byte offset) {
        return new Endpoint().setAddress(new byte[] {0x00, 0x01, 0x02, 0x03}).setPort(42 + offset);
    }

    private static Endpoint spawnTestEndpointDomainOnly(byte offset) {
        return new Endpoint().setDomainName(offset + "unit.test.com").setPort(42 + offset);
    }

    private NodeUpdateTransaction spawnTestTransaction() {
        return new NodeUpdateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), TEST_VALID_START))
                .setNodeId(TEST_NODE_ID)
                .setAccountId(TEST_ACCOUNT_ID)
                .setDescription(TEST_DESCRIPTION)
                .setGossipEndpoints(TEST_GOSSIP_ENDPOINTS)
                .setServiceEndpoints(TEST_SERVICE_ENDPOINTS)
                .setGossipCaCertificate(TEST_GOSSIP_CA_CERTIFICATE)
                .setGrpcCertificateHash(TEST_GRPC_CERTIFICATE_HASH)
                .setAdminKey(TEST_ADMIN_KEY)
                .setMaxTransactionFee(new Hbar(1))
                .setDeclineReward(true)
                .setGrpcWebProxyEndpoint(TEST_GRPC_WEB_PROXY_ENDPOINT)
                .freeze()
                .sign(TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = NodeUpdateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new NodeUpdateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void testUnrecognizedServicePort() throws Exception {
        var tx = new NodeUpdateTransaction()
                .setServiceEndpoints(
                        List.of(new Endpoint().setDomainName("unit.test.com").setPort(50111)));
        var tx2 = NodeUpdateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void testEmptyCertificates() throws Exception {
        // Empty gRPC certificate hash is allowed (network validates it)
        // But empty gossip CA certificate should throw
        var tx = new NodeUpdateTransaction()
                .setGrpcCertificateHash(new byte[] {})
                .setNodeId(0l);
        var tx2Bytes = tx.toBytes();
        NodeUpdateTransaction deserializedTx = (NodeUpdateTransaction) Transaction.fromBytes(tx2Bytes);
        assertThat(deserializedTx.getGrpcCertificateHash()).isEqualTo(new byte[] {});
    }

    @Test
    void testSetNull() {
        new NodeUpdateTransaction()
                .setDescription(null)
                .setAccountId(null)
                .setGrpcCertificateHash(null)
                .setAdminKey(null);
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setNodeUpdate(NodeUpdateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(NodeUpdateTransaction.class);
    }

    @Test
    void constructNodeUpdateTransactionFromTransactionBodyProtobuf() {
        var transactionBodyBuilder = NodeUpdateTransactionBody.newBuilder();

        transactionBodyBuilder.setNodeId(TEST_NODE_ID);
        transactionBodyBuilder.setAccountId(TEST_ACCOUNT_ID.toProtobuf());
        transactionBodyBuilder.setDescription(StringValue.of(TEST_DESCRIPTION));

        for (Endpoint gossipEndpoint : TEST_GOSSIP_ENDPOINTS) {
            transactionBodyBuilder.addGossipEndpoint(gossipEndpoint.toProtobuf());
        }

        for (Endpoint serviceEndpoint : TEST_SERVICE_ENDPOINTS) {
            transactionBodyBuilder.addServiceEndpoint(serviceEndpoint.toProtobuf());
        }

        transactionBodyBuilder.setGossipCaCertificate(BytesValue.of(ByteString.copyFrom(TEST_GOSSIP_CA_CERTIFICATE)));
        transactionBodyBuilder.setGrpcCertificateHash(BytesValue.of(ByteString.copyFrom(TEST_GRPC_CERTIFICATE_HASH)));
        transactionBodyBuilder.setAdminKey(TEST_ADMIN_KEY.toProtobufKey());
        transactionBodyBuilder.setDeclineReward(BoolValue.of(true));

        var tx = TransactionBody.newBuilder()
                .setNodeUpdate(transactionBodyBuilder.build())
                .build();
        var nodeUpdateTransaction = new NodeUpdateTransaction(tx);

        assertThat(nodeUpdateTransaction.getNodeId()).isEqualTo(TEST_NODE_ID);
        assertThat(nodeUpdateTransaction.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
        assertThat(nodeUpdateTransaction.getDescription()).isEqualTo(TEST_DESCRIPTION);
        assertThat(nodeUpdateTransaction.getGossipEndpoints()).hasSize(TEST_GOSSIP_ENDPOINTS.size());
        assertThat(nodeUpdateTransaction.getServiceEndpoints()).hasSize(TEST_SERVICE_ENDPOINTS.size());
        assertThat(nodeUpdateTransaction.getGossipCaCertificate()).isEqualTo(TEST_GOSSIP_CA_CERTIFICATE);
        assertThat(nodeUpdateTransaction.getGrpcCertificateHash()).isEqualTo(TEST_GRPC_CERTIFICATE_HASH);
        assertThat(nodeUpdateTransaction.getAdminKey()).isEqualTo(TEST_ADMIN_KEY);
        assertThat(nodeUpdateTransaction.getDeclineReward()).isEqualTo(true);
    }

    @Test
    void getSetNodeId() {
        var nodeUpdateTransaction = new NodeUpdateTransaction().setNodeId(TEST_NODE_ID);
        assertThat(nodeUpdateTransaction.getNodeId()).isEqualTo(TEST_NODE_ID);
    }

    @Test
    void getSetNodeIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setNodeId(TEST_NODE_ID));
    }

    @Test
    void getSetAccountId() {
        var nodeUpdateTransaction = new NodeUpdateTransaction().setAccountId(TEST_ACCOUNT_ID);
        assertThat(nodeUpdateTransaction.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    }

    @Test
    void getSetAccountIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAccountId(TEST_ACCOUNT_ID));
    }

    @Test
    void getSetDescription() {
        var nodeUpdateTransaction = new NodeUpdateTransaction().setDescription(TEST_DESCRIPTION);
        assertThat(nodeUpdateTransaction.getDescription()).isEqualTo(TEST_DESCRIPTION);
    }

    @Test
    void getSetDescriptionFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setDescription(TEST_DESCRIPTION));
    }

    @Test
    void getSetGossipEndpoints() {
        var nodeUpdateTransaction = new NodeUpdateTransaction().setGossipEndpoints(TEST_GOSSIP_ENDPOINTS);
        assertThat(nodeUpdateTransaction.getGossipEndpoints()).isEqualTo(TEST_GOSSIP_ENDPOINTS);
    }

    @Test
    void setTestGossipEndpointsFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setGossipEndpoints(TEST_GOSSIP_ENDPOINTS));
    }

    @Test
    void getSetServiceEndpoints() {
        var nodeUpdateTransaction = new NodeUpdateTransaction().setServiceEndpoints(TEST_SERVICE_ENDPOINTS);
        assertThat(nodeUpdateTransaction.getServiceEndpoints()).isEqualTo(TEST_SERVICE_ENDPOINTS);
    }

    @Test
    void getSetServiceEndpointsFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setServiceEndpoints(TEST_SERVICE_ENDPOINTS));
    }

    @Test
    void getSetGossipCaCertificate() {
        var nodeUpdateTransaction = new NodeUpdateTransaction().setGossipCaCertificate(TEST_GOSSIP_CA_CERTIFICATE);
        assertThat(nodeUpdateTransaction.getGossipCaCertificate()).isEqualTo(TEST_GOSSIP_CA_CERTIFICATE);
    }

    @Test
    void getSetGossipCaCertificateFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setGossipCaCertificate(TEST_GOSSIP_CA_CERTIFICATE));
    }

    @Test
    void getSetGrpcCertificateHash() {
        var nodeUpdateTransaction = new NodeUpdateTransaction().setGrpcCertificateHash(TEST_GRPC_CERTIFICATE_HASH);
        assertThat(nodeUpdateTransaction.getGrpcCertificateHash()).isEqualTo(TEST_GRPC_CERTIFICATE_HASH);
    }

    @Test
    void getSetGrpcCertificateHashFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setGrpcCertificateHash(TEST_GRPC_CERTIFICATE_HASH));
    }

    @Test
    void getSetAdminKey() {
        var nodeUpdateTransaction = new NodeUpdateTransaction().setAdminKey(TEST_ADMIN_KEY);
        assertThat(nodeUpdateTransaction.getAdminKey()).isEqualTo(TEST_ADMIN_KEY);
    }

    @Test
    void getSetAdminKeyFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAdminKey(TEST_ADMIN_KEY));
    }

    @Test
    void getSetDeclineReward() {
        var tx = new NodeUpdateTransaction().setDeclineReward(true);
        assertThat(tx.getDeclineReward()).isEqualTo(true);
    }

    @Test
    void getSetDeclineRewardFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setDeclineReward(false));
    }

    @Test
    void getGrpcWebProxyEndpoint() {
        var nodeUpdateTransaction = new NodeUpdateTransaction().setGrpcWebProxyEndpoint(TEST_GRPC_WEB_PROXY_ENDPOINT);
        assertThat(nodeUpdateTransaction.getGrpcWebProxyEndpoint()).isEqualTo(TEST_GRPC_WEB_PROXY_ENDPOINT);
    }

    @Test
    void setGrpcWebProxyEndpointRequiresFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setGrpcWebProxyEndpoint(TEST_GRPC_WEB_PROXY_ENDPOINT));
    }

    @Test
    @DisplayName("should freeze successfully when nodeId is set")
    void shouldFreezeSuccessfullyWhenNodeIdIsSet() {
        var transaction = new NodeUpdateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(TEST_ACCOUNT_ID, TEST_VALID_START))
                .setNodeId(TEST_NODE_ID);

        assertThatCode(() -> transaction.freezeWith(null)).doesNotThrowAnyException();
        assertThat(transaction.getNodeId()).isEqualTo(TEST_NODE_ID);
    }

    @Test
    @DisplayName("should throw error when freezing without setting nodeId")
    void shouldThrowErrorWhenFreezingWithoutSettingNodeId() {
        var transaction = new NodeUpdateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(TEST_ACCOUNT_ID, TEST_VALID_START));

        var exception = assertThrows(IllegalStateException.class, () -> transaction.freezeWith(null));
        assertThat(exception.getMessage())
                .isEqualTo("NodeUpdateTransaction: 'nodeId' must be explicitly set before calling freeze().");
    }

    @Test
    @DisplayName("should throw error when freezing with nodeId null")
    void shouldThrowErrorWhenFreezingWithZeroNodeId() {
        var transaction = new NodeUpdateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(TEST_ACCOUNT_ID, TEST_VALID_START));

        var exception = assertThrows(IllegalStateException.class, () -> transaction.freezeWith(null));
        assertThat(exception.getMessage())
                .isEqualTo("NodeUpdateTransaction: 'nodeId' must be explicitly set before calling freeze().");
    }

    @Test
    @DisplayName("should freeze successfully with actual client when nodeId is set")
    void shouldFreezeSuccessfullyWithActualClientWhenNodeIdIsSet() {
        var transaction = new NodeUpdateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(TEST_ACCOUNT_ID, TEST_VALID_START))
                .setNodeId(TEST_NODE_ID);

        var mockClient = Client.forTestnet();

        assertThatCode(() -> transaction.freezeWith(mockClient)).doesNotThrowAnyException();
        assertThat(transaction.getNodeId()).isEqualTo(TEST_NODE_ID);
    }

    @Test
    @DisplayName("should freeze successfully when nodeId is set with additional fields")
    void shouldFreezeSuccessfullyWhenNodeIdIsSetWithAdditionalFields() {
        var transaction = new NodeUpdateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.3")))
                .setTransactionId(TransactionId.withValidStart(TEST_ACCOUNT_ID, TEST_VALID_START))
                .setNodeId(TEST_NODE_ID)
                .setDescription(TEST_DESCRIPTION)
                .setAccountId(TEST_ACCOUNT_ID)
                .setDeclineReward(false);

        assertThatCode(() -> transaction.freezeWith(null)).doesNotThrowAnyException();
        assertThat(transaction.getNodeId()).isEqualTo(TEST_NODE_ID);
        assertThat(transaction.getDescription()).isEqualTo(TEST_DESCRIPTION);
        assertThat(transaction.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
        assertThat(transaction.getDeclineReward()).isEqualTo(false);
    }

    @Test
    @DisplayName("should throw error when getting nodeId without setting it")
    void shouldThrowErrorWhenGettingNodeIdWithoutSettingIt() {
        var transaction = new NodeUpdateTransaction();

        var exception = assertThrows(IllegalStateException.class, () -> transaction.getNodeId());
        assertThat(exception.getMessage()).isEqualTo("NodeUpdateTransaction: 'nodeId' has not been set");
    }

    // ===== Validation Tests =====

    @Test
    @DisplayName("should throw error when setting negative nodeId")
    void shouldThrowErrorWhenSettingNegativeNodeId() {
        var transaction = new NodeUpdateTransaction();

        var exception = assertThrows(IllegalArgumentException.class, () -> transaction.setNodeId(-1));
        assertThat(exception.getMessage()).isEqualTo("nodeId must be non-negative");
    }

    @Test
    @DisplayName("should allow setting nodeId to zero")
    void shouldAllowSettingNodeIdToZero() {
        var transaction = new NodeUpdateTransaction().setNodeId(0);
        assertThat(transaction.getNodeId()).isEqualTo(0);
    }

    @Test
    @DisplayName("should throw error when description exceeds 100 bytes in UTF-8")
    void shouldThrowErrorWhenDescriptionExceeds100Bytes() {
        var transaction = new NodeUpdateTransaction();
        // Create a 101-byte UTF-8 string
        var longDescription = "a".repeat(101);

        var exception = assertThrows(IllegalArgumentException.class, () -> transaction.setDescription(longDescription));
        assertThat(exception.getMessage()).isEqualTo("Description must not exceed 100 bytes when encoded as UTF-8");
    }

    @Test
    @DisplayName("should allow description with exactly 100 bytes in UTF-8")
    void shouldAllowDescriptionWith100Bytes() {
        var transaction = new NodeUpdateTransaction();
        var description = "a".repeat(100);

        assertThatCode(() -> transaction.setDescription(description)).doesNotThrowAnyException();
        assertThat(transaction.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("should allow null description")
    void shouldAllowNullDescription() {
        var transaction = new NodeUpdateTransaction();
        assertThatCode(() -> transaction.setDescription(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw error when setting empty gossip endpoints list")
    void shouldThrowErrorWhenSettingEmptyGossipEndpointsList() {
        var transaction = new NodeUpdateTransaction();

        var exception = assertThrows(IllegalArgumentException.class, () -> transaction.setGossipEndpoints(List.of()));
        assertThat(exception.getMessage()).isEqualTo("Gossip endpoints list must not be empty");
    }

    @Test
    @DisplayName("should throw error when setting more than 10 gossip endpoints")
    void shouldThrowErrorWhenSettingMoreThan10GossipEndpoints() {
        var transaction = new NodeUpdateTransaction();
        var endpoints = List.of(
                spawnTestEndpointIpOnly((byte) 0),
                spawnTestEndpointIpOnly((byte) 1),
                spawnTestEndpointIpOnly((byte) 2),
                spawnTestEndpointIpOnly((byte) 3),
                spawnTestEndpointIpOnly((byte) 4),
                spawnTestEndpointIpOnly((byte) 5),
                spawnTestEndpointIpOnly((byte) 6),
                spawnTestEndpointIpOnly((byte) 7),
                spawnTestEndpointIpOnly((byte) 8),
                spawnTestEndpointIpOnly((byte) 9),
                spawnTestEndpointIpOnly((byte) 10));

        var exception = assertThrows(IllegalArgumentException.class, () -> transaction.setGossipEndpoints(endpoints));
        assertThat(exception.getMessage()).isEqualTo("Gossip endpoints list must not contain more than 10 entries");
    }

    @Test
    @DisplayName("should allow exactly 10 gossip endpoints")
    void shouldAllowExactly10GossipEndpoints() {
        var transaction = new NodeUpdateTransaction();
        var endpoints = List.of(
                spawnTestEndpointIpOnly((byte) 0),
                spawnTestEndpointIpOnly((byte) 1),
                spawnTestEndpointIpOnly((byte) 2),
                spawnTestEndpointIpOnly((byte) 3),
                spawnTestEndpointIpOnly((byte) 4),
                spawnTestEndpointIpOnly((byte) 5),
                spawnTestEndpointIpOnly((byte) 6),
                spawnTestEndpointIpOnly((byte) 7),
                spawnTestEndpointIpOnly((byte) 8),
                spawnTestEndpointIpOnly((byte) 9));

        assertThatCode(() -> transaction.setGossipEndpoints(endpoints)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw error when gossip endpoint has both IP and domain name")
    void shouldThrowErrorWhenGossipEndpointHasBothIpAndDomain() {
        var transaction = new NodeUpdateTransaction();
        var endpoint = new Endpoint()
                .setAddress(new byte[] {127, 0, 0, 1})
                .setDomainName("example.com")
                .setPort(50211);

        var exception =
                assertThrows(IllegalArgumentException.class, () -> transaction.setGossipEndpoints(List.of(endpoint)));
        assertThat(exception.getMessage()).isEqualTo("Endpoint must not contain both ipAddressV4 and domainName");
    }

    @Test
    @DisplayName("should throw error when setting empty service endpoints list")
    void shouldThrowErrorWhenSettingEmptyServiceEndpointsList() {
        var transaction = new NodeUpdateTransaction();

        var exception = assertThrows(IllegalArgumentException.class, () -> transaction.setServiceEndpoints(List.of()));
        assertThat(exception.getMessage()).isEqualTo("Service endpoints list must not be empty");
    }

    @Test
    @DisplayName("should throw error when setting more than 8 service endpoints")
    void shouldThrowErrorWhenSettingMoreThan8ServiceEndpoints() {
        var transaction = new NodeUpdateTransaction();
        var endpoints = List.of(
                spawnTestEndpointIpOnly((byte) 0),
                spawnTestEndpointIpOnly((byte) 1),
                spawnTestEndpointIpOnly((byte) 2),
                spawnTestEndpointIpOnly((byte) 3),
                spawnTestEndpointIpOnly((byte) 4),
                spawnTestEndpointIpOnly((byte) 5),
                spawnTestEndpointIpOnly((byte) 6),
                spawnTestEndpointIpOnly((byte) 7),
                spawnTestEndpointIpOnly((byte) 8));

        var exception = assertThrows(IllegalArgumentException.class, () -> transaction.setServiceEndpoints(endpoints));
        assertThat(exception.getMessage()).isEqualTo("Service endpoints list must not contain more than 8 entries");
    }

    @Test
    @DisplayName("should allow exactly 8 service endpoints")
    void shouldAllowExactly8ServiceEndpoints() {
        var transaction = new NodeUpdateTransaction();
        var endpoints = List.of(
                spawnTestEndpointIpOnly((byte) 0),
                spawnTestEndpointIpOnly((byte) 1),
                spawnTestEndpointIpOnly((byte) 2),
                spawnTestEndpointIpOnly((byte) 3),
                spawnTestEndpointIpOnly((byte) 4),
                spawnTestEndpointIpOnly((byte) 5),
                spawnTestEndpointIpOnly((byte) 6),
                spawnTestEndpointIpOnly((byte) 7));

        assertThatCode(() -> transaction.setServiceEndpoints(endpoints)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw error when service endpoint has both IP and domain name")
    void shouldThrowErrorWhenServiceEndpointHasBothIpAndDomain() {
        var transaction = new NodeUpdateTransaction();
        var endpoint = new Endpoint()
                .setAddress(new byte[] {127, 0, 0, 1})
                .setDomainName("example.com")
                .setPort(50212);

        var exception =
                assertThrows(IllegalArgumentException.class, () -> transaction.setServiceEndpoints(List.of(endpoint)));
        assertThat(exception.getMessage()).isEqualTo("Endpoint must not contain both ipAddressV4 and domainName");
    }

    @Test
    @DisplayName("should throw error when setting null gossip CA certificate")
    void shouldThrowErrorWhenSettingNullGossipCaCertificate() {
        var transaction = new NodeUpdateTransaction();

        var exception = assertThrows(IllegalArgumentException.class, () -> transaction.setGossipCaCertificate(null));
        assertThat(exception.getMessage()).isEqualTo("Gossip CA certificate must not be null or empty");
    }

    @Test
    @DisplayName("should throw error when setting empty gossip CA certificate")
    void shouldThrowErrorWhenSettingEmptyGossipCaCertificate() {
        var transaction = new NodeUpdateTransaction();

        var exception =
                assertThrows(IllegalArgumentException.class, () -> transaction.setGossipCaCertificate(new byte[] {}));
        assertThat(exception.getMessage()).isEqualTo("Gossip CA certificate must not be null or empty");
    }

    @Test
    @DisplayName("should allow valid gossip CA certificate")
    void shouldAllowValidGossipCaCertificate() {
        var transaction = new NodeUpdateTransaction();
        var cert = new byte[] {1, 2, 3, 4, 5};

        assertThatCode(() -> transaction.setGossipCaCertificate(cert)).doesNotThrowAnyException();
        assertThat(transaction.getGossipCaCertificate()).isEqualTo(cert);
    }

    @Test
    @DisplayName("should throw error when setting gRPC certificate hash with wrong size")
    void shouldThrowErrorWhenSettingGrpcCertificateHashWithWrongSize() {
        var transaction = new NodeUpdateTransaction();
        var wrongSizeHash = new byte[32]; // SHA-256 size, but we need SHA-384 (48 bytes)

        var exception =
                assertThrows(IllegalArgumentException.class, () -> transaction.setGrpcCertificateHash(wrongSizeHash));
        assertThat(exception.getMessage()).isEqualTo("gRPC certificate hash must be exactly 48 bytes (SHA-384)");
    }

    @Test
    @DisplayName("should allow gRPC certificate hash with 48 bytes (SHA-384)")
    void shouldAllowGrpcCertificateHashWith48Bytes() {
        var transaction = new NodeUpdateTransaction();
        var validHash = new byte[48]; // SHA-384 size

        assertThatCode(() -> transaction.setGrpcCertificateHash(validHash)).doesNotThrowAnyException();
        assertThat(transaction.getGrpcCertificateHash()).isEqualTo(validHash);
    }

    @Test
    @DisplayName("should allow null gRPC certificate hash")
    void shouldAllowNullGrpcCertificateHash() {
        var transaction = new NodeUpdateTransaction();

        assertThatCode(() -> transaction.setGrpcCertificateHash(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should allow empty gRPC certificate hash")
    void shouldAllowEmptyGrpcCertificateHash() {
        var transaction = new NodeUpdateTransaction();

        // Empty is allowed because network will validate it
        assertThatCode(() -> transaction.setGrpcCertificateHash(new byte[] {})).doesNotThrowAnyException();
    }
}
