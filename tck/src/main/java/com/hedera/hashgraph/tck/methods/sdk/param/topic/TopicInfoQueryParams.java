// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.topic;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TopicInfoQueryParams extends JSONRPC2Param {
    private String topicId;
    private String queryPayment;
    private String maxQueryPayment;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        Objects.requireNonNull(jrpcParams, "jrpcParams must not be null");

        var parsedTopicId = (String) jrpcParams.get("topicId");
        var parsedQueryPayment = (String) jrpcParams.get("queryPayment");
        var parsedMaxQueryPayment = (String) jrpcParams.get("maxQueryPayment");

        return new TopicInfoQueryParams(
                parsedTopicId,
                parsedQueryPayment,
                parsedMaxQueryPayment,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
