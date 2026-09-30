// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.schedule;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * ScheduleSignParams for sign schedule method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record ScheduleSignParams(
        Optional<String> scheduleId, Optional<CommonTransactionParams> commonTransactionParams, String sessionId)
        implements JSONRPC2Param {
    public static ScheduleSignParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedScheduleId = Optional.ofNullable((String) jrpcParams.get("scheduleId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new ScheduleSignParams(
                parsedScheduleId, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
