// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.CryptoUpdateTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Modify the current state of an account.
 *
 * ### Requirements
 * - The `key` for this account MUST sign all account update transactions.
 * - If the `key` field is set for this transaction, then _both_ the current
 *   `key` and the new `key` MUST sign this transaction, for security and to
 *   prevent setting the `key` field to an invalid value.
 * - If the `auto_renew_account` field is set for this transaction, the account
 *   identified in that field MUST sign this transaction.
 * - Fields set to non-default values in this transaction SHALL be updated on
 *   success. Fields not set to non-default values SHALL NOT be
 *   updated on success.
 * - All fields that may be modified in this transaction SHALL have a
 *   default value of unset (a.k.a. `null`).
 *
 * ### Block Stream Effects
 * None
 */
public final class AccountUpdateTransaction extends Transaction<AccountUpdateTransaction> {
    @Nullable
    private AccountId accountId = null;

    @Nullable
    private AccountId proxyAccountId = null;

    @Nullable
    private Key key = null;

    @Nullable
    private Instant expirationTime = null;

    private Duration expirationTimeDuration = null;

    @Nullable
    private Duration autoRenewPeriod = null;

    @Nullable
    private Boolean receiverSigRequired = null;

    @Nullable
    private String accountMemo = null;

    @Nullable
    private Integer maxAutomaticTokenAssociations = null;

    @Nullable
    private Key aliasKey;

    @Nullable
    private AccountId stakedAccountId = null;

    @Nullable
    private Long stakedNodeId = null;

    @Nullable
    private Boolean declineStakingReward = null;

    private List<Long> hookIdsToDelete = new ArrayList<>();

    private List<HookCreationDetails> hookCreationDetails = new ArrayList<>();

    /**
     * Constructor.
     */
    public AccountUpdateTransaction() {}

    /**
     * Constructor.
     *
     * @param txs                       Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    AccountUpdateTransaction(
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
    AccountUpdateTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the account id.
     *
     * @return                          the account id
     */
    @Nullable
    public AccountId getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID which is being updated in this transaction.
     *
     * @param accountId The AccountId to be set
     * @return {@code this}
     */
    public AccountUpdateTransaction setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.accountId = accountId;
        return this;
    }

    /**
     * Extract the key.
     *
     * @return                          the key
     */
    @Nullable
    public Key getKey() {
        return key;
    }

    /**
     * An account key.<br/>
     * This may be a "primitive" key (a singly cryptographic key), or a
     * composite key.
     * <p>
     * If set, this key MUST be a valid key.<br/>
     * If set, the previous key and new key MUST both sign this transaction.
     *
     * @param key The Key to be set
     * @return {@code this}
     */
    public AccountUpdateTransaction setKey(Key key) {
        Objects.requireNonNull(key);
        requireNotFrozen();
        this.key = key;
        return this;
    }

    /**
     * @deprecated with no replacement
     * @return the alias key
     */
    @Deprecated
    @Nullable
    public Key getAliasKey() {
        return aliasKey;
    }

    /**
     * @deprecated with no replacement
     *
     * Sets the new key.
     *
     * @param aliasKey The Key to be set
     * @return {@code this}
     */
    @Deprecated
    public AccountUpdateTransaction setAliasKey(Key aliasKey) {
        Objects.requireNonNull(aliasKey);
        requireNotFrozen();
        this.aliasKey = aliasKey;
        return this;
    }

    /**
     * Extract the proxy account id.
     *
     * @return                          the proxy account id
     */
    @Nullable
    public AccountId getProxyAccountId() {
        return proxyAccountId;
    }

    /**
     * Sets the ID of the account to which this account is proxy staked.
     * <p>
     * If proxyAccountID is null, or is an invalid account, or is an account
     * that isn't a node, then this account is automatically proxy staked to
     * a node chosen by the network, but without earning payments.
     * <p>
     * If the proxyAccountID account refuses to accept proxy staking, or
     * if it is not currently running a node, then it
     * will behave as if proxyAccountID was null.
     *
     * @param proxyAccountId The AccountId to be set
     * @return {@code this}
     */
    public AccountUpdateTransaction setProxyAccountId(AccountId proxyAccountId) {
        Objects.requireNonNull(proxyAccountId);
        requireNotFrozen();
        this.proxyAccountId = proxyAccountId;
        return this;
    }

    /**
     * Extract the expiration time.
     *
     * @return                          the expiration time
     */
    @Nullable
    public Instant getExpirationTime() {
        return expirationTime;
    }

    /**
     * A new account expiration time, in seconds since the epoch.
     * <p>
     * For this purpose, `epoch` SHALL be the UNIX epoch with 0
     * at `1970-01-01T00:00:00.000Z`.<br/>
     * If set, this value MUST be later than the current consensus time.<br/>
     * If set, this value MUST be earlier than the current consensus time
     * extended by the current maximum expiration time configured for the
     * network.
     *
     * @param expirationTime The Instant to be set as the expiration time
     * @return {@code this}
     */
    public AccountUpdateTransaction setExpirationTime(Instant expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        this.expirationTime = expirationTime;
        return this;
    }

    public AccountUpdateTransaction setExpirationTime(Duration expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        this.expirationTime = null;
        this.expirationTimeDuration = expirationTime;
        return this;
    }

    /**
     * Extract the auto renew period.
     *
     * @return                          the auto renew period
     */
    @Nullable
    public Duration getAutoRenewPeriod() {
        return autoRenewPeriod;
    }

    /**
     * A duration to extend account expiration.<br/>
     * An amount of time, in seconds, to extend the expiration date for this
     * account when _automatically_ renewed.
     * <p>
     * This duration MUST be between the current configured minimum and maximum
     * values defined for the network.<br/>
     * This duration SHALL be applied only when _automatically_ extending the
     * account expiration.
     *
     * @param autoRenewPeriod The Duration to be set for auto renewal
     * @return {@code this}
     */
    public AccountUpdateTransaction setAutoRenewPeriod(Duration autoRenewPeriod) {
        Objects.requireNonNull(autoRenewPeriod);
        requireNotFrozen();
        this.autoRenewPeriod = autoRenewPeriod;
        return this;
    }

    /**
     * Is the receiver required to sign?
     *
     * @return                          is the receiver required to sign
     */
    @Nullable
    public Boolean getReceiverSignatureRequired() {
        return receiverSigRequired;
    }

    /**
     * Removed to distinguish between unset and a default value.<br/>
     * Do NOT use this field to set a false value because the server cannot
     * distinguish from the default value. Use receiverSigRequiredWrapper
     * field for this purpose.
     *
     * @param receiverSignatureRequired The bool to be set
     * @return {@code this}
     */
    public AccountUpdateTransaction setReceiverSignatureRequired(boolean receiverSignatureRequired) {
        requireNotFrozen();
        receiverSigRequired = receiverSignatureRequired;
        return this;
    }

    /**
     * Extract the maximum automatic token associations.
     *
     * @return                          the max automatic token associations
     */
    @Nullable
    public Integer getMaxAutomaticTokenAssociations() {
        return maxAutomaticTokenAssociations;
    }

    /**
     * A maximum number of tokens that can be auto-associated
     * with this account.<br/>
     * By default this value is 0 for all accounts except for automatically
     * created accounts (i.e smart contracts) which default to -1.
     * <p>
     * If this value is `0`, then this account MUST manually associate to
     * a token before holding or transacting in that token.<br/>
     * This value MAY also be `-1` to indicate no limit.<br/>
     * If set, this value MUST NOT be less than `-1`.<br/>
     *
     * @param amount                    the amount of tokens
     * @return                          {@code this}
     */
    public AccountUpdateTransaction setMaxAutomaticTokenAssociations(int amount) {
        requireNotFrozen();
        maxAutomaticTokenAssociations = amount;
        return this;
    }

    /**
     * Extract the account memo.
     *
     * @return                          the account memo
     */
    @Nullable
    public String getAccountMemo() {
        return accountMemo;
    }

    /**
     * A short description of this Account.
     * <p>
     * This value, if set, MUST NOT exceed `transaction.maxMemoUtf8Bytes`
     * (default 100) bytes when encoded as UTF-8.
     *
     * @param memo                      the memo
     * @return                          {@code this}
     */
    public AccountUpdateTransaction setAccountMemo(String memo) {
        requireNotFrozen();
        Objects.requireNonNull(memo);
        accountMemo = memo;
        return this;
    }

    /**
     * Erase the memo field.
     *
     * @return {@code this}
     */
    public AccountUpdateTransaction clearMemo() {
        requireNotFrozen();
        accountMemo = "";
        return this;
    }

    /**
     * ID of the account to which this account will stake
     *
     * @return ID of the account to which this account will stake.
     */
    @Nullable
    public AccountId getStakedAccountId() {
        return stakedAccountId;
    }

    /**
     * ID of the account to which this account is staking its balances.
     * <p>
     * If this account is not currently staking its balances, then this
     * field, if set, MUST be the sentinel value of `0.0.0`.
     *
     * @param stakedAccountId ID of the account to which this account will stake.
     * @return {@code this}
     */
    public AccountUpdateTransaction setStakedAccountId(@Nullable AccountId stakedAccountId) {
        requireNotFrozen();
        this.stakedAccountId = stakedAccountId;
        this.stakedNodeId = null;
        return this;
    }

    /**
     * Clear the staked account ID
     *
     * @return {@code this}
     */
    public AccountUpdateTransaction clearStakedAccountId() {
        requireNotFrozen();
        this.stakedAccountId = new AccountId(0, 0, 0);
        this.stakedNodeId = null;
        return this;
    }

    /**
     * The node to which this account will stake
     *
     * @return ID of the node this account will be staked to.
     */
    @Nullable
    public Long getStakedNodeId() {
        return stakedNodeId;
    }

    /**
     * ID of the node this account is staked to.
     * <p>
     * If this account is not currently staking its balances, then this
     * field, if set, SHALL be the sentinel value of `-1`.<br/>
     * Wallet software SHOULD surface staking issues to users and provide a
     * simple mechanism to update staking to a new node ID in the event the
     * prior staked node ID ceases to be valid.
     *
     * @param stakedNodeId ID of the node this account will be staked to.
     * @return {@code this}
     */
    public AccountUpdateTransaction setStakedNodeId(@Nullable Long stakedNodeId) {
        requireNotFrozen();
        this.stakedNodeId = stakedNodeId;
        this.stakedAccountId = null;
        return this;
    }

    /**
     * Clear the staked node
     *
     * @return {@code this}
     */
    public AccountUpdateTransaction clearStakedNodeId() {
        requireNotFrozen();
        this.stakedNodeId = -1L;
        this.stakedAccountId = null;
        return this;
    }

    /**
     * If true, the account declines receiving a staking reward. The default value is false.
     *
     * @return If true, the account declines receiving a staking reward. The default value is false.
     */
    @Nullable
    public Boolean getDeclineStakingReward() {
        return declineStakingReward;
    }

    /**
     * A boolean indicating that this account has chosen to decline rewards for
     * staking its balances.
     * <p>
     * This account MAY still stake its balances, but SHALL NOT receive reward
     * payments for doing so, if this value is set, and `true`.
     *
     * @param declineStakingReward - If true, the account declines receiving a staking reward. The default value is false.
     * @return {@code this}
     */
    public AccountUpdateTransaction setDeclineStakingReward(boolean declineStakingReward) {
        requireNotFrozen();
        this.declineStakingReward = declineStakingReward;
        return this;
    }

    /**
     * Clear decline staking reward
     *
     * @return {@code this}
     */
    public AccountUpdateTransaction clearDeclineStakingReward() {
        requireNotFrozen();
        this.declineStakingReward = null;
        return this;
    }

    /**
     * Add a hook to be created for the account.
     *
     * @param hookDetails the hook creation details to add
     * @return {@code this}
     */
    public AccountUpdateTransaction addHookToCreate(HookCreationDetails hookDetails) {
        requireNotFrozen();
        Objects.requireNonNull(hookDetails, "hookDetails cannot be null");
        this.hookCreationDetails.add(hookDetails);
        return this;
    }

    /**
     * Set hooks to be created with the account.
     *
     * @param hookDetails list of hook creation details
     * @return {@code this}
     */
    public AccountUpdateTransaction setHooksToCreate(List<HookCreationDetails> hookDetails) {
        requireNotFrozen();
        Objects.requireNonNull(hookDetails, "hookDetails cannot be null");
        this.hookCreationDetails = new ArrayList<>(hookDetails);
        return this;
    }

    /**
     * Mark a hook for deletion from the account.
     *
     * @param hookId the hook id to delete
     * @return {@code this}
     */
    public AccountUpdateTransaction addHookToDelete(Long hookId) {
        requireNotFrozen();
        Objects.requireNonNull(hookId, "hookId cannot be null");
        this.hookIdsToDelete.add(hookId);
        return this;
    }

    /**
     * Mark hooks for deletion from the account.
     *
     * @param hookIds list of hook ids to delete
     * @return {@code this}
     */
    public AccountUpdateTransaction setHooksToDelete(List<Long> hookIds) {
        requireNotFrozen();
        Objects.requireNonNull(hookIds, "hookIds cannot be null");
        this.hookIdsToDelete = new ArrayList<>(hookIds);
        return this;
    }

    /**
     * Get the list of hooks to be created.
     *
     * @return a copy of the hook creation details list
     */
    public List<HookCreationDetails> getHooksToCreate() {
        return new ArrayList<>(hookCreationDetails);
    }

    /**
     * Get the list of hook IDs to be deleted.
     *
     * @return a copy of the hook IDs list
     */
    public List<Long> getHooksToDelete() {
        return new ArrayList<>(hookIdsToDelete);
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (accountId != null) {
            accountId.validateChecksum(client);
        }
        if (proxyAccountId != null) {
            proxyAccountId.validateChecksum(client);
        }

        if (stakedAccountId != null) {
            stakedAccountId.validateChecksum(client);
        }
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getCryptoUpdateAccount();

        if (body.hasAccountIDToUpdate()) {
            accountId = AccountId.fromProtobuf(body.getAccountIDToUpdate());
        }
        if (body.hasProxyAccountID()) {
            proxyAccountId = AccountId.fromProtobuf(body.getProxyAccountID());
        }
        if (body.hasKey()) {
            key = Key.fromProtobufKey(body.getKey());
        }
        if (body.hasExpirationTime()) {
            expirationTime = InstantConverter.fromProtobuf(body.getExpirationTime());
        }
        if (body.hasAutoRenewPeriod()) {
            autoRenewPeriod = DurationConverter.fromProtobuf(body.getAutoRenewPeriod());
        }
        if (body.hasReceiverSigRequiredWrapper()) {
            receiverSigRequired = body.getReceiverSigRequiredWrapper().getValue();
        }
        if (body.hasMemo()) {
            accountMemo = body.getMemo().getValue();
        }
        if (body.hasMaxAutomaticTokenAssociations()) {
            maxAutomaticTokenAssociations =
                    body.getMaxAutomaticTokenAssociations().getValue();
        }

        if (body.hasDeclineReward()) {
            declineStakingReward = body.getDeclineReward().getValue();
        }

        if (body.hasStakedAccountId()) {
            stakedAccountId = AccountId.fromProtobuf(body.getStakedAccountId());
        }

        if (body.hasStakedNodeId()) {
            stakedNodeId = body.getStakedNodeId();
        }

        // Initialize hook create/delete details
        hookCreationDetails.clear();
        for (var protoHookDetails : body.getHookCreationDetailsList()) {
            hookCreationDetails.add(HookCreationDetails.fromProtobuf(protoHookDetails));
        }

        hookIdsToDelete.clear();
        hookIdsToDelete.addAll(body.getHookIdsToDeleteList());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return CryptoServiceGrpc.getUpdateAccountMethod();
    }

    /**
     * Create the builder.
     *
     * @return                          the transaction builder
     */
    CryptoUpdateTransactionBody.Builder build() {
        var builder = CryptoUpdateTransactionBody.newBuilder();
        if (accountId != null) {
            builder.setAccountIDToUpdate(accountId.toProtobuf());
        }
        if (proxyAccountId != null) {
            builder.setProxyAccountID(proxyAccountId.toProtobuf());
        }
        if (key != null) {
            builder.setKey(key.toProtobufKey());
        }
        if (expirationTime != null) {
            builder.setExpirationTime(InstantConverter.toProtobuf(expirationTime));
        }
        if (expirationTimeDuration != null) {
            builder.setExpirationTime(InstantConverter.toProtobuf(expirationTimeDuration));
        }
        if (autoRenewPeriod != null) {
            builder.setAutoRenewPeriod(DurationConverter.toProtobuf(autoRenewPeriod));
        }
        if (receiverSigRequired != null) {
            builder.setReceiverSigRequiredWrapper(BoolValue.of(receiverSigRequired));
        }
        if (accountMemo != null) {
            builder.setMemo(StringValue.of(accountMemo));
        }
        if (maxAutomaticTokenAssociations != null) {
            builder.setMaxAutomaticTokenAssociations(Int32Value.of(maxAutomaticTokenAssociations));
        }

        if (stakedAccountId != null) {
            builder.setStakedAccountId(stakedAccountId.toProtobuf());
        } else if (stakedNodeId != null) {
            builder.setStakedNodeId(stakedNodeId);
        }

        if (declineStakingReward != null) {
            builder.setDeclineReward(
                    BoolValue.newBuilder().setValue(declineStakingReward).build());
        }

        for (HookCreationDetails hookDetails : hookCreationDetails) {
            builder.addHookCreationDetails(hookDetails.toProtobuf());
        }

        if (!hookIdsToDelete.isEmpty()) {
            builder.addAllHookIdsToDelete(hookIdsToDelete);
        }

        return builder;
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setCryptoUpdateAccount(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setCryptoUpdateAccount(build());
    }
}
