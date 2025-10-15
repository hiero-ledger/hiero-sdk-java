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
        FungibleHookCall hookCall;

        HbarTransfer(AccountId accountId, Hbar amount, boolean isApproved) {
            this.accountId = accountId;
            this.amount = amount;
            this.isApproved = isApproved;
            this.hookCall = null;
        }

        HbarTransfer(AccountId accountId, Hbar amount, boolean isApproved, FungibleHookCall hookCall) {
            this.accountId = accountId;
            this.amount = amount;
            this.isApproved = isApproved;
            this.hookCall = hookCall;
        }

        AccountAmount toProtobuf() {
            var builder = AccountAmount.newBuilder()
                    .setAccountID(accountId.toProtobuf())
                    .setAmount(amount.toTinybars())
                    .setIsApproval(isApproved);

            // Add hook call if present
            if (hookCall != null) {
                switch (hookCall.getType()) {
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

    // --- Unified fungible token transfer helper (local to TransferTransaction) ---
    private TransferTransaction doAddTokenTransferUnified(
            TokenId tokenId,
            AccountId accountId,
            long amount,
            boolean isApproved,
            Integer expectedDecimals,
            FungibleHookCall hookCall) {
        requireNotFrozen();

        // Merge if same token/account/approval/decimals
        for (var transfer : tokenTransfers) {
            if (transfer.tokenId.equals(tokenId)
                    && transfer.accountId.equals(accountId)
                    && transfer.isApproved == isApproved) {
                if (transfer.expectedDecimals != null && !transfer.expectedDecimals.equals(expectedDecimals)) {
                    throw new IllegalArgumentException("expected decimals for a token cannot be changed once set");
                }
                transfer.expectedDecimals = expectedDecimals;
                transfer.amount += amount;
                transfer.hookCall = hookCall;
                return this;
            }
        }

        // Create new record
        var tt = new TokenTransfer(tokenId, accountId, amount, expectedDecimals, isApproved);
        tt.hookCall = hookCall;
        tokenTransfers.add(tt);
        return this;
    }

    // Public convenience: no hook, no decimals, not approved
    @Override
    public TransferTransaction addTokenTransfer(TokenId tokenId, AccountId accountId, long value) {
        Objects.requireNonNull(tokenId, "tokenId cannot be null");
        Objects.requireNonNull(accountId, "accountId cannot be null");
        return doAddTokenTransferUnified(tokenId, accountId, value, false, null, null);
    }

    // Public convenience: approved
    @Override
    public TransferTransaction addApprovedTokenTransfer(TokenId tokenId, AccountId accountId, long value) {
        Objects.requireNonNull(tokenId, "tokenId cannot be null");
        Objects.requireNonNull(accountId, "accountId cannot be null");
        return doAddTokenTransferUnified(tokenId, accountId, value, true, null, null);
    }

    // Public convenience: with decimals (unapproved)
    @Override
    public TransferTransaction addTokenTransferWithDecimals(
            TokenId tokenId, AccountId accountId, long value, int decimals) {
        Objects.requireNonNull(tokenId, "tokenId cannot be null");
        Objects.requireNonNull(accountId, "accountId cannot be null");
        return doAddTokenTransferUnified(tokenId, accountId, value, false, decimals, null);
    }

    // Public convenience: with decimals (approved)
    @Override
    public TransferTransaction addApprovedTokenTransferWithDecimals(
            TokenId tokenId, AccountId accountId, long value, int decimals) {
        Objects.requireNonNull(tokenId, "tokenId cannot be null");
        Objects.requireNonNull(accountId, "accountId cannot be null");
        return doAddTokenTransferUnified(tokenId, accountId, value, true, decimals, null);
    }

    // New: with typed fungible hook (unapproved)
    public TransferTransaction addTokenTransferWithHook(
            TokenId tokenId, AccountId accountId, long value, FungibleHookCall hookCall) {
        Objects.requireNonNull(tokenId, "tokenId cannot be null");
        Objects.requireNonNull(accountId, "accountId cannot be null");
        Objects.requireNonNull(hookCall, "hookCall cannot be null");
        return doAddTokenTransferUnified(tokenId, accountId, value, false, null, hookCall);
    }

    /**
     * Add an NFT transfer with optional sender/receiver allowance hooks.
     *
     * @param nftId the NFT id
     * @param senderAccountId the sender
     * @param receiverAccountId the receiver
     * @param senderHookCall optional sender hook call
     * @param receiverHookCall optional receiver hook call
     * @return the updated transaction
     */
    public TransferTransaction addNftTransferWithHook(
            NftId nftId,
            AccountId senderAccountId,
            AccountId receiverAccountId,
            NftHookCall senderHookCall,
            NftHookCall receiverHookCall) {
        Objects.requireNonNull(nftId, "nftId cannot be null");
        Objects.requireNonNull(senderAccountId, "senderAccountId cannot be null");
        Objects.requireNonNull(receiverAccountId, "receiverAccountId cannot be null");

        return doAddNftTransferWithHook(
                nftId, senderAccountId, receiverAccountId, false, senderHookCall, receiverHookCall);
    }

    /**
     * Add an HBAR transfer with a fungible hook.
     *
     * @param accountId the account id
     * @param amount    the amount to transfer
     * @param hookCall  the fungible hook call to execute
     * @return the updated transaction
     * @throws IllegalArgumentException if hookCall is null
     */
    public TransferTransaction addHbarTransferWithHook(AccountId accountId, Hbar amount, FungibleHookCall hookCall) {
        requireNotFrozen();
        Objects.requireNonNull(accountId, "accountId cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(hookCall, "hookCall cannot be null");

        // Check if there's already a transfer for this account without a hook
        for (var transfer : hbarTransfers) {
            if (transfer.accountId.equals(accountId) && transfer.hookCall == null) {
                // Update existing transfer to include hook
                transfer.hookCall = hookCall;
                return this;
            }
        }

        // Add new transfer with hook
        hbarTransfers.add(new HbarTransfer(accountId, amount, false, hookCall));
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
            FungibleHookCall typedHook = null;
            if (transfer.hasPreTxAllowanceHook()) {
                typedHook = toFungibleHook(transfer.getPreTxAllowanceHook(), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
            } else if (transfer.hasPrePostTxAllowanceHook()) {
                typedHook = toFungibleHook(
                        transfer.getPrePostTxAllowanceHook(), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);
            }

            if (typedHook != null) {
                hbarTransfers.add(new HbarTransfer(
                        AccountId.fromProtobuf(transfer.getAccountID()),
                        Hbar.fromTinybars(transfer.getAmount()),
                        transfer.getIsApproval(),
                        typedHook));
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
                FungibleHookCall typedHook = null;
                if (transfer.hasPreTxAllowanceHook()) {
                    typedHook =
                            toFungibleHook(transfer.getPreTxAllowanceHook(), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
                } else if (transfer.hasPrePostTxAllowanceHook()) {
                    typedHook = toFungibleHook(
                            transfer.getPrePostTxAllowanceHook(), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);
                }

                var acctId = AccountId.fromProtobuf(transfer.getAccountID());
                Integer expectedDecimals = tokenTransferList.hasExpectedDecimals()
                        ? tokenTransferList.getExpectedDecimals().getValue()
                        : null;

                if (typedHook != null) {
                    tokenTransfers.add(new TokenTransfer(
                            token,
                            acctId,
                            transfer.getAmount(),
                            expectedDecimals,
                            transfer.getIsApproval(),
                            typedHook));
                } else {
                    tokenTransfers.add(new TokenTransfer(
                            token, acctId, transfer.getAmount(), expectedDecimals, transfer.getIsApproval()));
                }
            }

            for (var transfer : tokenTransferList.getNftTransfersList()) {
                NftHookCall senderHookCall = null;
                NftHookCall receiverHookCall = null;

                if (transfer.hasPreTxSenderAllowanceHook()) {
                    senderHookCall = toNftHook(transfer.getPreTxSenderAllowanceHook(), NftHookType.PRE_HOOK_SENDER);
                } else if (transfer.hasPrePostTxSenderAllowanceHook()) {
                    senderHookCall =
                            toNftHook(transfer.getPrePostTxSenderAllowanceHook(), NftHookType.PRE_POST_HOOK_SENDER);
                }

                if (transfer.hasPreTxReceiverAllowanceHook()) {
                    receiverHookCall =
                            toNftHook(transfer.getPreTxReceiverAllowanceHook(), NftHookType.PRE_HOOK_RECEIVER);
                } else if (transfer.hasPrePostTxReceiverAllowanceHook()) {
                    receiverHookCall =
                            toNftHook(transfer.getPrePostTxReceiverAllowanceHook(), NftHookType.PRE_POST_HOOK_RECEIVER);
                }

                var sender = AccountId.fromProtobuf(transfer.getSenderAccountID());
                var receiver = AccountId.fromProtobuf(transfer.getReceiverAccountID());

                if (senderHookCall != null || receiverHookCall != null) {
                    nftTransfers.add(new TokenNftTransfer(
                            token,
                            sender,
                            receiver,
                            transfer.getSerialNumber(),
                            transfer.getIsApproval(),
                            senderHookCall,
                            receiverHookCall));
                } else {
                    nftTransfers.add(new TokenNftTransfer(
                            token, sender, receiver, transfer.getSerialNumber(), transfer.getIsApproval()));
                }
            }
        }
    }

    // --- Parsing helpers for typed hooks ---
    private static FungibleHookCall toFungibleHook(
            com.hedera.hashgraph.sdk.proto.HookCall proto, FungibleHookType type) {
        var base = HookCall.fromProtobuf(proto);
        return base.hasFullHookId()
                ? new FungibleHookCall(base.getFullHookId(), base.getEvmHookCall(), type)
                : new FungibleHookCall(base.getHookId(), base.getEvmHookCall(), type);
    }

    private static NftHookCall toNftHook(com.hedera.hashgraph.sdk.proto.HookCall proto, NftHookType type) {
        var base = HookCall.fromProtobuf(proto);
        return base.hasFullHookId()
                ? new NftHookCall(base.getFullHookId(), base.getEvmHookCall(), type)
                : new NftHookCall(base.getHookId(), base.getEvmHookCall(), type);
    }
}
