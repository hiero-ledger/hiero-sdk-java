// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.param.CustomFee;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TokenCreateParams for token create method
 */
public record TokenCreateParams(
        Optional<String> name,
        Optional<String> symbol,
        Optional<Long> decimals,
        Optional<String> initialSupply,
        Optional<String> treasuryAccountId,
        Optional<String> adminKey,
        Optional<String> kycKey,
        Optional<String> freezeKey,
        Optional<String> wipeKey,
        Optional<String> supplyKey,
        Optional<String> feeScheduleKey,
        Optional<String> pauseKey,
        Optional<String> metadataKey,
        Optional<Boolean> freezeDefault,
        Optional<String> expirationTime,
        Optional<String> autoRenewAccountId,
        Optional<String> autoRenewPeriod,
        Optional<String> memo,
        Optional<String> tokenType,
        Optional<String> supplyType,
        Optional<String> maxSupply,
        Optional<List<CustomFee>> customFees,
        Optional<String> metadata,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static TokenCreateParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedName = Optional.ofNullable((String) jrpcParams.get("name"));
        var parsedSymbol = Optional.ofNullable((String) jrpcParams.get("symbol"));
        var parsedDecimals = Optional.ofNullable((Long) jrpcParams.get("decimals"));
        var parsedInitialSupply = Optional.ofNullable((String) jrpcParams.get("initialSupply"));
        var parsedTreasuryAccountId = Optional.ofNullable((String) jrpcParams.get("treasuryAccountId"));
        var parsedAdminKey = Optional.ofNullable((String) jrpcParams.get("adminKey"));
        var parsedKycKey = Optional.ofNullable((String) jrpcParams.get("kycKey"));
        var parsedFreezeKey = Optional.ofNullable((String) jrpcParams.get("freezeKey"));
        var parsedWipeKey = Optional.ofNullable((String) jrpcParams.get("wipeKey"));
        var parsedSupplyKey = Optional.ofNullable((String) jrpcParams.get("supplyKey"));
        var parsedFeeScheduleKey = Optional.ofNullable((String) jrpcParams.get("feeScheduleKey"));
        var parsedPauseKey = Optional.ofNullable((String) jrpcParams.get("pauseKey"));
        var parsedMetadataKey = Optional.ofNullable((String) jrpcParams.get("metadataKey"));
        var parsedFreezeDefault = Optional.ofNullable((Boolean) jrpcParams.get("freezeDefault"));
        var parsedExpirationTime = Optional.ofNullable((String) jrpcParams.get("expirationTime"));
        var parsedAutoRenewAccountId = Optional.ofNullable((String) jrpcParams.get("autoRenewAccountId"));
        var parsedAutoRenewPeriod = Optional.ofNullable((String) jrpcParams.get("autoRenewPeriod"));
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedTokenType = Optional.ofNullable((String) jrpcParams.get("tokenType"));
        var parsedSupplyType = Optional.ofNullable((String) jrpcParams.get("supplyType"));
        var parsedMaxSupply = Optional.ofNullable((String) jrpcParams.get("maxSupply"));
        var parsedMetadata = Optional.ofNullable((String) jrpcParams.get("metadata"));

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        var parsedCustomFees = JSONRPCParamParser.parseCustomFees(jrpcParams);

        return new TokenCreateParams(
                parsedName,
                parsedSymbol,
                parsedDecimals,
                parsedInitialSupply,
                parsedTreasuryAccountId,
                parsedAdminKey,
                parsedKycKey,
                parsedFreezeKey,
                parsedWipeKey,
                parsedSupplyKey,
                parsedFeeScheduleKey,
                parsedPauseKey,
                parsedMetadataKey,
                parsedFreezeDefault,
                parsedExpirationTime,
                parsedAutoRenewAccountId,
                parsedAutoRenewPeriod,
                parsedMemo,
                parsedTokenType,
                parsedSupplyType,
                parsedMaxSupply,
                parsedCustomFees,
                parsedMetadata,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
