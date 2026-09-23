// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import javax.annotation.Nullable;

/**
 * A failure raised by the mirror node REST adapter rather than by a transport.
 *
 * <p>Two identifiers are its own: {@link HttpTransportErrorKind#RETRIES_EXHAUSTED_ERROR} when a
 * retryable status survived every attempt, and {@link HttpTransportErrorKind#DEADLINE_EXCEEDED_ERROR}
 * when the call's total deadline elapsed. The difference matters to a caller deciding whether to raise
 * {@code requestTimeout} or go and look at the node.
 *
 * <p>Carries the last response where there was one, so the message can name what the mirror node
 * actually said.
 */
final class MirrorNodeHttpException extends HttpTransportException {
    private static final long serialVersionUID = 1L;

    @Nullable
    private final transient HttpResponse lastResponse;

    MirrorNodeHttpException(HttpTransportErrorKind kind, String message, @Nullable HttpResponse lastResponse) {
        super(kind, message);
        this.lastResponse = lastResponse;
    }

    /**
     * Extract the last response received, if any.
     *
     * @return the last response, or null if no response was ever received
     */
    @Nullable
    HttpResponse getLastResponse() {
        return lastResponse;
    }
}
