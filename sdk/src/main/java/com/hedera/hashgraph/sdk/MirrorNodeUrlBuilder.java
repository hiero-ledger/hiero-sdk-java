// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Utility class for constructing mirror node URLs while preserving scheme and port information.
 * This class centralizes URL construction logic to ensure consistent handling of custom ports
 * and schemes in mirror node configurations.
 */
class MirrorNodeUrlBuilder {

    /**
     * Builds a complete API URL for mirror node requests.
     *
     * @param client the client containing mirror network configuration
     * @param apiEndpoint the API endpoint path (e.g., "/accounts/0x123...")
     * @param isContractCall whether this is a contract call (affects port selection for local networks)
     * @return the complete URL for the mirror node API request
     * @throws IllegalArgumentException if no valid mirror URL is found
     */
    static String buildApiUrl(Client client, String apiEndpoint, boolean isContractCall) {
        List<String> mirrorNetwork = client.getMirrorNetwork();
        if (mirrorNetwork.isEmpty()) {
            throw new IllegalArgumentException("Mirror URL not found");
        }
        String mirrorUrl = mirrorNetwork.get(0);

        if (mirrorUrl.contains("://")) {
            try {
                MirrorUrlComponents components = parseFullUrl(mirrorUrl);

                return buildCompleteUrl(components, apiEndpoint, client.getLedgerId() == null, isContractCall);

            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid mirror URL format: " + mirrorUrl, e);
            }
        } else {
            return buildUrlFromHostPort(mirrorUrl, apiEndpoint, client.getLedgerId() == null, isContractCall);
        }
    }

    /**
     * Parses a full URL string into its components.
     */
    private static MirrorUrlComponents parseFullUrl(String mirrorUrl) throws URISyntaxException {
        URI uri = new URI(mirrorUrl);
        return new MirrorUrlComponents(
            uri.getScheme(),
            uri.getHost(),
            uri.getPort() != -1 ? uri.getPort() : getDefaultPort(uri.getScheme()),
            uri.getPath() != null ? uri.getPath() : ""
        );
    }

    /**
     * Builds URL from host:port format (existing behavior).
     */
    private static String buildUrlFromHostPort(String hostPort, String apiEndpoint, boolean isLocalNetwork, boolean isContractCall) {
        int colonIndex = hostPort.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("URL must contain host:port format");
        }

        String host = hostPort.substring(0, colonIndex);
        int port = Integer.parseInt(hostPort.substring(colonIndex + 1));

        String scheme = "https";
        if (isLocalNetwork) {
            scheme = "http";
            if (isContractCall) {
                port = 8545;
            } else {
                port = 5551;
            }
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(scheme).append("://").append(host);

        if (!isDefaultPort(scheme, port)) {
            urlBuilder.append(":").append(port);
        }

        urlBuilder.append("/api/v1").append(apiEndpoint);

        return urlBuilder.toString();
    }

    /**
     * Builds the complete URL for the API request.
     */
    private static String buildCompleteUrl(MirrorUrlComponents components, String apiEndpoint,
                                         boolean isLocalNetwork, boolean isContractCall) {
        String scheme = components.scheme;
        String host = components.host;
        int port = components.port;
        String basePath = components.basePath;

        if (isLocalNetwork) {
            scheme = "http";
            if (isContractCall) {
                port = 8545;
            } else {
                port = 5551;
            }
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(scheme).append("://").append(host);

        if (!isDefaultPort(scheme, port)) {
            urlBuilder.append(":").append(port);
        }

        if (!basePath.isEmpty() && !basePath.equals("/")) {
            urlBuilder.append(basePath);
        }
        urlBuilder.append("/api/v1").append(apiEndpoint);

        return urlBuilder.toString();
    }

    /**
     * Gets the default port for a given scheme.
     */
    private static int getDefaultPort(String scheme) {
        return switch (scheme.toLowerCase()) {
            case "http" -> 80;
            case "https" -> 443;
            default -> 443; // Default to HTTPS port
        };
    }

    /**
     * Checks if the given port is the default port for the scheme.
     */
    private static boolean isDefaultPort(String scheme, int port) {
        return port == getDefaultPort(scheme);
    }

    /**
     * Internal class to hold parsed URL components.
     */
    private static class MirrorUrlComponents {
        final String scheme;
        final String host;
        final int port;
        final String basePath;

        MirrorUrlComponents(String scheme, String host, int port, String basePath) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.basePath = basePath;
        }
    }
}
