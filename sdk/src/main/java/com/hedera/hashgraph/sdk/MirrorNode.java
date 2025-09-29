// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.concurrent.ExecutorService;

/**
 * An individual mirror node.
 */
class MirrorNode extends BaseNode<MirrorNode, BaseNodeAddress> {
    /**
     * Constructor.
     *
     * @param address                   the node address as a managed node address
     * @param executor                  the executor service
     */
    MirrorNode(BaseNodeAddress address, ExecutorService executor) {
        super(address, executor);
    }

    /**
     * Constructor.
     *
     * @param address                   the node address as a string
     * @param executor                  the executor service
     */
    MirrorNode(String address, ExecutorService executor) {
        this(BaseNodeAddress.fromString(address), executor);
    }

    @Override
    protected String getAuthority() {
        return null;
    }

    @Override
    BaseNodeAddress getKey() {
        return address;
    }

    /**
     * Build the REST base URL for this mirror node.
     *
     * @param isContractCall when true and local, use 8545; otherwise 5551
     * @return scheme://host[:port]/api/v1
     */
    String getRestBaseUrl(boolean isContractCall) {
        String host = address.getAddress();
        int port = address.getPort();

        if (host == null) {
            throw new IllegalStateException("mirror node address is not set");
        }

        boolean isLocalHost = "localhost".equals(host) || "127.0.0.1".equals(host);
        if (isLocalHost) {
            String localHost = "localhost";
            int localPort = isContractCall ? 8545 : 5551;
            return "http://" + localHost + ":" + localPort + "/api/v1";
        }

        String scheme;
        if (port == 80) {
            scheme = "http";
        } else if (port == 443) {
            scheme = "https";
        } else {
            scheme = "https";
        }

        StringBuilder base = new StringBuilder();
        base.append(scheme).append("://").append(host);
        // Omit default ports
        if (!((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443))) {
            base.append(":").append(port);
        }
        base.append("/api/v1");
        return base.toString();
    }
}
