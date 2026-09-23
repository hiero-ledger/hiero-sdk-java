// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * How the SDK-supplied {@link DefaultHttpTransport} is built.
 *
 * <p>It is ignored when an application injects its own transport through
 * {@link MirrorNodeHttpConfig#withTransport(HttpTransport)} — an injected transport brings its own
 * connect timeout, redirect bound and body cap.
 *
 * <p>Instances are derived from {@link #defaults()} with the {@code withX} methods. There is no public
 * constructor, so a partially populated instance is unreachable.
 */
public final class HttpTransportConfiguration {
    /**
     * 32 MiB. Caps what a mirror node can make the SDK allocate, since the body is buffered.
     */
    private static final long DEFAULT_MAX_RESPONSE_BYTES = 33554432L;

    private static final HttpTransportConfiguration DEFAULTS =
            new HttpTransportConfiguration(Duration.ZERO, 5, DEFAULT_MAX_RESPONSE_BYTES, Collections.emptyMap());

    private final Duration connectTimeout;
    private final int maxRedirects;
    private final long maxResponseBytes;
    private final Map<String, String> defaultHeaders;

    private HttpTransportConfiguration(
            Duration connectTimeout, int maxRedirects, long maxResponseBytes, Map<String, String> defaultHeaders) {
        this.connectTimeout = connectTimeout;
        this.maxRedirects = maxRedirects;
        this.maxResponseBytes = maxResponseBytes;
        this.defaultHeaders = defaultHeaders;
    }

    /**
     * A configuration with every field at its declared default.
     *
     * @return the default configuration
     */
    public static HttpTransportConfiguration defaults() {
        return DEFAULTS;
    }

    /**
     * Derive a copy with a different connect timeout.
     *
     * @param connectTimeout the bound on TCP connect and the TLS handshake together; {@link Duration#ZERO}
     *                       keeps the platform stack's own bound rather than meaning "no bound"
     * @return the derived configuration
     */
    public HttpTransportConfiguration withConnectTimeout(Duration connectTimeout) {
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        if (connectTimeout.isNegative()) {
            throw new IllegalArgumentException("connectTimeout must not be negative");
        }
        return new HttpTransportConfiguration(connectTimeout, maxRedirects, maxResponseBytes, defaultHeaders);
    }

    /**
     * Derive a copy with a different redirect bound.
     *
     * @param maxRedirects the number of hops to follow; {@code 0} follows none
     * @return the derived configuration
     */
    public HttpTransportConfiguration withMaxRedirects(int maxRedirects) {
        if (maxRedirects < 0) {
            throw new IllegalArgumentException("maxRedirects must not be negative");
        }
        return new HttpTransportConfiguration(connectTimeout, maxRedirects, maxResponseBytes, defaultHeaders);
    }

    /**
     * Derive a copy with a different response body cap.
     *
     * @param maxResponseBytes the cap, in bytes; must be at least 1
     * @return the derived configuration
     */
    public HttpTransportConfiguration withMaxResponseBytes(long maxResponseBytes) {
        if (maxResponseBytes < 1) {
            throw new IllegalArgumentException("maxResponseBytes must be at least 1");
        }
        return new HttpTransportConfiguration(connectTimeout, maxRedirects, maxResponseBytes, defaultHeaders);
    }

    /**
     * Derive a copy whose default headers are replaced wholesale.
     *
     * <p>These sit at the bottom of the header precedence order: they belong to the SDK-built transport
     * alone, and are overridden by the caller headers on {@link MirrorNodeHttpConfig}, by any header the
     * endpoint sets, and by the SDK identity header.
     *
     * @param defaultHeaders the headers to send on every request; names are lowercased
     * @return the derived configuration
     */
    public HttpTransportConfiguration withDefaultHeaders(Map<String, String> defaultHeaders) {
        Objects.requireNonNull(defaultHeaders, "defaultHeaders must not be null");

        var copy = new LinkedHashMap<String, String>(defaultHeaders.size());
        for (var entry : defaultHeaders.entrySet()) {
            copy.put(
                    Objects.requireNonNull(entry.getKey()).toLowerCase(Locale.ROOT),
                    Objects.requireNonNull(entry.getValue()));
        }

        return new HttpTransportConfiguration(
                connectTimeout, maxRedirects, maxResponseBytes, Collections.unmodifiableMap(copy));
    }

    /**
     * Extract the connect timeout.
     *
     * @return the bound on connect and handshake; {@link Duration#ZERO} means the platform default
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Extract the redirect bound.
     *
     * @return the number of hops to follow
     */
    public int getMaxRedirects() {
        return maxRedirects;
    }

    /**
     * Extract the response body cap.
     *
     * @return the cap, in bytes
     */
    public long getMaxResponseBytes() {
        return maxResponseBytes;
    }

    /**
     * Extract the transport's default headers.
     *
     * @return an unmodifiable map, never null
     */
    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    @Override
    public String toString() {
        return "HttpTransportConfiguration{connectTimeout=" + connectTimeout + ", maxRedirects=" + maxRedirects
                + ", maxResponseBytes=" + maxResponseBytes + "}";
    }
}
