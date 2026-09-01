// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Represent the addressBook query response.
 *
 * @param nodeAddresses the list of node address containing network node information
 */
public record AddressBookResponse(List<NodeAddress> nodeAddresses) {
    public AddressBookResponse() {
        this(new ArrayList<>());
    }

    /**
     * Represent the node address.
     *
     * @param publicKey the RSA public key of the node
     * @param accountId the account to be paid for queries and transactions sent to this node
     * @param nodeId a non-sequential identifier for the node
     * @param certHash a hash of the X509 cert used for gRPC traffic to this node
     * @param serviceEndpoints a node's service IP addresses and ports
     * @param description a description of the node, with UTF-8 encoding up to 100 bytes
     * @param stake the amount of tinybars staked to the node
     */
    public record NodeAddress(
            @Nullable String publicKey,
            @Nullable String accountId,
            long nodeId,
            @Nullable String certHash,
            List<Endpoint> serviceEndpoints,
            @Nullable String description,
            long stake) {}

    /**
     * Represent endpoint for the node address.
     *
     * @param address the IPv4 address in hex format
     * @param port the port number for the service endpoint
     * @param domainName the domain name for the endpoint
     */
    public record Endpoint(@Nullable String address, int port, String domainName) {}

    public void addNodeAddress(NodeAddress nodeAddress) {
        Objects.requireNonNull(nodeAddress, "nodeAddress must not be null");
        nodeAddresses.add(nodeAddress);
    }
}
