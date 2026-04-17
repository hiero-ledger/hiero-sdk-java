// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BlockNodeServiceEndpointTest {
    private final byte[] TEST_IP_ADDRESS = new byte[] {1, 2, 3, 4};
    private final String TEST_DOMAIN_NAME = "test.block.com";
    private final int TEST_PORT = 443;
    private final boolean TEST_REQUIRES_TLS = true;
    private final List<BlockNodeApi> TEST_BLOCK_APIS = List.of(BlockNodeApi.STATUS);

    private final RegisteredServiceEndpoint blockNodeEndpointWithDomain = RegisteredServiceEndpoint.newBuilder()
            .setDomainName(TEST_DOMAIN_NAME)
            .setPort(TEST_PORT)
            .setRequiresTls(TEST_REQUIRES_TLS)
            .setBlockNode(RegisteredServiceEndpoint.BlockNodeEndpoint.newBuilder()
                    .addAllEndpointApi(TEST_BLOCK_APIS.stream().map(e -> e.code).toList()))
            .build();

    private final RegisteredServiceEndpoint blockNodeEndpointWithIp = RegisteredServiceEndpoint.newBuilder()
            .setIpAddress(ByteString.copyFrom(TEST_IP_ADDRESS))
            .setPort(TEST_PORT)
            .setRequiresTls(TEST_REQUIRES_TLS)
            .setBlockNode(RegisteredServiceEndpoint.BlockNodeEndpoint.newBuilder()
                    .addAllEndpointApi(TEST_BLOCK_APIS.stream().map(e -> e.code).toList()))
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
        SnapshotMatcher.expect(BlockNodeServiceEndpoint.fromProtobuf(blockNodeEndpointWithDomain)
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toProtobufWithDomain() {
        SnapshotMatcher.expect(BlockNodeServiceEndpoint.fromProtobuf(blockNodeEndpointWithDomain)
                        .toProtobuf()
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromProtobufWithIp() {
        SnapshotMatcher.expect(BlockNodeServiceEndpoint.fromProtobuf(blockNodeEndpointWithIp)
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void toProtobufWithIp() {
        SnapshotMatcher.expect(BlockNodeServiceEndpoint.fromProtobuf(blockNodeEndpointWithIp)
                        .toProtobuf()
                        .toString())
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
        assertThatThrownBy(() -> endpoint.setPort(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setPortThrowsOnGreaterThan65535() {
        var endpoint = new BlockNodeServiceEndpoint();
        assertThatThrownBy(() -> endpoint.setPort(65536)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setRequiresTls() {
        var endpoint = new BlockNodeServiceEndpoint().setRequiresTls(TEST_REQUIRES_TLS);
        assertThat(endpoint.isRequiresTls()).isEqualTo(TEST_REQUIRES_TLS);
    }

    @Test
    void setEndpointApis() {
        var endpoint = new BlockNodeServiceEndpoint().setEndpointApis(TEST_BLOCK_APIS);
        assertThat(endpoint.getEndpointApis()).isEqualTo(TEST_BLOCK_APIS);
    }

    @Test
    void addEndpointApi() {
        var endpoint = new BlockNodeServiceEndpoint()
                .addEndpointApi(BlockNodeApi.STATUS)
                .addEndpointApi(BlockNodeApi.OTHER);

        assertThat(endpoint.getEndpointApis()).containsExactly(BlockNodeApi.STATUS, BlockNodeApi.OTHER);
    }

    @Test
    void clearEndpointApis() {
        var endpoint =
                new BlockNodeServiceEndpoint().setEndpointApis(TEST_BLOCK_APIS).clearEndpointApis();

        assertThat(endpoint.getEndpointApis()).isEmpty();
    }
}
