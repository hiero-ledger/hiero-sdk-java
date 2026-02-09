// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AddressBookResponse {
    List<NodeAddress> nodeAddresses = new ArrayList<>();

    @Data
    @AllArgsConstructor
    public static class NodeAddress {
        /**
         * The RSA public key of the node.
         */
        @Nullable
        String publicKey;
        /**
         * The account to be paid for queries and transactions sent to this node.
         */
        @Nullable
        String accountId;
        /**
         * A non-sequential identifier for the node.
         */
        long nodeId;
        /**
         * A hash of the X509 cert used for gRPC traffic to this node.
         */
        @Nullable
        String certHash;
        /**
         * A node's service IP addresses and ports.
         */
        List<Endpoint> serviceEndpoints;
        /**
         * A description of the node, with UTF-8 encoding up to 100 bytes.
         */
        @Nullable
        String description;
        /**
         * The amount of tinybars staked to the node.
         */
        long stake;
    }

    @Data
    @AllArgsConstructor
    public static class Endpoint {
        @Nullable
        String address;

        int port;

        String domainName;
    }

    public void addNodeAddress(NodeAddress nodeAddress) {
        nodeAddresses.add(nodeAddress);
    }
}
