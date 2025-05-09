// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.CryptoCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;

/*
 * Create a new Hedera™ account.
 *
 * If the auto_renew_account field is set, the key of the referenced account
 * MUST sign this transaction.
 * Current limitations REQUIRE that `shardID` and `realmID` both MUST be `0`.
 * This is expected to change in the future.
 *
 * ### Block Stream Effects
 * The newly created account SHALL be included in State Changes.
 */

public final class AccountCreateTransaction extends Transaction<AccountCreateTransaction> {
    @Nullable
    private AccountId proxyAccountId = null;

    @Nullable
    private Key key = null;

    private String accountMemo = "";
    private Hbar initialBalance = new Hbar(0);
    private boolean receiverSigRequired = false;
    private Duration autoRenewPeriod = DEFAULT_AUTO_RENEW_PERIOD;
    private int maxAutomaticTokenAssociations = 0;

    @Nullable
    private AccountId stakedAccountId = null;

    @Nullable
    private Long stakedNodeId = null;

    private boolean declineStakingReward = false;

    @Nullable
    private EvmAddress alias = null;

    /**
     * Constructor.
     */
    public AccountCreateTransaction() {
        defaultMaxTransactionFee = Hbar.from(5);
    }

    /**
     * Constructor.
     *
     * @param txs                                   Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    AccountCreateTransaction(
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
    AccountCreateTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the key.
     *
     * @return                          the creating account's key
     */
    @Nullable
    public Key getKey() {
        return key;
    }

    /**
     * The identifying key for this account.
     * This key represents the account owner, and is required for most actions
     * involving this account that do not modify the account itself. This key
     * may also identify the account for smart contracts.
     * <p>
     * This field is REQUIRED.
     * This `Key` MUST NOT be an empty `KeyList` and MUST contain at least one
     * "primitive" (i.e. cryptographic) key value.
     *
     * @param key the key for this account.
     * @return {@code this}
     */
    @Deprecated
    public AccountCreateTransaction setKey(Key key) {
        Objects.requireNonNull(key);
        requireNotFrozen();
        this.key = key;
        return this;
    }

    /**
     * Sets ECDSA private key, derives and sets it's EVM address in the background. Essentially does
     * {@link AccountCreateTransaction#setKey(Key)} +
     * {@link AccountCreateTransaction#setAlias(EvmAddress)}
     *
     * @param key
     * @return this
     */
    public AccountCreateTransaction setKeyWithAlias(Key key) {
        Objects.requireNonNull(key);
        requireNotFrozen();
        this.key = key;
        this.alias = extractAlias(key);
        return this;
    }

    /**
     * Sets the account key and a separate ECDSA key that the EVM address is derived from.
     * A user must sign the transaction with both keys for this flow to be successful.
     *
     * @param key
     * @param ecdsaKey
     * @return this
     */
    public AccountCreateTransaction setKeyWithAlias(Key key, Key ecdsaKey) {
        Objects.requireNonNull(key);
        requireNotFrozen();
        this.key = key;
        this.alias = extractAlias(ecdsaKey);
        return this;
    }

    /**
     * Sets key where it is explicitly called out that the alias is not set
     * @param key
     * @return this
     */
    public AccountCreateTransaction setKeyWithoutAlias(Key key) {
        Objects.requireNonNull(key);
        requireNotFrozen();
        this.key = key;
        return this;
    }

    /**
     * Extract the amount in hbar.
     *
     * @return                          the initial balance for the new account
     */
    public Hbar getInitialBalance() {
        return initialBalance;
    }

    /**
     * An amount, in tinybar, to deposit to the newly created account.
     * <p>
     * The deposited amount SHALL be debited to the "payer" account for this
     * transaction.
     *
     * @param initialBalance the initial balance.
     * @return {@code this}
     */
    public AccountCreateTransaction setInitialBalance(Hbar initialBalance) {
        Objects.requireNonNull(initialBalance);
        requireNotFrozen();
        this.initialBalance = initialBalance;
        return this;
    }

    /**
     * Is the receiver required to sign?
     *
     * @return                          is the receiver required to sign
     */
    public boolean getReceiverSignatureRequired() {
        return receiverSigRequired;
    }

    /**
     * A flag indicating the account holder must authorize all incoming
     * token transfers.
     * <p>
     * If this flag is set then any transaction that would result in adding
     * hbar or other tokens to this account balance MUST be signed by the
     * identifying key of this account (that is, the `key` field).<br/>
     * If this flag is set, then the account key (`key` field) MUST sign
     * this create transaction, in addition to the transaction payer.
     *
     * @param receiveSignatureRequired true to require a signature when receiving hbars.
     * @return {@code this}
     */
    public AccountCreateTransaction setReceiverSignatureRequired(boolean receiveSignatureRequired) {
        requireNotFrozen();
        receiverSigRequired = receiveSignatureRequired;
        return this;
    }

    /**
     * @deprecated with no replacement
     *
     * Extract the proxy account id.
     *
     * @return                          the proxy account id
     */
    @Nullable
    @Deprecated
    public AccountId getProxyAccountId() {
        return proxyAccountId;
    }

    /**
     * @deprecated with no replacement
     *
     * Set the ID of the account to which this account is proxy staked.
     *
     * Use `staked_id` instead.<br/>
     * An account identifier for a staking proxy.
     *
     * @param proxyAccountId the proxy account ID.
     * @return {@code this}
     */
    @Deprecated
    public AccountCreateTransaction setProxyAccountId(AccountId proxyAccountId) {
        requireNotFrozen();
        Objects.requireNonNull(proxyAccountId);
        this.proxyAccountId = proxyAccountId;
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
     * Set the auto renew period for this account.
     *
     * <p>A Hedera™ account is charged to extend its expiration date every renew period. If it
     * doesn't have enough balance, it extends as long as possible. If the balance is zero when it
     * expires, then the account is deleted.
     *
     * <p>This is defaulted to 3 months by the SDK.
     *
     * @param autoRenewPeriod the auto renew period for this account.
     * @return {@code this}
     */
    public AccountCreateTransaction setAutoRenewPeriod(Duration autoRenewPeriod) {
        requireNotFrozen();
        Objects.requireNonNull(autoRenewPeriod);
        this.autoRenewPeriod = autoRenewPeriod;
        return this;
    }

    /**
     * Extract the maximum automatic token associations.
     *
     * @return                          the max automatic token associations
     */
    public int getMaxAutomaticTokenAssociations() {
        return maxAutomaticTokenAssociations;
    }

    /**
     * A maximum number of tokens that can be auto-associated
     * with this account.<br/>
     * By default this value is 0 for all accounts except for automatically
     * created accounts (e.g. smart contracts), which default to -1.
     * <p>
     * If this value is `0`, then this account MUST manually associate to
     * a token before holding or transacting in that token.<br/>
     * This value MAY also be `-1` to indicate no limit.<br/>
     * This value MUST NOT be less than `-1`.
     *
     * @param amount                    the amount of tokens
     * @return                          {@code this}
     */
    public AccountCreateTransaction setMaxAutomaticTokenAssociations(int amount) {
        requireNotFrozen();
        maxAutomaticTokenAssociations = amount;
        return this;
    }

    /**
     * Extract the account memo.
     *
     * @return                          the account memo
     */
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
    public AccountCreateTransaction setAccountMemo(String memo) {
        Objects.requireNonNull(memo);
        requireNotFrozen();
        accountMemo = memo;
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
     * Set the account to which this account will stake
     *
     * @param stakedAccountId ID of the account to which this account will stake.
     * @return {@code this}
     */
    public AccountCreateTransaction setStakedAccountId(@Nullable AccountId stakedAccountId) {
        requireNotFrozen();
        this.stakedAccountId = stakedAccountId;
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
    public AccountCreateTransaction setStakedNodeId(@Nullable Long stakedNodeId) {
        requireNotFrozen();
        this.stakedNodeId = stakedNodeId;
        this.stakedAccountId = null;
        return this;
    }

    /**
     * If true, the account declines receiving a staking reward. The default value is false.
     *
     * @return If true, the account declines receiving a staking reward. The default value is false.
     */
    public boolean getDeclineStakingReward() {
        return declineStakingReward;
    }

    /**
     * If true, the account declines receiving a staking reward. The default value is false.
     *
     * @param declineStakingReward - If true, the account declines receiving a staking reward. The default value is false.
     * @return {@code this}
     */
    public AccountCreateTransaction setDeclineStakingReward(boolean declineStakingReward) {
        requireNotFrozen();
        this.declineStakingReward = declineStakingReward;
        return this;
    }

    /**
     * The bytes to be used as the account's alias.
     * <p>
     * The bytes must be formatted as the calcluated last 20 bytes of the
     * keccak-256 of the ECDSA primitive key.
     * <p>
     * All other types of keys, including but not limited to ED25519, ThresholdKey, KeyList, ContractID, and
     * delegatable_contract_id, are not supported.
     * <p>
     * At most only one account can ever have a given alias on the network.
     */
    @Nullable
    public EvmAddress getAlias() {
        return alias;
    }

    /**
     * The bytes to be used as the account's alias.
     * <p>
     * The bytes must be formatted as the calcluated last 20 bytes of the
     * keccak-256 of the ECDSA primitive key.
     * <p>
     * All other types of keys, including but not limited to ED25519, ThresholdKey, KeyList, ContractID, and
     * delegatable_contract_id, are not supported.
     * <p>
     * At most only one account can ever have a given alias on the network.
     *
     * @param alias The ethereum account 20-byte EVM address
     * @return {@code this}
     */
    public AccountCreateTransaction setAlias(EvmAddress alias) {
        requireNotFrozen();
        this.alias = alias;
        return this;
    }

    /**
     * The ethereum account 20-byte EVM address to be used as the account's alias. This EVM address may be either
     * the encoded form of the shard.realm.num or the keccak-256 hash of a ECDSA_SECP256K1 primitive key.
     * <p>
     * A given alias can map to at most one account on the network at a time. This uniqueness will be enforced
     * relative to aliases currently on the network at alias assignment.
     * <p>
     * If a transaction creates an account using an alias, any further crypto transfers to that alias will
     * simply be deposited in that account, without creating anything, and with no creation fee being charged.
     *
     * @param aliasEvmAddress The ethereum account 20-byte EVM address
     * @return {@code this}
     * @throws IllegalArgumentException when evmAddress is invalid
     */
    public AccountCreateTransaction setAlias(String aliasEvmAddress) {
        if ((aliasEvmAddress.startsWith("0x") && aliasEvmAddress.length() == 42) || aliasEvmAddress.length() == 40) {
            EvmAddress address = EvmAddress.fromString(aliasEvmAddress);
            return this.setAlias(address);
        } else {
            throw new IllegalArgumentException("evmAddress must be an a valid EVM address with \"0x\" prefix");
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link com.hedera.hashgraph.sdk.proto.CryptoCreateTransactionBody}
     */
    CryptoCreateTransactionBody.Builder build() {
        var builder = CryptoCreateTransactionBody.newBuilder()
                .setInitialBalance(initialBalance.toTinybars())
                .setReceiverSigRequired(receiverSigRequired)
                .setAutoRenewPeriod(DurationConverter.toProtobuf(autoRenewPeriod))
                .setMemo(accountMemo)
                .setMaxAutomaticTokenAssociations(maxAutomaticTokenAssociations)
                .setDeclineReward(declineStakingReward);

        if (proxyAccountId != null) {
            builder.setProxyAccountID(proxyAccountId.toProtobuf());
        }

        if (key != null) {
            builder.setKey(key.toProtobufKey());
        }

        if (alias != null) {
            builder.setAlias(ByteString.copyFrom(alias.toBytes()));
        }

        if (stakedAccountId != null) {
            builder.setStakedAccountId(stakedAccountId.toProtobuf());
        } else if (stakedNodeId != null) {
            builder.setStakedNodeId(stakedNodeId);
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
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
        var body = sourceTransactionBody.getCryptoCreateAccount();

        if (body.hasProxyAccountID()) {
            proxyAccountId = AccountId.fromProtobuf(body.getProxyAccountID());
        }
        if (body.hasKey()) {
            key = Key.fromProtobufKey(body.getKey());
        }
        if (body.hasAutoRenewPeriod()) {
            autoRenewPeriod = DurationConverter.fromProtobuf(body.getAutoRenewPeriod());
        }
        initialBalance = Hbar.fromTinybars(body.getInitialBalance());
        accountMemo = body.getMemo();
        receiverSigRequired = body.getReceiverSigRequired();
        maxAutomaticTokenAssociations = body.getMaxAutomaticTokenAssociations();
        declineStakingReward = body.getDeclineReward();

        if (body.hasStakedAccountId()) {
            stakedAccountId = AccountId.fromProtobuf(body.getStakedAccountId());
        }

        if (body.hasStakedNodeId()) {
            stakedNodeId = body.getStakedNodeId();
        }

        alias = EvmAddress.fromAliasBytes(body.getAlias());
    }

    private EvmAddress extractAlias(Key key) {
        var isPrivateEcdsaKey = key instanceof PrivateKeyECDSA;
        var isPublicEcdsaKey = key instanceof PublicKeyECDSA;
        if (isPrivateEcdsaKey) {
            return ((PrivateKeyECDSA) key).getPublicKey().toEvmAddress();
        } else if (isPublicEcdsaKey) {
            return ((PublicKeyECDSA) key).toEvmAddress();
        } else {
            throw new BadKeyException("Private key is not ECDSA");
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return CryptoServiceGrpc.getCreateAccountMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setCryptoCreateAccount(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setCryptoCreateAccount(build());
    }
}
