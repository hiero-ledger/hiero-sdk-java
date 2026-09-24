// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.topic;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.param.CustomFee;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CreateTopicParams for topic create method
 */
public record CreateTopicParams(
        Optional<String> memo,
        Optional<String> adminKey,
        Optional<String> submitKey,
        Optional<String> autoRenewPeriod,
        Optional<String> autoRenewAccountId,
        Optional<String> feeScheduleKey,
        Optional<List<String>> feeExemptKeys,
        Optional<List<CustomFee>> customFees,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static CreateTopicParams parse(Map<String, Object> jrpcParams) throws Exception {
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
