// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.topic;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SubmitTopicMessageParams for topic message submit method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubmitTopicMessageParams extends JSONRPC2Param {
    private Optional<String> topicId;
    private Optional<String> message;
    private Optional<Long> maxChunks;
    private Optional<Long> chunkSize;
    private Optional<List<CustomFeeLimit>> customFeeLimits;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
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
                            return (CustomFeeLimit) new CustomFeeLimit().parse(customFeeLimitMap);
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
