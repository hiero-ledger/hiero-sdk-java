// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

public record AddressBookResponse(List<NodeAddress> nodeAddresses) {
    public AddressBookResponse() {
        this(new ArrayList<>());
    }

    public record NodeAddress(
            @Nullable String publicKey,
            @Nullable String accountId,
            long nodeId,
            @Nullable String certHash,
            List<Endpoint> serviceEndpoints,
            @Nullable String description,
            long stake) {}

    public record Endpoint(@Nullable String address, int port, String domainName) {}

    public void addNodeAddress(NodeAddress nodeAddress) {
        Objects.requireNonNull(nodeAddress, "nodeAddress must not be null");
        nodeAddresses.add(nodeAddress);
    }
}
