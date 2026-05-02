// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import org.hiero.sdk.proto.AccountAmount;

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

    Transfer(AccountId accountId, Hbar amount) {
        this.accountId = accountId;
        this.amount = amount;
    }

    /**
     * Create a transfer from a protobuf.
     *
     * @param accountAmount             the protobuf
     * @return                          the new transfer
     */
    static Transfer fromProtobuf(AccountAmount accountAmount) {
        return new Transfer(
                AccountId.fromProtobuf(accountAmount.getAccountID()), Hbar.fromTinybars(accountAmount.getAmount()));
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
                .build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountId", accountId)
                .add("amount", amount)
                .toString();
    }
}
