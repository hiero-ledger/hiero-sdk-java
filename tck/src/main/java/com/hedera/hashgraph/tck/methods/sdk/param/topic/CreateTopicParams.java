// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.topic;

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
 * CreateTopicParams for topic create method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateTopicParams extends JSONRPC2Param {
    private Optional<String> memo;
    private Optional<String> adminKey;
    private Optional<String> submitKey;
    private Optional<String> autoRenewPeriod;
    private Optional<String> autoRenewAccountId;
    private Optional<String> feeScheduleKey;
    private Optional<List<String>> feeExemptKeys;
    private Optional<List<CustomFee>> customFees;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedAdminKey = Optional.ofNullable((String) jrpcParams.get("adminKey"));
        var parsedSubmitKey = Optional.ofNullable((String) jrpcParams.get("submitKey"));
        var parsedAutoRenewPeriod = Optional.ofNullable((String) jrpcParams.get("autoRenewPeriod"));
        var parsedAutoRenewAccountId = Optional.ofNullable((String) jrpcParams.get("autoRenewAccountId"));
        var parsedFeeScheduleKey = Optional.ofNullable((String) jrpcParams.get("feeScheduleKey"));

        @SuppressWarnings("unchecked")
        var parsedFeeExemptKeys = Optional.ofNullable((List<String>) jrpcParams.get("feeExemptKeys"));

        var parsedCustomFees = JSONRPCParamParser.parseCustomFees(jrpcParams);
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new CreateTopicParams(
                parsedMemo,
                parsedAdminKey,
                parsedSubmitKey,
                parsedAutoRenewPeriod,
                parsedAutoRenewAccountId,
                parsedFeeScheduleKey,
                parsedFeeExemptKeys,
                parsedCustomFees,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
