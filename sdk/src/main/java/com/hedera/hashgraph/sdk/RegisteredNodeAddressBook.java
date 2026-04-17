// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.List;

/**
 * Collection of RegisteredNode objects.
 */
public class RegisteredNodeAddressBook {
    public final List<RegisteredNode> registeredNodes;

    /**
     * Constructor.
     *
     * @param registeredNodes list of RegisterNode
     */
    RegisteredNodeAddressBook(List<RegisteredNode> registeredNodes) {
        this.registeredNodes = registeredNodes;
    }

    /**
     * Build RegisterNodeAddressBook from protobuf.
     *
     * @param registeredNodes the list of RegisterNode protobuf representation.
     * @return {@code RegisterNodeAddressBook} new object of RegisterNodeAddressBook.
     */
    static RegisteredNodeAddressBook fromProtobuf(List<com.hedera.hashgraph.sdk.proto.RegisteredNode> registeredNodes) {
        return new RegisteredNodeAddressBook(
                registeredNodes.stream().map(RegisteredNode::fromProtobuf).toList());
    }
}
