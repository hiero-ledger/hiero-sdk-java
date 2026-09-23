// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Everything a {@link Client} holds for mirror node REST access, in one value.
 *
 * <p>One type rather than a setter per knob, because {@code Client} is already the most overloaded
 * class in the SDK. Four rules make the collapse safe:
 *
 * <ol>
 *   <li><b>{@link #getTransport()} carries provenance.</b> Null means the SDK builds the transport and
 *       owns it; non-null means the application supplied it and owns it, and
 *       {@link Client#close()} will never close it.
 *   <li><b>{@link Client#getMirrorNodeHttpConfig()} returns the configuration as supplied, never as
 *       resolved.</b> Inspecting a {@code Client} never constructs a transport, and
 *       {@code setMirrorNodeHttpConfig(getMirrorNodeHttpConfig())} is a true no-op rather than a
 *       statement of ownership.
 *   <li><b>Setting replaces; it does not merge.</b> The getter never returns null, so
 *       {@code set(get().withX(...))} is the idiom.
 *   <li><b>{@link #getTransportConfiguration()} is ignored when a transport is injected.</b> It
 *       configures the transport the SDK would have built.
 * </ol>
 */
public final class MirrorNodeHttpConfig {
    /**
     * Reserved because the SDK owns its own identity header. Matched case-insensitively.
     */
    private static final Set<String> RESERVED_HEADERS = Set.of("user-agent", "x-user-agent");

    private static final MirrorNodeHttpConfig DEFAULTS = new MirrorNodeHttpConfig(
            null, HttpTransportConfiguration.defaults(), MirrorNodeHttpRetryPolicy.defaults(), Collections.emptyMap());

    @Nullable
    private final HttpTransport transport;

    private final HttpTransportConfiguration transportConfiguration;
    private final MirrorNodeHttpRetryPolicy retryPolicy;
    private final Map<String, String> requestHeaders;

    private MirrorNodeHttpConfig(
            @Nullable HttpTransport transport,
            HttpTransportConfiguration transportConfiguration,
            MirrorNodeHttpRetryPolicy retryPolicy,
            Map<String, String> requestHeaders) {
        this.transport = transport;
        this.transportConfiguration = transportConfiguration;
        this.retryPolicy = retryPolicy;
        this.requestHeaders = requestHeaders;
    }

    /**
     * A configuration with every field at its declared default and no injected transport.
     *
     * @return the default configuration
     */
    public static MirrorNodeHttpConfig defaults() {
        return DEFAULTS;
    }

    /**
     * Derive a copy that uses an application-supplied transport.
     *
     * <p>This is the single extension point for Android, corporate proxies, mTLS, tracing wrappers and
     * test fakes. It must be installed before the first mirror node REST call, because the transport is
     * built once per {@link Client}. An injected transport is never closed by the SDK.
     *
     * @param transport the transport to use, or null to let the SDK build and own one
     * @return the derived configuration
     */
    public MirrorNodeHttpConfig withTransport(@Nullable HttpTransport transport) {
        return new MirrorNodeHttpConfig(transport, transportConfiguration, retryPolicy, requestHeaders);
    }

    /**
     * Derive a copy that builds the SDK's own transport differently.
     *
     * <p>Ignored when a transport is injected.
     *
     * @param transportConfiguration how to build the SDK-supplied transport
     * @return the derived configuration
     */
    public MirrorNodeHttpConfig withTransportConfiguration(HttpTransportConfiguration transportConfiguration) {
        Objects.requireNonNull(transportConfiguration, "transportConfiguration must not be null");
        return new MirrorNodeHttpConfig(transport, transportConfiguration, retryPolicy, requestHeaders);
    }

    /**
     * Derive a copy with a different retry policy.
     *
     * @param retryPolicy the budget for every mirror node REST call made through this client
     * @return the derived configuration
     */
    public MirrorNodeHttpConfig withRetryPolicy(MirrorNodeHttpRetryPolicy retryPolicy) {
        Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");
        return new MirrorNodeHttpConfig(transport, transportConfiguration, retryPolicy, requestHeaders);
    }

    /**
     * Derive a copy carrying one more caller header.
     *
     * @param name the header name; {@code user-agent} and {@code x-user-agent} are reserved and rejected
     *             case-insensitively, because the SDK owns that header
     * @param value the header value
     * @return the derived configuration
     */
    public MirrorNodeHttpConfig withRequestHeader(String name, String value) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");

        var merged = new LinkedHashMap<>(requestHeaders);
        merged.put(requireNotReserved(name), value);
        return new MirrorNodeHttpConfig(
                transport, transportConfiguration, retryPolicy, Collections.unmodifiableMap(merged));
    }

    /**
     * Derive a copy whose caller headers are replaced wholesale.
     *
     * @param requestHeaders the headers to send on every mirror node REST request; names are lowercased,
     *                       and {@code user-agent} and {@code x-user-agent} are rejected
     * @return the derived configuration
     */
    public MirrorNodeHttpConfig withRequestHeaders(Map<String, String> requestHeaders) {
        Objects.requireNonNull(requestHeaders, "requestHeaders must not be null");

        var copy = new LinkedHashMap<String, String>(requestHeaders.size());
        for (var entry : requestHeaders.entrySet()) {
            copy.put(
                    requireNotReserved(Objects.requireNonNull(entry.getKey())),
                    Objects.requireNonNull(entry.getValue()));
        }

        return new MirrorNodeHttpConfig(
                transport, transportConfiguration, retryPolicy, Collections.unmodifiableMap(copy));
    }

    /**
     * Extract the application-supplied transport.
     *
     * @return the injected transport, or null when the SDK builds and owns one
     */
    @Nullable
    public HttpTransport getTransport() {
        return transport;
    }

    /**
     * Extract how the SDK-supplied transport is built.
     *
     * @return the transport configuration; ignored when a transport is injected
     */
    public HttpTransportConfiguration getTransportConfiguration() {
        return transportConfiguration;
    }

    /**
     * Extract the retry policy.
     *
     * @return the policy
     */
    public MirrorNodeHttpRetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    /**
     * Extract the caller headers.
     *
     * @return an unmodifiable map with lowercased names, never null
     */
    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    private static String requireNotReserved(String name) {
        var lowercased = name.toLowerCase(Locale.ROOT);
        if (RESERVED_HEADERS.contains(lowercased)) {
            throw new IllegalArgumentException(
                    "the " + lowercased + " header is owned by the SDK and cannot be set by a caller");
        }
        return lowercased;
    }

    @Override
    public String toString() {
        return "MirrorNodeHttpConfig{transport=" + (transport == null ? "sdk-owned" : "injected")
                + ", transportConfiguration=" + transportConfiguration + ", retryPolicy=" + retryPolicy + "}";
    }
}
