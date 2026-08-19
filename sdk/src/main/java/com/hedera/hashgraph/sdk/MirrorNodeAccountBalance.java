// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The HBAR balance of an account as reported by the mirror node REST API.
 *
 * <p>Returned by {@link MirrorNodeAccountBalanceQuery}. Token balances are not included — see
 * {@link MirrorNodeTokenBalanceQuery} and {@link MirrorNodeTokenBalance} for those.
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
     * <p>The mirror node reports an account it does not know with an empty {@code balances} array — not
     * a 404. That case is signalled by returning {@code null}; the caller decides how to report it. An
     * account that exists but holds no HBAR is a populated entry with {@code "balance": 0} and is parsed
     * normally, so a genuine zero is never mistaken for a missing account.
     *
     * @param root the JSON object returned by {@code GET /api/v1/balances}
     * @return the new balance, or {@code null} if the mirror node knows no such account
     * @throws IllegalStateException if the payload is not a well-formed balances response
     */
    @Nullable
    static MirrorNodeAccountBalance fromJson(JsonObject root) {
        if (!root.has("balances") || root.get("balances").isJsonNull()) {
            throw new IllegalStateException("Mirror Node returned a malformed response: no `balances` array");
        }

        JsonArray balances = root.getAsJsonArray("balances");
        if (balances.isEmpty()) {
            return null;
        }

        JsonObject balance = balances.get(0).getAsJsonObject();
        if (!balance.has("balance") || balance.get("balance").isJsonNull()) {
            throw new IllegalStateException(
                    "Mirror Node returned a malformed response: balances entry has no `balance` field");
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
