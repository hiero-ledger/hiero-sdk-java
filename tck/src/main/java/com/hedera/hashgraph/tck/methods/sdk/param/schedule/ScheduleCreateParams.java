// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.schedule;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * ScheduleCreateParams for schedule create method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record ScheduleCreateParams(
        Optional<ScheduledTransaction> scheduledTransaction,
        Optional<String> memo,
        Optional<String> adminKey,
        Optional<String> payerAccountId,
        Optional<String> expirationTime,
        Optional<Boolean> waitForExpiry,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static ScheduleCreateParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedScheduledTransaction = Optional.ofNullable(
                        (Map<String, Object>) jrpcParams.get("scheduledTransaction"))
                .map(ScheduledTransaction::new);
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedAdminKey = Optional.ofNullable((String) jrpcParams.get("adminKey"));
        var parsedPayerAccountId = Optional.ofNullable((String) jrpcParams.get("payerAccountId"));
        var parsedExpirationTime = Optional.ofNullable((String) jrpcParams.get("expirationTime"));
        var parsedWaitForExpiry = Optional.ofNullable((Boolean) jrpcParams.get("waitForExpiry"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new ScheduleCreateParams(
                parsedScheduledTransaction,
                parsedMemo,
                parsedAdminKey,
                parsedPayerAccountId,
                parsedExpirationTime,
                parsedWaitForExpiry,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }

    /**
     * Represents a scheduled transaction with method and params
     */
    public record ScheduledTransaction(String method, Map<String, Object> params) {
        public ScheduledTransaction(Map<String, Object> data) {
            this((String) data.get("method"), (Map<String, Object>) data.get("params"));
        }
    }
}
