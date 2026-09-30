// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * The HTTP method of an {@link HttpRequest}.
 *
 * <p>Only {@link #GET} and {@link #POST} are reachable from mirror node REST today. The enumeration is
 * complete so that it never has to be reopened; a {@code String} field would admit {@code "get"},
 * {@code "Get"} and {@code "PROPFIND"} alike.
 */
public enum HttpMethod {
    /**
     * The HTTP {@code GET} method.
     */
    GET,
    /**
     * The HTTP {@code HEAD} method.
     */
    HEAD,
    /**
     * The HTTP {@code POST} method.
     */
    POST,
    /**
     * The HTTP {@code PUT} method.
     */
    PUT,
    /**
     * The HTTP {@code PATCH} method.
     */
    PATCH,
    /**
     * The HTTP {@code DELETE} method.
     */
    DELETE,
    /**
     * The HTTP {@code OPTIONS} method.
     */
    OPTIONS,
    /**
     * The HTTP {@code TRACE} method.
     */
    TRACE,
    /**
     * The HTTP {@code CONNECT} method.
     */
    CONNECT
}
