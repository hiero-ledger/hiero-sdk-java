// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.exception;

import com.hedera.hashgraph.sdk.Status;

/**
 * A runtime exception used by the TCK to signal a specific Hedera {@link Status}
 * for JSON-RPC error mapping, without requiring SDK-internal exception types.
 */
public class HederaStatusException extends RuntimeException {
    public final Status status;

    public HederaStatusException(Status status, String message) {
        super(message);
        this.status = status;
    }
}


