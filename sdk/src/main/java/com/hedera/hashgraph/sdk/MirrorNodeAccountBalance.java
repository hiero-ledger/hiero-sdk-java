// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * The HBAR balance of an account as reported by the mirror node REST API.
 *
 * <p>Returned by {@link MirrorNodeAccountBalanceQuery}. Token balances are not included.
 */
public final class MirrorNodeAccountBalance {
    /**
     * The HBAR balance of the account.
     */
    public final Hbar hbars;

    /**
     * Constructor.
     *
     * @param hbars the HBAR balance of the account
     */
    MirrorNodeAccountBalance(Hbar hbars) {
        this.hbars = Objects.requireNonNull(hbars);
    }

    /**
     * Create a balance from a mirror node REST JSON payload.
     *
     * <p>The mirror node returns an empty {@code balances} array — rather than a 404 — for an
     * account that does not exist. In that case the balance is reported as zero.
     *
     * @param root the JSON object returned by {@code GET /api/v1/balances}
     * @return the new balance
     */
    static MirrorNodeAccountBalance fromJson(JsonObject root) {
        if (!root.has("balances") || root.get("balances").isJsonNull()) {
            return new MirrorNodeAccountBalance(Hbar.ZERO);
        }

        JsonArray balances = root.getAsJsonArray("balances");
        if (balances.isEmpty()) {
            return new MirrorNodeAccountBalance(Hbar.ZERO);
        }

        JsonObject balance = balances.get(0).getAsJsonObject();
        if (!balance.has("balance") || balance.get("balance").isJsonNull()) {
            return new MirrorNodeAccountBalance(Hbar.ZERO);
        }

        return new MirrorNodeAccountBalance(
                Hbar.fromTinybars(balance.get("balance").getAsLong()));
    }

    /**
     * Extract the HBAR balance.
     *
     * @return the HBAR balance of the account
     */
    public Hbar getHbars() {
        return hbars;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("hbars", hbars).toString();
    }
}
