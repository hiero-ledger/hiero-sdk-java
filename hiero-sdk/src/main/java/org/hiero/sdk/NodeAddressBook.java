// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A list of nodes and their metadata.
 *
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/basic-types/nodeaddressbook">Hedera Documentation</a>
 */
public class NodeAddressBook {
    List<NodeAddress> nodeAddresses = Collections.emptyList();

    /**
     * Constructor.
     */
    NodeAddressBook() {}

    /**
     * Extract the of node addresses.
     *
     * @return                          list of node addresses
     */
    public List<NodeAddress> getNodeAddresses() {
        return cloneNodeAddresses(nodeAddresses);
    }

    /**
     * Assign the list of node addresses.
     *
     * @param nodeAddresses             list of node addresses
     * @return {@code this}
     */
    public NodeAddressBook setNodeAddresses(List<NodeAddress> nodeAddresses) {
        this.nodeAddresses = cloneNodeAddresses(nodeAddresses);
        return this;
    }

    static List<NodeAddress> cloneNodeAddresses(List<NodeAddress> addresses) {
        List<NodeAddress> cloneAddresses = new ArrayList<>(addresses.size());
        for (var address : addresses) {
            cloneAddresses.add(address.clone());
        }
        return cloneAddresses;
    }

    /**
     * Create a node address book from a protobuf.
     *
     * @param book                      the protobuf
     * @return                          the new node address book
     */
    static NodeAddressBook fromProtobuf(org.hiero.sdk.proto.NodeAddressBook book) {
        var addresses = new ArrayList<NodeAddress>(book.getNodeAddressCount());

        for (var address : book.getNodeAddressList()) {
            addresses.add(NodeAddress.fromProtobuf(address));
        }

        return new NodeAddressBook().setNodeAddresses(addresses);
    }

    /**
     * Create a node address book from a byte string.
     *
     * @param bytes                     the byte string
     * @return                          the new node address book
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static NodeAddressBook fromBytes(ByteString bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(org.hiero.sdk.proto.NodeAddressBook.parseFrom(bytes));
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    org.hiero.sdk.proto.NodeAddressBook toProtobuf() {
        var builder = org.hiero.sdk.proto.NodeAddressBook.newBuilder();

        for (var nodeAdress : nodeAddresses) {
            builder.addNodeAddress(nodeAdress.toProtobuf());
        }

        return builder.build();
    }

    /**
     * Create the byte string.
     *
     * @return                          the byte string representation
     */
    public ByteString toBytes() {
        return toProtobuf().toByteString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("nodeAddresses", nodeAddresses)
                .toString();
    }
}
