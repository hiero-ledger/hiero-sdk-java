// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint;
import java.util.Objects;

/**
 * Represent a Registered Rpc Relay
 */
public class RpcRelayServiceEndpoint extends RegisteredServiceEndpointBase<RpcRelayServiceEndpoint> {
    /**
     * Constructor.
     *
     */
    public RpcRelayServiceEndpoint() {}

    /**
     * Create a RpcRelayServiceEndpoint object from protobuf
     *
     * @param serviceEndpoint the protobuf object
     * @return the new instance of RpcRelayServiceEndpoint
     */
    static RpcRelayServiceEndpoint fromProtobuf(
            com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint serviceEndpoint) {
        Objects.requireNonNull(serviceEndpoint, "serviceEndpoint must not be null");
        var rpcRelay = new RpcRelayServiceEndpoint()
                .setPort(serviceEndpoint.getPort())
                .setRequiresTls(serviceEndpoint.getRequiresTls());

        if (serviceEndpoint.hasIpAddress()) {
            rpcRelay.setIpAddress(serviceEndpoint.getIpAddress().toByteArray());
        }

        if (serviceEndpoint.hasDomainName()) {
            rpcRelay.setDomainName(serviceEndpoint.getDomainName());
        }

        return rpcRelay;
    }

    @Override
    com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint toProtobuf() {
        var registeredServiceEndpoint = com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint.newBuilder()
                .setPort(port)
                .setRequiresTls(requiresTls)
                .setRpcRelay(RegisteredServiceEndpoint.RpcRelayEndpoint.newBuilder());

        if (ipAddress != null) {
            registeredServiceEndpoint.setIpAddress(ByteString.copyFrom(ipAddress));
        }

        if (domainName != null) {
            registeredServiceEndpoint.setDomainName(domainName);
        }

        return registeredServiceEndpoint.build();
    }

    /**
     * Parses RpcRelayEndpoint from the type-specific JSON object the MirrorNode.
     *
     * @param json the json containing rpc relay specific data
     * @return {@code this}
     */
    static RpcRelayServiceEndpoint fromJson(JsonObject json) {
        Objects.requireNonNull(json, "json must not be null");
        return new RpcRelayServiceEndpoint();
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
