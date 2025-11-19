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
 * ScheduleSignParams for sign schedule method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleSignParams extends JSONRPC2Param {
    private Optional<String> scheduleId;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public ScheduleSignParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedScheduleId = Optional.ofNullable((String) jrpcParams.get("scheduleId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new ScheduleSignParams(
                parsedScheduleId, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
