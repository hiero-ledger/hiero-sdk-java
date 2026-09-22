// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.schedule;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScheduleInfoParams implements JSONRPC2Param {
    private String scheduleId;
    private String queryPayment;
    private String maxQueryPayment;
    private Boolean getCost;
    private String sessionId;

    public static JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        Objects.requireNonNull(jrpcParams, "jrpcParams must not be null");

        var parsedScheduleId = (String) jrpcParams.get("scheduleId");
        var parsedQueryPayment = (String) jrpcParams.get("queryPayment");
        var parsedMaxQueryPayment = (String) jrpcParams.get("maxQueryPayment");
        var parsedGetCost = (Boolean) jrpcParams.get("getCost");

        return new ScheduleInfoParams(
                parsedScheduleId,
                parsedQueryPayment,
                parsedMaxQueryPayment,
                parsedGetCost != null ? parsedGetCost : false,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
