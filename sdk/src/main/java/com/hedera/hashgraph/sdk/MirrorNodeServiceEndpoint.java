// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import java.util.Objects;

/**
 * Represent a Registered Mirror Node
 */
public class MirrorNodeServiceEndpoint extends RegisteredServiceEndpointBase<MirrorNodeServiceEndpoint> {
    /**
     * Constructor.
     *
     */
    public MirrorNodeServiceEndpoint() {}

    /**
     * Create a MirrorNodeServiceEndpoint object from protobuf
     *
     * @param serviceEndpoint the protobuf object
     * @return the new instance of MirrorNodeServiceEndpoint
     */
    static MirrorNodeServiceEndpoint fromProtobuf(
            com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint serviceEndpoint) {
        Objects.requireNonNull(serviceEndpoint, "serviceEndpoint must not be null");

        var mirrorNode = new MirrorNodeServiceEndpoint()
                .setPort(serviceEndpoint.getPort())
                .setRequiresTls(serviceEndpoint.getRequiresTls());

        if (serviceEndpoint.hasIpAddress()) {
            mirrorNode.setIpAddress(serviceEndpoint.getIpAddress().toByteArray());
        }

        if (serviceEndpoint.hasDomainName()) {
            mirrorNode.setDomainName(serviceEndpoint.getDomainName());
        }

        return mirrorNode;
    }

    @Override
    com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint toProtobuf() {
        var registeredServiceEndpoint = com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.newBuilder()
                .setPort(port)
                .setRequiresTls(requiresTls);

        if (ipAddress != null) {
            registeredServiceEndpoint.setIpAddress(ByteString.copyFrom(ipAddress));
        }

        if (domainName != null) {
            registeredServiceEndpoint.setDomainName(domainName);
        }

        return registeredServiceEndpoint.build();
    }

    /**
     * Parses MirrorNodeServiceEndpoint from the type-specific JSON object the MirrorNode.
     *
     * @param json the json containing mirror node specific data
     * @return {@code this}
     */
    static MirrorNodeServiceEndpoint fromJson(JsonObject json) {
        Objects.requireNonNull(json, "json must not be null");
        return new MirrorNodeServiceEndpoint();
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
