// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ClprEndpointManifest;
import com.hedera.hashgraph.sdk.proto.ClprGetEndpointManifestResponse;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;

/**
 * Read-only CLPR endpoint manifest paired with its opaque state proof.
 * <p>
 * Keep the manifest and proof together when building a peer endpoint cache or a manifest
 * recovery request. An empty proof is a successful read when no signed snapshot is available
 * yet; retry later for proof material. Byte values returned by this model are copies, and
 * the endpoint list is unmodifiable.
 * CLPR is off by default and must be enabled with {@code clpr.enabled}.
 */
public final class ClprEndpointManifestInfo {
    private final long version;
    private final byte[] serviceAddress;
    private final List<ClprEndpoint> endpoints;
    private final byte[] manifestStateProof;

    private ClprEndpointManifestInfo(
            long version, byte[] serviceAddress, List<ClprEndpoint> endpoints, byte[] manifestStateProof) {
        this.version = version;
        this.serviceAddress = serviceAddress;
        this.endpoints = List.copyOf(endpoints);
        this.manifestStateProof = manifestStateProof;
    }

    static ClprEndpointManifestInfo fromProtobuf(ClprGetEndpointManifestResponse response) {
        var manifest = response.getManifest();
        return new ClprEndpointManifestInfo(
                manifest.getVersion(),
                manifest.getServiceAddress().toByteArray(),
                manifest.getEndpointsList().stream()
                        .map(ClprEndpoint::fromProtobuf)
                        .toList(),
                response.getManifestStateProof().toByteArray());
    }

    /**
     * Creates the manifest from a {@code ClprGetEndpointManifestResponse} protobuf byte array.
     *
     * @param bytes the byte array
     * @return the manifest
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static ClprEndpointManifestInfo fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(ClprGetEndpointManifestResponse.parseFrom(bytes));
    }

    ClprGetEndpointManifestResponse toProtobuf() {
        var manifest = ClprEndpointManifest.newBuilder()
                .setVersion(version)
                .setServiceAddress(ByteString.copyFrom(serviceAddress));
        for (var endpoint : endpoints) {
            manifest.addEndpoints(endpoint.toProtobuf());
        }
        return ClprGetEndpointManifestResponse.newBuilder()
                .setManifest(manifest)
                .setManifestStateProof(ByteString.copyFrom(manifestStateProof))
                .build();
    }

    /**
     * Serializes the manifest to a {@code ClprGetEndpointManifestResponse} protobuf byte array.
     *
     * @return the byte array
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    /**
     * @return the manifest version, as an unsigned uint64
     */
    public long getVersion() {
        return version;
    }

    /**
     * @return a copy of the on-ledger address of the CLPR service this manifest belongs to
     */
    public byte[] getServiceAddress() {
        return serviceAddress.clone();
    }

    /**
     * @return the current endpoint set, as an unmodifiable list that may be empty
     */
    public List<ClprEndpoint> getEndpoints() {
        return endpoints;
    }

    /**
     * @return a copy of the state proof paired with this manifest; empty bytes are a successful read
     */
    public byte[] getManifestStateProof() {
        return manifestStateProof.clone();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("version", Long.toUnsignedString(version))
                .add("serviceAddress", Hex.toHexString(serviceAddress))
                .add("endpoints", endpoints)
                .add("manifestStateProof", Hex.toHexString(manifestStateProof))
                .toString();
    }
}
