// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

/**
 * Signals that a query will cost more than a pre-configured maximum payment amount.
 */
public final class MaxQueryPaymentExceededException extends RuntimeException {
    /**
     * The cost of the query that was attempted as returned by {@link Query#getCost(Client)}.
     */
    public final Hbar queryCost;

    /**
     * The limit for a single automatic query payment, set by
     * {@link Client#setDefaultMaxQueryPayment(Hbar)} (Hbar)} or {@link Query#setMaxQueryPayment(Hbar)}.
     */
    public final Hbar maxQueryPayment;

    /**
     * Constructor.
     *
     * @param builder                   the query builder object
     * @param cost                      the query cost
     * @param maxQueryPayment           the maximum query payment
     */
    MaxQueryPaymentExceededException(Query<?, ?> builder, Hbar cost, Hbar maxQueryPayment) {
        super(String.format(
                "cost for %s, of %s, without explicit payment is greater than " + "the maximum allowed payment of %s",
                builder.getClass().getSimpleName(), cost, maxQueryPayment));

        this.queryCost = cost;
        this.maxQueryPayment = maxQueryPayment;
    }
}
