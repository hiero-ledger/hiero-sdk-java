// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.schedule;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ScheduleDeleteParams for delete schedule method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
public class ScheduleDeleteParams implements JSONRPC2Param {
    private Optional<String> scheduleId;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    public static ScheduleDeleteParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedScheduleId = Optional.ofNullable((String) jrpcParams.get("scheduleId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new ScheduleDeleteParams(
                parsedScheduleId, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
