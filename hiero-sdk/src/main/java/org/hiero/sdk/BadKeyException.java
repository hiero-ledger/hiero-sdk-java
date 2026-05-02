// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

/**
 * Signals that a key could not be realized from the given input.
 * <p>
 * This exception can be raised by any of the {@code from} methods
 * on {@link PrivateKey} or {@link PublicKey}.
 */
public final class BadKeyException extends IllegalArgumentException {
    /**
     * @param message                   the message
     */
    BadKeyException(String message) {
        super(message);
    }

    /**
     * @param cause                     the cause
     */
    BadKeyException(Throwable cause) {
        super(cause);
    }
}
