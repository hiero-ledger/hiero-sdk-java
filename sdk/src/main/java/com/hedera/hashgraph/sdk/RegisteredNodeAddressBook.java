// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.List;

public class RegisteredNodeAddressBook {
    public final List<RegisteredNode> registeredNodes;

    RegisteredNodeAddressBook(List<RegisteredNode> registeredNodes) {
        this.registeredNodes = registeredNodes;
    }

    static RegisteredNodeAddressBook fromProtobuf(List<com.hedera.hashgraph.sdk.proto.RegisteredNode> registeredNodes) {
        return new RegisteredNodeAddressBook(registeredNodes.stream()
                .map(n -> RegisteredNode.fromProtobuf(n))
                .toList());
    }
}
