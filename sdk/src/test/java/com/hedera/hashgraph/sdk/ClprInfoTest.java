// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.ClprEndpointManifest;
import com.hedera.hashgraph.sdk.proto.ClprGetEndpointManifestResponse;
import com.hedera.hashgraph.sdk.proto.ClprGetLedgerConfigurationResponse;
import com.hedera.hashgraph.sdk.proto.ClprLedgerConfiguration;
import com.hedera.hashgraph.sdk.proto.Timestamp;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ClprInfoTest {
    static final ClprGetLedgerConfigurationResponse CONFIGURATION_RESPONSE =
            ClprGetLedgerConfigurationResponse.newBuilder()
                    .setConfiguration(ClprLedgerConfiguration.newBuilder()
                            .setProtocolVersion(1)
                            .setChainId("hedera-local")
                            .setServiceAddress(ByteString.fromHex("000000000000000000000000000000000000016e"))
                            .setTimestamp(Timestamp.newBuilder()
                                    .setSeconds(1554158542)
                                    .setNanos(7))
                            .setThrottles(com.hedera.hashgraph.sdk.proto.ClprThrottles.newBuilder()
                                    .setMaxMessagesPerBundle(10)
                                    .setMaxMessagePayloadBytes(4096)
                                    .setMaxGasPerMessage(-1L)
                                    .setMaxQueueDepth(-1)
                                    .setMaxSyncBytes(1 << 20)
                                    .setMaxPeerEndpoints(5))
                            .setInitialTrustAnchor(ByteString.fromHex("aabb"))
                            .setInitialTrustAnchorId(ByteString.fromHex("ccdd")))
                    .setConfigurationStateProof(ByteString.fromHex("deadbeef"))
                    .build();

    static final ClprGetEndpointManifestResponse MANIFEST_RESPONSE = ClprGetEndpointManifestResponse.newBuilder()
            .setManifest(ClprEndpointManifest.newBuilder()
                    .setVersion(-1L)
                    .setServiceAddress(ByteString.fromHex("000000000000000000000000000000000000016e"))
                    .addEndpoints(com.hedera.hashgraph.sdk.proto.ClprEndpoint.newBuilder()
                            .setServiceEndpoint(com.hedera.hashgraph.sdk.proto.ClprServiceEndpoint.newBuilder()
                                    .setIpAddress("127.0.0.1")
                                    .setPort(50211))
                            .setTlsCertificate(ByteString.fromHex("3082"))
                            .setAccountId(ByteString.fromHex("0102"))))
            .setManifestStateProof(ByteString.fromHex("cafe"))
            .build();

    @BeforeAll
    static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void ledgerConfigurationShouldSerialize() {
        SnapshotMatcher.expect(ClprLedgerConfigurationInfo.fromProtobuf(CONFIGURATION_RESPONSE)
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void endpointManifestShouldSerialize() {
        SnapshotMatcher.expect(
                        ClprEndpointManifestInfo.fromProtobuf(MANIFEST_RESPONSE).toString())
                .toMatchSnapshot();
    }

    @Test
    void ledgerConfigurationDecodesAllFields() throws Exception {
        var info = ClprLedgerConfigurationInfo.fromBytes(CONFIGURATION_RESPONSE.toByteArray());
        assertThat(info.getProtocolVersion()).isEqualTo(1);
        assertThat(info.getChainId()).isEqualTo("hedera-local");
        assertThat(info.getServiceAddress()).isEqualTo(ClprOwnership.CLPR_SERVICE_ADDRESS);
        assertThat(info.getTimestamp()).isEqualTo(Instant.ofEpochSecond(1554158542, 7));
        assertThat(info.getThrottles().getMaxQueueDepth()).isEqualTo(4_294_967_295L);
        assertThat(Long.toUnsignedString(info.getThrottles().getMaxGasPerMessage()))
                .isEqualTo("18446744073709551615");
        assertThat(info.getInitialTrustAnchor()).containsExactly(0xaa, 0xbb);
        assertThat(info.getInitialTrustAnchorId()).containsExactly(0xcc, 0xdd);
        assertThat(info.getConfigurationStateProof()).containsExactly(0xde, 0xad, 0xbe, 0xef);
        assertThat(info.toBytes()).isEqualTo(CONFIGURATION_RESPONSE.toByteArray());
    }

    @Test
    void ledgerConfigurationReturnsCopies() {
        var info = ClprLedgerConfigurationInfo.fromProtobuf(CONFIGURATION_RESPONSE);
        info.getServiceAddress()[0] = 9;
        info.getConfigurationStateProof()[0] = 9;
        assertThat(info.getServiceAddress()[0]).isZero();
        assertThat(info.getConfigurationStateProof()[0]).isEqualTo((byte) 0xde);
    }

    @Test
    void emptyResponsesAreSuccessfulReads() {
        var configuration =
                ClprLedgerConfigurationInfo.fromProtobuf(ClprGetLedgerConfigurationResponse.getDefaultInstance());
        assertThat(configuration.getConfigurationStateProof()).isEmpty();
        assertThat(configuration.getChainId()).isEmpty();

        var manifest = ClprEndpointManifestInfo.fromProtobuf(ClprGetEndpointManifestResponse.getDefaultInstance());
        assertThat(manifest.getEndpoints()).isEmpty();
        assertThat(manifest.getManifestStateProof()).isEmpty();
    }

    @Test
    void endpointManifestDecodesAllFields() throws Exception {
        var info = ClprEndpointManifestInfo.fromBytes(MANIFEST_RESPONSE.toByteArray());
        assertThat(Long.toUnsignedString(info.getVersion())).isEqualTo("18446744073709551615");
        assertThat(info.getServiceAddress()).isEqualTo(ClprOwnership.CLPR_SERVICE_ADDRESS);
        assertThat(info.getEndpoints()).hasSize(1);
        var endpoint = info.getEndpoints().get(0);
        assertThat(endpoint.getServiceEndpoint().getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(endpoint.getServiceEndpoint().getPort()).isEqualTo(50211);
        assertThat(endpoint.getTlsCertificate()).containsExactly(0x30, 0x82);
        assertThat(endpoint.getAccountId()).containsExactly(1, 2);
        assertThat(info.getManifestStateProof()).containsExactly(0xca, 0xfe);
        assertThat(info.toBytes()).isEqualTo(MANIFEST_RESPONSE.toByteArray());
    }

    @Test
    void endpointListIsUnmodifiable() {
        var info = ClprEndpointManifestInfo.fromProtobuf(MANIFEST_RESPONSE);
        assertThatThrownBy(() -> info.getEndpoints().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
