// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.CryptoApproveAllowanceTransactionBody;
import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * # Approve Allowance
 * This transaction body provides a mechanism to add "allowance" entries
 * for an account. These allowances enable one account to spend or transfer
 * token balances (for fungible/common tokens), individual tokens (for
 * non-fungible/unique tokens), or all non-fungible tokens owned by the
 * account, now or in the future (if `approved_for_all` is set).
 *
 **/
public class AccountAllowanceApproveTransaction extends Transaction<AccountAllowanceApproveTransaction> {
    private final List<HbarAllowance> hbarAllowances = new ArrayList<>();
    private final List<TokenAllowance> tokenAllowances = new ArrayList<>();
    private final List<TokenNftAllowance> nftAllowances = new ArrayList<>();
    // key is "{ownerId}:{spenderId}".  OwnerId may be "FEE_PAYER"
    // <ownerId:spenderId, <tokenId, index>>
    private final Map<String, Map<TokenId, Integer>> nftMap = new HashMap<>();

    /**
     * Constructor.
     */
    public AccountAllowanceApproveTransaction() {}

    /**
     * Constructor.
     *
     * @param txs                                   Compound list of transaction id's list of (AccountId, Transaction) records
     */
    AccountAllowanceApproveTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody                    protobuf TransactionBody
     */
    AccountAllowanceApproveTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    private void initFromTransactionBody() {
        var body = sourceTransactionBody.getCryptoApproveAllowance();
        for (var allowanceProto : body.getCryptoAllowancesList()) {
            hbarAllowances.add(HbarAllowance.fromProtobuf(allowanceProto));
        }
        for (var allowanceProto : body.getTokenAllowancesList()) {
            tokenAllowances.add(TokenAllowance.fromProtobuf(allowanceProto));
        }
        for (var allowanceProto : body.getNftAllowancesList()) {
            if (allowanceProto.hasApprovedForAll()
                    && allowanceProto.getApprovedForAll().getValue()) {
                nftAllowances.add(TokenNftAllowance.fromProtobuf(allowanceProto));
            } else {
                getNftSerials(
                                allowanceProto.hasOwner() ? AccountId.fromProtobuf(allowanceProto.getOwner()) : null,
                                AccountId.fromProtobuf(allowanceProto.getSpender()),
                                allowanceProto.hasDelegatingSpender()
                                        ? AccountId.fromProtobuf(allowanceProto.getDelegatingSpender())
                                        : null,
                                TokenId.fromProtobuf(allowanceProto.getTokenId()))
                        .addAll(allowanceProto.getSerialNumbersList());
            }
        }
    }

    /**
     * <b>Deprecated.</b> This method is deprecated and will be removed in a future release.<br>
     * <p>
     * Please use {@link #approveHbarAllowance(AccountId, AccountId, Hbar)} instead for granting Hbar allowance.
     * </p>
     *
     * @param spenderAccountId the spender account id
     * @param amount the amount of hbar
     * @return an account allowance approve transaction
     * @deprecated Use {@link #approveHbarAllowance(AccountId, AccountId, Hbar)} instead.
     */
    @Deprecated
    public AccountAllowanceApproveTransaction addHbarAllowance(AccountId spenderAccountId, Hbar amount) {
        return approveHbarAllowance(null, spenderAccountId, amount);
    }

    /**
     * Approves the Hbar allowance.
     *
     * @param ownerAccountId            owner's account id
     * @param spenderAccountId          spender's account id
     * @param amount                    amount of hbar add
     * @return {@code this}
     */
    public AccountAllowanceApproveTransaction approveHbarAllowance(
            @Nullable AccountId ownerAccountId, AccountId spenderAccountId, Hbar amount) {
        requireNotFrozen();
        Objects.requireNonNull(amount);
        hbarAllowances.add(new HbarAllowance(ownerAccountId, Objects.requireNonNull(spenderAccountId), amount));
        return this;
    }

    /**
     * @deprecated - Use {@link #getHbarApprovals()} instead
     *
     * @return                          list of hbar allowance records
     */
    @Deprecated
    public List<HbarAllowance> getHbarAllowances() {
        return getHbarApprovals();
    }

    /**
     * Extract the list of hbar allowances.
     *
     * @return                          array list of hbar allowances
     */
    public List<HbarAllowance> getHbarApprovals() {
        return new ArrayList<>(hbarAllowances);
    }

    /**
     * @deprecated - Use {@link #approveTokenAllowance(TokenId, AccountId, AccountId, long)} instead
     *
     * @param tokenId                   the token id
     * @param spenderAccountId          the spenders account id
     * @param amount                    the hbar amount
     * @return                          an account allowance approve transaction
     */
    @Deprecated
    public AccountAllowanceApproveTransaction addTokenAllowance(
            TokenId tokenId, AccountId spenderAccountId, long amount) {
        return approveTokenAllowance(tokenId, null, spenderAccountId, amount);
    }

    /**
     * Approves the Token allowance.
     *
     * @param tokenId                   the token's id
     * @param ownerAccountId            owner's account id
     * @param spenderAccountId          spender's account id
     * @param amount                    amount of tokens
     * @return {@code this}
     */
    public AccountAllowanceApproveTransaction approveTokenAllowance(
            TokenId tokenId, @Nullable AccountId ownerAccountId, AccountId spenderAccountId, long amount) {
        requireNotFrozen();
        tokenAllowances.add(new TokenAllowance(
                Objects.requireNonNull(tokenId), ownerAccountId, Objects.requireNonNull(spenderAccountId), amount));
        return this;
    }

    /**
     * @deprecated - Use {@link #getTokenApprovals()} instead
     *
     * @return                          a list of token allowances
     */
    @Deprecated
    public List<TokenAllowance> getTokenAllowances() {
        return getTokenApprovals();
    }

    /**
     * Extract a list of token allowance approvals.
     *
     * @return                          array list of token approvals.
     */
    public List<TokenAllowance> getTokenApprovals() {
        return new ArrayList<>(tokenAllowances);
    }

    /**
     * Extract the owner as a string.
     *
     * @param ownerAccountId            owner's account id
     * @return                          a string representation of the account id
     *                                  or FEE_PAYER
     */
    private static String ownerToString(@Nullable AccountId ownerAccountId) {
        return ownerAccountId != null ? ownerAccountId.toString() : "FEE_PAYER";
    }

    /**
     * Return a list of NFT serial numbers.
     *
     * @param ownerAccountId            owner's account id
     * @param spenderAccountId          spender's account id
     * @param delegatingSpender         delegating spender's account id
     * @param tokenId                   the token's id
     * @return list of NFT serial numbers
     */
    private List<Long> getNftSerials(
            @Nullable AccountId ownerAccountId,
            AccountId spenderAccountId,
            @Nullable AccountId delegatingSpender,
            TokenId tokenId) {
        var key = ownerToString(ownerAccountId) + ":" + spenderAccountId;
        if (nftMap.containsKey(key)) {
            var innerMap = nftMap.get(key);
            if (innerMap.containsKey(tokenId)) {
                return Objects.requireNonNull(nftAllowances.get(innerMap.get(tokenId)).serialNumbers);
            } else {
                return newNftSerials(ownerAccountId, spenderAccountId, delegatingSpender, tokenId, innerMap);
            }
        } else {
            Map<TokenId, Integer> innerMap = new HashMap<>();
            nftMap.put(key, innerMap);
            return newNftSerials(ownerAccountId, spenderAccountId, delegatingSpender, tokenId, innerMap);
        }
    }

    /**
     * Add NFT serials.
     *
     * @param ownerAccountId            owner's account id
     * @param spenderAccountId          spender's account id
     * @param delegatingSpender         delegating spender's account id
     * @param tokenId                   the token's id
     * @param innerMap                  list of token id's and serial number records
     * @return list of NFT serial numbers
     */
    private List<Long> newNftSerials(
            @Nullable AccountId ownerAccountId,
            AccountId spenderAccountId,
            @Nullable AccountId delegatingSpender,
            TokenId tokenId,
            Map<TokenId, Integer> innerMap) {
        innerMap.put(tokenId, nftAllowances.size());
        TokenNftAllowance newAllowance = new TokenNftAllowance(
                tokenId, ownerAccountId, spenderAccountId, delegatingSpender, new ArrayList<>(), null);
        nftAllowances.add(newAllowance);
        return newAllowance.serialNumbers;
    }

    /**
     * @deprecated - Use {@link #approveTokenNftAllowance(NftId, AccountId, AccountId, AccountId)} instead
     *
     * @param nftId                     the nft id
     * @param spenderAccountId          the spender's account id
     * @return {@code this}
     */
    @Deprecated
    public AccountAllowanceApproveTransaction addTokenNftAllowance(NftId nftId, AccountId spenderAccountId) {
        requireNotFrozen();
        getNftSerials(null, spenderAccountId, null, nftId.tokenId).add(nftId.serial);
        return this;
    }

    /**
     * @deprecated - Use {@link #approveTokenNftAllowanceAllSerials(TokenId, AccountId, AccountId)} instead
     *
     * @param tokenId                   the token id
     * @param spenderAccountId          the spender's account id
     * @return {@code this}
     */
    @Deprecated
    public AccountAllowanceApproveTransaction addAllTokenNftAllowance(TokenId tokenId, AccountId spenderAccountId) {
        requireNotFrozen();
        nftAllowances.add(new TokenNftAllowance(tokenId, null, spenderAccountId, null, Collections.emptyList(), true));
        return this;
    }

    /**
     * Approve the NFT allowance.
     *
     * @param nftId                     nft's id
     * @param ownerAccountId            owner's account id
     * @param spenderAccountId          spender's account id
     * @param delegatingSpender         delegating spender's account id
     * @return {@code this}
     */
    public AccountAllowanceApproveTransaction approveTokenNftAllowance(
            NftId nftId,
            @Nullable AccountId ownerAccountId,
            AccountId spenderAccountId,
            @Nullable AccountId delegatingSpender) {
        requireNotFrozen();
        Objects.requireNonNull(nftId);
        getNftSerials(ownerAccountId, Objects.requireNonNull(spenderAccountId), delegatingSpender, nftId.tokenId)
                .add(nftId.serial);
        return this;
    }

    /**
     * Approve the NFT allowance.
     *
     * @param nftId                     nft's id
     * @param ownerAccountId            owner's account id
     * @param spenderAccountId          spender's account id
     * @return {@code this}
     */
    public AccountAllowanceApproveTransaction approveTokenNftAllowance(
            NftId nftId, @Nullable AccountId ownerAccountId, AccountId spenderAccountId) {
        requireNotFrozen();
        Objects.requireNonNull(nftId);
        getNftSerials(ownerAccountId, Objects.requireNonNull(spenderAccountId), null, nftId.tokenId)
                .add(nftId.serial);
        return this;
    }

    /**
     * Approve the token nft allowance on all serials.
     *
     * @param tokenId                   the token's id
     * @param ownerAccountId            owner's account id
     * @param spenderAccountId          spender's account id
     * @return {@code this}
     */
    public AccountAllowanceApproveTransaction approveTokenNftAllowanceAllSerials(
            TokenId tokenId, @Nullable AccountId ownerAccountId, AccountId spenderAccountId) {
        requireNotFrozen();
        nftAllowances.add(new TokenNftAllowance(
                Objects.requireNonNull(tokenId),
                ownerAccountId,
                Objects.requireNonNull(spenderAccountId),
                null,
                Collections.emptyList(),
                true));
        return this;
    }

    /**
     * Delete the token nft allowance on all serials.
     *
     * @param tokenId                   the token's id
     * @param ownerAccountId            owner's account id
     * @param spenderAccountId          spender's account id
     * @return {@code this}
     */
    public AccountAllowanceApproveTransaction deleteTokenNftAllowanceAllSerials(
            TokenId tokenId, @Nullable AccountId ownerAccountId, AccountId spenderAccountId) {
        requireNotFrozen();
        nftAllowances.add(new TokenNftAllowance(
                Objects.requireNonNull(tokenId),
                ownerAccountId,
                Objects.requireNonNull(spenderAccountId),
                null,
                Collections.emptyList(),
                false));
        return this;
    }

    /**
     * @deprecated - Use {@link #getTokenNftApprovals()} instead
     *
     * @return {@code this}
     */
    @Deprecated
    public List<TokenNftAllowance> getTokenNftAllowances() {
        return getTokenNftApprovals();
    }

    /**
     * Returns the list of token nft allowances.
     *
     * @return  list of token nft allowances.
     */
    public List<TokenNftAllowance> getTokenNftApprovals() {
        List<TokenNftAllowance> retval = new ArrayList<>(nftAllowances.size());
        for (var allowance : nftAllowances) {
            retval.add(TokenNftAllowance.copyFrom(allowance));
        }
        return retval;
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return CryptoServiceGrpc.getApproveAllowancesMethod();
    }

    /**
     * Build the correct transaction body.
     *
     * @return {@link com.hedera.hashgraph.sdk.proto.CryptoApproveAllowanceTransactionBody builder }
     */
    CryptoApproveAllowanceTransactionBody.Builder build() {
        var builder = CryptoApproveAllowanceTransactionBody.newBuilder();

        for (var allowance : hbarAllowances) {
            builder.addCryptoAllowances(allowance.toProtobuf());
        }
        for (var allowance : tokenAllowances) {
            builder.addTokenAllowances(allowance.toProtobuf());
        }
        for (var allowance : nftAllowances) {
            builder.addNftAllowances(allowance.toProtobuf());
        }
        return builder;
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setCryptoApproveAllowance(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setCryptoApproveAllowance(build());
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        for (var allowance : hbarAllowances) {
            allowance.validateChecksums(client);
        }
        for (var allowance : tokenAllowances) {
            allowance.validateChecksums(client);
        }
        for (var allowance : nftAllowances) {
            allowance.validateChecksums(client);
        }
    }
}
