// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 *  Abstract class representing the service endpoint published by a registered node.
 */
public abstract class RegisteredServiceEndpoint {
    @Nullable
    protected byte[] ipAddress;

    @Nullable
    protected String domainName;

    protected int port;
    protected boolean requiresTls;

    RegisteredServiceEndpoint() {}

    /**
     * Get the IP address of the endpoint.
     *
     * @return the IP address, or null if using a domain name
     */
    @Nullable
    public byte[] getIpAddress() {
        return ipAddress != null ? ipAddress.clone() : null;
    }

    /**
     * Get the domain name of the endpoint.
     *
     * @return the domain name, or null if using an IP address
     */
    @Nullable
    public String getDomainName() {
        return domainName;
    }

    /**
     * Get the port used by this endpoint.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Check whether TLS is required for this endpoint.
     *
     * @return true if TLS is required
     */
    public boolean isRequiresTls() {
        return requiresTls;
    }

    /**
     * Validate that the endpoint does not contain both an IP address and a domain name.
     *
     * @param serviceEndpoint the endpoint to validate
     * @throws IllegalArgumentException if both ipAddressV4 and domainName are present
     */
    public static void validateNoIpAndDomain(RegisteredServiceEndpoint serviceEndpoint) {
        if (serviceEndpoint == null) {
            return;
        }
        if (serviceEndpoint.getIpAddress() != null) {
            var dn = serviceEndpoint.getDomainName();
            if (dn != null && !dn.isEmpty()) {
                throw new IllegalArgumentException("Service endpoint must not contain both ipAddressV4 and domainName");
            }
        }
    }

    static RegisteredServiceEndpoint fromProtobuf(
            com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint serviceEndpoint) {
        Objects.requireNonNull(serviceEndpoint, "serviceEndpoint cannot be null");

        return switch (serviceEndpoint.getEndpointTypeCase()) {
            case BLOCK_NODE -> BlockNodeServiceEndpoint.fromProtobuf(serviceEndpoint);
            case MIRROR_NODE -> MirrorNodeServiceEndpoint.fromProtobuf(serviceEndpoint);
            case RPC_RELAY -> RpcRelayServiceEndpoint.fromProtobuf(serviceEndpoint);
            default -> throw new IllegalArgumentException("Unable to decode registered service endpoint");
        };
    }

    /**
     * Build the protobuf.
     *
     * @return the protobuf representation
     */
    abstract com.hedera.hashgraph.sdk.proto.RegisteredServiceEndpoint toProtobuf();

    /**
     * Serializes the class to ToStringHelper
     *
     * @return the {@link com.google.common.base.MoreObjects.ToStringHelper}
     */
    protected MoreObjects.ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
            .add("ipAddress", ipAddress)
            .add("domainName", domainName)
            .add("port", port)
            .add("requiresTls", requiresTls);
    }
}
