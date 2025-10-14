// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ContractCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.SmartContractServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.bouncycastle.util.Arrays;

/**
 * Start a new smart contract instance.
 * After the instance is created,
 * the ContractID for it is in the receipt.
 * <p>
 * The instance will exist for autoRenewPeriod seconds. When that is reached, it will renew itself for another
 * autoRenewPeriod seconds by charging its associated cryptocurrency account (which it creates here).
 * If it has insufficient cryptocurrency to extend that long, it will extend as long as it can.
 * If its balance is zero, the instance will be deleted.
 * <p>
 * A smart contract instance normally enforces rules, so "the code is law". For example, an
 * ERC-20 contract prevents a transfer from being undone without a signature by the recipient of the transfer.
 * This is always enforced if the contract instance was created with the adminKeys being null.
 * But for some uses, it might be desirable to create something like an ERC-20 contract that has a
 * specific group of trusted individuals who can act as a "supreme court" with the ability to override the normal
 * operation, when a sufficient number of them agree to do so. If adminKeys is not null, then they can
 * sign a transaction that can change the state of the smart contract in arbitrary ways, such as to reverse
 * a transaction that violates some standard of behavior that is not covered by the code itself.
 * The admin keys can also be used to change the autoRenewPeriod, and change the adminKeys field itself.
 * The API currently does not implement this ability. But it does allow the adminKeys field to be set and
 * queried, and will in the future implement such admin abilities for any instance that has a non-null adminKeys.
 * <p>
 * If this constructor stores information, it is charged gas to store it. There is a fee in hbars to
 * maintain that storage until the expiration time, and that fee is added as part of the transaction fee.
 * <p>
 * An entity (account, file, or smart contract instance) must be created in a particular realm.
 * If the realmID is left null, then a new realm will be created with the given admin key. If a new realm has
 * a null adminKey, then anyone can create/modify/delete entities in that realm. But if an admin key is given,
 * then any transaction to create/modify/delete an entity in that realm must be signed by that key,
 * though anyone can still call functions on smart contract instances that exist in that realm.
 * A realm ceases to exist when everything within it has expired and no longer exists.
 * <p>
 * The current API ignores shardID, realmID, and newRealmAdminKey, and creates everything in shard 0 and realm 0,
 * with a null key. Future versions of the API will support multiple realms and multiple shards.
 * <p>
 * The optional memo field can contain a string whose length is up to 100 bytes. That is the size after Unicode
 * NFD then UTF-8 conversion. This field can be used to describe the smart contract. It could also be used for
 * other purposes. One recommended purpose is to hold a hexadecimal string that is the SHA-384 hash of a
 * PDF file containing a human-readable legal contract. Then, if the admin keys are the
 * public keys of human arbitrators, they can use that legal document to guide their decisions during a binding
 * arbitration tribunal, convened to consider any changes to the smart contract in the future. The memo field can only
 * be changed using the admin keys. If there are no admin keys, then it cannot be
 * changed after the smart contract is created.
 */
public final class ContractCreateTransaction extends Transaction<ContractCreateTransaction> {

    @Nullable
    private FileId bytecodeFileId = null;

    @Nullable
    private byte[] bytecode = null;

    /**
     * @deprecated with no replacement
     */
    @Nullable
    @Deprecated
    private AccountId proxyAccountId = null;

    @Nullable
    private Key adminKey = null;

    private long gas = 0;
    private Hbar initialBalance = new Hbar(0);
    private int maxAutomaticTokenAssociations = 0;

    @Nullable
    private Duration autoRenewPeriod = null;

    private byte[] constructorParameters = {};
    private String contractMemo = "";

    @Nullable
    private AccountId stakedAccountId = null;

    @Nullable
    private Long stakedNodeId = null;

    private boolean declineStakingReward = false;

    @Nullable
    private AccountId autoRenewAccountId = null;

    private List<HookCreationDetails> hookCreationDetails = new ArrayList<>();

    /**
     * Constructor.
     */
    public ContractCreateTransaction() {
        setAutoRenewPeriod(DEFAULT_AUTO_RENEW_PERIOD);
        defaultMaxTransactionFee = new Hbar(20);
    }

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    ContractCreateTransaction(
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
    ContractCreateTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the file id.
     *
     * @return                          the file id as a byte code
     */
    @Nullable
    public FileId getBytecodeFileId() {
        return bytecodeFileId;
    }

    /**
     * Sets the file containing the smart contract byte code.
     * <p>
     * A copy will be made and held by the contract instance, and have the same expiration time as
     * the instance.
     * <p>
     * The file must be the ASCII hexadecimal representation of the smart contract bytecode.
     * The contract bytecode is limited in size only by the
     * network file size limit.
     *
     * @param bytecodeFileId The FileId to be set
     * @return {@code this}
     */
    public ContractCreateTransaction setBytecodeFileId(FileId bytecodeFileId) {
        Objects.requireNonNull(bytecodeFileId);
        requireNotFrozen();
        this.bytecode = null;
        this.bytecodeFileId = bytecodeFileId;
        return this;
    }

    /**
     * Extract the bytecode.
     *
     * @return                          the bytecode
     */
    @Nullable
    public byte[] getBytecode() {
        return bytecode != null ? Arrays.copyOf(bytecode, bytecode.length) : null;
    }

    /**
     * Sets the source for the smart contract EVM bytecode.
     * <p>
     * The bytes of the smart contract initCode. A copy of the contents
     * SHALL be made and held as `bytes` in smart contract state.<br/>
     * This value is limited in length by the network transaction size
     * limit. This entire transaction, including all fields and signatures,
     * MUST be less than the network transaction size limit.
     *
     * @param bytecode The bytecode
     * @return {@code this}
     */
    public ContractCreateTransaction setBytecode(byte[] bytecode) {
        Objects.requireNonNull(bytecode);
        requireNotFrozen();
        this.bytecodeFileId = null;
        this.bytecode = Arrays.copyOf(bytecode, bytecode.length);
        return this;
    }

    /**
     * Get the admin key
     *
     * @return the adminKey
     */
    @Nullable
    public Key getAdminKey() {
        return adminKey;
    }

    /**
     * Access control for modification of the smart contract after
     * it is created.
     * <p>
     * If this field is set, that key MUST sign this transaction.<br/>
     * If this field is set, that key MUST sign each future transaction to
     * update or delete the contract.<br/>
     * An updateContract transaction that _only_ extends the topic
     * expirationTime (a "manual renewal" transaction) SHALL NOT require
     * admin key signature.
     * <p>
     * A contract without an admin key SHALL be immutable, except for
     * expiration and renewal.
     *
     * @param adminKey The Key to be set
     * @return {@code this}
     */
    public ContractCreateTransaction setAdminKey(Key adminKey) {
        Objects.requireNonNull(adminKey);
        requireNotFrozen();
        this.adminKey = adminKey;
        return this;
    }

    /**
     * Extract the gas.
     *
     * @return                          the gas amount that was set
     */
    public long getGas() {
        return gas;
    }

    /**
     * A maximum limit to the amount of gas to use for the constructor call.
     * <p>
     * The network SHALL charge the greater of the following, but SHALL NOT
     * charge more than the value of this field.
     * <ol>
     *   <li>The actual gas consumed by the smart contract
     *       constructor call.</li>
     *   <li>`80%` of this value.</li>
     * </ol>
     * The `80%` factor encourages reasonable estimation, while allowing for
     * some overage to ensure successful execution.
     *
     * @param gas The long to be set as gas
     * @return {@code this}
     */
    public ContractCreateTransaction setGas(long gas) {
        requireNotFrozen();
        if (gas < 0) {
            throw new IllegalArgumentException("Gas must be non-negative");
        }
        this.gas = gas;
        return this;
    }

    /**
     * Extract the initial balance.
     *
     * @return                          the initial balance in hbar
     */
    public Hbar getInitialBalance() {
        return initialBalance;
    }

    /**
     * The amount of HBAR to use as an initial balance for the account
     * representing the new smart contract.
     * <p>
     * This value is presented in tinybar
     * (10<sup><strong>-</strong>8</sup> HBAR).<br/>
     * The HBAR provided here will be withdrawn from the payer account that
     * signed this transaction.
     *
     * @param initialBalance The Hbar to be set as the initial balance
     * @return {@code this}
     */
    public ContractCreateTransaction setInitialBalance(Hbar initialBalance) {
        Objects.requireNonNull(initialBalance);
        requireNotFrozen();
        this.initialBalance = initialBalance;
        return this;
    }

    /**
     * @deprecated with no replacement
     *
     * Extract the proxy account id.
     *
     * @return                          the proxy account id
     */
    @Deprecated
    @Nullable
    public AccountId getProxyAccountId() {
        return proxyAccountId;
    }

    /**
     * @deprecated with no replacement
     *
     * Sets the ID of the account to which this account is proxy staked.
     * <p>
     * If proxyAccountID is null, or is an invalid account, or is an account that isn't a node,
     * then this account is automatically proxy staked to a node chosen by the network, but without earning payments.
     * <p>
     * If the proxyAccountID account refuses to accept proxy staking , or if it is not currently running a node,
     * then it will behave as if  proxyAccountID was null.
     *
     * @param proxyAccountId The AccountId to be set
     * @return {@code this}
     */
    @Deprecated
    public ContractCreateTransaction setProxyAccountId(AccountId proxyAccountId) {
        Objects.requireNonNull(proxyAccountId);
        requireNotFrozen();
        this.proxyAccountId = proxyAccountId;
        return this;
    }

    /**
     * Get the maximum number of tokens that this contract can be
     * automatically associated with (i.e., receive air-drops from).
     *
     * @return the maxAutomaticTokenAssociations
     */
    public int getMaxAutomaticTokenAssociations() {
        return maxAutomaticTokenAssociations;
    }

    /**
     * The maximum number of tokens that can be auto-associated with this
     * smart contract.
     * <p>
     * If this is less than or equal to `used_auto_associations` (or 0), then
     * this contract MUST manually associate with a token before transacting
     * in that token.<br/>
     * Following HIP-904 This value may also be `-1` to indicate no limit.<br/>
     * This value MUST NOT be less than `-1`.
     *
     * @param maxAutomaticTokenAssociations The maximum automatic token associations
     * @return  {@code this}
     */
    public ContractCreateTransaction setMaxAutomaticTokenAssociations(int maxAutomaticTokenAssociations) {
        requireNotFrozen();
        this.maxAutomaticTokenAssociations = maxAutomaticTokenAssociations;
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
     * The initial lifetime, in seconds, for the smart contract, and the number
     * of seconds for which the smart contract will be automatically renewed
     * upon expiration.
     * <p>
     * This value MUST be set.<br/>
     * This value MUST be greater than the configured MIN_AUTORENEW_PERIOD.<br/>
     * This value MUST be less than the configured MAX_AUTORENEW_PERIOD.<br/>
     *
     * @param autoRenewPeriod The Duration to be set for auto renewal
     * @return {@code this}
     */
    public ContractCreateTransaction setAutoRenewPeriod(Duration autoRenewPeriod) {
        Objects.requireNonNull(autoRenewPeriod);
        requireNotFrozen();
        this.autoRenewPeriod = autoRenewPeriod;
        return this;
    }

    /**
     * Extract the constructor parameters.
     *
     * @return                          the byte string representation
     */
    public ByteString getConstructorParameters() {
        return ByteString.copyFrom(constructorParameters);
    }

    /**
     * Sets the constructor parameters as their raw bytes.
     * <p>
     * Use this instead of {@link #setConstructorParameters(ContractFunctionParameters)} if you have already
     * pre-encoded a solidity function call.
     *
     * @param constructorParameters The constructor parameters
     * @return {@code this}
     */
    public ContractCreateTransaction setConstructorParameters(byte[] constructorParameters) {
        requireNotFrozen();
        this.constructorParameters = Arrays.copyOf(constructorParameters, constructorParameters.length);
        return this;
    }

    /**
     * Sets the parameters to pass to the constructor.
     *
     * @param constructorParameters The contructor parameters
     * @return {@code this}
     */
    public ContractCreateTransaction setConstructorParameters(ContractFunctionParameters constructorParameters) {
        Objects.requireNonNull(constructorParameters);
        return setConstructorParameters(constructorParameters.toBytes(null).toByteArray());
    }

    /**
     * Extract the contract memo.
     *
     * @return                          the contract's memo
     */
    public String getContractMemo() {
        return contractMemo;
    }

    /**
     * A short memo for this smart contract.
     * <p>
     * This value, if set, MUST NOT exceed `transaction.maxMemoUtf8Bytes`
     * (default 100) bytes when encoded as UTF-8.
     *
     * @param memo The String to be set as the memo
     * @return {@code this}
     */
    public ContractCreateTransaction setContractMemo(String memo) {
        requireNotFrozen();
        Objects.requireNonNull(memo);
        contractMemo = memo;
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
     * Set the account to which this contract will stake
     *
     * @param stakedAccountId ID of the account to which this contract will stake.
     * @return {@code this}
     */
    public ContractCreateTransaction setStakedAccountId(@Nullable AccountId stakedAccountId) {
        requireNotFrozen();
        this.stakedAccountId = stakedAccountId;
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
     * The ID of a network node.
     * <p>
     * This smart contract SHALL stake its HBAR to this node.
     * <p>
     * <blockquote>Note: node IDs do fluctuate as node operators change.
     * Most contracts are immutable, and a contract staking to an invalid
     * node ID SHALL NOT participate in staking. Immutable contracts MAY
     * find it more reliable to use a proxy account for staking
     * (via `staked_account_id`) to enable updating the _effective_ staking
     * node ID when necessary through updating the proxy
     * account.</blockquote>
     *
     * @param stakedNodeId ID of the node this contract will be staked to.
     * @return {@code this}
     */
    public ContractCreateTransaction setStakedNodeId(@Nullable Long stakedNodeId) {
        requireNotFrozen();
        this.stakedNodeId = stakedNodeId;
        this.stakedAccountId = null;
        return this;
    }

    /**
     * If true, the contract declines receiving a staking reward. The default value is false.
     *
     * @return If true, the contract declines receiving a staking reward. The default value is false.
     */
    public boolean getDeclineStakingReward() {
        return declineStakingReward;
    }

    /**
     * A flag indicating that this smart contract declines to receive any
     * reward for staking its HBAR balance to help secure the network.
     * <p>
     * If set to true, this smart contract SHALL NOT receive any reward for
     * staking its HBAR balance to help secure the network, regardless of
     * staking configuration, but MAY stake HBAR to support the network
     * without reward.
     *
     * @param declineStakingReward - If true, the contract declines receiving a staking reward. The default value is false.
     * @return {@code this}
     */
    public ContractCreateTransaction setDeclineStakingReward(boolean declineStakingReward) {
        requireNotFrozen();
        this.declineStakingReward = declineStakingReward;
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
     * The id of an account, in the same shard and realm as this smart
     * contract, that has signed this transaction, allowing the network to use
     * its balance, when needed, to automatically extend this contract's
     * expiration time.
     * <p>
     * If this field is set, that key MUST sign this transaction.<br/>
     * If this field is set, then the network SHALL deduct the necessary fees
     * from the designated auto-renew account, if that account has sufficient
     * balance. If the auto-renewal account does not have sufficient balance,
     * then the fees for contract renewal SHALL be deducted from the HBAR
     * balance held by the smart contract.<br/>
     * If this field is not set, then all renewal fees SHALL be deducted from
     * the HBAR balance held by this contract.
     *
     * @param autoRenewAccountId The AccountId to be set for auto-renewal
     * @return {@code this}
     */
    public ContractCreateTransaction setAutoRenewAccountId(AccountId autoRenewAccountId) {
        Objects.requireNonNull(autoRenewAccountId);
        requireNotFrozen();
        this.autoRenewAccountId = autoRenewAccountId;
        return this;
    }

    /**
     * Get the hook creation details for this contract.
     *
     * @return a copy of the hook creation details list
     */
    public List<HookCreationDetails> getHooks() {
        return new ArrayList<>(hookCreationDetails);
    }

    /**
     * Add a hook creation detail to this contract.
     *
     * @param hookDetails the hook creation details to add
     * @return {@code this}
     */
    public ContractCreateTransaction addHook(HookCreationDetails hookDetails) {
        requireNotFrozen();
        Objects.requireNonNull(hookDetails, "hookDetails cannot be null");
        this.hookCreationDetails.add(hookDetails);
        return this;
    }

    /**
     * Set the hook creation details for this contract.
     *
     * @param hookDetails the list of hook creation details
     * @return {@code this}
     */
    public ContractCreateTransaction setHooks(List<HookCreationDetails> hookDetails) {
        requireNotFrozen();
        Objects.requireNonNull(hookDetails, "hookDetails cannot be null");
        this.hookCreationDetails = new ArrayList<>(hookDetails);
        return this;
    }

    /**
     * Build the transaction body.
     *
     * @return {@link ContractCreateTransactionBody}
     */
    ContractCreateTransactionBody.Builder build() {
        var builder = ContractCreateTransactionBody.newBuilder();
        if (bytecodeFileId != null) {
            builder.setFileID(bytecodeFileId.toProtobuf());
        }
        if (bytecode != null) {
            builder.setInitcode(ByteString.copyFrom(bytecode));
        }
        if (proxyAccountId != null) {
            builder.setProxyAccountID(proxyAccountId.toProtobuf());
        }
        if (adminKey != null) {
            builder.setAdminKey(adminKey.toProtobufKey());
        }
        builder.setMaxAutomaticTokenAssociations(maxAutomaticTokenAssociations);
        if (autoRenewPeriod != null) {
            builder.setAutoRenewPeriod(DurationConverter.toProtobuf(autoRenewPeriod));
        }
        builder.setGas(gas);
        builder.setInitialBalance(initialBalance.toTinybars());
        builder.setConstructorParameters(ByteString.copyFrom(constructorParameters));
        builder.setMemo(contractMemo);
        builder.setDeclineReward(declineStakingReward);

        if (stakedAccountId != null) {
            builder.setStakedAccountId(stakedAccountId.toProtobuf());
        } else if (stakedNodeId != null) {
            builder.setStakedNodeId(stakedNodeId);
        }

        if (autoRenewAccountId != null) {
            builder.setAutoRenewAccountId(autoRenewAccountId.toProtobuf());
        }

        for (HookCreationDetails hookDetails : hookCreationDetails) {
            builder.addHookCreationDetails(hookDetails.toProtobuf());
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (bytecodeFileId != null) {
            bytecodeFileId.validateChecksum(client);
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

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getContractCreateInstance();

        if (body.hasFileID()) {
            bytecodeFileId = FileId.fromProtobuf(body.getFileID());
        }
        if (body.hasInitcode()) {
            bytecode = body.getInitcode().toByteArray();
        }
        if (body.hasProxyAccountID()) {
            proxyAccountId = AccountId.fromProtobuf(body.getProxyAccountID());
        }
        if (body.hasAdminKey()) {
            adminKey = Key.fromProtobufKey(body.getAdminKey());
        }
        maxAutomaticTokenAssociations = body.getMaxAutomaticTokenAssociations();
        if (body.hasAutoRenewPeriod()) {
            autoRenewPeriod = DurationConverter.fromProtobuf(body.getAutoRenewPeriod());
        }
        gas = body.getGas();
        initialBalance = Hbar.fromTinybars(body.getInitialBalance());
        constructorParameters = body.getConstructorParameters().toByteArray();
        contractMemo = body.getMemo();
        declineStakingReward = body.getDeclineReward();

        if (body.hasStakedAccountId()) {
            stakedAccountId = AccountId.fromProtobuf(body.getStakedAccountId());
        }

        if (body.hasStakedNodeId()) {
            stakedNodeId = body.getStakedNodeId();
        }

        if (body.hasAutoRenewAccountId()) {
            autoRenewAccountId = AccountId.fromProtobuf(body.getAutoRenewAccountId());
        }

        // Initialize hook creation details
        hookCreationDetails.clear();
        for (var protoHookDetails : body.getHookCreationDetailsList()) {
            hookCreationDetails.add(HookCreationDetails.fromProtobuf(protoHookDetails));
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return SmartContractServiceGrpc.getCreateContractMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setContractCreateInstance(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setContractCreateInstance(build());
    }
}
