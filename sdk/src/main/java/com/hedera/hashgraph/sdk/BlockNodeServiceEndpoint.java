// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import java.util.Objects;

/**
 * Represent a Registered Block Node
 */
public class BlockNodeServiceEndpoint extends RegisteredServiceEndpointBase<BlockNodeServiceEndpoint> {
    /**
     * An indicator of what API this endpoint supports.
     */
    private BlockNodeApi endpointApi = BlockNodeApi.OTHER;

    /**
     * Constructor.
     */
    public BlockNodeServiceEndpoint() {}

    public BlockNodeApi getEndpointApi() {
        return endpointApi;
    }

    public BlockNodeServiceEndpoint setEndpointApi(BlockNodeApi endpointApi) {
        this.endpointApi = endpointApi;
        return this;
    }

    /**
     * Create a BlockNodeServiceEndpoint object from protobuf
     *
     * @param serviceEndpoint the protobuf object
     * @return the new instance of BlockNodeServiceEndpoint
     */
    static BlockNodeServiceEndpoint fromProtobuf(
            com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint serviceEndpoint) {
        Objects.requireNonNull(serviceEndpoint, "serviceEndpoint must not be null");

        var blockNodeEndpoint = new BlockNodeServiceEndpoint()
                .setPort(serviceEndpoint.getPort())
                .setRequiresTls(serviceEndpoint.getRequiresTls())
                .setEndpointApi(
                        BlockNodeApi.valueOf(serviceEndpoint.getBlockNode().getEndpointApi()));

        if (serviceEndpoint.hasIpAddress()) {
            blockNodeEndpoint.setIpAddress(serviceEndpoint.getIpAddress().toByteArray());
        }

        if (serviceEndpoint.hasDomainName()) {
            blockNodeEndpoint.setDomainName(serviceEndpoint.getDomainName());
        }

        return blockNodeEndpoint;
    }

    @Override
    com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint toProtobuf() {
        if (ipAddress == null && domainName == null) {
            throw new IllegalArgumentException(
                    "RegisterServiceEndpoint must define either an ipAddress or  domainName");
        }

        var registeredServiceEndpoint = com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.newBuilder()
                .setPort(port)
                .setRequiresTls(requiresTls)
                .setBlockNode(com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.BlockNodeEndpoint.newBuilder()
                        .setEndpointApi(endpointApi.code)
                        .build());

        if (ipAddress != null) {
            registeredServiceEndpoint.setIpAddress(ByteString.copyFrom(this.ipAddress));
        }

        if (domainName != null) {
            registeredServiceEndpoint.setDomainName(this.domainName);
        }

        return registeredServiceEndpoint.build();
    }

    @Override
    public String toString() {
        return toStringHelper().add("endpointApi", endpointApi).toString();
    }
}
