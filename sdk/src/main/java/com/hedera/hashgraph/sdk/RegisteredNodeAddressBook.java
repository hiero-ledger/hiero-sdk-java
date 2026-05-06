// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(registeredNodes, "registeredNodes must not be null");
        this.registeredNodes = registeredNodes;
    }
}
