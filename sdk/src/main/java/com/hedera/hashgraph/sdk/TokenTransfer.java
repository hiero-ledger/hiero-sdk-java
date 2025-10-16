// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static com.hedera.hashgraph.sdk.TransferTransaction.toFungibleHook;

import com.google.common.base.MoreObjects;
import com.hedera.hashgraph.sdk.proto.AccountAmount;
import com.hedera.hashgraph.sdk.proto.TokenTransferList;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A token transfer record.
 * <p>
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
     * @param tokenId    the token id
     * @param accountId  the account id
     * @param amount     the amount
     * @param isApproved is it approved
     */
    TokenTransfer(TokenId tokenId, AccountId accountId, long amount, boolean isApproved) {
        this(tokenId, accountId, amount, null, isApproved);
    }

    /**
     * Constructor.
     *
     * @param tokenId          the token id
     * @param accountId        the account id
     * @param amount           the amount
     * @param expectedDecimals the expected decimals
     * @param isApproved       is it approved
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
            @Nullable FungibleHookCall hookCall) {
        this.tokenId = tokenId;
        this.accountId = accountId;
        this.amount = amount;
        this.expectedDecimals = expectedDecimals;
        this.isApproved = isApproved;
        this.hookCall = hookCall;
    }

    static List<TokenTransfer> fromProtobuf(TokenTransferList tokenTransferList) {
        var token = TokenId.fromProtobuf(tokenTransferList.getToken());
        var tokenTransfers = new ArrayList<TokenTransfer>();

        for (var transfer : tokenTransferList.getTransfersList()) {
            FungibleHookCall typedHook = null;
            if (transfer.hasPreTxAllowanceHook()) {
                typedHook = toFungibleHook(transfer.getPreTxAllowanceHook(), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
            } else if (transfer.hasPrePostTxAllowanceHook()) {
                typedHook = toFungibleHook(
                        transfer.getPrePostTxAllowanceHook(), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);
            }

            var acctId = AccountId.fromProtobuf(transfer.getAccountID());
            Integer expectedDecimals = tokenTransferList.hasExpectedDecimals()
                    ? tokenTransferList.getExpectedDecimals().getValue()
                    : null;

            tokenTransfers.add(new TokenTransfer(
                    token, acctId, transfer.getAmount(), expectedDecimals, transfer.getIsApproval(), typedHook));
        }
        return tokenTransfers;
    }

    /**
     * Create the protobuf.
     *
     * @return an account amount protobuf
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
