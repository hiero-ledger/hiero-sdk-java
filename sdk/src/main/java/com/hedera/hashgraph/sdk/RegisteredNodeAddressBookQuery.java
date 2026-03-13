// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.concurrent.CompletableFuture;

public class RegisteredNodeAddressBookQuery {
    // TODO: Implementation should be deferred until the mirror node API is available
    private String nodeType;

    public RegisteredNodeAddressBookQuery setNodeType(String nodeType) {
        this.nodeType = nodeType;
        return this;
    }

    public String getNodeType() {
        return this.nodeType;
    }

    public RegisteredNodeAddressBook execute(Client client) {
        throw new RuntimeException("Method not implemented");
    }

    private CompletableFuture<String> executeMirrorNodeRequest(Client client) {
        throw new RuntimeException("Method not implemented");
    }

    private RegisteredNodeAddressBook parseRegisterNodeAddressBook(String responseBody) {
        throw new RuntimeException("Method not implemented");
    }
}
