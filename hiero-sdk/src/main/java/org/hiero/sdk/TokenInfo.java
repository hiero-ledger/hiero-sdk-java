// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.TokenFreezeStatus;
import org.hiero.sdk.proto.TokenGetInfoResponse;
import org.hiero.sdk.proto.TokenKycStatus;
import org.hiero.sdk.proto.TokenPauseStatus;

/**
 * Gets information about a fungible or non-fungible token instance.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/get-token-info">Hedera Documentation</a>
 */
public class TokenInfo {
    /**
     * The ID of the token for which information is requested.
     */
    public final TokenId tokenId;

    /**
     * Name of token.
     */
    public final String name;

    /**
     * Symbol of token.
     */
    public final String symbol;

    /**
     * The amount of decimal places that this token supports.
     */
    public final int decimals;

    /**
     * Total Supply of token.
     */
    public final long totalSupply;

    /**
     * The ID of the account which is set as Treasury
     */
    public final AccountId treasuryAccountId;

    /**
     * The key which can perform update/delete operations on the token. If empty, the token can be perceived as immutable (not being able to be updated/deleted)
     */
    @Nullable
    public final Key adminKey;

    /**
     * The key which can grant or revoke KYC of an account for the token's transactions. If empty, KYC is not required, and KYC grant or revoke operations are not possible.
     */
    @Nullable
    public final Key kycKey;

    /**
     * The key which can freeze or unfreeze an account for token transactions. If empty, freezing is not possible
     */
    @Nullable
    public final Key freezeKey;

    /**
     * The key which can wipe token balance of an account. If empty, wipe is not possible
     */
    @Nullable
    public final Key wipeKey;

    /**
     * The key which can change the supply of a token. The key is used to sign Token Mint/Burn operations
     */
    @Nullable
    public final Key supplyKey;

    /**
     * The key which can change the custom fees of the token; if not set, the fees are immutable
     */
    @Nullable
    public final Key feeScheduleKey;

    /**
     * The default Freeze status (not applicable, frozen or unfrozen) of Hedera accounts relative to this token. FreezeNotApplicable is returned if Token Freeze Key is empty. Frozen is returned if Token Freeze Key is set and defaultFreeze is set to true. Unfrozen is returned if Token Freeze Key is set and defaultFreeze is set to false
     */
    @Nullable
    public final Boolean defaultFreezeStatus;

    /**
     * The default KYC status (KycNotApplicable or Revoked) of Hedera accounts relative to this token. KycNotApplicable is returned if KYC key is not set, otherwise Revoked
     */
    @Nullable
    public final Boolean defaultKycStatus;

    /**
     * Specifies whether the token was deleted or not
     */
    public final boolean isDeleted;

    /**
     * An account which will be automatically charged to renew the token's expiration, at autoRenewPeriod interval
     */
    @Nullable
    public final AccountId autoRenewAccount;

    /**
     * The interval at which the auto-renew account will be charged to extend the token's expiry
     */
    @Nullable
    public final Duration autoRenewPeriod;

    /**
     * The epoch second at which the token will expire
     */
    @Nullable
    public final Instant expirationTime;

    /**
     * The memo associated with the token
     */
    public final String tokenMemo;

    /**
     * The custom fees to be assessed during a CryptoTransfer that transfers units of this token
     */
    public final List<CustomFee> customFees;

    /**
     * The token type
     */
    public final TokenType tokenType;

    /**
     * The token supply type
     */
    public final TokenSupplyType supplyType;

    /**
     * For tokens of type FUNGIBLE_COMMON - The Maximum number of fungible tokens that can be in
     * circulation. For tokens of type NON_FUNGIBLE_UNIQUE - the maximum number of NFTs (serial
     * numbers) that can be in circulation
     */
    public final long maxSupply;

    /**
     * The Key which can pause and unpause the Token.
     */
    @Nullable
    public final Key pauseKey;

    /**
     * Specifies whether the token is paused or not. Null if pauseKey is not set.
     */
    @Nullable
    public final Boolean pauseStatus;

    /**
     * Represents the metadata of the token definition.
     */
    public byte[] metadata = {};

    /**
     * The key which can change the metadata of a token
     * (token definition and individual NFTs).
     */
    @Nullable
    public final Key metadataKey;

    /**
     * The ledger ID the response was returned from; please see <a href="https://github.com/hashgraph/hedera-improvement-proposal/blob/master/HIP/hip-198.md">HIP-198</a> for the network-specific IDs.
     */
    public final LedgerId ledgerId;

    TokenInfo(
            TokenId tokenId,
            String name,
            String symbol,
            int decimals,
            long totalSupply,
            AccountId treasuryAccountId,
            @Nullable Key adminKey,
            @Nullable Key kycKey,
            @Nullable Key freezeKey,
            @Nullable Key wipeKey,
            @Nullable Key supplyKey,
            @Nullable Key feeScheduleKey,
            @Nullable Boolean defaultFreezeStatus,
            @Nullable Boolean defaultKycStatus,
            boolean isDeleted,
            @Nullable AccountId autoRenewAccount,
            @Nullable Duration autoRenewPeriod,
            @Nullable Instant expirationTime,
            String tokenMemo,
            List<CustomFee> customFees,
            TokenType tokenType,
            TokenSupplyType supplyType,
            long maxSupply,
            @Nullable Key pauseKey,
            @Nullable Boolean pauseStatus,
            byte[] metadata,
            @Nullable Key metadataKey,
            LedgerId ledgerId) {
        this.tokenId = tokenId;
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.totalSupply = totalSupply;
        this.treasuryAccountId = treasuryAccountId;
        this.adminKey = adminKey;
        this.kycKey = kycKey;
        this.freezeKey = freezeKey;
        this.wipeKey = wipeKey;
        this.supplyKey = supplyKey;
        this.feeScheduleKey = feeScheduleKey;
        this.defaultFreezeStatus = defaultFreezeStatus;
        this.defaultKycStatus = defaultKycStatus;
        this.isDeleted = isDeleted;
        this.autoRenewAccount = autoRenewAccount;
        this.autoRenewPeriod = autoRenewPeriod;
        this.expirationTime = expirationTime;
        this.tokenMemo = tokenMemo;
        this.customFees = customFees;
        this.tokenType = tokenType;
        this.supplyType = supplyType;
        this.maxSupply = maxSupply;
        this.pauseKey = pauseKey;
        this.pauseStatus = pauseStatus;
        this.metadata = metadata;
        this.metadataKey = metadataKey;
        this.ledgerId = ledgerId;
    }

    /**
     * Are we frozen?
     *
     * @param freezeStatus              the freeze status
     * @return                          true / false / null
     */
    @Nullable
    static Boolean freezeStatusFromProtobuf(TokenFreezeStatus freezeStatus) {
        return freezeStatus == TokenFreezeStatus.FreezeNotApplicable ? null : freezeStatus == TokenFreezeStatus.Frozen;
    }

    /**
     * Is kyc required?
     *
     * @param kycStatus                 the kyc status
     * @return                          true / false / null
     */
    @Nullable
    static Boolean kycStatusFromProtobuf(TokenKycStatus kycStatus) {
        return kycStatus == TokenKycStatus.KycNotApplicable ? null : kycStatus == TokenKycStatus.Granted;
    }

    /**
     * Are we paused?
     *
     * @param pauseStatus               the paused status
     * @return                          true / false / null
     */
    @Nullable
    static Boolean pauseStatusFromProtobuf(TokenPauseStatus pauseStatus) {
        return pauseStatus == TokenPauseStatus.PauseNotApplicable ? null : pauseStatus == TokenPauseStatus.Paused;
    }

    /**
     * Create a token info object from a protobuf.
     *
     * @param response                  the protobuf
     * @return                          new token info object
     */
    static TokenInfo fromProtobuf(TokenGetInfoResponse response) {
        var info = response.getTokenInfo();

        return new TokenInfo(
                TokenId.fromProtobuf(info.getTokenId()),
                info.getName(),
                info.getSymbol(),
                info.getDecimals(),
                info.getTotalSupply(),
                AccountId.fromProtobuf(info.getTreasury()),
                info.hasAdminKey() ? Key.fromProtobufKey(info.getAdminKey()) : null,
                info.hasKycKey() ? Key.fromProtobufKey(info.getKycKey()) : null,
                info.hasFreezeKey() ? Key.fromProtobufKey(info.getFreezeKey()) : null,
                info.hasWipeKey() ? Key.fromProtobufKey(info.getWipeKey()) : null,
                info.hasSupplyKey() ? Key.fromProtobufKey(info.getSupplyKey()) : null,
                info.hasFeeScheduleKey() ? Key.fromProtobufKey(info.getFeeScheduleKey()) : null,
                freezeStatusFromProtobuf(info.getDefaultFreezeStatus()),
                kycStatusFromProtobuf(info.getDefaultKycStatus()),
                info.getDeleted(),
                info.hasAutoRenewAccount() ? AccountId.fromProtobuf(info.getAutoRenewAccount()) : null,
                info.hasAutoRenewPeriod() ? DurationConverter.fromProtobuf(info.getAutoRenewPeriod()) : null,
                info.hasExpiry() ? InstantConverter.fromProtobuf(info.getExpiry()) : null,
                info.getMemo(),
                customFeesFromProto(info),
                TokenType.valueOf(info.getTokenType()),
                TokenSupplyType.valueOf(info.getSupplyType()),
                info.getMaxSupply(),
                info.hasPauseKey() ? Key.fromProtobufKey(info.getPauseKey()) : null,
                pauseStatusFromProtobuf(info.getPauseStatus()),
                info.getMetadata().toByteArray(),
                info.hasMetadataKey() ? Key.fromProtobufKey(info.getMetadataKey()) : null,
                LedgerId.fromByteString(info.getLedgerId()));
    }

    /**
     * Create a token info object from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new token info object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static TokenInfo fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(TokenGetInfoResponse.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Create custom fee list from protobuf.
     *
     * @param info                      the protobuf
     * @return                          the list of custom fee's
     */
    private static List<CustomFee> customFeesFromProto(org.hiero.sdk.proto.TokenInfo info) {
        var returnCustomFees = new ArrayList<CustomFee>(info.getCustomFeesCount());
        for (var feeProto : info.getCustomFeesList()) {
            returnCustomFees.add(CustomFee.fromProtobuf(feeProto));
        }
        return returnCustomFees;
    }

    /**
     * Create a token freeze status protobuf.
     *
     * @param freezeStatus              the freeze status
     * @return                          the protobuf
     */
    static TokenFreezeStatus freezeStatusToProtobuf(@Nullable Boolean freezeStatus) {
        return freezeStatus == null
                ? TokenFreezeStatus.FreezeNotApplicable
                : freezeStatus ? TokenFreezeStatus.Frozen : TokenFreezeStatus.Unfrozen;
    }

    /**
     * Create a kyc status protobuf.
     *
     * @param kycStatus                 the kyc status
     * @return                          the protobuf
     */
    static TokenKycStatus kycStatusToProtobuf(@Nullable Boolean kycStatus) {
        return kycStatus == null
                ? TokenKycStatus.KycNotApplicable
                : kycStatus ? TokenKycStatus.Granted : TokenKycStatus.Revoked;
    }

    /**
     * Create a pause status protobuf.
     *
     * @param pauseStatus               the pause status
     * @return                          the protobuf
     */
    static TokenPauseStatus pauseStatusToProtobuf(@Nullable Boolean pauseStatus) {
        return pauseStatus == null
                ? TokenPauseStatus.PauseNotApplicable
                : pauseStatus ? TokenPauseStatus.Paused : TokenPauseStatus.Unpaused;
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    TokenGetInfoResponse toProtobuf() {
        var tokenInfoBuilder = org.hiero.sdk.proto.TokenInfo.newBuilder()
                .setTokenId(tokenId.toProtobuf())
                .setName(name)
                .setSymbol(symbol)
                .setDecimals(decimals)
                .setTotalSupply(totalSupply)
                .setTreasury(treasuryAccountId.toProtobuf())
                .setDefaultFreezeStatus(freezeStatusToProtobuf(defaultFreezeStatus))
                .setDefaultKycStatus(kycStatusToProtobuf(defaultKycStatus))
                .setDeleted(isDeleted)
                .setMemo(tokenMemo)
                .setTokenType(tokenType.code)
                .setSupplyType(supplyType.code)
                .setMaxSupply(maxSupply)
                .setPauseStatus(pauseStatusToProtobuf(pauseStatus))
                .setLedgerId(ledgerId.toByteString());
        if (adminKey != null) {
            tokenInfoBuilder.setAdminKey(adminKey.toProtobufKey());
        }
        if (kycKey != null) {
            tokenInfoBuilder.setKycKey(kycKey.toProtobufKey());
        }
        if (freezeKey != null) {
            tokenInfoBuilder.setFreezeKey(freezeKey.toProtobufKey());
        }
        if (wipeKey != null) {
            tokenInfoBuilder.setWipeKey(wipeKey.toProtobufKey());
        }
        if (supplyKey != null) {
            tokenInfoBuilder.setSupplyKey(supplyKey.toProtobufKey());
        }
        if (feeScheduleKey != null) {
            tokenInfoBuilder.setFeeScheduleKey(feeScheduleKey.toProtobufKey());
        }
        if (pauseKey != null) {
            tokenInfoBuilder.setPauseKey(pauseKey.toProtobufKey());
        }
        if (metadata != null) {
            tokenInfoBuilder.setMetadata(ByteString.copyFrom(metadata));
        }
        if (metadataKey != null) {
            tokenInfoBuilder.setMetadataKey(metadataKey.toProtobufKey());
        }
        if (autoRenewAccount != null) {
            tokenInfoBuilder.setAutoRenewAccount(autoRenewAccount.toProtobuf());
        }
        if (autoRenewPeriod != null) {
            tokenInfoBuilder.setAutoRenewPeriod(DurationConverter.toProtobuf(autoRenewPeriod));
        }
        if (expirationTime != null) {
            tokenInfoBuilder.setExpiry(InstantConverter.toProtobuf(expirationTime));
        }
        for (var fee : customFees) {
            tokenInfoBuilder.addCustomFees(fee.toProtobuf());
        }
        return TokenGetInfoResponse.newBuilder().setTokenInfo(tokenInfoBuilder).build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tokenId", tokenId)
                .add("name", name)
                .add("symbol", symbol)
                .add("decimals", decimals)
                .add("totalSupply", totalSupply)
                .add("treasuryAccountId", treasuryAccountId)
                .add("adminKey", adminKey)
                .add("kycKey", kycKey)
                .add("freezeKey", freezeKey)
                .add("wipeKey", wipeKey)
                .add("supplyKey", supplyKey)
                .add("feeScheduleKey", feeScheduleKey)
                .add("defaultFreezeStatus", defaultFreezeStatus)
                .add("defaultKycStatus", defaultKycStatus)
                .add("isDeleted", isDeleted)
                .add("autoRenewAccount", autoRenewAccount)
                .add("autoRenewPeriod", autoRenewPeriod)
                .add("expirationTime", expirationTime)
                .add("tokenMemo", tokenMemo)
                .add("customFees", customFees)
                .add("tokenType", tokenType)
                .add("supplyType", supplyType)
                .add("maxSupply", maxSupply)
                .add("pauseKey", pauseKey)
                .add("pauseStatus", pauseStatus)
                .add("metadata", metadata)
                .add("metadataKey", metadataKey)
                .add("ledgerId", ledgerId)
                .toString();
    }

    /**
     * Create the byte array.
     *
     * @return                          the byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }
}
