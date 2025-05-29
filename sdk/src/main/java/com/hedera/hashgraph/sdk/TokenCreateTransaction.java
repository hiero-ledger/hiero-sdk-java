// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TokenCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.TokenServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

/**
 * Create an HTS token.
 *
 * #### Keys
 * Each token has several keys that, separately, control different functions
 * for that token. It is *_strongly_* recommended that each key assigned to
 * a token be unique, or disabled by assigning an empty `KeyList`.
 * Keys and purpose
 * - `adminKey` is a general access and may authorize a token update
 *   transaction as well as _update the other keys_. Even the admin key
 *   cannot authorize _adding_ a key that is not present, however.<br/>
 *   The admin key may also delete the token entirely.
 * - `fee_schedule` may authorize updating the token custom fees. If this
 *   key is not present, the custom fees for the token are fixed and immutable.
 * - `freeze` may authorize a token freeze or unfreeze transaction.
 *   If this key is not present, accounts holding this token cannot have
 *   their tokens frozen or unfrozen.
 * - `kyc` may authorize a token grant KYC or revoke KYC transaction.
 *   If this key is not present, accounts holding this token cannot have
 *   KYC status granted or revoked.
 * - `metadata` may authorize token update nfts transactions.
 *   If this key is not present, the token metadata values for that
 *   non-fungible/unique token _type_ will be immutable.
 * - `pause` may authorize a token pause or token unpause transaction.
 *   If this key is not present, the token cannot be paused (preventing any
 *   account from transacting in that token) or resumed.
 * - `supply` may authorize a token mint or burn transaction.
 *   If this key is not present, the token cannot mint additional supply and
 *   existing tokens cannot be "burned" from the treasury (but _might_ still be
 *   "burned" from individual accounts, c.f. `wipeKey` and `tokenWipe`).
 * - `wipe` may authorize a token wipe account transaction.
 *   If this key is not present, accounts holding this token cannot have
 *   their balance or NFTs wiped (effectively burned).
 *
 * #### Requirements
 * If `tokenType` is fungible/common, the `initialSupply` MUST be strictly
 * greater than zero(`0`).<br/>
 * If `tokenType` is non-fungible/unique, the `initialSupply` MUST
 * be zero(`0`).<br/>
 * If `tokenSupplyType` is "infinite", the `maxSupply` MUST be zero(`0`).<br/>
 * If `tokenSupplyType` is "finite", the `maxSupply` MUST be strictly
 * greater than zero(`0`).<br/>
 *
 * ### Block Stream Effects
 * If the token is created, the Token Identifier SHALL be in the receipt.<br/>
 */
public class TokenCreateTransaction extends Transaction<TokenCreateTransaction> {
    private List<CustomFee> customFees = new ArrayList<>();

    @Nullable
    private AccountId treasuryAccountId = null;

    @Nullable
    private AccountId autoRenewAccountId = null;

    private String tokenName = "";

    private String tokenSymbol = "";

    private int decimals = 0;

    private long initialSupply = 0;

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

    private boolean freezeDefault = false;

    @Nullable
    private Instant expirationTime = null;

    private Duration expirationTimeDuration = null;

    @Nullable
    private Duration autoRenewPeriod = null;

    private String tokenMemo = "";

    private TokenType tokenType = TokenType.FUNGIBLE_COMMON;

    private TokenSupplyType tokenSupplyType = TokenSupplyType.INFINITE;

    private long maxSupply = 0;

    private byte[] tokenMetadata = {};
    /**
     * Constructor.
     */
    public TokenCreateTransaction() {
        autoRenewPeriod = DEFAULT_AUTO_RENEW_PERIOD;
        defaultMaxTransactionFee = new Hbar(40);
    }

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenCreateTransaction(
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
    TokenCreateTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
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
     * A name for the token.<br/>
     * This is generally the "full name" displayed in wallet software.
     * <p>
     * This field is REQUIRED.<br/>
     * This value MUST NOT exceed 100 bytes when encoded as UTF-8.<br/>
     * This value MUST NOT contain the Unicode NUL codepoint.
     *
     * @param name                      the token name
     * @return {@code this}
     */
    public TokenCreateTransaction setTokenName(String name) {
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
     * A symbol to use for the token.
     * <p>
     * This field is REQUIRED.<br/>
     * This value MUST NOT exceed 100 bytes when encoded as UTF-8.<br/>
     * This value MUST NOT contain the Unicode NUL codepoint.
     *
     * @param symbol                    the token symbol
     * @return {@code this}
     */
    public TokenCreateTransaction setTokenSymbol(String symbol) {
        Objects.requireNonNull(symbol);
        requireNotFrozen();
        tokenSymbol = symbol;
        return this;
    }

    /**
     * Extract the decimals.
     *
     * @return                          the decimals
     */
    public int getDecimals() {
        return decimals;
    }

    /**
     * A decimal precision of the token's smallest denomination.<br/>
     * Most values are described in terms of this smallest denomination,
     * so the token initial supply, for instance, must be divided by
     * <tt>10<sup>decimals</sup></tt> to get whole tokens.
     * <p>
     * This MUST be zero(`0`) for non-fungible/unique tokens.
     *
     * @param decimals                  the number of decimals
     * @return {@code this}
     */
    public TokenCreateTransaction setDecimals(@Nonnegative int decimals) {
        requireNotFrozen();
        this.decimals = decimals;
        return this;
    }

    /**
     * Extract the initial supply of tokens.
     *
     * @return                          the initial supply of tokens
     */
    public long getInitialSupply() {
        return initialSupply;
    }

    /**
     * An initial supply, in the smallest denomination for the token.
     * <p>
     * This amount SHALL be transferred to the treasury account as part
     * of this transaction.<br/>
     * This amount MUST be specified in the smallest denomination for the
     * token (i.e. <tt>10<sup>-decimals</sup></tt> whole tokens).<br/>
     * This MUST be zero(`0`) for a non-fungible/unique token.
     *
     * @param initialSupply             the initial supply of tokens
     * @return {@code this}
     */
    public TokenCreateTransaction setInitialSupply(@Nonnegative long initialSupply) {
        requireNotFrozen();
        this.initialSupply = initialSupply;
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
     * A treasury account identifier.
     * <p>
     * This field is REQUIRED.<br/>
     * The identified account SHALL be designated the "treasury" for the
     * new token, and all tokens "minted" SHALL be delivered to that account,
     * including the initial supply, if any.<br/>
     * The identified account MUST exist, MUST NOT be expired, and SHOULD
     * have a non-zero HBAR balance.<br/>
     * The identified account SHALL be associated to the new token.
     *
     * @param accountId                 the treasury account id
     * @return {@code this}
     */
    public TokenCreateTransaction setTreasuryAccountId(AccountId accountId) {
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
     * An Hedera key for token administration.
     * <p>
     * This key, if set, SHALL have administrative authority for this token and
     * MAY authorize token update and/or token delete transactions.<br/>
     * If this key is not set, or is an empty `KeyList`, this token SHALL be
     * immutable, except for expiration and renewal.
     *
     * @param key                       the admin key
     * @return {@code this}
     */
    public TokenCreateTransaction setAdminKey(Key key) {
        requireNotFrozen();
        Objects.requireNonNull(key);
        adminKey = key;
        return this;
    }

    /**
     * Extract the know your customer key.
     *
     * @return                          the know your customer key
     */
    @Nullable
    public Key getKycKey() {
        return kycKey;
    }

    /**
     * An Hedera key for managing account KYC.
     * <p>
     * This key, if set, SHALL have KYC authority for this token and
     * MAY authorize transactions to grant or revoke KYC for accounts.<br/>
     * If this key is not set, or is an empty `KeyList`, KYC status for this
     * token SHALL NOT be granted or revoked for any account.<br/>
     * If this key is removed after granting KYC, those grants SHALL remain
     * and cannot be revoked.
     *
     * @param key                       the know your customer key
     * @return {@code this}
     */
    public TokenCreateTransaction setKycKey(Key key) {
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
     * An Hedera key for managing asset "freeze".
     * <p>
     * This key, if set, SHALL have "freeze" authority for this token and
     * MAY authorize transactions to freeze or unfreeze accounts
     * with respect to this token.<br/>
     * If this key is not set, or is an empty `KeyList`, this token
     * SHALL NOT be frozen or unfrozen for any account.<br/>
     * If this key is removed after freezing accounts, those accounts
     * SHALL remain frozen and cannot be unfrozen.
     *
     * @param key                       the freeze key
     * @return {@code this}
     */
    public TokenCreateTransaction setFreezeKey(Key key) {
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
     * An Hedera key for wiping tokens from accounts.
     * <p>
     * This key, if set, SHALL have "wipe" authority for this token and
     * MAY authorize transactions to "wipe" any amount of this token from
     * any account, effectively burning the tokens "wiped".<br/>
     * If this key is not set, or is an empty `KeyList`, it SHALL NOT be
     * possible to "wipe" this token from an account.
     *
     * @param key                       the wipe key
     * @return {@code this}
     */
    public TokenCreateTransaction setWipeKey(Key key) {
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
     * If this key is not set, or is an empty `KeyList`, it SHALL NOT be
     * possible to change the supply of tokens and neither "mint" nor "burn"
     * transactions SHALL be permitted.
     *
     * @param key                       the supply key
     * @return {@code this}
     */
    public TokenCreateTransaction setSupplyKey(Key key) {
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
     * If this key is not set, or is an empty `KeyList`, the `custom_fees`
     * for this token SHALL NOT be modified.
     *
     * @param key                       the fee schedule key
     * @return {@code this}
     */
    public TokenCreateTransaction setFeeScheduleKey(Key key) {
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
     * If this key is not set, or is an empty `KeyList`, this token
     * SHALL NOT be paused or unpaused.<br/>
     * If this key is removed while the token is paused, the token cannot
     * be unpaused and SHALL remain paused.
     *
     * @param key                      the pause key
     * @return {@code this}
     */
    public TokenCreateTransaction setPauseKey(Key key) {
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
     * An Hedera key for managing the token `metadata`.
     * <p>
     * This key, if set, MAY authorize transactions to modify the
     * `metadata` for this token.<br/>
     * If this key is not set, or is an empty `KeyList`, the `metadata`
     * for this token SHALL NOT be modified.
     *
     * @param key                      the metadata key
     * @return {@code this}
     */
    public TokenCreateTransaction setMetadataKey(Key key) {
        requireNotFrozen();
        Objects.requireNonNull(key);
        metadataKey = key;
        return this;
    }

    /**
     * Extract the freeze default.
     *
     * @return                          the freeze default
     */
    public boolean getFreezeDefault() {
        return freezeDefault;
    }

    /**
     * An initial Freeze status for accounts associated to this token.
     * <p>
     * If this value is set, an account MUST be the subject of a
     * `tokenUnfreeze` transaction after associating to the token before
     * that account can send or receive this token.<br/>
     * If this value is set, the `freezeKey` SHOULD be set.<br/>
     * If the `freezeKey` is not set, any account associated to this token
     * while this value is set SHALL be permanently frozen.
     * <p>
     * <blockquote>REVIEW NOTE<blockquote>
     * Should we prevent setting this value true for tokens with no freeze
     * key?<br/>
     * Should we set this value to false if a freeze key is removed?
     * </blockquote></blockquote>
     *
     * @param freezeDefault             the freeze default
     * @return {@code this}
     */
    public TokenCreateTransaction setFreezeDefault(boolean freezeDefault) {
        requireNotFrozen();
        this.freezeDefault = freezeDefault;
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
     * If the `autoRenewAccount` and `autoRenewPeriod` fields are set, this
     * value SHALL be replaced with the current consensus time extended
     * by the `autoRenewPeriod` duration.<br/>
     * If this value is set and token expiration is enabled in network
     * configuration, this token SHALL expire when consensus time exceeds
     * this value, and MAY be subsequently removed from the network state.<br/>
     * If this value is not set, and the automatic renewal account is also not
     * set, then this value SHALL default to the current consensus time
     * extended by the "default" expiration period from network configuration.
     *
     * Setting this value will clear the autoRenewPeriod as the autoRenewPeriod period has default value
     * of 7890000 seconds and leaving it set will override the expiration time
     *
     * @param expirationTime            the expiration time
     * @return {@code this}
     */
    public TokenCreateTransaction setExpirationTime(Instant expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        autoRenewPeriod = null;
        this.expirationTimeDuration = null;
        this.expirationTime = expirationTime;
        return this;
    }

    public TokenCreateTransaction setExpirationTime(Duration expirationTime) {
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
     * If this value is set, the token lifetime SHALL be extended by the
     * _smallest_ of the following:
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
     *
     * @param accountId                 the auto renew account id
     * @return {@code this}
     */
    public TokenCreateTransaction setAutoRenewAccountId(AccountId accountId) {
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
     * This value MUST be set.<br/>
     * This value MUST be greater than the configured
     * MIN_AUTORENEW_PERIOD.<br/>
     * This value MUST be less than the configured MAX_AUTORENEW_PERIOD.
     *
     * If expirationTime is set - autoRenewPeriod will be effectively ignored,
     * and it's effect will be replaced by expirationTime
     *
     * @param period                    the auto renew period
     * @return {@code this}
     */
    public TokenCreateTransaction setAutoRenewPeriod(Duration period) {
        Objects.requireNonNull(period);
        requireNotFrozen();
        autoRenewPeriod = period;
        return this;
    }

    /**
     * Extract the token's memo 100 bytes max.
     *
     * @return                          the token's memo 100 bytes max
     */
    public String getTokenMemo() {
        return tokenMemo;
    }

    /**
     * A short description for this token.
     * <p>
     * This value, if set, MUST NOT exceed `transaction.maxMemoUtf8Bytes`
     * (default 100) bytes when encoded as UTF-8.
     *
     * @param memo                      the token's memo 100 bytes max
     * @return {@code this}
     */
    public TokenCreateTransaction setTokenMemo(String memo) {
        Objects.requireNonNull(memo);
        requireNotFrozen();
        tokenMemo = memo;
        return this;
    }

    /**
     * Extract the custom fees.
     *
     * @return                          the custom fees
     */
    @Nullable
    public List<CustomFee> getCustomFees() {
        return CustomFee.deepCloneList(customFees);
    }

    /**
     * A list of custom fees representing a fee schedule.
     * <p>
     * This list MAY be empty, which SHALL mean that there
     * are no custom fees for this token.<br/>
     * If this token is a non-fungible/unique type, the entries
     * in this list MUST NOT declare a `fractional_fee`.<br/>
     * If this token is a fungible/common type, the entries in this
     * list MUST NOT declare a `royalty_fee`.<br/>
     * Any token type MAY include entries that declare a `fixed_fee`.
     *
     * @param customFees                the custom fees
     * @return {@code this}
     */
    public TokenCreateTransaction setCustomFees(List<CustomFee> customFees) {
        requireNotFrozen();
        this.customFees = CustomFee.deepCloneList(customFees);
        return this;
    }

    /**
     * Extract the token type.
     *
     * @return                          the token type
     */
    public TokenType getTokenType() {
        return tokenType;
    }

    /**
     * A type for this token, according to IWA classification.
     * <p>
     * If this value is not set, the token SHALL have the default type of
     * fungible/common.<br/>
     * This field SHALL be immutable.
     *
     * @param tokenType                 the token type
     * @return {@code this}
     */
    public TokenCreateTransaction setTokenType(TokenType tokenType) {
        requireNotFrozen();
        Objects.requireNonNull(tokenType);
        this.tokenType = tokenType;
        return this;
    }

    /**
     * Extract the supply type.
     *
     * @return                          the supply type
     */
    public TokenSupplyType getSupplyType() {
        return tokenSupplyType;
    }

    /**
     * A supply type for this token, according to IWA classification.
     * <p>
     * If this value is not set, the token SHALL have the default supply
     * type of "infinite" (which is, as a practical matter,
     * (2<sup><i>63</i></sup>-1)/10<sup><i>decimals</i></sup>).<br/>
     * This field SHALL be immutable.
     *
     * @param supplyType               the supply type
     * @return {@code this}
     */
    public TokenCreateTransaction setSupplyType(TokenSupplyType supplyType) {
        requireNotFrozen();
        Objects.requireNonNull(supplyType);
        tokenSupplyType = supplyType;
        return this;
    }

    /**
     * Extract the max supply of tokens.
     *
     * @return                          the max supply of tokens
     */
    public long getMaxSupply() {
        return maxSupply;
    }

    /**
     * A maximum supply for this token.
     * <p>
     * This SHALL be interpreted in terms of the smallest fractional unit for
     * this token.<br/>
     * If `supplyType` is "infinite", this MUST be `0`.<br/>
     * This field SHALL be immutable.
     *
     * @param maxSupply                 the max supply of tokens
     * @return {@code this}
     */
    public TokenCreateTransaction setMaxSupply(@Nonnegative long maxSupply) {
        requireNotFrozen();
        this.maxSupply = maxSupply;
        return this;
    }

    /**
     * Extract the token metadata.
     *
     * @return the token metadata
     */
    public byte[] getTokenMetadata() {
        return tokenMetadata;
    }

    /**
     * Token "Metadata".
     * <p>
     * The value, if set, MUST NOT exceed 100 bytes.<br/>
     * <dl><dt>Examples</dt>
     *   <dd>hcs://1/0.0.4896575</dd>
     *   <dd>ipfs://bafkreifd7tcjjuwxxf4qkaibkj62pj4mhfuud7plwrc3pfoygt55al6syi</dd>
     * </dl>
     *
     * @param tokenMetadata the token metadata
     * @return {@code this}
     */
    public TokenCreateTransaction setTokenMetadata(byte[] tokenMetadata) {
        requireNotFrozen();
        this.tokenMetadata = tokenMetadata;
        return this;
    }

    @Override
    public TokenCreateTransaction freezeWith(@Nullable Client client) {
        if (this.autoRenewAccountId == null
                && client != null
                && client.getOperatorAccountId() != null
                && autoRenewPeriod != null
                && !autoRenewPeriod.isZero()) {
            this.autoRenewAccountId = this.transactionIds != null
                            && !this.transactionIds.isEmpty()
                            && this.transactionIds.getCurrent() != null
                    ? this.transactionIds.getCurrent().accountId
                    : client.getOperatorAccountId();
        }

        return super.freezeWith(client);
    }

    /**
     * Build the transaction body.
     *
     * @return {@link com.hedera.hashgraph.sdk.proto.TokenCreateTransactionBody}
     */
    TokenCreateTransactionBody.Builder build() {
        var builder = TokenCreateTransactionBody.newBuilder();
        if (treasuryAccountId != null) {
            builder.setTreasury(treasuryAccountId.toProtobuf());
        }

        if (autoRenewAccountId != null) {
            builder.setAutoRenewAccount(autoRenewAccountId.toProtobuf());
        }
        builder.setName(tokenName);
        builder.setSymbol(tokenSymbol);
        builder.setDecimals(decimals);
        builder.setInitialSupply(initialSupply);
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
        builder.setFreezeDefault(freezeDefault);
        if (expirationTime != null) {
            builder.setExpiry(InstantConverter.toProtobuf(expirationTime));
        }
        if (expirationTimeDuration != null) {
            builder.setExpiry(InstantConverter.toProtobuf(expirationTimeDuration));
        }

        if (autoRenewPeriod != null) {
            builder.setAutoRenewPeriod(DurationConverter.toProtobuf(autoRenewPeriod));
        }
        builder.setMemo(tokenMemo);
        builder.setTokenType(tokenType.code);
        builder.setSupplyType(tokenSupplyType.code);
        builder.setMaxSupply(maxSupply);
        builder.setMetadata(ByteString.copyFrom(tokenMetadata));

        for (var fee : customFees) {
            builder.addCustomFees(fee.toProtobuf());
        }

        return builder;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenCreation();
        if (body.hasTreasury()) {
            treasuryAccountId = AccountId.fromProtobuf(body.getTreasury());
        }
        if (body.hasAutoRenewAccount()) {
            autoRenewAccountId = AccountId.fromProtobuf(body.getAutoRenewAccount());
        }
        tokenName = body.getName();
        tokenSymbol = body.getSymbol();
        decimals = body.getDecimals();
        initialSupply = body.getInitialSupply();
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
        freezeDefault = body.getFreezeDefault();
        if (body.hasExpiry()) {
            expirationTime = InstantConverter.fromProtobuf(body.getExpiry());
        }
        if (body.hasAutoRenewPeriod()) {
            autoRenewPeriod = DurationConverter.fromProtobuf(body.getAutoRenewPeriod());
        }
        tokenMemo = body.getMemo();
        tokenType = TokenType.valueOf(body.getTokenType());
        tokenSupplyType = TokenSupplyType.valueOf(body.getSupplyType());
        maxSupply = body.getMaxSupply();
        tokenMetadata = body.getMetadata().toByteArray();

        for (var fee : body.getCustomFeesList()) {
            customFees.add(CustomFee.fromProtobuf(fee));
        }
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        for (var fee : customFees) {
            fee.validateChecksums(client);
        }

        if (treasuryAccountId != null) {
            treasuryAccountId.validateChecksum(client);
        }

        if (autoRenewAccountId != null) {
            autoRenewAccountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getCreateTokenMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenCreation(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenCreation(build());
    }
}
