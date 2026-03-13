// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

/**
 * Class representing single registered node in the network state.
 * Each registered node in the network state SHALL represent a single
 * non-consensus node that is registered on the network.
 * Registered node identifiers SHALL only be unique within a single
 * realm and shard combination.
 */
public class RegisteredNode {
    /**
     * A registered node identifier.
     */
    @Nonnegative
    public final long registeredNodeId;

    /**
     * An administrative key controlled by the node operator.
     */
    public final Key adminKey;

    /**
     * A short description of the node.
     */
    public final String description;

    /**
     * A list of service endpoints for client calls.
     */
    public final List<RegisteredServiceEndpoint> serviceEndpoints;

    /**
     * An account identifier.
     * This account identifies the entity financially responsible for this
     * registered node.
     */
    @Nullable
    public final AccountId nodeAccount;

    /**
     * Constructor.
     *
     * @param registeredNodeId the registered node identifier.
     * @param adminKey the admin key.
     * @param description the description of the node.
     * @param serviceEndpoint the list of service endpoints.
     * @param nodeAccount the account identifier.
     */
    RegisteredNode(
            long registeredNodeId,
            Key adminKey,
            String description,
            List<RegisteredServiceEndpoint> serviceEndpoint,
            @Nullable AccountId nodeAccount) {
        this.registeredNodeId = registeredNodeId;
        this.adminKey = adminKey;
        this.description = description;
        this.serviceEndpoints = Collections.unmodifiableList(serviceEndpoint);
        this.nodeAccount = nodeAccount;
    }

    /**
     * Extract the registeredNode from the protobuf.
     *
     * @param registeredNode the protobuf
     * @return {@code this} the contract object
     */
    static RegisteredNode fromProtobuf(com.hedera.hashgraph.sdk.proto.RegisteredNode registeredNode) {
        Objects.requireNonNull(registeredNode, "registeredNode cannot be null");
        var registerNodeId = registeredNode.getRegisteredNodeId();
        var adminKey = Key.fromProtobufKey(registeredNode.getAdminKey());
        var description = registeredNode.getDescription();

        var serviceEndpoint = registeredNode.getServiceEndpointList().stream()
                .map(s -> RegisteredServiceEndpoint.fromProtobuf(s))
                .toList();
        var nodeAccount =
                registeredNode.hasNodeAccount() ? AccountId.fromProtobuf(registeredNode.getNodeAccount()) : null;

        return new RegisteredNode(registerNodeId, adminKey, description, serviceEndpoint, nodeAccount);
    }

    /**
     * Extract the registeredNode from a byte array.
     *
     * @param bytes the byte array
     * @return {@code RegisteredNode} the extracted registeredNode
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static RegisteredNode fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(com.hedera.hashgraph.sdk.proto.RegisteredNode.parseFrom(bytes).toBuilder()
                .build());
    }

    /**
     * Build the protobuf.
     * @return {@code this} the protobuf representation
     */
    com.hedera.hashgraph.sdk.proto.RegisteredNode toProtobuf() {
        var registeredNode = com.hedera.hashgraph.sdk.proto.RegisteredNode.newBuilder()
                .setRegisteredNodeId(registeredNodeId)
                .setAdminKey(adminKey.toProtobufKey())
                .setDescription(description);

        if (nodeAccount != null) {
            registeredNode.setNodeAccount(nodeAccount.toProtobuf());
        }

        for (RegisteredServiceEndpoint serviceEndpoint : serviceEndpoints) {
            registeredNode.addServiceEndpoint(serviceEndpoint.toProtobuf());
        }

        return registeredNode.build();
    }

    /**
     * Create a byte array representation.
     * @return {@code byte[]} the byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("registeredNodeId", registeredNodeId)
                .add("adminKey", adminKey)
                .add("description", description)
                .add("nodeAccount", nodeAccount)
                .add("serviceEndpoints", serviceEndpoints)
                .toString();
    }
}
