// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.hedera.hashgraph.sdk.proto.AccountAmount;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A token transfer record.
 *
 * Internal utility class.
 */
public class TokenTransfer {
    final TokenId tokenId;
    final AccountId accountId;

    @Nullable
    Integer expectedDecimals;

    long amount;

    boolean isApproved;

    FungibleHookCall hookCall;

    /**
     * Constructor.
     *
     * @param tokenId                   the token id
     * @param accountId                 the account id
     * @param amount                    the amount
     * @param isApproved                is it approved
     */
    TokenTransfer(TokenId tokenId, AccountId accountId, long amount, boolean isApproved) {
        this(tokenId, accountId, amount, null, isApproved);
    }

    /**
     * Constructor.
     *
     * @param tokenId                   the token id
     * @param accountId                 the account id
     * @param amount                    the amount
     * @param expectedDecimals          the expected decimals
     * @param isApproved                is it approved
     */
    TokenTransfer(
            TokenId tokenId, AccountId accountId, long amount, @Nullable Integer expectedDecimals, boolean isApproved) {
        this.tokenId = tokenId;
        this.accountId = accountId;
        this.amount = amount;
        this.expectedDecimals = expectedDecimals;
        this.isApproved = isApproved;
        this.hookCall = null;
    }

    TokenTransfer(
            TokenId tokenId,
            AccountId accountId,
            long amount,
            @Nullable Integer expectedDecimals,
            boolean isApproved,
            FungibleHookCall hookCall) {
        this.tokenId = tokenId;
        this.accountId = accountId;
        this.amount = amount;
        this.expectedDecimals = expectedDecimals;
        this.isApproved = isApproved;
        this.hookCall = hookCall;
    }

    /**
     * Create a list of token transfer records from a protobuf.
     *
     * @param tokenTransferLists        the protobuf
     * @return                          the list of token transfer records
     */
    static List<TokenTransfer> fromProtobuf(List<com.hedera.hashgraph.sdk.proto.TokenTransferList> tokenTransferLists) {
        var transfers = new ArrayList<TokenTransfer>();

        for (var tokenTransferList : tokenTransferLists) {
            var tokenId = TokenId.fromProtobuf(tokenTransferList.getToken());

            for (var transfer : tokenTransferList.getTransfersList()) {
                transfers.add(new TokenTransfer(
                        tokenId,
                        AccountId.fromProtobuf(transfer.getAccountID()),
                        transfer.getAmount(),
                        tokenTransferList.hasExpectedDecimals()
                                ? tokenTransferList.getExpectedDecimals().getValue()
                                : null,
                        transfer.getIsApproval()));
            }
        }

        return transfers;
    }

    /**
     * Create the protobuf.
     *
     * @return                          an account amount protobuf
     */
    AccountAmount toProtobuf() {
        var builder = AccountAmount.newBuilder()
                .setAccountID(accountId.toProtobuf())
                .setAmount(amount)
                .setIsApproval(isApproved);

        if (hookCall != null) {
            switch (hookCall.getType()) {
                case PRE_TX_ALLOWANCE_HOOK -> builder.setPreTxAllowanceHook(hookCall.toProtobuf());
                case PRE_POST_TX_ALLOWANCE_HOOK -> builder.setPrePostTxAllowanceHook(hookCall.toProtobuf());
                default -> {}
            }
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tokenId", tokenId)
                .add("accountId", accountId)
                .add("amount", amount)
                .add("expectedDecimals", expectedDecimals)
                .add("isApproved", isApproved)
                .toString();
    }
}
