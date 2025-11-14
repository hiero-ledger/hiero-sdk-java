// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.file;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FileAppendParams for file append method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileAppendParams extends JSONRPC2Param {
    private Optional<String> fileId;
    private Optional<String> contents;
    private Optional<Long> maxChunks;
    private Optional<Long> chunkSize;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public FileAppendParams parse(Map<String, Object> jrpcParams) throws Exception {
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
