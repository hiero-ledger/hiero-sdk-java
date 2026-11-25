// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.clpr;

import java.util.List;

/**
 * Response for {@code getClprEndpointManifest}. Byte fields are hex without {@code 0x};
 * the state proof is {@code ""} when no signed snapshot is available.
 *
 * @param version            the manifest version, as an unsigned decimal string
 * @param serviceAddress     the CLPR service address
 * @param endpoints          the endpoints
 * @param manifestStateProof the manifest state proof
 */
public record ClprEndpointManifestResponse(
        String version, String serviceAddress, List<Endpoint> endpoints, String manifestStateProof) {
    /**
     * An endpoint in the manifest.
     *
     * @param serviceEndpoint the network address
     * @param tlsCertificate  the DER TLS certificate
     * @param accountId       the raw account identifier
     */
    public record Endpoint(ServiceEndpoint serviceEndpoint, String tlsCertificate, String accountId) {}

    /**
     * An IP address and port.
     *
     * @param ipAddress the IP address
     * @param port      the port
     */
    public record ServiceEndpoint(String ipAddress, int port) {}
}
