// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * TokenUpdateParams for token update method
 */
public record TokenUpdateParams(
        Optional<String> tokenId,
        Optional<String> name,
        Optional<String> symbol,
        Optional<String> treasuryAccountId,
        Optional<String> adminKey,
        Optional<String> kycKey,
        Optional<String> freezeKey,
        Optional<String> wipeKey,
        Optional<String> supplyKey,
        Optional<String> feeScheduleKey,
        Optional<String> pauseKey,
        Optional<String> metadataKey,
        Optional<String> expirationTime,
        Optional<String> autoRenewAccountId,
        Optional<String> autoRenewPeriod,
        Optional<String> memo,
        Optional<String> metadata,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static TokenUpdateParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var parsedName = Optional.ofNullable((String) jrpcParams.get("name"));
        var parsedSymbol = Optional.ofNullable((String) jrpcParams.get("symbol"));
        var parsedTreasuryAccountId = Optional.ofNullable((String) jrpcParams.get("treasuryAccountId"));
        var parsedAdminKey = Optional.ofNullable((String) jrpcParams.get("adminKey"));
        var parsedKycKey = Optional.ofNullable((String) jrpcParams.get("kycKey"));
        var parsedFreezeKey = Optional.ofNullable((String) jrpcParams.get("freezeKey"));
        var parsedWipeKey = Optional.ofNullable((String) jrpcParams.get("wipeKey"));
        var parsedSupplyKey = Optional.ofNullable((String) jrpcParams.get("supplyKey"));
        var parsedFeeScheduleKey = Optional.ofNullable((String) jrpcParams.get("feeScheduleKey"));
        var parsedPauseKey = Optional.ofNullable((String) jrpcParams.get("pauseKey"));
        var parsedMetadataKey = Optional.ofNullable((String) jrpcParams.get("metadataKey"));
        var parsedExpirationTime = Optional.ofNullable((String) jrpcParams.get("expirationTime"));
        var parsedAutoRenewAccountId = Optional.ofNullable((String) jrpcParams.get("autoRenewAccountId"));
        var parsedAutoRenewPeriod = Optional.ofNullable((String) jrpcParams.get("autoRenewPeriod"));
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedMetadata = Optional.ofNullable((String) jrpcParams.get("metadata"));

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new TokenUpdateParams(
                parsedTokenId,
                parsedName,
                parsedSymbol,
                parsedTreasuryAccountId,
                parsedAdminKey,
                parsedKycKey,
                parsedFreezeKey,
                parsedWipeKey,
                parsedSupplyKey,
                parsedFeeScheduleKey,
                parsedPauseKey,
                parsedMetadataKey,
                parsedExpirationTime,
                parsedAutoRenewAccountId,
                parsedAutoRenewPeriod,
                parsedMemo,
                parsedMetadata,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
