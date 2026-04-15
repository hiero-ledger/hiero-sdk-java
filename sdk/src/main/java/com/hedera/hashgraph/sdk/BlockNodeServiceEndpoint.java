// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represent a Registered Block Node
 */
public class BlockNodeServiceEndpoint extends RegisteredServiceEndpointBase<BlockNodeServiceEndpoint> {
    /**
     * An indicator of what API this endpoint supports.
     */
    private List<BlockNodeApi> endpointApis = new ArrayList<>();

    /**
     * Constructor.
     */
    public BlockNodeServiceEndpoint() {}

    public List<BlockNodeApi> getEndpointApis() {
        return endpointApis;
    }

    public BlockNodeServiceEndpoint setEndpointApis(List<BlockNodeApi> endpointApis) {
        Objects.requireNonNull(endpointApis, "endpointApis must not be null");
        this.endpointApis = new ArrayList<>(endpointApis);
        return this;
    }

    public BlockNodeServiceEndpoint addEndpointApi(BlockNodeApi endpointApi) {
        Objects.requireNonNull(endpointApi, "endpointApi must not be null");
        this.endpointApis.add(endpointApi);
        return this;
    }

    public BlockNodeServiceEndpoint clearEndpointApis() {
        endpointApis.clear();
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
                .setRequiresTls(serviceEndpoint.getRequiresTls());

        if (serviceEndpoint.hasBlockNode()) {
            for (var apiProto : serviceEndpoint.getBlockNode().getEndpointApiList()) {
                blockNodeEndpoint.addEndpointApi(BlockNodeApi.valueOf(apiProto));
            }
        }

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

        var blockNodeBuilder = RegisteredServiceEndpoint.BlockNodeEndpoint.newBuilder()
                .addAllEndpointApi(endpointApis.stream().map(api -> api.code).toList());

        var registeredServiceEndpoint = com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.newBuilder()
                .setPort(port)
                .setRequiresTls(requiresTls)
                .setBlockNode(blockNodeBuilder);

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
        return toStringHelper().add("endpointApis", endpointApis).toString();
    }
}
