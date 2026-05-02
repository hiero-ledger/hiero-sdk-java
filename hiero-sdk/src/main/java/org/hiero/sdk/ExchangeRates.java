// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Contains a set of Exchange Rates (current and next).
 */
public final class ExchangeRates {
    /**
     * Current Exchange Rate
     */
    public final ExchangeRate currentRate;

    /**
     * Next Exchange Rate
     */
    public final ExchangeRate nextRate;

    private ExchangeRates(ExchangeRate currentRate, ExchangeRate nextRate) {
        this.currentRate = currentRate;
        this.nextRate = nextRate;
    }

    /**
     * Create an Exchange Rates from a protobuf.
     *
     * @param pb                        the protobuf
     * @return                          the new exchange rates
     */
    static ExchangeRates fromProtobuf(org.hiero.sdk.proto.ExchangeRateSet pb) {
        return new ExchangeRates(
                ExchangeRate.fromProtobuf(pb.getCurrentRate()), ExchangeRate.fromProtobuf(pb.getNextRate()));
    }

    /**
     * Create an Exchange Rates from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new exchange rates
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static ExchangeRates fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(
                org.hiero.sdk.proto.ExchangeRateSet.parseFrom(bytes).toBuilder().build());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("currentRate", currentRate.toString())
                .add("nextRate", nextRate.toString())
                .toString();
    }
}
