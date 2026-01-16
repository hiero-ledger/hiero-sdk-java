// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The fee estimate for a fee component (node or service).
 * <p>
 * Includes the base fee and any extras associated with it.
 */
public final class FeeEstimate {
    /**
     * The base fee price, in tinycents.
     */
    private final long base;

    /**
     * The extra fees that apply for this fee component.
     */
    private final List<FeeExtra> extras;

    /**
     * Constructor.
     *
     * @param base   the base fee price in tinycents
     * @param extras the list of extra fees
     */
    FeeEstimate(long base, List<FeeExtra> extras) {
        this.base = base;
        this.extras = Collections.unmodifiableList(new ArrayList<>(extras));
    }

    /**
     * Create a FeeEstimate from a JSON object returned by the mirror node REST API.
     *
     * @param feeEstimate the JSON representation
     * @return the new FeeEstimate
     */
    static FeeEstimate fromJson(JsonObject feeEstimate) {
        long base = getLong(feeEstimate, "base", "base_fee");

        List<FeeExtra> extras = new ArrayList<>();
        if (feeEstimate.has("extras") && feeEstimate.get("extras").isJsonArray()) {
            JsonArray extrasArray = feeEstimate.getAsJsonArray("extras");
            for (JsonElement element : extrasArray) {
                if (element.isJsonObject()) {
                    extras.add(FeeExtra.fromJson(element.getAsJsonObject()));
                }
            }
        }

        return new FeeEstimate(base, extras);
    }

    /**
     * Extract the base fee price in tinycents.
     *
     * @return the base fee price in tinycents
     */
    public long getBase() {
        return base;
    }

    /**
     * Extract the list of extra fees.
     *
     * @return an unmodifiable list of extra fees
     */
    public List<FeeExtra> getExtras() {
        return extras;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("base", base)
                .add("extras", extras)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeeEstimate that)) {
            return false;
        }
        return base == that.base && Objects.equals(extras, that.extras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, extras);
    }

    private static long getLong(JsonObject object, String primaryKey, String alternateKey) {
        if (object.has(primaryKey)) {
            return object.get(primaryKey).getAsLong();
        }
        if (object.has(alternateKey)) {
            return object.get(alternateKey).getAsLong();
        }
        throw new IllegalArgumentException("Missing expected fee estimate field: " + primaryKey);
    }
}
