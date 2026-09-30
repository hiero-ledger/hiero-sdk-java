// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.file;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * FileAppendParams for file append method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record FileAppendParams(
        Optional<String> fileId,
        Optional<String> contents,
        Optional<Long> maxChunks,
        Optional<Long> chunkSize,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static FileAppendParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedFileId = Optional.ofNullable((String) jrpcParams.get("fileId"));
        var parsedContents = Optional.ofNullable((String) jrpcParams.get("contents"));
        var parsedMaxChunks = Optional.ofNullable((Long) jrpcParams.get("maxChunks"));
        var parsedChunkSize = Optional.ofNullable((Long) jrpcParams.get("chunkSize"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new FileAppendParams(
                parsedFileId,
                parsedContents,
                parsedMaxChunks,
                parsedChunkSize,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
