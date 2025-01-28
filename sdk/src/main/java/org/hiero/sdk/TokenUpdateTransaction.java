// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.TokenUpdateTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Update an existing token.
 *
 * This transaction SHALL NOT update any field that is not set.<br/>
 * Most changes MUST be signed by the current `admin_key` of the token. If the
 * token does not currently have a valid `admin_key`, then this transaction
 * MUST NOT set any value other than `expiry` or a non-admin key.<br/>
 * If the `treasury` is set to a new account, the new account MUST sign this
 * transaction.<br/>
 * If the `treasury` is set to a new account for a _non-fungible/unique_ token,
 * The current treasury MUST NOT hold any tokens, or the network configuration
 * property `tokens.nfts.useTreasuryWildcards` MUST be set.
 *
 * #### Requirements for Keys
 * Any of the key values may be changed, even without an admin key, but the
 * key to be changed MUST have an existing valid key assigned, and both the
 * current key and the new key MUST sign the transaction.<br/>
 * A key value MAY be set to an empty `KeyList`. In this case the existing
 * key MUST sign this transaction, but the new value is not a valid key, and the
 * update SHALL effectively remove the existing key.
 *
 * ### Block Stream Effects
 * None
 */
public class TokenUpdateTransaction extends Transaction<TokenUpdateTransaction> {
    /**
     * The token's id
     */
    @Nullable
    private TokenId tokenId = null;

    @Nullable
    private AccountId treasuryAccountId = null;

    @Nullable
    private AccountId autoRenewAccountId = null;

    private String tokenName = "";

    private String tokenSymbol = "";

    @Nullable
    private Key adminKey = null;

    @Nullable
    private Key kycKey = null;

    @Nullable
    private Key freezeKey = null;

    @Nullable
    private Key wipeKey = null;

    @Nullable
    private Key supplyKey = null;

    @Nullable
    private Key feeScheduleKey = null;

    @Nullable
    private Key pauseKey = null;

    @Nullable
    private Key metadataKey = null;

    @Nullable
    private Instant expirationTime = null;

    private Duration expirationTimeDuration = null;

    @Nullable
    private Duration autoRenewPeriod = null;

    @Nullable
    private String tokenMemo = null;

    private byte[] tokenMetadata = null;

    private TokenKeyValidation tokenKeyVerificationMode = TokenKeyValidation.FULL_VALIDATION;

    /**
     * Constructor.
     */
    public TokenUpdateTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenUpdateTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TokenUpdateTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the token id.
     *
     * @return                          the token id
     */
    @Nullable
    public TokenId getTokenId() {
        return tokenId;
    }

    /**
     * A token identifier.
     * <p>
     * This SHALL identify the token type to delete.<br/>
     * The identified token MUST exist, and MUST NOT be deleted.<br/>
     * If any field other than `expiry` is set, the identified token MUST
     * have a valid `admin_key`.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public TokenUpdateTransaction setTokenId(TokenId tokenId) {
        requireNotFrozen();
        Objects.requireNonNull(tokenId);
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Extract the token name.
     *
     * @return                          the token name
     */
    @Nullable
    public String getTokenName() {
        return tokenName;
    }

    /**
     * A new name for the token.<br/>
     * This is generally the "full name" displayed in wallet software.
     * <p>
     * This value, if set, MUST NOT exceed 100 bytes when encoded as UTF-8.<br/>
     * This value, if set, MUST NOT contain the Unicode NUL codepoint.
     *
     * @param name                      the token name
     * @return {@code this}
     */
    public TokenUpdateTransaction setTokenName(String name) {
        Objects.requireNonNull(name);
        requireNotFrozen();
        tokenName = name;
        return this;
    }

    /**
     * Extract the token symbol.
     *
     * @return                          the token symbol
     */
    public String getTokenSymbol() {
        return tokenSymbol;
    }

    /**
     * A new symbol to use for the token.
     * <p>
     * This value, if set, MUST NOT exceed 100 bytes when encoded as UTF-8.<br/>
     * This value, if set, MUST NOT contain the Unicode NUL codepoint.
     *
     * @param symbol                    the token symbol
     * @return {@code this}
     */
    public TokenUpdateTransaction setTokenSymbol(String symbol) {
        Objects.requireNonNull(symbol);
        requireNotFrozen();
        tokenSymbol = symbol;
        return this;
    }

    /**
     * Extract the treasury account id.
     *
     * @return                          the treasury account id
     */
    @Nullable
    public AccountId getTreasuryAccountId() {
        return treasuryAccountId;
    }

    /**
     * A new treasury account identifier.
     * <p>
     * If set,
     * - The identified account SHALL be designated the "treasury" for the
     *   token, and all tokens "minted" SHALL be delivered to that account
     *   following this transaction.<br/>
     * - The identified account MUST exist, MUST NOT be expired, MUST NOT be
     *   deleted, and SHOULD have a non-zero HBAR balance.<br/>
     * - The identified account SHALL be associated to this token.
     * - The full balance of this token held by the prior treasury account
     *   SHALL be transferred to the new treasury account, if the token type
     *   is fungible/common.
     * - If the token type is non-fungible/unique, the previous treasury
     *   account MUST NOT hold any tokens of this type.
     * - The new treasury account key MUST sign this transaction.
     *
     * @param accountId                 the account id
     * @return {@code this}
     */
    public TokenUpdateTransaction setTreasuryAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.treasuryAccountId = accountId;
        return this;
    }

    /**
     * Extract the admin key.
     *
     * @return                          the admin key
     */
    @Nullable
    public Key getAdminKey() {
        return adminKey;
    }

    /**
     * A Hedera key for token administration.
     * <p>
     * This key, if set, SHALL have administrative authority for this token and
     * MAY authorize token update and/or token delete transactions.<br/>
     * If this key is set to an empty `KeyList`, this token SHALL be
     * immutable thereafter, except for expiration and renewal.<br/>
     * If set, this key MUST be a valid key or an empty `KeyList`.<br/>
     * If set to a valid key, the previous key and new key MUST both
     * sign this transaction.
     *
     * @param key                       the key
     * @return {@code this}
     */
    public TokenUpdateTransaction setAdminKey(Key key) {
        requireNotFrozen();
        Objects.requireNonNull(key);
        adminKey = key;
        return this;
    }

    /**
     * Extract the kyc key.
     *
     * @return                          the kyc key
     */
    @Nullable
    public Key getKycKey() {
        return kycKey;
    }

    /**
     * A Hedera key for managing account KYC.
     * <p>
     * This key, if set, SHALL have KYC authority for this token and
     * MAY authorize transactions to grant or revoke KYC for accounts.<br/>
     * If this key is not set, or is an empty `KeyList`, KYC status for this
     * token SHALL NOT be granted or revoked for any account.<br/>
     * If this key is removed after granting KYC, those grants SHALL remain
     * and cannot be revoked.<br/>
     * If set, this key MUST be a valid key or an empty `KeyList`.<br/>
     * If set to a valid key, the previous key and new key MUST both
     * sign this transaction.
     *
     * @param key                       the kyc key
     * @return {@code this}
     */
    public TokenUpdateTransaction setKycKey(Key key) {
        requireNotFrozen();
        kycKey = key;
        return this;
    }

    /**
     * Extract the freeze key.
     *
     * @return                          the freeze key
     */
    @Nullable
    public Key getFreezeKey() {
        return freezeKey;
    }

    /**
     * A Hedera key for managing asset "freeze".
     * <p>
     * This key, if set, SHALL have "freeze" authority for this token and
     * MAY authorize transactions to freeze or unfreeze accounts
     * with respect to this token.<br/>
     * If this key is set to an empty `KeyList`, this token
     * SHALL NOT be frozen or unfrozen for any account.<br/>
     * If this key is removed after freezing accounts, those accounts
     * SHALL remain frozen and cannot be unfrozen.<br/>
     * If set, this key MUST be a valid key or an empty `KeyList`.<br/>
     * If set to a valid key, the previous key and new key MUST both
     * sign this transaction.
     *
     * @param key                       the freeze key
     * @return {@code this}
     */
    public TokenUpdateTransaction setFreezeKey(Key key) {
        Objects.requireNonNull(key);
        requireNotFrozen();
        freezeKey = key;
        return this;
    }

    /**
     * Extract the wipe key.
     *
     * @return                          the wipe key
     */
    @Nullable
    public Key getWipeKey() {
        return wipeKey;
    }

    /**
     * A Hedera key for wiping tokens from accounts.
     * <p>
     * This key, if set, SHALL have "wipe" authority for this token and
     * MAY authorize transactions to "wipe" any amount of this token from
     * any account, effectively burning the tokens "wiped".<br/>
     * If this key is set to an empty `KeyList`, it SHALL NOT be
     * possible to "wipe" this token from an account.<br/>
     * If set, this key MUST be a valid key or an empty `KeyList`.<br/>
     * If set to a valid key, the previous key and new key MUST both
     * sign this transaction.
     *
     * @param key                       the wipe key
     * @return {@code this}
     */
    public TokenUpdateTransaction setWipeKey(Key key) {
        Objects.requireNonNull(key);
        requireNotFrozen();
        wipeKey = key;
        return this;
    }

    /**
     * Extract the supply key.
     *
     * @return                          the supply key
     */
    @Nullable
    public Key getSupplyKey() {
        return supplyKey;
    }

    /**
     * An Hedera key for "minting" and "burning" tokens.
     * <p>
     * This key, if set, MAY authorize transactions to "mint" new tokens to
     * be delivered to the token treasury or "burn" tokens held by the
     * token treasury.<br/>
     * If this key is set to an empty `KeyList`, it SHALL NOT be
     * possible to change the supply of tokens and neither "mint" nor "burn"
     * transactions SHALL be permitted.<br/>
     * If set, this key MUST be a valid key or an empty `KeyList`.<br/>
     * If set to a valid key, the previous key and new key MUST both
     * sign this transaction.
     *
     * @param key                       the supply key
     * @return {@code this}
     */
    public TokenUpdateTransaction setSupplyKey(Key key) {
        Objects.requireNonNull(key);
        requireNotFrozen();
        supplyKey = key;
        return this;
    }

    /**
     * Extract the fee schedule key.
     *
     * @return                          the fee schedule key
     */
    @Nullable
    public Key getFeeScheduleKey() {
        return feeScheduleKey;
    }

    /**
     * An Hedera key for managing the token custom fee schedule.
     * <p>
     * This key, if set, MAY authorize transactions to modify the
     * `custom_fees` for this token.<br/>
     * If this key is set to an empty `KeyList`, the `custom_fees`
     * for this token SHALL NOT be modified.<br/>
     * If set, this key MUST be a valid key or an empty `KeyList`.<br/>
     * If set to a valid key, the previous key and new key MUST both
     * sign this transaction.
     *
     * @param key                       the fee schedule key
     * @return {@code this}
     */
    public TokenUpdateTransaction setFeeScheduleKey(Key key) {
        requireNotFrozen();
        Objects.requireNonNull(key);
        feeScheduleKey = key;
        return this;
    }

    /**
     * Extract the pause key.
     *
     * @return                          the pause key
     */
    @Nullable
    public Key getPauseKey() {
        return pauseKey;
    }

    /**
     * An Hedera key for managing token "pause".
     * <p>
     * This key, if set, SHALL have "pause" authority for this token and
     * MAY authorize transactions to pause or unpause this token.<br/>
     * If this key is set to an empty `KeyList`, this token
     * SHALL NOT be paused or unpaused.<br/>
     * If this key is removed while the token is paused, the token cannot
     * be unpaused and SHALL remain paused.<br/>
     * If set, this key MUST be a valid key or an empty `KeyList`.<br/>
     * If set to a valid key, the previous key and new key MUST both
     * sign this transaction.
     *
     * @param key                       the pause key
     * @return {@code this}
     */
    public TokenUpdateTransaction setPauseKey(Key key) {
        requireNotFrozen();
        Objects.requireNonNull(key);
        pauseKey = key;
        return this;
    }

    /**
     * Extract the metadata key.
     *
     * @return                          the metadata key
     */
    @Nullable
    public Key getMetadataKey() {
        return metadataKey;
    }

    /**
     * A Hedera key for managing the token `metadata`.
     * <p>
     * This key, if set, MAY authorize transactions to modify the
     * `metadata` for this token.<br/>
     * If this key is set to an empty `KeyList`, the `metadata`
     * for this token SHALL NOT be modified.<br/>
     * If set, this key MUST be a valid key or an empty `KeyList`.<br/>
     * If set to a valid key, the previous key and new key MUST both
     * sign this transaction.
     *
     * @param key                       the metadata key
     * @return {@code this}
     */
    public TokenUpdateTransaction setMetadataKey(Key key) {
        requireNotFrozen();
        Objects.requireNonNull(key);
        metadataKey = key;
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
     * An expiration timestamp.
     * <p>
     * If this value is set, the automatic renewal account is not set for the
     * identified token, and token expiration is enabled in network
     * configuration, this token SHALL expire when the consensus time exceeds
     * this value, and MAY be subsequently removed from the network state.<br/>
     * If `autoRenewAccount` is set or the `auto_renew_account_id` is set for
     * the identified token, the token SHALL be subject to automatic renewal
     * when the consensus time exceeds this value.
     *
     * @param expirationTime            the expiration time
     * @return {@code this}
     */
    public TokenUpdateTransaction setExpirationTime(Instant expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        autoRenewPeriod = null;
        this.expirationTime = expirationTime;
        this.expirationTimeDuration = null;
        return this;
    }

    public TokenUpdateTransaction setExpirationTime(Duration expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        autoRenewPeriod = null;
        this.expirationTime = null;
        this.expirationTimeDuration = expirationTime;
        return this;
    }

    /**
     * Extract the auto renew account id.
     *
     * @return                          the auto renew account id
     */
    @Nullable
    public AccountId getAutoRenewAccountId() {
        return autoRenewAccountId;
    }

    /**
     * An identifier for the account to be charged renewal fees at the token's
     * expiry to extend the lifetime of the token.
     * <p>
     * If this value is set for the identified token, the token lifetime SHALL
     * be extended by the _smallest_ of the following at expiration:
     * <ul>
     *   <li>The current `autoRenewPeriod` duration.</li>
     *   <li>The maximum duration that this account has funds to purchase.</li>
     *   <li>The configured MAX_AUTORENEW_PERIOD at the time of automatic
     *       renewal.</li>
     * </ul>
     * If this account's HBAR balance is `0` when the token must be
     * renewed, then the token SHALL be expired, and MAY be subsequently
     * removed from state.<br/>
     * If this value is set, the referenced account MUST sign this
     * transaction.
     * <p>
     * <blockquote>Note<blockquote>
     * It is not currently possible to remove an automatic renewal account.
     * Once set, it can only be replaced by a valid account.
     * </blockquote></blockquote>
     *
     * @param accountId                 the account id
     * @return {@code this}
     */
    public TokenUpdateTransaction setAutoRenewAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.autoRenewAccountId = accountId;
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
     * A duration between token automatic renewals.<br/>
     * All entities in state may be charged "rent" occasionally (typically
     * every 90 days) to prevent unnecessary growth of the ledger. This value
     * sets the interval between such events for this token.
     * <p>
     * If set, this value MUST be greater than the configured
     * `MIN_AUTORENEW_PERIOD`.<br/>
     * If set, this value MUST be less than the configured
     * `MAX_AUTORENEW_PERIOD`.
     *
     * @param period                    the auto renew period
     * @return {@code this}
     */
    public TokenUpdateTransaction setAutoRenewPeriod(Duration period) {
        Objects.requireNonNull(period);
        requireNotFrozen();
        autoRenewPeriod = period;
        return this;
    }

    /**
     * Extract the token memo.
     *
     * @return                          the token memo
     */
    @Nullable
    public String getTokenMemo() {
        return tokenMemo;
    }

    /**
     * A short description for this token.
     * <p>
     * This value, if set, MUST NOT exceed `transaction.maxMemoUtf8Bytes`
     * (default 100) bytes when encoded as UTF-8.
     *
     * @param memo                      the token memo 100 bytes max
     * @return {@code this}
     */
    public TokenUpdateTransaction setTokenMemo(String memo) {
        Objects.requireNonNull(memo);
        requireNotFrozen();
        tokenMemo = memo;
        return this;
    }

    /**
     * Remove the token memo.
     *
     * @return {@code this}
     */
    public TokenUpdateTransaction clearMemo() {
        requireNotFrozen();
        tokenMemo = "";
        return this;
    }

    /**
     * Extract the metadata.
     *
     * @return the metadata
     */
    @Nullable
    public byte[] getTokenMetadata() {
        return tokenMetadata;
    }

    /**
     * Assign the metadata.
     *
     * @param tokenMetadata the metadata
     * @return {@code this}
     */
    public TokenUpdateTransaction setTokenMetadata(byte[] tokenMetadata) {
        requireNotFrozen();
        this.tokenMetadata = tokenMetadata;
        return this;
    }

    /**
     * Extract the key verification mode
     *
     * @return the key verification mode
     */
    public TokenKeyValidation getKeyVerificationMode() {
        return tokenKeyVerificationMode;
    }

    /**
     * Set a key validation mode.<br/>
     * Any key may be updated by a transaction signed by the token `admin_key`.
     * Each role key may _also_ sign a transaction to update that key.
     * If a role key signs an update to change that role key both old
     * and new key must sign the transaction, _unless_ this field is set
     * to `NO_VALIDATION`, in which case the _new_ key is not required to
     * sign the transaction (the existing key is still required).<br/>
     * The primary intent for this field is to allow a role key (e.g. a
     * `pause_key`) holder to "remove" that key from the token by signing
     * a transaction to set that role key to an empty `KeyList`.
     * <p>
     * If set to `FULL_VALIDATION`, either the `admin_key` or _both_ current
     * and new key MUST sign this transaction to update a "key" field for the
     * identified token.<br/>
     * If set to `NO_VALIDATION`, either the `admin_key` or the current
     * key MUST sign this transaction to update a "key" field for the
     * identified token.<br/>
     * This field SHALL be treated as `FULL_VALIDATION` if not set.
     *
     * @param tokenKeyVerificationMode the key verification mode
     * @return {@code this}
     */
    public TokenUpdateTransaction setKeyVerificationMode(TokenKeyValidation tokenKeyVerificationMode) {
        requireNotFrozen();
        Objects.requireNonNull(tokenKeyVerificationMode);
        this.tokenKeyVerificationMode = tokenKeyVerificationMode;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenUpdate();
        if (body.hasToken()) {
            tokenId = TokenId.fromProtobuf(body.getToken());
        }
        if (body.hasTreasury()) {
            treasuryAccountId = AccountId.fromProtobuf(body.getTreasury());
        }
        if (body.hasAutoRenewAccount()) {
            autoRenewAccountId = AccountId.fromProtobuf(body.getAutoRenewAccount());
        }
        tokenName = body.getName();
        tokenSymbol = body.getSymbol();
        if (body.hasAdminKey()) {
            adminKey = Key.fromProtobufKey(body.getAdminKey());
        }
        if (body.hasKycKey()) {
            kycKey = Key.fromProtobufKey(body.getKycKey());
        }
        if (body.hasFreezeKey()) {
            freezeKey = Key.fromProtobufKey(body.getFreezeKey());
        }
        if (body.hasWipeKey()) {
            wipeKey = Key.fromProtobufKey(body.getWipeKey());
        }
        if (body.hasSupplyKey()) {
            supplyKey = Key.fromProtobufKey(body.getSupplyKey());
        }
        if (body.hasFeeScheduleKey()) {
            feeScheduleKey = Key.fromProtobufKey(body.getFeeScheduleKey());
        }
        if (body.hasPauseKey()) {
            pauseKey = Key.fromProtobufKey(body.getPauseKey());
        }
        if (body.hasMetadataKey()) {
            metadataKey = Key.fromProtobufKey(body.getMetadataKey());
        }
        if (body.hasExpiry()) {
            expirationTime = InstantConverter.fromProtobuf(body.getExpiry());
        }
        if (body.hasAutoRenewPeriod()) {
            autoRenewPeriod = DurationConverter.fromProtobuf(body.getAutoRenewPeriod());
        }
        if (body.hasMemo()) {
            tokenMemo = body.getMemo().getValue();
        }
        if (body.hasMetadata()) {
            tokenMetadata = body.getMetadata().getValue().toByteArray();
        }
        tokenKeyVerificationMode = TokenKeyValidation.valueOf(body.getKeyVerificationMode());
    }

    /**
     * Build the transaction body.
     *
     * @return {@link
     *         org.hiero.sdk.proto.TokenUpdateTransactionBody}
     */
    TokenUpdateTransactionBody.Builder build() {
        var builder = TokenUpdateTransactionBody.newBuilder();
        if (tokenId != null) {
            builder.setToken(tokenId.toProtobuf());
        }
        if (treasuryAccountId != null) {
            builder.setTreasury(treasuryAccountId.toProtobuf());
        }

        if (autoRenewAccountId != null) {
            builder.setAutoRenewAccount(autoRenewAccountId.toProtobuf());
        }
        builder.setName(tokenName);
        builder.setSymbol(tokenSymbol);
        if (adminKey != null) {
            builder.setAdminKey(adminKey.toProtobufKey());
        }
        if (kycKey != null) {
            builder.setKycKey(kycKey.toProtobufKey());
        }
        if (freezeKey != null) {
            builder.setFreezeKey(freezeKey.toProtobufKey());
        }
        if (wipeKey != null) {
            builder.setWipeKey(wipeKey.toProtobufKey());
        }
        if (supplyKey != null) {
            builder.setSupplyKey(supplyKey.toProtobufKey());
        }
        if (feeScheduleKey != null) {
            builder.setFeeScheduleKey(feeScheduleKey.toProtobufKey());
        }
        if (pauseKey != null) {
            builder.setPauseKey(pauseKey.toProtobufKey());
        }
        if (metadataKey != null) {
            builder.setMetadataKey(metadataKey.toProtobufKey());
        }
        if (expirationTime != null) {
            builder.setExpiry(InstantConverter.toProtobuf(expirationTime));
        }
        if (expirationTimeDuration != null) {
            builder.setExpiry(InstantConverter.toProtobuf(expirationTimeDuration));
        }
        if (autoRenewPeriod != null) {
            builder.setAutoRenewPeriod(DurationConverter.toProtobuf(autoRenewPeriod));
        }
        if (tokenMemo != null) {
            builder.setMemo(StringValue.of(tokenMemo));
        }
        if (tokenMetadata != null) {
            builder.setMetadata(BytesValue.of(ByteString.copyFrom(tokenMetadata)));
        }
        builder.setKeyVerificationMode(tokenKeyVerificationMode.code);

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (tokenId != null) {
            tokenId.validateChecksum(client);
        }

        if (treasuryAccountId != null) {
            treasuryAccountId.validateChecksum(client);
        }

        if (autoRenewAccountId != null) {
            autoRenewAccountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getUpdateTokenMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenUpdate(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenUpdate(build());
    }
}
