// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A failure to obtain an HTTP response, carrying one of the cross-SDK
 * {@link HttpTransportErrorKind} identifiers.
 *
 * <p>A non-2xx status is <b>not</b> one of these — it is a successful exchange, returned as an
 * {@link HttpResponse} carrying its status code. Only a failure to obtain a response at all is
 * signalled this way.
 */
public class HttpTransportException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final HttpTransportErrorKind kind;

    /**
     * Constructor.
     *
     * @param kind the identifier this failure carries
     * @param message the detail message
     * @param cause the underlying failure, may be null
     */
    public HttpTransportException(HttpTransportErrorKind kind, String message, @Nullable Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
    }

    /**
     * Constructor.
     *
     * @param kind the identifier this failure carries
     * @param message the detail message
     */
    public HttpTransportException(HttpTransportErrorKind kind, String message) {
        this(kind, message, null);
    }

    /**
     * Extract the identifier this failure carries.
     *
     * @return the identifier
     */
    public HttpTransportErrorKind getKind() {
        return kind;
    }

    /**
     * Whether repeating the exchange can help.
     *
     * @return true if the failure is retryable
     */
    public boolean isRetryable() {
        return kind.isRetryable();
    }
}
