// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.Objects;

/**
 * A path below a mirror node's REST base URL.
 *
 * <p>The type is the mechanism: no mirror node REST call site can name a foreign host, because the only
 * way to reach {@link MirrorNodeHttpClient} is through this type and this type cannot express one.
 * Nothing downstream has to check.
 *
 * <p>Resolution against the base URL is plain concatenation. Every URL-reference resolver in the JDK —
 * {@code URI.resolve} included — lets an absolute or protocol-relative reference replace the host
 * outright, which is exactly the failure this type exists to prevent.
 */
final class MirrorNodeRestPath {
    private static final String API_PREFIX = "/api/v1";

    private final String value;

    private MirrorNodeRestPath(String value) {
        this.value = value;
    }

    /**
     * Build a path.
     *
     * @param path a path beginning with {@code /}, with no host, no protocol-relative prefix and no
     *             {@code ..} segment
     * @return the new path
     * @throws IllegalArgumentException if the path could name somewhere other than the mirror node
     */
    static MirrorNodeRestPath of(String path) {
        Objects.requireNonNull(path, "path must not be null");

        if (path.isEmpty() || path.charAt(0) != '/') {
            throw new IllegalArgumentException("a mirror node REST path must begin with '/': " + path);
        }

        // "//host/..." is protocol-relative and never what a caller meant.
        if (path.startsWith("//")) {
            throw new IllegalArgumentException("a mirror node REST path must not be protocol-relative: " + path);
        }

        for (var character : path.toCharArray()) {
            if (Character.isWhitespace(character)) {
                throw new IllegalArgumentException("a mirror node REST path must not contain whitespace: " + path);
            }
        }

        var pathOnly = path;
        var queryStart = pathOnly.indexOf('?');
        if (queryStart >= 0) {
            pathOnly = pathOnly.substring(0, queryStart);
        }

        for (var segment : pathOnly.split("/", -1)) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("a mirror node REST path must not contain a '..' segment: " + path);
            }
        }

        return new MirrorNodeRestPath(path);
    }

    /**
     * Convert a mirror node {@code links.next} cursor into a path.
     *
     * <p>The mirror node includes the {@code /api/v1} prefix that the base URL already carries, so it is
     * stripped exactly once.
     *
     * @param link the {@code links.next} value
     * @return the new path
     * @throws IllegalArgumentException if the cursor names a host
     */
    static MirrorNodeRestPath fromNextLink(String link) {
        Objects.requireNonNull(link, "link must not be null");

        if (link.contains("://")) {
            throw new IllegalArgumentException("a mirror node next link must not name a host: " + link);
        }

        var stripped = link;
        if (stripped.equals(API_PREFIX)) {
            stripped = "/";
        } else if (stripped.startsWith(API_PREFIX + "/")) {
            stripped = stripped.substring(API_PREFIX.length());
        }

        return of(stripped);
    }

    /**
     * Extract the path.
     *
     * @return the path, beginning with {@code /}
     */
    String getValue() {
        return value;
    }

    /**
     * Resolve against a mirror node REST base URL by concatenation.
     *
     * @param baseUrl the base URL, with or without a trailing slash
     * @return the absolute URL
     */
    String resolveAgainst(String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");

        var trimmed = baseUrl;
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed + value;
    }

    @Override
    public String toString() {
        return value;
    }
}
