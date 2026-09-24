// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.file;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Objects;

public record FileInfoQueryParams(String fileId, String queryPayment, String maxQueryPayment, String sessionId)
        implements JSONRPC2Param {
    public static FileInfoQueryParams parse(Map<String, Object> jrpcParams) throws Exception {
        Objects.requireNonNull(jrpcParams, "jrpcParams must not be null");

        var parsedFileId = (String) jrpcParams.get("fileId");
        var parsedQueryPayment = (String) jrpcParams.get("queryPayment");
        var parseMaxQueryPayment = (String) jrpcParams.get("maxQueryPayment");

        return new FileInfoQueryParams(
                parsedFileId, parsedQueryPayment, parseMaxQueryPayment, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
