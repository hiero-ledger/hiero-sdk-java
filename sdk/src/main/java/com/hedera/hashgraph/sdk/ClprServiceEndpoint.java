// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;

/**
 * Read-only IP address and port for a CLPR endpoint.
 * <p>
 * The IP address is an IPv4 or IPv6 numeric string.
 * CLPR is off by default and must be enabled with {@code clpr.enabled}.
 */
public final class ClprServiceEndpoint {
    private final String ipAddress;
    private final int port;

    private ClprServiceEndpoint(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    static ClprServiceEndpoint fromProtobuf(com.hedera.hashgraph.sdk.proto.ClprServiceEndpoint endpoint) {
        return new ClprServiceEndpoint(endpoint.getIpAddress(), endpoint.getPort());
    }

    com.hedera.hashgraph.sdk.proto.ClprServiceEndpoint toProtobuf() {
        return com.hedera.hashgraph.sdk.proto.ClprServiceEndpoint.newBuilder()
                .setIpAddress(ipAddress)
                .setPort(port)
                .build();
    }

    /**
     * @return the IPv4 or IPv6 numeric address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("ipAddress", ipAddress)
                .add("port", port)
                .toString();
    }
}
