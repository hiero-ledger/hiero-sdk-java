// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * The value the SDK sends as {@code x-user-agent}, on the gRPC leg and the mirror node REST leg alike.
 *
 * <p>Shared so that the two provably agree. {@code x-user-agent} rather than {@code User-Agent} because
 * a browser cannot set the latter — every Hiero SDK's gRPC path already sends the custom spelling, and
 * sending both would invent a difference between runtimes that no SDK has today.
 */
final class SdkUserAgent {
    /**
     * The header name, already lowercased.
     */
    static final String HEADER_NAME = "x-user-agent";

    private SdkUserAgent() {}

    /**
     * Extract the user agent. This information is used to gather usage metrics. If the version is not
     * available, the user agent is {@code hiero-sdk-java/DEV}.
     *
     * @return the user agent value
     */
    static String value() {
        var thePackage = SdkUserAgent.class.getPackage();
        var implementationVersion = thePackage != null ? thePackage.getImplementationVersion() : null;
        return "hiero-sdk-java/" + ((implementationVersion != null) ? (implementationVersion) : "DEV");
    }
}
