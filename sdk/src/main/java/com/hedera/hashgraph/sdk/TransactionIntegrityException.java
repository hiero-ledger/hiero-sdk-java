// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

public class TransactionIntegrityException extends IllegalArgumentException {
    public TransactionIntegrityException(String message) {
        super(message);
    }
}
