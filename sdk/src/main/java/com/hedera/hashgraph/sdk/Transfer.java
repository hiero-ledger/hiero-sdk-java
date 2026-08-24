// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.proto.AccountAmount;

/**
 * A transfer of Hbar that occurred within a transaction.
 * <p>
 * Returned with a {@link TransactionRecord}.
 */
public final class Transfer {
    /**
     * The Account ID that sends or receives crypto-currency.
     */
    public final AccountId accountId;

    /**
     * The amount that the account sends (negative) or receives (positive).
     */
    public final Hbar amount;

    /**
     * If true then the transfer is expected to be an approved allowance and the
     * accountId is expected to be the owner. The default is false.
     */
    public final boolean isApproved;

    Transfer(AccountId accountId, Hbar amount) {
        this(accountId, amount, false);
    }

    Transfer(AccountId accountId, Hbar amount, boolean isApproved) {
        this.accountId = accountId;
        this.amount = amount;
        this.isApproved = isApproved;
    }

    /**
     * Create a transfer from a protobuf.
     *
     * @param accountAmount             the protobuf
     * @return                          the new transfer
     */
    static Transfer fromProtobuf(AccountAmount accountAmount) {
        return new Transfer(
                AccountId.fromProtobuf(accountAmount.getAccountID()),
                Hbar.fromTinybars(accountAmount.getAmount()),
                accountAmount.getIsApproval());
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    AccountAmount toProtobuf() {
        return AccountAmount.newBuilder()
                .setAccountID(accountId.toProtobuf())
                .setAmount(amount.toTinybars())
                .setIsApproval(isApproved)
                .build();
    }

    /**
     * Build a Gson representation of this transfer, mirroring the JS SDK's {@code Transfer.toJSON()}.
     *
     * @return the JSON object
     */
    JsonObject toJsonObject() {
        var json = new JsonObject();
        json.addProperty("accountId", accountId.toString());
        json.addProperty("amount", String.valueOf(amount.toTinybars()));
        json.addProperty("isApproved", isApproved);
        return json;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountId", accountId)
                .add("amount", amount)
                .toString();
    }
}
