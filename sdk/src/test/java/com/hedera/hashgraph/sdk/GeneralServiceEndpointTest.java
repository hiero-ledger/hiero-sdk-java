// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GeneralServiceEndpointTest {
    private static final byte[] TEST_IP_ADDRESS = new byte[] {1, 2, 3, 4};
    private static final String TEST_DOMAIN_NAME = "general.service.com";
    private static final String TEST_DESCRIPTION = "A general purpose endpoint.";
    private static final int TEST_PORT = 8080;
    private static final boolean TEST_REQUIRES_TLS = false;

    private final RegisteredServiceEndpoint generalEndpointWithDomain = RegisteredServiceEndpoint.newBuilder()
            .setDomainName(TEST_DOMAIN_NAME)
            .setPort(TEST_PORT)
            .setRequiresTls(TEST_REQUIRES_TLS)
            .setGeneralService(RegisteredServiceEndpoint.GeneralServiceEndpoint.newBuilder()
                    .setDescription(TEST_DESCRIPTION))
            .build();

    private final RegisteredServiceEndpoint generalEndpointWithIp = RegisteredServiceEndpoint.newBuilder()
            .setIpAddress(ByteString.copyFrom(TEST_IP_ADDRESS))
            .setPort(TEST_PORT)
            .setRequiresTls(TEST_REQUIRES_TLS)
            .setGeneralService(RegisteredServiceEndpoint.GeneralServiceEndpoint.newBuilder()
                    .setDescription(TEST_DESCRIPTION))
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
    void fromProtobufWithIp() {
        SnapshotMatcher.expect(GeneralServiceEndpoint.fromProtobuf(generalEndpointWithIp)
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void fromProtobufWithDomain() {
        SnapshotMatcher.expect(GeneralServiceEndpoint.fromProtobuf(generalEndpointWithDomain)
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void setDescription() {
        var endpoint = new GeneralServiceEndpoint().setDescription(TEST_DESCRIPTION);
        assertThat(endpoint.getDescription()).isEqualTo(TEST_DESCRIPTION);
    }
}
