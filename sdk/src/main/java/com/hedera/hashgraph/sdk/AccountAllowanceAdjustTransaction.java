// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
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
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

/**
 * <b>Deprecated.</b> This class is deprecated and will be removed in a future release.<br>
 * <p>
 * There is no direct replacement for this class. For new implementations, consider using
 * {@link com.hedera.hashgraph.sdk.AccountAllowanceApproveTransaction} and related methods for managing allowances.
 * </p>
 *
 * @deprecated This class is deprecated and will be removed in a future release. Use {@link com.hedera.hashgraph.sdk.AccountAllowanceApproveTransaction} where possible.
 */
@Deprecated
public class AccountAllowanceAdjustTransaction extends Transaction<AccountAllowanceAdjustTransaction> {
    private final List<HbarAllowance> hbarAllowances = new ArrayList<>();
    private final List<TokenAllowance> tokenAllowances = new ArrayList<>();
    private final List<TokenNftAllowance> nftAllowances = new ArrayList<>();
    // key is "{ownerId}:{spenderId}".  OwnerId may be "FEE_PAYER"
    private final Map<String, Map<TokenId, Integer>> nftMap = new HashMap<>();

    /**
     * Constructor.
     */
    public AccountAllowanceAdjustTransaction() {}

    AccountAllowanceAdjustTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    AccountAllowanceAdjustTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    private void initFromTransactionBody() {
        throw new UnsupportedOperationException("Cannot construct AccountAllowanceAdjustTransaction from bytes");
    }

    private AccountAllowanceAdjustTransaction adjustHbarAllowance(
            @Nullable AccountId ownerAccountId, AccountId spenderAccountId, Hbar amount) {
        requireNotFrozen();
        hbarAllowances.add(new HbarAllowance(ownerAccountId, Objects.requireNonNull(spenderAccountId), amount));
        return this;
    }

    /**
     * <b>Deprecated.</b> This method is deprecated and will be removed in a future release.<br>
     * <p>
     * Please use {@link #grantHbarAllowance(AccountId, AccountId, Hbar)} to grant an allowance or
     * {@link #revokeHbarAllowance(AccountId, AccountId, Hbar)} to revoke an allowance instead.
     * </p>
     *
     * @param spenderAccountId the spender account id
     * @param amount the amount of hbar
     * @return an account allowance adjust transaction
     * @deprecated Use {@link #grantHbarAllowance(AccountId, AccountId, Hbar)} or
     * {@link #revokeHbarAllowance(AccountId, AccountId, Hbar)} instead.
     */
    @Deprecated
    public AccountAllowanceAdjustTransaction addHbarAllowance(AccountId spenderAccountId, Hbar amount) {
        return adjustHbarAllowance(null, spenderAccountId, Objects.requireNonNull(amount));
    }

    /**
     *  Grants Hbar allowance.
     *
     * @param ownerAccountId    the owner's account id
     * @param spenderAccountId  the spender's account id
     * @param amount            the amount of Hbar
     * @return {@code this}
     */
    public AccountAllowanceAdjustTransaction grantHbarAllowance(
            AccountId ownerAccountId, AccountId spenderAccountId, Hbar amount) {
        Objects.requireNonNull(amount);
        if (amount.compareTo(Hbar.ZERO) < 0) {
            throw new IllegalArgumentException("amount passed to grantHbarAllowance must be positive");
        }
        return adjustHbarAllowance(Objects.requireNonNull(ownerAccountId), spenderAccountId, amount);
    }

    /**
     * Revokes Hbar allowance
     *
     * @param ownerAccountId    the owner's account id
     * @param spenderAccountId  the spender's account id
     * @param amount            the amount of Hbar
     * @return {@code this}
     */
    public AccountAllowanceAdjustTransaction revokeHbarAllowance(
            AccountId ownerAccountId, AccountId spenderAccountId, Hbar amount) {
        Objects.requireNonNull(amount);
        if (amount.compareTo(Hbar.ZERO) < 0) {
            throw new IllegalArgumentException("amount passed to revokeHbarAllowance must be positive");
        }
        return adjustHbarAllowance(Objects.requireNonNull(ownerAccountId), spenderAccountId, amount.negated());
    }

    /**
     * Get the Hbar allowances
     *
     * @return the Hbar allowances
     */
    public List<HbarAllowance> getHbarAllowances() {
        return new ArrayList<>(hbarAllowances);
    }

    private AccountAllowanceAdjustTransaction adjustTokenAllowance(
            TokenId tokenId, @Nullable AccountId ownerAccountId, AccountId spenderAccountId, long amount) {
        requireNotFrozen();
        tokenAllowances.add(new TokenAllowance(
                Objects.requireNonNull(tokenId), ownerAccountId, Objects.requireNonNull(spenderAccountId), amount));
        return this;
    }

    /**
     * @deprecated - Use {@link #grantTokenAllowance(TokenId, AccountId, AccountId, long)} or
     * {@link #revokeTokenAllowance(TokenId, AccountId, AccountId, long)} instead
     *
     * @param tokenId                   the token's id
     * @param spenderAccountId          the spender's account id
     * @param amount                    the amount of hbar
     * @return                          an account allowance adjust transaction
     */
    @Deprecated
    public AccountAllowanceAdjustTransaction addTokenAllowance(
            TokenId tokenId, AccountId spenderAccountId, long amount) {
        return adjustTokenAllowance(tokenId, null, spenderAccountId, amount);
    }

    /**
     * Grants token allowance.
     *
     * @param tokenId           the token's id
     * @param ownerAccountId    the owner's id
     * @param spenderAccountId  the spender's id
     * @param amount            the amount of tokens
     * @return {@code this}
     */
    public AccountAllowanceAdjustTransaction grantTokenAllowance(
            TokenId tokenId, AccountId ownerAccountId, AccountId spenderAccountId, @Nonnegative long amount) {
        return adjustTokenAllowance(tokenId, Objects.requireNonNull(ownerAccountId), spenderAccountId, amount);
    }

    /**
     * Revokes token allowance.
     *
     * @param tokenId           the token's id
     * @param ownerAccountId    the owner's id
     * @param spenderAccountId  the spender's id
     * @param amount            the amount of tokens
     * @return {@code this}
     */
    public AccountAllowanceAdjustTransaction revokeTokenAllowance(
            TokenId tokenId, AccountId ownerAccountId, AccountId spenderAccountId, @Nonnegative long amount) {
        return adjustTokenAllowance(tokenId, Objects.requireNonNull(ownerAccountId), spenderAccountId, -amount);
    }

    /**
     * Get the token allowances
     *
     * @return the token allowances
     */
    public List<TokenAllowance> getTokenAllowances() {
        return new ArrayList<>(tokenAllowances);
    }

    private static String ownerToString(@Nullable AccountId ownerAccountId) {
        return ownerAccountId != null ? ownerAccountId.toString() : "FEE_PAYER";
    }

    private List<Long> getNftSerials(@Nullable AccountId ownerAccountId, AccountId spenderAccountId, TokenId tokenId) {
        var key = ownerToString(ownerAccountId) + ":" + spenderAccountId;
        if (nftMap.containsKey(key)) {
            var innerMap = nftMap.get(key);
            if (innerMap.containsKey(tokenId)) {
                return Objects.requireNonNull(nftAllowances.get(innerMap.get(tokenId)).serialNumbers);
            } else {
                return newNftSerials(ownerAccountId, spenderAccountId, tokenId, innerMap);
            }
        } else {
            Map<TokenId, Integer> innerMap = new HashMap<>();
            nftMap.put(key, innerMap);
            return newNftSerials(ownerAccountId, spenderAccountId, tokenId, innerMap);
        }
    }

    private List<Long> newNftSerials(
            @Nullable AccountId ownerAccountId,
            AccountId spenderAccountId,
            TokenId tokenId,
            Map<TokenId, Integer> innerMap) {
        innerMap.put(tokenId, nftAllowances.size());
        TokenNftAllowance newAllowance =
                new TokenNftAllowance(tokenId, ownerAccountId, spenderAccountId, null, new ArrayList<>(), null);
        nftAllowances.add(newAllowance);
        return newAllowance.serialNumbers;
    }

    private AccountAllowanceAdjustTransaction adjustNftAllowance(
            TokenId tokenId, long serial, @Nullable AccountId ownerAccountId, AccountId spenderAccountId) {
        requireNotFrozen();
        getNftSerials(ownerAccountId, Objects.requireNonNull(spenderAccountId), tokenId)
                .add(serial);
        return this;
    }

    private AccountAllowanceAdjustTransaction adjustNftAllowanceAllSerials(
            TokenId tokenId, boolean allSerials, @Nullable AccountId ownerAccountId, AccountId spenderAccountId) {
        requireNotFrozen();
        nftAllowances.add(new TokenNftAllowance(
                Objects.requireNonNull(tokenId),
                ownerAccountId,
                Objects.requireNonNull(spenderAccountId),
                null,
                Collections.emptyList(),
                allSerials));
        return this;
    }

    /**
     * @deprecated - Use {@link #grantTokenNftAllowance(NftId, AccountId, AccountId)} or
     * {@link #revokeTokenNftAllowance(NftId, AccountId, AccountId)} instead
     *
     * @param nftId                 the NFT's id
     * @param spenderAccountId      the spender's account id
     * @return                      an account allowance adjust transaction
     */
    @Deprecated
    public AccountAllowanceAdjustTransaction addTokenNftAllowance(NftId nftId, AccountId spenderAccountId) {
        Objects.requireNonNull(nftId);
        return adjustNftAllowance(nftId.tokenId, nftId.serial, null, spenderAccountId);
    }

    /**
     * @deprecated - Use {@link #grantTokenNftAllowanceAllSerials(TokenId, AccountId, AccountId)} or
     * {@link #revokeTokenNftAllowanceAllSerials(TokenId, AccountId, AccountId)} instead
     * @param tokenId               the token's id
     * @param spenderAccountId      the spender's account id
     * @return                      an account allowance adjust transaction
     */
    @Deprecated
    public AccountAllowanceAdjustTransaction addAllTokenNftAllowance(TokenId tokenId, AccountId spenderAccountId) {
        return adjustNftAllowanceAllSerials(tokenId, true, null, spenderAccountId);
    }

    /**
     * Grants NFT allowance.
     *
     * @param nftId             the NFT's id
     * @param ownerAccountId    the owner's id
     * @param spenderAccountId  the spender's id
     * @return {@code this}
     */
    public AccountAllowanceAdjustTransaction grantTokenNftAllowance(
            NftId nftId, AccountId ownerAccountId, AccountId spenderAccountId) {
        Objects.requireNonNull(nftId);
        Objects.requireNonNull(ownerAccountId);
        return adjustNftAllowance(nftId.tokenId, nftId.serial, ownerAccountId, spenderAccountId);
    }

    /**
     * Grants allowance for all NFT serials of a token
     *
     * @param tokenId               the token's id
     * @param ownerAccountId        the owner's account id
     * @param spenderAccountId      the spender's account id
     * @return                      an account allowance adjust transaction
     */
    public AccountAllowanceAdjustTransaction grantTokenNftAllowanceAllSerials(
            TokenId tokenId, AccountId ownerAccountId, AccountId spenderAccountId) {
        Objects.requireNonNull(ownerAccountId);
        return adjustNftAllowanceAllSerials(tokenId, true, ownerAccountId, spenderAccountId);
    }

    /**
     * @deprecated with no replacement
     * @param nftId                 the NFT's id
     * @param ownerAccountId        the owner's account id
     * @param spenderAccountId      the spender's account id
     * @return                      an account allowance adjust transaction
     */
    @Deprecated
    public AccountAllowanceAdjustTransaction revokeTokenNftAllowance(
            NftId nftId, AccountId ownerAccountId, AccountId spenderAccountId) {
        Objects.requireNonNull(nftId);
        Objects.requireNonNull(ownerAccountId);
        return adjustNftAllowance(nftId.tokenId, -nftId.serial, ownerAccountId, spenderAccountId);
    }

    /**
     * Revokes allowance for all NFT serials of a token
     *
     * @param tokenId               the token's id
     * @param ownerAccountId        the owner's account id
     * @param spenderAccountId      the spender's account id
     * @return                      an account allowance adjust transaction
     */
    public AccountAllowanceAdjustTransaction revokeTokenNftAllowanceAllSerials(
            TokenId tokenId, AccountId ownerAccountId, AccountId spenderAccountId) {
        Objects.requireNonNull(ownerAccountId);
        return adjustNftAllowanceAllSerials(tokenId, false, ownerAccountId, spenderAccountId);
    }

    /**
     * Get the NFT allowances
     *
     * @return a copy of {@link #nftAllowances}
     */
    public List<TokenNftAllowance> getTokenNftAllowances() {
        List<TokenNftAllowance> retval = new ArrayList<>(nftAllowances.size());
        for (var allowance : nftAllowances) {
            retval.add(TokenNftAllowance.copyFrom(allowance));
        }
        return retval;
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        throw new UnsupportedOperationException("Cannot get method descriptor for AccountAllowanceAdjustTransaction");
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        // do nothing
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException("Cannot schedule AccountAllowanceAdjustTransaction");
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
