// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.topic;

import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.util.JSONRPCParamParser;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TopicInfoQueryParams extends JSONRPC2Param {
    private String topicId;
    private String queryPayment;
    private String maxQueryPayment;
    private String sessionId;

    @Override
    public TopicInfoQueryParams parse(Map<String, Object> jrpcParams) throws Exception {
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
