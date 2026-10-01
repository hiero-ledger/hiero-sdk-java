// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import org.jspecify.annotations.Nullable;

/**
 * Signals that a transaction has failed the pre-check.
 * <p>
 * Before a node submits a transaction to the rest of the network,
 * it attempts some cheap assertions. This process is called the "pre-check".
 */
@Deprecated
public final class HederaPreCheckStatusException extends PrecheckStatusException {
    HederaPreCheckStatusException(Status status, @Nullable TransactionId transactionId) {
        super(status, transactionId);
    }
}
