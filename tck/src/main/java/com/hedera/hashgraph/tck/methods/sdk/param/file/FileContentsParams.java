// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.file;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GetFileContentsParams for get file contents method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileContentsParams extends JSONRPC2Param {
    private String fileId;
    private Optional<String> queryPayment;
    private Optional<String> maxQueryPayment;
    private String sessionId;

    @Override
    public FileContentsParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedFileId = (String) jrpcParams.get("fileId");
        var parsedQueryPayment = Optional.ofNullable((String) jrpcParams.get("queryPayment"));
        var parsedMaxQueryPayment = Optional.ofNullable((String) jrpcParams.get("maxQueryPayment"));

        return new FileContentsParams(
                parsedFileId, parsedQueryPayment, parsedMaxQueryPayment, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
