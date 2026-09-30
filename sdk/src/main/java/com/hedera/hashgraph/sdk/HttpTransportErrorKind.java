// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

/**
 * The stable identifiers an {@link HttpTransport} fails with.
 *
 * <p>These are the same seven identifiers in every Hiero SDK, so a test asserting on one asserts the
 * same thing in every language. The second property of each is whether repeating the exchange can
 * help: without it a mistyped host name would cost the whole attempt budget before an obvious error
 * surfaced, and certificate validation would be re-run purely to burn attempts.
 *
 * <p>A transport failure that cannot be classified as one of these is treated as non-retryable. That is
 * the safe direction: a wrongly terminal error surfaces a visible failure after one attempt, while a
 * wrongly retryable one costs the whole budget and hides the cause.
 */
public enum HttpTransportErrorKind {
    /**
     * The connection was refused, reset, or the peer was unreachable. Retryable.
     */
    CONNECTION_ERROR("connection-error", true),
    /**
     * The per-attempt deadline elapsed. Retryable.
     */
    TIMEOUT_ERROR("timeout-error", true),
    /**
     * The host name does not resolve.
     *
     * <p>Every name resolution failure maps here, including a transient {@code SERVFAIL}:
     * {@link java.net.UnknownHostException} carries no code and no fields, so one SDK cannot tell a
     * permanent failure from a temporary one. Not retryable.
     */
    UNKNOWN_HOST_ERROR("unknown-host-error", false),
    /**
     * Certificate verification or the TLS handshake failed. Not retryable.
     */
    TLS_ERROR("tls-error", false),
    /**
     * The transport was closed. Not retryable.
     */
    CLIENT_CLOSED_ERROR("client-closed-error", false),
    /**
     * The caller cancelled the call. Not retryable.
     */
    CANCELLED_ERROR("cancelled-error", false),
    /**
     * The response body exceeded {@link HttpTransportConfiguration#getMaxResponseBytes()}. Not
     * retryable.
     */
    RESPONSE_TOO_LARGE_ERROR("response-too-large-error", false),
    /**
     * A retryable status survived every attempt.
     *
     * <p>Raised by the mirror node REST adapter rather than by a transport, and distinct from
     * {@link #DEADLINE_EXCEEDED_ERROR}: a node failing repeatedly is not the clock running out.
     */
    RETRIES_EXHAUSTED_ERROR("retries-exhausted-error", false),
    /**
     * The call's total deadline elapsed, or an honoured {@code Retry-After} exceeded the time left in
     * it.
     *
     * <p>Raised by the mirror node REST adapter rather than by a transport.
     */
    DEADLINE_EXCEEDED_ERROR("deadline-exceeded-error", false);

    private final String id;
    private final boolean retryable;

    HttpTransportErrorKind(String id, boolean retryable) {
        this.id = id;
        this.retryable = retryable;
    }

    /**
     * Extract the cross-SDK identifier, such as {@code "connection-error"}.
     *
     * @return the identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Whether repeating the exchange can help.
     *
     * @return true if the failure is retryable
     */
    public boolean isRetryable() {
        return retryable;
    }

    @Override
    public String toString() {
        return id;
    }
}
