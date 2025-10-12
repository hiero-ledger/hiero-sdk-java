// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.AccountAmount;
import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.CryptoTransferTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import com.hedera.hashgraph.sdk.proto.TransferList;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A transaction that transfers hbars and tokens between Hedera accounts. You can enter multiple transfers in a single
 * transaction. The net value of hbars between the sending accounts and receiving accounts must equal zero.
 * <p>
 * See <a href="https://docs.hedera.com/guides/docs/sdks/cryptocurrency/transfer-cryptocurrency">Hedera
 * Documentation</a>
 */
public class TransferTransaction extends AbstractTokenTransferTransaction<TransferTransaction> {
    private final ArrayList<HbarTransfer> hbarTransfers = new ArrayList<>();

    private static class HbarTransfer {
        final AccountId accountId;
        Hbar amount;
        boolean isApproved;
        HookCall hookCall;
        HookType hookType;

        HbarTransfer(AccountId accountId, Hbar amount, boolean isApproved) {
            this.accountId = accountId;
            this.amount = amount;
            this.isApproved = isApproved;
            this.hookCall = null;
            this.hookType = null;
        }

        HbarTransfer(AccountId accountId, Hbar amount, boolean isApproved, HookCall hookCall, HookType hookType) {
            this.accountId = accountId;
            this.amount = amount;
            this.isApproved = isApproved;
            this.hookCall = hookCall;
            this.hookType = hookType;
        }

        AccountAmount toProtobuf() {
            var builder = AccountAmount.newBuilder()
                    .setAccountID(accountId.toProtobuf())
                    .setAmount(amount.toTinybars())
                    .setIsApproval(isApproved);

            // Add hook call if present
            if (hookCall != null && hookType != null) {
                switch (hookType) {
                    case PRE_TX_ALLOWANCE_HOOK:
                        builder.setPreTxAllowanceHook(hookCall.toProtobuf());
                        break;
                    case PRE_POST_TX_ALLOWANCE_HOOK:
                        builder.setPrePostTxAllowanceHook(hookCall.toProtobuf());
                        break;
                }
            }

            return builder.build();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("accountId", accountId)
                    .add("amount", amount)
                    .add("isApproved", isApproved)
                    .add("hookCall", hookCall)
                    .add("hookType", hookType)
                    .toString();
        }
    }

    /**
     * Constructor.
     */
    public TransferTransaction() {
        defaultMaxTransactionFee = new Hbar(1);
    }

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    TransferTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TransferTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the of hbar transfers.
     *
     * @return list of hbar transfers
     */
    public Map<AccountId, Hbar> getHbarTransfers() {
        Map<AccountId, Hbar> transfers = new HashMap<>();

        for (var transfer : hbarTransfers) {
            transfers.put(transfer.accountId, transfer.amount);
        }

        return transfers;
    }

    private TransferTransaction doAddHbarTransfer(AccountId accountId, Hbar value, boolean isApproved) {
        requireNotFrozen();

        for (var transfer : hbarTransfers) {
            if (transfer.accountId.equals(accountId) && transfer.isApproved == isApproved) {
                transfer.amount = Hbar.fromTinybars(transfer.amount.toTinybars() + value.toTinybars());
                return this;
            }
        }

        hbarTransfers.add(new HbarTransfer(accountId, value, isApproved));
        return this;
    }

    /**
     * Add a non approved hbar transfer to an EVM address.
     *
     * @param evmAddress the EVM address
     * @param value      the value
     * @return the updated transaction
     */
    public TransferTransaction addHbarTransfer(EvmAddress evmAddress, Hbar value) {
        AccountId accountId = AccountId.fromEvmAddress(evmAddress, 0, 0);
        return doAddHbarTransfer(accountId, value, false);
    }

    /**
     * Add a non approved hbar transfer.
     *
     * @param accountId the account id
     * @param value     the value
     * @return the updated transaction
     */
    public TransferTransaction addHbarTransfer(AccountId accountId, Hbar value) {
        return doAddHbarTransfer(accountId, value, false);
    }

    /**
     * Add an approved hbar transfer.
     *
     * @param accountId the account id
     * @param value     the value
     * @return the updated transaction
     */
    public TransferTransaction addApprovedHbarTransfer(AccountId accountId, Hbar value) {
        return doAddHbarTransfer(accountId, value, true);
    }

    /**
     * Add an HBAR transfer with a hook.
     *
     * @param accountId the account id
     * @param amount    the amount to transfer
     * @param hookCall  the hook call to execute
     * @param hookType  the type of hook (PRE_TX_ALLOWANCE_HOOK or PRE_POST_TX_ALLOWANCE_HOOK)
     * @return the updated transaction
     * @throws IllegalArgumentException if hookType is null or if hookCall is null
     */
    public TransferTransaction addHbarTransferWithHook(
            AccountId accountId, Hbar amount, HookCall hookCall, HookType hookType) {
        requireNotFrozen();
        Objects.requireNonNull(accountId, "accountId cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(hookCall, "hookCall cannot be null");
        Objects.requireNonNull(hookType, "hookType cannot be null");

        // Check if there's already a transfer for this account without a hook
        for (var transfer : hbarTransfers) {
            if (transfer.accountId.equals(accountId) && transfer.hookCall == null) {
                // Update existing transfer to include hook
                transfer.hookCall = hookCall;
                transfer.hookType = hookType;
                return this;
            }
        }

        // Add new transfer with hook
        hbarTransfers.add(new HbarTransfer(accountId, amount, false, hookCall, hookType));
        return this;
    }

    /**
     * @param accountId  the account id
     * @param isApproved whether the transfer is approved
     * @return {@code this}
     * @deprecated - Use {@link #addApprovedHbarTransfer(AccountId, Hbar)} instead
     */
    @Deprecated
    public TransferTransaction setHbarTransferApproval(AccountId accountId, boolean isApproved) {
        requireNotFrozen();

        for (var transfer : hbarTransfers) {
            if (transfer.accountId.equals(accountId)) {
                transfer.isApproved = isApproved;
                return this;
            }
        }

        return this;
    }

    /**
     * Build the transaction body.
     *
     * @return {@link com.hedera.hashgraph.sdk.proto.CryptoTransferTransactionBody}
     */
    CryptoTransferTransactionBody.Builder build() {
        var transfers = sortTransfersAndBuild();

        var builder = CryptoTransferTransactionBody.newBuilder();

        this.hbarTransfers.sort(
                Comparator.comparing((HbarTransfer a) -> a.accountId).thenComparing(a -> a.isApproved));
        var hbarTransfersList = TransferList.newBuilder();
        for (var transfer : hbarTransfers) {
            hbarTransfersList.addAccountAmounts(transfer.toProtobuf());
        }
        builder.setTransfers(hbarTransfersList);

        for (var transfer : transfers) {
            builder.addTokenTransfers(transfer.toProtobuf());
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        super.validateChecksums(client);
        for (var transfer : hbarTransfers) {
            transfer.accountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return CryptoServiceGrpc.getCryptoTransferMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setCryptoTransfer(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setCryptoTransfer(build());
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getCryptoTransfer();

        for (var transfer : body.getTransfers().getAccountAmountsList()) {
            HookCall hookCall = null;
            HookType hookType = null;

            // Check for hook calls
            if (transfer.hasPreTxAllowanceHook()) {
                hookCall = HookCall.fromProtobuf(transfer.getPreTxAllowanceHook());
                hookType = HookType.PRE_TX_ALLOWANCE_HOOK;
            } else if (transfer.hasPrePostTxAllowanceHook()) {
                hookCall = HookCall.fromProtobuf(transfer.getPrePostTxAllowanceHook());
                hookType = HookType.PRE_POST_TX_ALLOWANCE_HOOK;
            }

            if (hookCall != null && hookType != null) {
                hbarTransfers.add(new HbarTransfer(
                        AccountId.fromProtobuf(transfer.getAccountID()),
                        Hbar.fromTinybars(transfer.getAmount()),
                        transfer.getIsApproval(),
                        hookCall,
                        hookType));
            } else {
                hbarTransfers.add(new HbarTransfer(
                        AccountId.fromProtobuf(transfer.getAccountID()),
                        Hbar.fromTinybars(transfer.getAmount()),
                        transfer.getIsApproval()));
            }
        }

        for (var tokenTransferList : body.getTokenTransfersList()) {
            var token = TokenId.fromProtobuf(tokenTransferList.getToken());

            for (var transfer : tokenTransferList.getTransfersList()) {
                tokenTransfers.add(new TokenTransfer(
                        token,
                        AccountId.fromProtobuf(transfer.getAccountID()),
                        transfer.getAmount(),
                        tokenTransferList.hasExpectedDecimals()
                                ? tokenTransferList.getExpectedDecimals().getValue()
                                : null,
                        transfer.getIsApproval()));
            }

            for (var transfer : tokenTransferList.getNftTransfersList()) {
                nftTransfers.add(new TokenNftTransfer(
                        token,
                        AccountId.fromProtobuf(transfer.getSenderAccountID()),
                        AccountId.fromProtobuf(transfer.getReceiverAccountID()),
                        transfer.getSerialNumber(),
                        transfer.getIsApproval()));
            }
        }
    }
}
