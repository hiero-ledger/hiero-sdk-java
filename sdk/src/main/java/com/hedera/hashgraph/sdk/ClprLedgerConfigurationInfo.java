// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprGetLedgerConfigurationResponse;
import com.hedera.hashgraph.sdk.proto.ClprLedgerConfiguration;
import java.time.Instant;
import org.bouncycastle.util.encoders.Hex;

/**
 * Read-only CLPR ledger configuration and its opaque state proof.
 * <p>
 * Configuration fields are live, while the proof describes a signed snapshot.
 * An empty proof is a successful read when no signed snapshot is available yet.
 * Byte values returned by this model are copies.
 * CLPR is off by default and must be enabled with {@code clpr.enabled}.
 */
public final class ClprLedgerConfigurationInfo {
    private final int protocolVersion;
    private final String chainId;
    private final byte[] serviceAddress;
    private final Instant timestamp;
    private final ClprThrottles throttles;
    private final byte[] initialTrustAnchor;
    private final byte[] initialTrustAnchorId;
    private final byte[] configurationStateProof;

    private ClprLedgerConfigurationInfo(
            int protocolVersion,
            String chainId,
            byte[] serviceAddress,
            Instant timestamp,
            ClprThrottles throttles,
            byte[] initialTrustAnchor,
            byte[] initialTrustAnchorId,
            byte[] configurationStateProof) {
        this.protocolVersion = protocolVersion;
        this.chainId = chainId;
        this.serviceAddress = serviceAddress;
        this.timestamp = timestamp;
        this.throttles = throttles;
        this.initialTrustAnchor = initialTrustAnchor;
        this.initialTrustAnchorId = initialTrustAnchorId;
        this.configurationStateProof = configurationStateProof;
    }

    static ClprLedgerConfigurationInfo fromProtobuf(ClprGetLedgerConfigurationResponse response) {
        var configuration = response.getConfiguration();
        return new ClprLedgerConfigurationInfo(
                configuration.getProtocolVersion(),
                configuration.getChainId(),
                configuration.getServiceAddress().toByteArray(),
                InstantConverter.fromProtobuf(configuration.getTimestamp()),
                ClprThrottles.fromProtobuf(configuration.getThrottles()),
                configuration.getInitialTrustAnchor().toByteArray(),
                configuration.getInitialTrustAnchorId().toByteArray(),
                response.getConfigurationStateProof().toByteArray());
    }

    /**
     * Creates the configuration from a {@code ClprGetLedgerConfigurationResponse} protobuf byte array.
     *
     * @param bytes the byte array
     * @return the configuration
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static ClprLedgerConfigurationInfo fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(ClprGetLedgerConfigurationResponse.parseFrom(bytes));
    }

    ClprGetLedgerConfigurationResponse toProtobuf() {
        return ClprGetLedgerConfigurationResponse.newBuilder()
                .setConfiguration(ClprLedgerConfiguration.newBuilder()
                        .setProtocolVersion(protocolVersion)
                        .setChainId(chainId)
                        .setServiceAddress(ByteString.copyFrom(serviceAddress))
                        .setTimestamp(InstantConverter.toProtobuf(timestamp))
                        .setThrottles(throttles.toProtobuf())
                        .setInitialTrustAnchor(ByteString.copyFrom(initialTrustAnchor))
                        .setInitialTrustAnchorId(ByteString.copyFrom(initialTrustAnchorId)))
                .setConfigurationStateProof(ByteString.copyFrom(configurationStateProof))
                .build();
    }

    /**
     * Serializes the configuration to a {@code ClprGetLedgerConfigurationResponse} protobuf byte array.
     *
     * @return the byte array
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    /**
     * @return the CLPR protocol version, as an unsigned uint32
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * @return the ledger's chain ID
     */
    public String getChainId() {
        return chainId;
    }

    /**
     * @return a copy of this ledger's CLPR service address
     */
    public byte[] getServiceAddress() {
        return serviceAddress.clone();
    }

    /**
     * @return the consensus time of the last configuration update
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * @return the ledger's capacity limits, including any server values outside the configuration-update rules
     */
    public ClprThrottles getThrottles() {
        return throttles;
    }

    /**
     * @return a copy of the opaque starting trust anchor
     */
    public byte[] getInitialTrustAnchor() {
        return initialTrustAnchor.clone();
    }

    /**
     * @return a copy of the opaque identifier for the starting trust anchor
     */
    public byte[] getInitialTrustAnchorId() {
        return initialTrustAnchorId.clone();
    }

    /**
     * @return a copy of the snapshot state proof; empty bytes are a successful read
     */
    public byte[] getConfigurationStateProof() {
        return configurationStateProof.clone();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("protocolVersion", Integer.toUnsignedString(protocolVersion))
                .add("chainId", chainId)
                .add("serviceAddress", Hex.toHexString(serviceAddress))
                .add("timestamp", timestamp)
                .add("throttles", throttles)
                .add("initialTrustAnchor", Hex.toHexString(initialTrustAnchor))
                .add("initialTrustAnchorId", Hex.toHexString(initialTrustAnchorId))
                .add("configurationStateProof", Hex.toHexString(configurationStateProof))
                .toString();
    }
}
