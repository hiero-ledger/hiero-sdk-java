// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

abstract class AbstractTokenTransferTransaction<T extends AbstractTokenTransferTransaction<T>> extends Transaction<T> {

    protected final ArrayList<TokenTransfer> tokenTransfers = new ArrayList<>();
    protected final ArrayList<TokenNftTransfer> nftTransfers = new ArrayList<>();

    protected AbstractTokenTransferTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    AbstractTokenTransferTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    AbstractTokenTransferTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
    }

    /**
     * Extract the list of token id decimals.
     *
     * @return the list of token id decimals
     */
    public Map<TokenId, Integer> getTokenIdDecimals() {
        Map<TokenId, Integer> decimalsMap = new HashMap<>();

        for (var transfer : tokenTransfers) {
            decimalsMap.put(transfer.tokenId, transfer.expectedDecimals);
        }

        return decimalsMap;
    }

    /**
     * Extract the list of token transfer records.
     *
     * @return the list of token transfer records
     */
    public Map<TokenId, Map<AccountId, Long>> getTokenTransfers() {
        Map<TokenId, Map<AccountId, Long>> transfers = new HashMap<>();

        for (var transfer : tokenTransfers) {
            var current = transfers.get(transfer.tokenId) != null
                    ? transfers.get(transfer.tokenId)
                    : new HashMap<AccountId, Long>();
            current.put(transfer.accountId, transfer.amount);
            transfers.put(transfer.tokenId, current);
        }

        return transfers;
    }

    protected T doAddTokenTransfer(
            TokenId tokenId,
            AccountId accountId,
            long amount,
            boolean isApproved,
            @Nullable Integer expectedDecimals,
            @Nullable FungibleHookCall hookCall) {
        requireNotFrozen();

        for (var transfer : tokenTransfers) {
            if (transfer.tokenId.equals(tokenId)) {
                if (transfer.expectedDecimals != null && !transfer.expectedDecimals.equals(expectedDecimals)) {
                    throw new IllegalArgumentException("expected decimals for a token cannot be changed once set");
                }
                if (transfer.accountId.equals(accountId) && transfer.isApproved == isApproved) {
                    if (expectedDecimals != null) {
                        transfer.expectedDecimals = expectedDecimals;
                    }
                    transfer.amount += amount;
                    transfer.hookCall = hookCall;
                    // noinspection unchecked
                    return (T) this;
                }
            }
        }

        // Create new record
        var tt = new TokenTransfer(tokenId, accountId, amount, expectedDecimals, isApproved, hookCall);
        tokenTransfers.add(tt);
        // noinspection unchecked
        return (T) this;
    }

    /**
     * Add a non-approved token transfer to the transaction.
     *
     * @param tokenId   the token id
     * @param accountId the account id
     * @param value     the value
     * @return the updated transaction
     */
    public T addTokenTransfer(TokenId tokenId, AccountId accountId, long value) {
        return doAddTokenTransfer(tokenId, accountId, value, false, null, null);
    }

    /**
     * Add an approved token transfer to the transaction.
     *
     * @param tokenId   the token id
     * @param accountId the account id
     * @param value     the value
     * @return the updated transaction
     */
    public T addApprovedTokenTransfer(TokenId tokenId, AccountId accountId, long value) {
        return doAddTokenTransfer(tokenId, accountId, value, true, null, null);
    }

    /**
     * Add a non-approved token transfer with decimals.
     *
     * @param tokenId   the token id
     * @param accountId the account id
     * @param value     the value
     * @param decimals  the decimals
     * @return the updated transaction
     */
    public T addTokenTransferWithDecimals(TokenId tokenId, AccountId accountId, long value, int decimals) {
        return doAddTokenTransfer(tokenId, accountId, value, false, decimals, null);
    }

    /**
     * Add an approved token transfer with decimals.
     *
     * @param tokenId   the token id
     * @param accountId the account id
     * @param value     the value
     * @param decimals  the decimals
     * @return the updated transaction
     */
    public T addApprovedTokenTransferWithDecimals(TokenId tokenId, AccountId accountId, long value, int decimals) {
        return doAddTokenTransfer(tokenId, accountId, value, true, decimals, null);
    }

    /**
     * @param tokenId    the token id
     * @param accountId  the account id
     * @param isApproved whether the transfer is approved
     * @return {@code this}
     * @deprecated - Use {@link #addApprovedTokenTransfer(TokenId, AccountId, long)} instead
     */
    @Deprecated
    public T setTokenTransferApproval(TokenId tokenId, AccountId accountId, boolean isApproved) {
        requireNotFrozen();

        for (var transfer : tokenTransfers) {
            if (transfer.tokenId.equals(tokenId) && transfer.accountId.equals(accountId)) {
                transfer.isApproved = isApproved;
                // noinspection unchecked
                return (T) this;
            }
        }

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Extract the of token nft transfers.
     *
     * @return list of token nft transfers
     */
    public Map<TokenId, List<TokenNftTransfer>> getTokenNftTransfers() {
        Map<TokenId, List<TokenNftTransfer>> transfers = new HashMap<>();

        for (var transfer : nftTransfers) {
            var current = transfers.get(transfer.tokenId) != null
                    ? transfers.get(transfer.tokenId)
                    : new ArrayList<TokenNftTransfer>();
            current.add(transfer);
            transfers.put(transfer.tokenId, current);
        }

        return transfers;
    }

    protected T doAddNftTransfer(
            NftId nftId,
            AccountId sender,
            AccountId receiver,
            boolean isApproved,
            @Nullable NftHookCall senderHookCall,
            @Nullable NftHookCall receiverHookCall) {
        requireNotFrozen();
        nftTransfers.add(new TokenNftTransfer(
                nftId.tokenId, sender, receiver, nftId.serial, isApproved, senderHookCall, receiverHookCall));
        // noinspection unchecked
        return (T) this;
    }

    /**
     * Add a non-approved nft transfer.
     *
     * @param nftId    the nft's id
     * @param sender   the sender account id
     * @param receiver the receiver account id
     * @return the updated transaction
     */
    public T addNftTransfer(NftId nftId, AccountId sender, AccountId receiver) {
        return doAddNftTransfer(nftId, sender, receiver, false, null, null);
    }

    /**
     * Add an approved nft transfer.
     *
     * @param nftId    the nft's id
     * @param sender   the sender account id
     * @param receiver the receiver account id
     * @return the updated transaction
     */
    public T addApprovedNftTransfer(NftId nftId, AccountId sender, AccountId receiver) {
        return doAddNftTransfer(nftId, sender, receiver, true, null, null);
    }

    /**
     * @param nftId      the NFT id
     * @param isApproved whether the transfer is approved
     * @return {@code this}
     * @deprecated - Use {@link #addApprovedNftTransfer(NftId, AccountId, AccountId)} instead
     */
    @Deprecated
    public T setNftTransferApproval(NftId nftId, boolean isApproved) {
        requireNotFrozen();

        for (var transfer : nftTransfers) {
            if (transfer.tokenId.equals(nftId.tokenId) && transfer.serial == nftId.serial) {
                transfer.isApproved = isApproved;
                // noinspection unchecked
                return (T) this;
            }
        }

        // noinspection unchecked
        return (T) this;
    }

    protected ArrayList<com.hedera.hashgraph.sdk.TokenTransferList> sortTransfersAndBuild() {
        var transferLists = new ArrayList<com.hedera.hashgraph.sdk.TokenTransferList>();

        this.tokenTransfers.sort(Comparator.comparing((TokenTransfer a) -> a.tokenId)
                .thenComparing(a -> a.accountId)
                .thenComparing(a -> a.isApproved));
        this.nftTransfers.sort(Comparator.comparing((TokenNftTransfer a) -> a.tokenId)
                .thenComparing(a -> a.sender)
                .thenComparing(a -> a.receiver)
                .thenComparing(a -> a.serial));

        var i = 0;
        var j = 0;

        // Effectively merge sort
        while (i < this.tokenTransfers.size() || j < this.nftTransfers.size()) {
            if (i < this.tokenTransfers.size() && j < this.nftTransfers.size()) {
                var iTokenId = this.tokenTransfers.get(i).tokenId;
                var jTokenId = this.nftTransfers.get(j).tokenId;
                var last = !transferLists.isEmpty() ? transferLists.get(transferLists.size() - 1) : null;
                var lastTokenId = last != null ? last.tokenId : null;

                if (last != null && iTokenId.compareTo(lastTokenId) == 0) {
                    last.transfers.add(this.tokenTransfers.get(i++));
                    continue;
                }

                if (last != null && jTokenId.compareTo(lastTokenId) == 0) {
                    last.nftTransfers.add(this.nftTransfers.get(j++));
                    continue;
                }

                var result = iTokenId.compareTo(jTokenId);

                if (result == 0) {
                    transferLists.add(new com.hedera.hashgraph.sdk.TokenTransferList(
                            iTokenId,
                            this.tokenTransfers.get(i).expectedDecimals,
                            this.tokenTransfers.get(i++),
                            this.nftTransfers.get(j++)));
                } else if (result < 0) {
                    transferLists.add(new com.hedera.hashgraph.sdk.TokenTransferList(
                            iTokenId, this.tokenTransfers.get(i).expectedDecimals, this.tokenTransfers.get(i++), null));
                } else {
                    transferLists.add(new com.hedera.hashgraph.sdk.TokenTransferList(
                            jTokenId, null, null, this.nftTransfers.get(j++)));
                }
            } else if (i < this.tokenTransfers.size()) {
                var iTokenId = this.tokenTransfers.get(i).tokenId;
                var last = !transferLists.isEmpty() ? transferLists.get(transferLists.size() - 1) : null;
                var lastTokenId = last != null ? last.tokenId : null;

                if (last != null && iTokenId.compareTo(lastTokenId) == 0) {
                    last.transfers.add(this.tokenTransfers.get(i++));
                    continue;
                }

                transferLists.add(new com.hedera.hashgraph.sdk.TokenTransferList(
                        iTokenId, this.tokenTransfers.get(i).expectedDecimals, this.tokenTransfers.get(i++), null));
            } else {
                var jTokenId = this.nftTransfers.get(j).tokenId;
                var last = !transferLists.isEmpty() ? transferLists.get(transferLists.size() - 1) : null;
                var lastTokenId = last != null ? last.tokenId : null;

                if (last != null && jTokenId.compareTo(lastTokenId) == 0) {
                    last.nftTransfers.add(this.nftTransfers.get(j++));
                    continue;
                }

                transferLists.add(new com.hedera.hashgraph.sdk.TokenTransferList(
                        jTokenId, null, null, this.nftTransfers.get(j++)));
            }
        }
        return transferLists;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        for (var transfer : nftTransfers) {
            transfer.tokenId.validateChecksum(client);
            transfer.sender.validateChecksum(client);
            transfer.receiver.validateChecksum(client);
        }

        for (var transfer : tokenTransfers) {
            transfer.tokenId.validateChecksum(client);
            transfer.accountId.validateChecksum(client);
        }
    }
}
