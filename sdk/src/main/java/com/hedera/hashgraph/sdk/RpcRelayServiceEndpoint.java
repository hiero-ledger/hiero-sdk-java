// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
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
                .setRequiresTls(requiresTls);

        if (ipAddress != null) {
            registeredServiceEndpoint.setIpAddress(ByteString.copyFrom(ipAddress));
        }

        if (domainName != null) {
            registeredServiceEndpoint.setDomainName(domainName);
        }

        return registeredServiceEndpoint.build();
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
