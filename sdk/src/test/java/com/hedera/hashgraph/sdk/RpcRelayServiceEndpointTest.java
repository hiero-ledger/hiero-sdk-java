package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RpcRelayServiceEndpointTest {
    private final byte[] TEST_IP_ADDRESS = new byte[] {1, 2, 3, 4};
    private final String TEST_DOMAIN_NAME = "test.mirror.com";
    private final int TEST_PORT = 443;
    private final boolean TEST_REQUIRES_TLS = true;

    private final com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint rpcEndpointWithDomain = com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.newBuilder()
        .setDomainName(TEST_DOMAIN_NAME)
        .setPort(TEST_PORT)
        .setRequiresTls(TEST_REQUIRES_TLS)
        .setRpcRelay(RegisteredServiceEndpoint.RpcRelayEndpoint.newBuilder())
        .build();

    private final com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint rpcEndpointWithIp = com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.newBuilder()
        .setIpAddress(ByteString.copyFrom(TEST_IP_ADDRESS))
        .setPort(TEST_PORT)
        .setRequiresTls(TEST_REQUIRES_TLS)
        .setRpcRelay(RegisteredServiceEndpoint.RpcRelayEndpoint.newBuilder())
        .build();

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void fromProtobufWithDomain() {
        SnapshotMatcher.expect(RpcRelayServiceEndpoint.fromProtobuf(rpcEndpointWithDomain).toString()).toMatchSnapshot();
    }

    @Test
    void toProtobufWithDomain() {
        SnapshotMatcher.expect(RpcRelayServiceEndpoint.fromProtobuf(rpcEndpointWithDomain).toProtobuf().toString())
            .toMatchSnapshot();
    }

    @Test
    void fromProtobufWithIp() {
        SnapshotMatcher.expect(RpcRelayServiceEndpoint.fromProtobuf(rpcEndpointWithIp).toString()).toMatchSnapshot();
    }

    @Test
    void toProtobufWithIp() {
        SnapshotMatcher.expect(RpcRelayServiceEndpoint.fromProtobuf(rpcEndpointWithIp).toProtobuf().toString())
            .toMatchSnapshot();
    }

    @Test
    void setIpAddress() {
        var endpoint = new BlockNodeServiceEndpoint().setIpAddress(TEST_IP_ADDRESS);
        assertThat(endpoint.getIpAddress()).isEqualTo(TEST_IP_ADDRESS);
    }

    @Test
    void setDomainName() {
        var endpoint = new BlockNodeServiceEndpoint().setDomainName(TEST_DOMAIN_NAME);
        assertThat(endpoint.getDomainName()).isEqualTo(TEST_DOMAIN_NAME);
    }

    @Test
    void setPort() {
        var endpoint = new BlockNodeServiceEndpoint().setPort(TEST_PORT);
        assertThat(endpoint.getPort()).isEqualTo(TEST_PORT);
    }

    @Test
    void setPortThrowsOnNegative() {
        var endpoint = new BlockNodeServiceEndpoint();
        assertThatThrownBy(() -> endpoint.setPort(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setPortThrowsOnGreaterThan65535() {
        var endpoint = new BlockNodeServiceEndpoint();
        assertThatThrownBy(() -> endpoint.setPort(65536))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setRequiresTls() {
        var endpoint = new BlockNodeServiceEndpoint().setRequiresTls(TEST_REQUIRES_TLS);
        assertThat(endpoint.isRequiresTls()).isEqualTo(TEST_REQUIRES_TLS);
    }
}
