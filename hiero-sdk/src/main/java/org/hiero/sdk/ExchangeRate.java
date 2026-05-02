// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import java.time.Instant;

/**
 * Denotes a conversion between Hbars and cents (USD).
 */
public final class ExchangeRate {
    /**
     * Denotes Hbar equivalent to cents (USD)
     */
    public final int hbars;

    /**
     * Denotes cents (USD) equivalent to Hbar
     */
    public final int cents;

    /**
     * Expiration time of this exchange rate
     */
    public final Instant expirationTime;

    /**
     * Calculated exchange rate
     */
    public final double exchangeRateInCents;

    ExchangeRate(int hbars, int cents, Instant expirationTime) {
        this.hbars = hbars;
        this.cents = cents;
        this.expirationTime = expirationTime;
        this.exchangeRateInCents = (double) cents / (double) hbars;
    }

    /**
     * Create an Exchange Rate from a protobuf.
     *
     * @param pb                        the protobuf
     * @return                          the new exchange rate
     */
    static ExchangeRate fromProtobuf(org.hiero.sdk.proto.ExchangeRate pb) {
        return new ExchangeRate(
                pb.getHbarEquiv(), pb.getCentEquiv(), InstantConverter.fromProtobuf(pb.getExpirationTime()));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hbars", hbars)
                .add("cents", cents)
                .add("expirationTime", expirationTime)
                .add("exchangeRateInCents", exchangeRateInCents)
                .toString();
    }
}
