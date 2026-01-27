// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.ServiceEndpoint;
import javax.annotation.Nullable;

/**
 * Utility class used internally by the sdk.
 */
public class Endpoint implements Cloneable {

    @Nullable
    byte[] address = null;

    int port;

    String domainName = "";

    /**
     * Constructor.
     */
    public Endpoint() {}

    /**
     * Create an endpoint object from a service endpoint protobuf.
     *
     * @param serviceEndpoint           the service endpoint protobuf
     * @return                          the endpoint object
     */
    static Endpoint fromProtobuf(ServiceEndpoint serviceEndpoint) {
        var port = (int) (serviceEndpoint.getPort() & 0x00000000ffffffffL);

        return new Endpoint()
                .setAddress(serviceEndpoint.getIpAddressV4().toByteArray())
                .setPort(port)
                .setDomainName(serviceEndpoint.getDomainName());
    }

    /**
     * Extract the ipv4 address.
     *
     * @return                          the ipv4 address
     */
    @Nullable
    public byte[] getAddress() {
        return address;
    }

    /**
     * Assign the ipv4 address.
     *
     * @param address                   the desired ipv4 address
     * @return {@code this}
     */
    public Endpoint setAddress(byte[] address) {
        this.address = address;
        return this;
    }

    /**
     * Extract the port number.
     *
     * @return                          the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Assign the desired port number.
     *
     * @param port                      the desired port number
     * @return {@code this}
     */
    public Endpoint setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Extract the domain name.
     *
     * @return                          the domain name
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Assign the desired domain name.
     *
     * @param domainName                      the desired domain name
     * @return {@code this}
     */
    public Endpoint setDomainName(String domainName) {
        this.domainName = domainName;
        return this;
    }

    /**
     * Validate that the endpoint does not contain both an IP address and a domain name.
     *
     * @param endpoint the endpoint to validate
     * @throws IllegalArgumentException if both ipAddressV4 and domainName are present
     */
    public static void validateNoIpAndDomain(Endpoint endpoint) {
        if (endpoint == null) {
            return;
        }
        if (endpoint.getAddress() != null) {
            var dn = endpoint.getDomainName();
            if (dn != null && !dn.isEmpty()) {
                throw new IllegalArgumentException("Endpoint must not contain both ipAddressV4 and domainName");
            }
        }
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    ServiceEndpoint toProtobuf() {
        var builder = ServiceEndpoint.newBuilder();

        if (address != null) {
            builder.setIpAddressV4(ByteString.copyFrom(address));
        }

        builder.setDomainName(domainName);

        return builder.setPort(port).build();
    }

    @Override
    public String toString() {
        if (this.domainName != null && !this.domainName.isEmpty()) {
            return domainName + ":" + port;
        } else {
            return ((int) address[0] & 0x000000FF) + "." + ((int) address[1] & 0x000000FF) + "."
                    + ((int) address[2] & 0x000000FF)
                    + "." + ((int) address[3] & 0x000000FF) + ":"
                    + port;
        }
    }

    @Override
    public Endpoint clone() {
        try {
            Endpoint clone = (Endpoint) super.clone();
            clone.address = address != null ? address.clone() : null;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
