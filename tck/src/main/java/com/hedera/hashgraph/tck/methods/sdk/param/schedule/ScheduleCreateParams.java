// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.schedule;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ScheduleCreateParams for schedule create method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleCreateParams extends JSONRPC2Param {
    private Optional<ScheduledTransaction> scheduledTransaction;
    private Optional<String> memo;
    private Optional<String> adminKey;
    private Optional<String> payerAccountId;
    private Optional<String> expirationTime;
    private Optional<Boolean> waitForExpiry;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public ScheduleCreateParams parse(Map<String, Object> jrpcParams) throws Exception {
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
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduledTransaction {
        private String method;
        private Map<String, Object> params;

        public ScheduledTransaction(Map<String, Object> data) {
            this.method = (String) data.get("method");
            this.params = (Map<String, Object>) data.get("params");
        }
    }
}
