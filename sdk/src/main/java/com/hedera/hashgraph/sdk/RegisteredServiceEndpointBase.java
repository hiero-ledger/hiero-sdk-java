// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import javax.annotation.Nullable;

abstract class RegisteredServiceEndpointBase<T extends RegisteredServiceEndpointBase<T>>
        extends RegisteredServiceEndpoint {
    /**
     * Set the IP address of the endpoint.
     *
     * @param ipAddress the IPv4 or IPv6 address
     * @return this endpoint
     */
    public T setIpAddress(@Nullable byte[] ipAddress) {
        this.ipAddress = ipAddress;
        // noinspection unchecked
        return (T) this;
    }

    /**
     * Set the domain name of the endpoint.
     *
     * @param domainName the fully qualified domain name
     * @return this endpoint
     */
    public T setDomainName(@Nullable String domainName) {
        this.domainName = domainName;
        // noinspection unchecked
        return (T) this;
    }

    /**
     * Set the port used by this endpoint.
     *
     * @param port the port number
     * @return this endpoint
     * @throws IllegalArgumentException if the port is outside the valid range
     */
    public T setPort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be in range [0, 65535]");
        }
        this.port = port;
        // noinspection unchecked
        return (T) this;
    }

    /**
     * Set whether TLS is required for this endpoint.
     *
     * @param requiresTls true if TLS is required
     * @return this endpoint
     */
    public T setRequiresTls(boolean requiresTls) {
        this.requiresTls = requiresTls;
        // noinspection unchecked
        return (T) this;
    }
}
