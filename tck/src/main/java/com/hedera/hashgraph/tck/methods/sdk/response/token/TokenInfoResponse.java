// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response.token;

import com.hedera.hashgraph.sdk.CustomFee;
import com.hedera.hashgraph.sdk.TokenSupplyType;
import com.hedera.hashgraph.sdk.TokenType;
import java.util.List;
import javax.annotation.Nullable;

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
