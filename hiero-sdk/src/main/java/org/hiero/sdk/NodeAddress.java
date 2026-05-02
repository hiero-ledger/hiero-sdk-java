// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.ServiceEndpoint;

/**
 * The metadata for a Node – including IP Address, and the crypto account associated with the Node.
 *
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/basic-types/nodeaddress">Hedera Documentation</a>
 */
public class NodeAddress implements Cloneable {
    /**
     * The RSA public key of the node.
     */
    @Nullable
    String publicKey;
    /**
     * The account to be paid for queries and transactions sent to this node.
     */
    @Nullable
    AccountId accountId;
    /**
     * A non-sequential identifier for the node.
     */
    long nodeId;
    /**
     * A hash of the X509 cert used for gRPC traffic to this node.
     */
    @Nullable
    ByteString certHash;
    /**
     * A node's service IP addresses and ports.
     */
    List<Endpoint> addresses = Collections.emptyList();
    /**
     * A description of the node, with UTF-8 encoding up to 100 bytes.
     */
    @Nullable
    String description = null;
    /**
     * The amount of tinybars staked to the node.
     */
    long stake;

    /**
     * Constructor.
     */
    NodeAddress() {}

    /**
     * Create a node from a protobuf.
     *
     * @param nodeAddress               the protobuf
     * @return                          the new node
     */
    static NodeAddress fromProtobuf(org.hiero.sdk.proto.NodeAddress nodeAddress) {
        var address = new ArrayList<Endpoint>(nodeAddress.getServiceEndpointCount());

        if (!nodeAddress.getIpAddress().isEmpty()) {
            address.add(Endpoint.fromProtobuf(ServiceEndpoint.newBuilder()
                    .setIpAddressV4(nodeAddress.getIpAddress())
                    .setPort(nodeAddress.getPortno())
                    .build()));
        }

        for (var endpoint : nodeAddress.getServiceEndpointList()) {
            address.add(Endpoint.fromProtobuf(endpoint));
        }

        var node = new NodeAddress()
                .setPublicKey(nodeAddress.getRSAPubKey())
                .setNodeId(nodeAddress.getNodeId())
                .setCertHash(nodeAddress.getNodeCertHash())
                .setAddresses(address)
                .setDescription(nodeAddress.getDescription())
                .setStake(nodeAddress.getStake());

        if (nodeAddress.hasNodeAccountId()) {
            node.setAccountId(AccountId.fromProtobuf(nodeAddress.getNodeAccountId()));
        }

        return node;
    }

    /**
     * Extract the public key.
     *
     * @return                          the public key
     */
    @Nullable
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Assign the public key.
     *
     * @param publicKey                the public key
     * @return {@code this}
     */
    public NodeAddress setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    /**
     * Extract the account id.
     *
     * @return                          the account id
     */
    @Nullable
    public AccountId getAccountId() {
        return accountId;
    }

    /**
     * Assign the account id.
     *
     * @param accountId                 the account id
     * @return {@code this}
     */
    public NodeAddress setAccountId(AccountId accountId) {
        this.accountId = accountId;
        return this;
    }

    /**
     * Extract the node id.
     *
     * @return                         the node id
     */
    public long getNodeId() {
        return nodeId;
    }

    /**
     * Assign the node id.
     *
     * @param nodeId                    the node id
     * @return {@code this}
     */
    public NodeAddress setNodeId(long nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    /**
     * Extract the certificate hash.
     *
     * @return                          the certificate hash
     */
    @Nullable
    public ByteString getCertHash() {
        return certHash;
    }

    /**
     * Assign the certificate hash.
     *
     * @param certHash                  the certificate hash
     * @return {@code this}
     */
    @Nullable
    public NodeAddress setCertHash(ByteString certHash) {
        this.certHash = certHash;
        return this;
    }

    /**
     * Extract the list of addresses.
     *
     * @return                          the list of addresses
     */
    public List<Endpoint> getAddresses() {
        return cloneEndpoints(addresses);
    }

    /**
     * Assign the list of addresses.
     *
     * @param addresses                 the list of addresses
     * @return {@code this}
     */
    public NodeAddress setAddresses(List<Endpoint> addresses) {
        this.addresses = cloneEndpoints(addresses);
        return this;
    }

    static List<Endpoint> cloneEndpoints(List<Endpoint> endpoints) {
        List<Endpoint> cloneEndpoints = new ArrayList<>(endpoints.size());
        for (var endpoint : endpoints) {
            cloneEndpoints.add(endpoint.clone());
        }
        return cloneEndpoints;
    }

    /**
     * Extract the description.
     *
     * @return                          the description
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Assign the description.
     *
     * @param description               the description
     * @return {@code this}
     */
    public NodeAddress setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Extract the tiny stake.
     *
     * @return                          the tiny stake
     */
    public long getStake() {
        return stake;
    }

    /**
     * Assign the tiny bar stake.
     *
     * @param stake                     the tiny bar stake
     * @return {@code this}
     */
    public NodeAddress setStake(long stake) {
        this.stake = stake;
        return this;
    }

    /**
     * Convert the node address object into a protobuf.
     *
     * @return                          the protobuf representation.
     */
    org.hiero.sdk.proto.NodeAddress toProtobuf() {
        var builder = org.hiero.sdk.proto.NodeAddress.newBuilder().setNodeId(nodeId);

        if (certHash != null) {
            builder.setNodeCertHash(certHash);
        }

        if (publicKey != null) {
            builder.setRSAPubKey(publicKey);
        }

        if (accountId != null) {
            builder.setNodeAccountId(accountId.toProtobuf());
        }

        if (description != null) {
            builder.setDescription(description);
        }

        for (var address : addresses) {
            builder.addServiceEndpoint(address.toProtobuf());
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("publicKey", publicKey)
                .add("accountId", accountId)
                .add("nodeId", nodeId)
                .add("certHash", certHash != null ? new String(certHash.toByteArray(), StandardCharsets.UTF_8) : null)
                .add("addresses", addresses)
                .add("description", description)
                .add("stake", stake)
                .toString();
    }

    @Override
    public NodeAddress clone() {
        try {
            NodeAddress clone = (NodeAddress) super.clone();
            clone.addresses = cloneEndpoints(addresses);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
