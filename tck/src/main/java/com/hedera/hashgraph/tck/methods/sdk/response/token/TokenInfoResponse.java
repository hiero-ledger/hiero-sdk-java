// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.CustomFee;
import com.hedera.hashgraph.sdk.TokenSupplyType;
import com.hedera.hashgraph.sdk.TokenType;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;

@Data
public class TokenInfoResponse {

    /**
     * The ID of the token for which information is requested.
     */
    public final String tokenId;

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
    public final String totalSupply;

    /**
     * The ID of the account which is set as Treasury
     */
    public final String treasuryAccountId;

    /**
     * The key which can perform update/delete operations on the token. If empty, the token can be perceived as immutable (not being able to be updated/deleted)
     */
    @Nullable
    public final String adminKey;

    /**
     * The key which can grant or revoke KYC of an account for the token's transactions. If empty, KYC is not required, and KYC grant or revoke operations are not possible.
     */
    @Nullable
    public final String kycKey;

    /**
     * The key which can freeze or unfreeze an account for token transactions. If empty, freezing is not possible
     */
    @Nullable
    public final String freezeKey;

    /**
     * The key which can wipe token balance of an account. If empty, wipe is not possible
     */
    @Nullable
    public final String wipeKey;

    /**
     * The key which can change the supply of a token. The key is used to sign Token Mint/Burn operations
     */
    @Nullable
    public final String supplyKey;

    /**
     * The key which can change the custom fees of the token; if not set, the fees are immutable
     */
    @Nullable
    public final String feeScheduleKey;

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
    public final String autoRenewAccountId;

    /**
     * The interval at which the auto-renew account will be charged to extend the token's expiry
     */
    @Nullable
    public final String autoRenewPeriod;

    /**
     * The epoch second at which the token will expire
     */
    @Nullable
    public final String expirationTime;

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
    public final String maxSupply;

    /**
     * The Key which can pause and unpause the Token.
     */
    @Nullable
    public final String pauseKey;

    /**
     * Specifies whether the token is paused or not. Null if pauseKey is not set.
     */
    @Nullable
    public final Boolean pauseStatus;

    /**
     * The key which can change the metadata of a token
     * (token definition and individual NFTs).
     */
    public final String metadata;

    /**
     * The key which can change the metadata of a token
     * (token definition and individual NFTs).
     */
    @Nullable
    public final String metadataKey;

    /**
     * The ledger ID the response was returned from; please see <a href="https://github.com/hashgraph/hedera-improvement-proposal/blob/master/HIP/hip-198.md">HIP-198</a> for the network-specific IDs.
     */
    public final String ledgerId;
}
