// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.CustomFee;
import com.hedera.hashgraph.sdk.TokenSupplyType;
import com.hedera.hashgraph.sdk.TokenType;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Represent the tokenInfo query response.
 *
 * @param tokenId the ID of the token for which information is requested
 * @param name the name of token
 * @param symbol the symbol of token
 * @param decimals the amount of decimal places that this token supports
 * @param totalSupply total Supply of token
 * @param treasuryAccountId the ID of the account which is set as treasury
 * @param adminKey the key which can perform update/delete operations on the token
 * @param kycKey the key which can grant or revoke KYC of an account for the token's transactions
 * @param freezeKey the key which can freeze or unfreeze an account for token transactions
 * @param wipeKey the key which can wipe token balance of an account
 * @param supplyKey the key which can change the supply of a token
 * @param feeScheduleKey the key which can change the custom fees of the token
 * @param defaultFreezeStatus the default Freeze status (not applicable, frozen or unfrozen) of Hedera accounts relative to this token
 * @param defaultKycStatus the default KYC status (KycNotApplicable or Revoked) of Hedera accounts relative to this token
 * @param isDeleted specifies whether the token was deleted or not
 * @param autoRenewAccountId an account which will be automatically charged to renew the token's expiration, at autoRenewPeriod interval
 * @param autoRenewPeriod the interval at which the auto_renew account will be charged to extend the token's expiry
 * @param expirationTime the epoch second at which the token will expire
 * @param tokenMemo the memo associated with the token
 * @param customFees the custom fees to be assessed during a CryptoTransfer that transfers units of this token
 * @param tokenType the token type
 * @param supplyType the token supply type
 * @param maxSupply the maximum number of fungible tokens that can be in circulation
 * @param pauseKey the Key which can pause and unpause the token
 * @param pauseStatus specifies whether the token is paused or not, null if pauseKey is not set.
 * @param metadata the metadata for the nft
 * @param metadataKey the key which can change the metadata of a token
 * @param ledgerId the ledger ID
 */
public record TokenInfoResponse(
        String tokenId,
        String name,
        String symbol,
        int decimals,
        String totalSupply,
        String treasuryAccountId,
        @Nullable String adminKey,
        @Nullable String kycKey,
        @Nullable String freezeKey,
        @Nullable String wipeKey,
        @Nullable String supplyKey,
        @Nullable String feeScheduleKey,
        @Nullable Boolean defaultFreezeStatus,
        @Nullable Boolean defaultKycStatus,
        boolean isDeleted,
        @Nullable String autoRenewAccountId,
        @Nullable String autoRenewPeriod,
        @Nullable String expirationTime,
        String tokenMemo,
        List<CustomFee> customFees,
        TokenType tokenType,
        TokenSupplyType supplyType,
        String maxSupply,
        @Nullable String pauseKey,
        @Nullable Boolean pauseStatus,
        String metadata,
        @Nullable String metadataKey,
        String ledgerId) {}
