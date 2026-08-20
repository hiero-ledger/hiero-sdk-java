// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The balance an account holds in a single token, as reported by the mirror node REST API.
 *
 * <p>Returned by {@link MirrorNodeTokenBalanceQuery}.
 */
public final class MirrorNodeTokenBalance {
    /**
     * The token this balance is for.
     */
    public final TokenId tokenId;

    /**
     * The balance the account holds in this token.
     *
     * <p>For a {@code FUNGIBLE_COMMON} token this is the amount in the token's smallest denomination.
     * For a {@code NON_FUNGIBLE_UNIQUE} token this is the number of NFTs the account holds.
     *
     * <p>Zero when the account holds no relationship to the token — check {@link #isAssociated()} to
     * tell that case apart from an associated account whose balance happens to be zero.
     */
    public final long balance;

    /**
     * The number of decimal places the token is divided into, or {@code null} if the mirror node does
     * not report it — which includes the case where the account is not associated with the token.
     */
    @Nullable
    public final Integer decimals;

    private final boolean associated;

    /**
     * Constructor.
     *
     * @param tokenId the token this balance is for
     * @param balance the balance held, in the token's smallest denomination or as an NFT count
     * @param decimals the token's decimal places, may be null
     * @param associated whether the account holds a relationship to the token at all
     */
    MirrorNodeTokenBalance(TokenId tokenId, long balance, @Nullable Integer decimals, boolean associated) {
        this.tokenId = Objects.requireNonNull(tokenId);
        this.balance = balance;
        this.decimals = decimals;
        this.associated = associated;
    }

    /**
     * Create a token balance from a mirror node REST JSON payload.
     *
     * <p>The mirror node answers with an empty {@code tokens} array when the entity exists but holds no
     * relationship to the requested token. That is reported as {@code isAssociated() == false} with a
     * zero balance, rather than as an error. A non-existent entity is a 404 and never reaches here.
     *
     * @param root the JSON object returned by {@code GET /api/v1/accounts/{id}/tokens}
     * @param requestedTokenId the token that was queried, used when the response carries no relationship
     * @return the new token balance
     */
    static MirrorNodeTokenBalance fromJson(JsonObject root, TokenId requestedTokenId) {
        if (!root.has("tokens") || root.get("tokens").isJsonNull()) {
            return notAssociated(requestedTokenId);
        }

        JsonArray tokens = root.getAsJsonArray("tokens");
        if (tokens.isEmpty()) {
            return notAssociated(requestedTokenId);
        }

        JsonObject relationship = tokens.get(0).getAsJsonObject();

        TokenId tokenId = requestedTokenId;
        if (relationship.has("token_id") && !relationship.get("token_id").isJsonNull()) {
            tokenId = TokenId.fromString(relationship.get("token_id").getAsString());
        }

        long balance = 0;
        if (relationship.has("balance") && !relationship.get("balance").isJsonNull()) {
            balance = relationship.get("balance").getAsLong();
        }

        Integer decimals = null;
        if (relationship.has("decimals") && !relationship.get("decimals").isJsonNull()) {
            decimals = relationship.get("decimals").getAsInt();
        }

        return new MirrorNodeTokenBalance(tokenId, balance, decimals, true);
    }

    /**
     * A balance for a token the account holds no relationship to — including the case where the mirror
     * node does not know the account at all.
     *
     * @param tokenId the token that was queried
     * @return a zero, non-associated balance
     */
    static MirrorNodeTokenBalance notAssociated(TokenId tokenId) {
        return new MirrorNodeTokenBalance(tokenId, 0L, null, false);
    }

    /**
     * Whether the account holds a relationship to this token at all.
     *
     * <p>Use this to distinguish "never associated with the token" from "associated, balance zero" —
     * both report a {@link #balance} of zero.
     *
     * @return true if the account is associated with the token
     */
    public boolean isAssociated() {
        return associated;
    }

    /**
     * Extract the token id.
     *
     * @return the token this balance is for
     */
    public TokenId getTokenId() {
        return tokenId;
    }

    /**
     * Extract the balance.
     *
     * @return the balance held, in the token's smallest denomination or as an NFT count
     */
    public long getBalance() {
        return balance;
    }

    /**
     * Extract the token's decimal places.
     *
     * @return the number of decimal places, or null if not reported
     */
    @Nullable
    public Integer getDecimals() {
        return decimals;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tokenId", tokenId)
                .add("balance", balance)
                .add("decimals", decimals)
                .add("associated", associated)
                .toString();
    }
}
