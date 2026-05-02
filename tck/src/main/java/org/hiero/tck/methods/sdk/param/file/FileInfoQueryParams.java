// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.file;

import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoQueryParams extends JSONRPC2Param {
    private String fileId;
    private String queryPayment;
    private String maxQueryPayment;
    private String sessionId;

    @Override
    public FileInfoQueryParams parse(Map<String, Object> jrpcParams) throws Exception {
        Objects.requireNonNull(jrpcParams, "jrpcParams must not be null");

        var parsedFileId = (String) jrpcParams.get("fileId");
        var parsedQueryPayment = (String) jrpcParams.get("queryPayment");
        var parseMaxQueryPayment = (String) jrpcParams.get("maxQueryPayment");

        return new FileInfoQueryParams(
                parsedFileId, parsedQueryPayment, parseMaxQueryPayment, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}

