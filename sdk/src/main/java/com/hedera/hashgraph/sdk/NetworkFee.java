// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * The network fee component which covers the cost of gossip, consensus,
 * signature verifications, fee payment, and storage.
 */
public final class NetworkFee {
    /**
     * Multiplied by the node fee to determine the total network fee.
     */
    private final int multiplier;

    /**
     * The subtotal in tinycents for the network fee component which is calculated by
     * multiplying the node subtotal by the network multiplier.
     */
    private final long subtotal;

    /**
     * Constructor.
     *
     * @param multiplier the network fee multiplier
     * @param subtotal   the network fee subtotal in tinycents
     */
    NetworkFee(int multiplier, long subtotal) {
        this.multiplier = multiplier;
        this.subtotal = subtotal;
    }

    /**
     * Create a NetworkFee from a JSON object returned by the mirror node REST API.
     *
     * @param networkFee the JSON representation
     * @return the new NetworkFee
     */
    static NetworkFee fromJson(com.google.gson.JsonObject networkFee) {
        int multiplier = networkFee.get("multiplier").getAsInt();
        long subtotal = networkFee.get("subtotal").getAsLong();
        return new NetworkFee(multiplier, subtotal);
    }

    /**
     * Extract the network fee multiplier.
     *
     * @return the network fee multiplier
     */
    public int getMultiplier() {
        return multiplier;
    }

    /**
     * Extract the network fee subtotal in tinycents.
     *
     * @return the network fee subtotal in tinycents
     */
    public long getSubtotal() {
        return subtotal;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("multiplier", multiplier)
                .add("subtotal", subtotal)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NetworkFee that)) {
            return false;
        }
        return multiplier == that.multiplier && subtotal == that.subtotal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(multiplier, subtotal);
    }
}
