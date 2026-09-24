// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.topic;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SubmitTopicMessageParams for topic message submit method
 */
public record SubmitTopicMessageParams(
        Optional<String> topicId,
        Optional<String> message,
        Optional<Long> maxChunks,
        Optional<Long> chunkSize,
        Optional<List<CustomFeeLimit>> customFeeLimits,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static SubmitTopicMessageParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedTopicId = Optional.ofNullable((String) jrpcParams.get("topicId"));
        var parsedMessage = Optional.ofNullable((String) jrpcParams.get("message"));
        var parsedMaxChunks = Optional.ofNullable((Long) jrpcParams.get("maxChunks"));
        var parsedChunkSize = Optional.ofNullable((Long) jrpcParams.get("chunkSize"));

        @SuppressWarnings("unchecked")
        var customFeeLimitsList = (List<Map<String, Object>>) jrpcParams.get("customFeeLimits");
        Optional<List<CustomFeeLimit>> parsedCustomFeeLimits = Optional.empty();

        if (customFeeLimitsList != null) {
            var customFeeLimits = customFeeLimitsList.stream()
                    .map(customFeeLimitMap -> {
                        try {
                            return CustomFeeLimit.parse(customFeeLimitMap);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse custom fee limit", e);
                        }
                    })
                    .toList();
            parsedCustomFeeLimits = Optional.of(customFeeLimits);
        }

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new SubmitTopicMessageParams(
                parsedTopicId,
                parsedMessage,
                parsedMaxChunks,
                parsedChunkSize,
                parsedCustomFeeLimits,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
