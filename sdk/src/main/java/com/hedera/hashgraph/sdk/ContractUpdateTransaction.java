// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.hedera.hashgraph.sdk.proto.*;
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
 * Modify a smart contract instance to have the given parameter values.
 * <p>
 * Any null field is ignored (left unchanged).
 * <p>
 * If only the contractInstanceExpirationTime is being modified, then no signature is
 * needed on this transaction other than for the account paying for the transaction itself.
 * <p>
 * But if any of the other fields are being modified, then it must be signed by the adminKey.
 * <p>
 * The use of adminKey is not currently supported in this API, but in the future will
 * be implemented to allow these fields to be modified, and also to make modifications
 * to the state of the instance.
 * <p>
 * If the contract is created with no admin key, then none of the fields can be
 * changed that need an admin signature, and therefore no admin key can ever be added.
 * So if there is no admin key, then things like the bytecode are immutable.
 * But if there is an admin key, then they can be changed. For example, the
 * admin key might be a threshold key, which requires 3 of 5 binding arbitration judges to
 * agree before the bytecode can be changed. This can be used to add flexibility to the management
 * of smart contract behavior. But this is optional. If the smart contract is created
 * without an admin key, then such a key can never be added, and its bytecode will be immutable.
 */
public final class ContractUpdateTransaction extends Transaction<ContractUpdateTransaction> {
    @Nullable
    private ContractId contractId = null;

    @Nullable
    private AccountId proxyAccountId = null;

    @Nullable
    private FileId bytecodeFileId = null;

    @Nullable
    private Instant expirationTime = null;

    private Duration expirationTimeDuration = null;

    @Nullable
    private Key adminKey = null;

    @Nullable
    private Integer maxAutomaticTokenAssociations = null;

    @Nullable
    private Duration autoRenewPeriod = null;

    @Nullable
    private String contractMemo = null;

    @Nullable
    private AccountId stakedAccountId = null;

    @Nullable
    private Long stakedNodeId = null;

    @Nullable
    private Boolean declineStakingReward = null;

    @Nullable
    private AccountId autoRenewAccountId = null;

    private List<Long> hookIdsToDelete = new ArrayList<>();

    private List<HookCreationDetails> hookCreationDetails = new ArrayList<>();

    /**
     * Contract.
     */
    public ContractUpdateTransaction() {}

    /**
     * Contract.
     *
     * @param txs                       Compound list of transaction id's list of (AccountId, Transaction) record
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    ContractUpdateTransaction(
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
    ContractUpdateTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the contract id.
     *
     * @return                          the contract id
     */
    @Nullable
    public ContractId getContractId() {
        return contractId;
    }

    /**
     * Sets the Contract ID instance to update.
     *
     * @param contractId The ContractId to be set
     * @return {@code this}
     */
    public ContractUpdateTransaction setContractId(ContractId contractId) {
        Objects.requireNonNull(contractId);
        requireNotFrozen();
        this.contractId = contractId;
        return this;
    }

    /**
     * Extract the contract expiration time.
     *
     * @return                          the contract expiration time
     */
    @Nullable
    public Instant getExpirationTime() {
        return expirationTime;
    }

    /**
     * If set, modify the time at which this contract will expire.<br/>
     * An expired contract requires a rent payment to "renew" the contract.
     * A transaction to update this field is how that rent payment is made.
     * <p>
     * This value MUST NOT be less than the current `expirationTime`
     * of the contract. If this value is earlier than the current
     * value, the transaction SHALL fail with response
     * code `EXPIRATION_REDUCTION_NOT_ALLOWED`.
     *
     * @param expirationTime The Instant to be set for expiration time
     * @return {@code this}
     */
    public ContractUpdateTransaction setExpirationTime(Instant expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        this.expirationTime = expirationTime;
        this.expirationTimeDuration = null;
        return this;
    }

    public ContractUpdateTransaction setExpirationTime(Duration expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        this.expirationTime = null;
        this.expirationTimeDuration = expirationTime;
        return this;
    }

    /**
     * Extract the administrator key.
     *
     * @return                          the administrator key
     */
    @Nullable
    public Key getAdminKey() {
        return adminKey;
    }

    /**
     * If set, modify the key that authorizes updates to the contract.
     * <p>
     * If this field is set to a valid Key, this key and the previously set key
     * MUST both sign this transaction.<br/>
     * If this value is an empty `KeyList`, the prior key MUST sign this
     * transaction, and the smart contract SHALL be immutable after this
     * transaction completes, except for expiration and renewal.<br/>
     * If this value is not an empty `KeyList`, but does not contain any
     * cryptographic keys, or is otherwise malformed, this transaction SHALL
     * fail with response code `INVALID_ADMIN_KEY`.
     *
     * @param adminKey The Key to be set
     * @return {@code this}
     */
    public ContractUpdateTransaction setAdminKey(Key adminKey) {
        Objects.requireNonNull(adminKey);
        requireNotFrozen();
        this.adminKey = adminKey;
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
     * that isn't a node, then this account is automatically proxy staked to a
     * node chosen by the network, but without earning payments.
     * <p>
     * If the proxyAccountID account refuses to accept proxy staking, or if it is
     * not currently running a node, then it will behave as if proxyAccountID was null.
     *
     * @param proxyAccountId The AccountId to be set
     * @return {@code this}
     */
    public ContractUpdateTransaction setProxyAccountId(AccountId proxyAccountId) {
        Objects.requireNonNull(proxyAccountId);
        requireNotFrozen();
        this.proxyAccountId = proxyAccountId;
        return this;
    }

    /**
     * Extract the auto renew period.
     *
     * @return                          the auto renew period
     */
    @Nullable
    public Integer getMaxAutomaticTokenAssociations() {
        return maxAutomaticTokenAssociations;
    }

    /**
     * If set, modify the maximum number of tokens that can be auto-associated with the
     * contract.
     * <p>
     * If this is set and less than or equal to `used_auto_associations`, or 0, then this contract
     * MUST manually associate with a token before transacting in that token.<br/>
     * This value MAY also be `-1` to indicate no limit.<br/>
     * This value MUST NOT be less than `-1`.
     *
     * @param maxAutomaticTokenAssociations The maximum automatic token associations
     * @return  {@code this}
     */
    public ContractUpdateTransaction setMaxAutomaticTokenAssociations(int maxAutomaticTokenAssociations) {
        requireNotFrozen();
        this.maxAutomaticTokenAssociations = maxAutomaticTokenAssociations;
        return this;
    }

    /**
     * Extract the duration for the auto renew period.
     *
     * @return                          the duration for auto-renew
     */
    @Nullable
    public Duration getAutoRenewPeriod() {
        return autoRenewPeriod;
    }

    /**
     * If set, modify the duration added to expiration time by each
     * auto-renewal to this value.
     *
     * @param autoRenewPeriod The Duration to be set for auto-renewal
     * @return {@code this}
     */
    public ContractUpdateTransaction setAutoRenewPeriod(Duration autoRenewPeriod) {
        Objects.requireNonNull(autoRenewPeriod);
        requireNotFrozen();
        this.autoRenewPeriod = autoRenewPeriod;
        return this;
    }

    /**
     * @deprecated with no replacement
     * @return the bytecodeFileId
     */
    @Nullable
    @Deprecated
    public FileId getBytecodeFileId() {
        return bytecodeFileId;
    }

    /**
     * @deprecated with no replacement
     *
     * Sets the file ID of file containing the smart contract byte code.
     * <p>
     * A copy will be made and held by the contract instance, and have the same expiration
     * time as the instance.
     *
     * @param bytecodeFileId The FileId to be set
     * @return {@code this}
     */
    @Deprecated
    public ContractUpdateTransaction setBytecodeFileId(FileId bytecodeFileId) {
        Objects.requireNonNull(bytecodeFileId);
        requireNotFrozen();
        this.bytecodeFileId = bytecodeFileId;
        return this;
    }

    /**
     * Extract the contents of the memo.
     *
     * @return                          the contents of the memo
     */
    @Nullable
    public String getContractMemo() {
        return contractMemo;
    }

    /**
     * Sets the memo associated with the contract (max: 100 bytes).
     *
     * @param memo The memo to be set
     * @return {@code this}
     */
    public ContractUpdateTransaction setContractMemo(String memo) {
        Objects.requireNonNull(memo);
        requireNotFrozen();
        contractMemo = memo;
        return this;
    }

    /**
     * Remove the memo contents.
     *
     * @return {@code this}
     */
    public ContractUpdateTransaction clearMemo() {
        requireNotFrozen();
        contractMemo = "";
        return this;
    }

    /**
     * ID of the account to which this contract will stake
     *
     * @return ID of the account to which this contract will stake.
     */
    @Nullable
    public AccountId getStakedAccountId() {
        return stakedAccountId;
    }

    /**
     * An account identifier.<br/>
     * A staked account acts as a proxy, and this contract effectively
     * nominates the same node as the identified account.
     * <p>
     * If set, modify this smart contract such that it SHALL stake its HBAR
     * to the same node as the identified account.<br/>
     * If this field is set to a default AccountID value (`0.0.0`), any
     * pre-existing `staked_account_id` value SHALL be removed on success.
     *
     * @param stakedAccountId ID of the account to which this contract will stake.
     * @return {@code this}
     */
    public ContractUpdateTransaction setStakedAccountId(@Nullable AccountId stakedAccountId) {
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
    public ContractUpdateTransaction clearStakedAccountId() {
        requireNotFrozen();
        this.stakedAccountId = new AccountId(0, 0, 0);
        this.stakedNodeId = null;
        return this;
    }

    /**
     * The node to which this contract will stake
     *
     * @return ID of the node this contract will be staked to.
     */
    @Nullable
    public Long getStakedNodeId() {
        return stakedNodeId;
    }

    /**
     * A node identifier.<br/>
     * A staked node identifier indicates the consensus node that this
     * account nominates for staking.
     * <p>
     * If set, modify this smart contract such that it SHALL stake its HBAR
     * to this node.
     * If set to the value `-1` any pre-existing `staked_node_id` value
     * SHALL be removed on success.
     * <p>
     * <blockquote>Note: node IDs do fluctuate as node operators change.
     * Most contracts are immutable, and a contract staking to an invalid
     * node ID SHALL NOT participate in staking. Immutable contracts may
     * find it more reliable to use a proxy account for staking (via
     * `staked_account_id`) to enable updating the _effective_ staking node
     * ID when necessary through updating the proxy account.</blockquote>
     *
     * @param stakedNodeId ID of the node this contract will be staked to.
     * @return {@code this}
     */
    public ContractUpdateTransaction setStakedNodeId(@Nullable Long stakedNodeId) {
        requireNotFrozen();
        this.stakedNodeId = stakedNodeId;
        this.stakedAccountId = null;
        return this;
    }

    /**
     * clear the staked node account ID
     *
     * @return {@code this}
     */
    public ContractUpdateTransaction clearStakedNodeId() {
        requireNotFrozen();
        this.stakedNodeId = -1L;
        this.stakedAccountId = null;
        return this;
    }

    /**
     * If true, the contract declines receiving a staking reward. The default value is false.
     *
     * @return If true, the contract declines receiving a staking reward. The default value is false.
     */
    @Nullable
    public Boolean getDeclineStakingReward() {
        return declineStakingReward;
    }

    /**
     * A flag indicating if staking rewards are declined.<br/>
     * If set, modify the flag indicating if this contract declines to accept
     * rewards for staking its HBAR to secure the network.
     * <p>
     * If set to true, this smart contract SHALL NOT receive any reward for
     * staking its HBAR balance to help secure the network, regardless of
     * staking configuration, but MAY stake HBAR to support the network
     * without reward.
     *
     * @param declineStakingReward - If true, the contract declines receiving a staking reward. The default value is false.
     * @return {@code this}
     */
    public ContractUpdateTransaction setDeclineStakingReward(boolean declineStakingReward) {
        requireNotFrozen();
        this.declineStakingReward = declineStakingReward;
        return this;
    }

    /**
     * Clear decline staking reward
     *
     * @return {@code this}
     */
    public ContractUpdateTransaction clearDeclineStakingReward() {
        requireNotFrozen();
        this.declineStakingReward = null;
        return this;
    }

    /**
     * Get the auto renew accountId.
     *
     * @return                          the auto renew accountId
     */
    @Nullable
    public AccountId getAutoRenewAccountId() {
        return autoRenewAccountId;
    }

    /**
     * If set, modify the account, in the same shard and realm as this smart
     * contract, that has agreed to allow the network to use its balance, when
     * needed, to automatically extend this contract's expiration time.
     * <p>
     * If this field is set to a non-default value, that Account MUST sign this
     * transaction.<br/>
     * If this field is set to a default AccountID value (`0.0.0`), any
     * pre-existing `auto_renew_account_id` value SHALL be removed on success.
     *
     * @param autoRenewAccountId The AccountId to be set for auto-renewal
     * @return {@code this}
     */
    public ContractUpdateTransaction setAutoRenewAccountId(AccountId autoRenewAccountId) {
        Objects.requireNonNull(autoRenewAccountId);
        requireNotFrozen();
        this.autoRenewAccountId = autoRenewAccountId;
        return this;
    }

    /**
     * Clears the auto-renew account ID
     *
     * @return {@code this}
     */
    public ContractUpdateTransaction clearAutoRenewAccountId() {
        this.autoRenewAccountId = new AccountId(0, 0, 0);
        return this;
    }

    /**
     * Add a hook to be created for the contract.
     *
     * @param hookDetails the hook creation details to add
     * @return {@code this}
     */
    public ContractUpdateTransaction addHookToCreate(HookCreationDetails hookDetails) {
        requireNotFrozen();
        Objects.requireNonNull(hookDetails, "hookDetails cannot be null");
        this.hookCreationDetails.add(hookDetails);
        return this;
    }

    /**
     * Set hooks to be created with the contract.
     *
     * @param hookDetails list of hook creation details
     * @return {@code this}
     */
    public ContractUpdateTransaction setHooksToCreate(List<HookCreationDetails> hookDetails) {
        requireNotFrozen();
        Objects.requireNonNull(hookDetails, "hookDetails cannot be null");
        this.hookCreationDetails = new ArrayList<>(hookDetails);
        return this;
    }

    /**
     * Mark a hook for deletion from the contract.
     *
     * @param hookId the hook id to delete
     * @return {@code this}
     */
    public ContractUpdateTransaction addHookToDelete(Long hookId) {
        requireNotFrozen();
        Objects.requireNonNull(hookId, "hookId cannot be null");
        this.hookIdsToDelete.add(hookId);
        return this;
    }

    /**
     * Mark hooks for deletion from the contract.
     *
     * @param hookIds list of hook ids to delete
     * @return {@code this}
     */
    public ContractUpdateTransaction setHooksToDelete(List<Long> hookIds) {
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

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getContractUpdateInstance();
        if (body.hasContractID()) {
            contractId = ContractId.fromProtobuf(body.getContractID());
        }
        if (body.hasProxyAccountID()) {
            proxyAccountId = AccountId.fromProtobuf(body.getProxyAccountID());
        }
        if (body.hasExpirationTime()) {
            expirationTime = InstantConverter.fromProtobuf(body.getExpirationTime());
        }
        if (body.hasAdminKey()) {
            adminKey = Key.fromProtobufKey(body.getAdminKey());
        }
        if (body.hasMaxAutomaticTokenAssociations()) {
            maxAutomaticTokenAssociations =
                    body.getMaxAutomaticTokenAssociations().getValue();
        }
        if (body.hasAutoRenewPeriod()) {
            autoRenewPeriod = DurationConverter.fromProtobuf(body.getAutoRenewPeriod());
        }
        if (body.hasMemoWrapper()) {
            contractMemo = body.getMemoWrapper().getValue();
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

        if (body.hasAutoRenewAccountId()) {
            autoRenewAccountId = AccountId.fromProtobuf(body.getAutoRenewAccountId());
        }

        hookCreationDetails.clear();
        for (var protoHookDetails : body.getHookCreationDetailsList()) {
            hookCreationDetails.add(HookCreationDetails.fromProtobuf(protoHookDetails));
        }

        hookIdsToDelete.clear();
        hookIdsToDelete.addAll(body.getHookIdsToDeleteList());
    }

    /**
     * Build the correct transaction body.
     *
     * @return {@link com.hedera.hashgraph.sdk.proto.ContractUpdateTransactionBody builder }
     */
    ContractUpdateTransactionBody.Builder build() {
        var builder = ContractUpdateTransactionBody.newBuilder();
        if (contractId != null) {
            builder.setContractID(contractId.toProtobuf());
        }
        if (proxyAccountId != null) {
            builder.setProxyAccountID(proxyAccountId.toProtobuf());
        }
        if (expirationTime != null) {
            builder.setExpirationTime(InstantConverter.toProtobuf(expirationTime));
        }
        if (expirationTimeDuration != null) {
            builder.setExpirationTime(InstantConverter.toProtobuf(expirationTimeDuration));
        }
        if (adminKey != null) {
            builder.setAdminKey(adminKey.toProtobufKey());
        }
        if (maxAutomaticTokenAssociations != null) {
            builder.setMaxAutomaticTokenAssociations(Int32Value.of(maxAutomaticTokenAssociations));
        }
        if (autoRenewPeriod != null) {
            builder.setAutoRenewPeriod(DurationConverter.toProtobuf(autoRenewPeriod));
        }
        if (contractMemo != null) {
            builder.setMemoWrapper(StringValue.of(contractMemo));
        }

        if (stakedAccountId != null) {
            builder.setStakedAccountId(stakedAccountId.toProtobuf());
        }

        if (stakedNodeId != null) {
            builder.setStakedNodeId(stakedNodeId);
        }

        if (declineStakingReward != null) {
            builder.setDeclineReward(
                    BoolValue.newBuilder().setValue(declineStakingReward).build());
        }

        if (autoRenewAccountId != null) {
            if (autoRenewAccountId.toString().equals("0.0.0")) {
                builder.setAutoRenewAccountId(AccountID.getDefaultInstance());
            } else {
                builder.setAutoRenewAccountId(autoRenewAccountId.toProtobuf());
            }
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
    void validateChecksums(Client client) throws BadEntityIdException {
        if (contractId != null) {
            contractId.validateChecksum(client);
        }

        if (proxyAccountId != null) {
            proxyAccountId.validateChecksum(client);
        }

        if (stakedAccountId != null) {
            stakedAccountId.validateChecksum(client);
        }

        if (autoRenewAccountId != null) {
            autoRenewAccountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return SmartContractServiceGrpc.getUpdateContractMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setContractUpdateInstance(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setContractUpdateInstance(build());
    }
}
