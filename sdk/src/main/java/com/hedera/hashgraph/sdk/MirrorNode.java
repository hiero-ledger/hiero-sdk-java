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
     * @return scheme://host[:port]/api/v1
     */
    String getRestBaseUrl() {
        String host = address.getAddress();
        int port = address.getPort();

        if (host == null) {
            throw new IllegalStateException("mirror node address is not set");
        }

        if (isLocalHost(host)) {
            // If the user configured the standard gRPC port (5600), map it to the standard REST port (5551).
            // Otherwise, honor the explicitly configured port (e.g., 8084 for Fee Estimates).
            int effectivePort = (port == 5600) ? 5551 : port;
            return "http://" + host + ":" + effectivePort + "/api/v1";
        }
        String scheme = chooseScheme(port);

        StringBuilder base = new StringBuilder();
        base.append(scheme).append("://").append(host);
        // Omit default ports
        if (!isDefaultPort(scheme, port)) {
            base.append(":").append(port);
        }
        base.append("/api/v1");
        return base.toString();
    }

    private static boolean isLocalHost(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host);
    }

    private static String chooseScheme(int port) {
        return port == 80 ? "http" : "https";
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
    }
}
