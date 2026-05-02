// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import javax.annotation.Nullable;

/**
 * Signals that a transaction has failed the pre-check.
 * <p>
 * Before a node submits a transaction to the rest of the network,
 * it attempts some cheap assertions. This process is called the "pre-check".
 */
public class PrecheckStatusException extends Exception {
    /**
     * The status of the failing transaction
     */
    public final Status status;

    /**
     * The ID of the transaction that failed.
     * <p>
     * This can be `null` if a query fails pre-check without an
     * associated payment transaction.
     */
    @Nullable
    public final TransactionId transactionId;

    /**
     * Constructor.
     *
     * @param status                    the status
     * @param transactionId             the transaction id
     */
    PrecheckStatusException(Status status, @Nullable TransactionId transactionId) {
        this.status = status;
        this.transactionId = transactionId;
    }

    @Override
    public String getMessage() {
        var stringBuilder = new StringBuilder();

        if (transactionId != null) {
            stringBuilder.append("Hedera transaction `").append(transactionId).append("` ");
        }

        stringBuilder
                .append("failed pre-check with the status `")
                .append(status)
                .append("`");

        return stringBuilder.toString();
    }
}
