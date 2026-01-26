// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.param.CustomFee;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TokenCreateParams for token create method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenCreateParams extends JSONRPC2Param {
    private Optional<String> name;
    private Optional<String> symbol;
    private Optional<Long> decimals;
    private Optional<String> initialSupply;
    private Optional<String> treasuryAccountId;
    private Optional<String> adminKey;
    private Optional<String> kycKey;
    private Optional<String> freezeKey;
    private Optional<String> wipeKey;
    private Optional<String> supplyKey;
    private Optional<String> feeScheduleKey;
    private Optional<String> pauseKey;
    private Optional<String> metadataKey;
    private Optional<Boolean> freezeDefault;
    private Optional<String> expirationTime;
    private Optional<String> autoRenewAccountId;
    private Optional<String> autoRenewPeriod;
    private Optional<String> memo;
    private Optional<String> tokenType;
    private Optional<String> supplyType;
    private Optional<String> maxSupply;
    private Optional<List<CustomFee>> customFees;
    private Optional<String> metadata;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
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
