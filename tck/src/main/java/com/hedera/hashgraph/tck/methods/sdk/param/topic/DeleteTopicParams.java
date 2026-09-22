// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.topic;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DeleteTopicParams for topic delete method
 */
@Getter
@AllArgsConstructor
public class DeleteTopicParams implements JSONRPC2Param {
    private Optional<String> topicId;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    public static DeleteTopicParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedTopicId = Optional.ofNullable((String) jrpcParams.get("topicId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new DeleteTopicParams(
                parsedTopicId, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
