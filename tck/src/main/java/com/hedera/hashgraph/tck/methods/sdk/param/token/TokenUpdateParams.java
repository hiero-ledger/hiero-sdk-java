// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TokenUpdateParams for token update method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenUpdateParams extends JSONRPC2Param {
    private Optional<String> tokenId;
    private Optional<String> name;
    private Optional<String> symbol;
    private Optional<String> treasuryAccountId;
    private Optional<String> adminKey;
    private Optional<String> kycKey;
    private Optional<String> freezeKey;
    private Optional<String> wipeKey;
    private Optional<String> supplyKey;
    private Optional<String> feeScheduleKey;
    private Optional<String> pauseKey;
    private Optional<String> metadataKey;
    private Optional<String> expirationTime;
    private Optional<String> autoRenewAccountId;
    private Optional<String> autoRenewPeriod;
    private Optional<String> memo;
    private Optional<String> metadata;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
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
