// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods;

import com.hedera.hashgraph.sdk.Status;

/**
 * Custom exception for validation errors that should be mapped to specific Hedera status codes
 * in JSON-RPC responses.
 */
public final class ValidationException extends RuntimeException {
    private final Status status;

    /**
     * @param status                   the Hedera status code to return in the JSON-RPC error response
     * @param message                  the error message
     */
    public ValidationException(Status status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * @param status                   the Hedera status code to return in the JSON-RPC error response
     * @param message                  the error message
     * @param cause                    the cause of this exception
     */
    public ValidationException(Status status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * @return                         the Hedera status code
     */
    public Status getStatus() {
        return status;
    }
}
