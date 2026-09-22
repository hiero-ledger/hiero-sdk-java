// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.file;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FileDeleteParams for file delete method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
public class FileDeleteParams implements JSONRPC2Param {
    private Optional<String> fileId;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    public static FileDeleteParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedFileId = Optional.ofNullable((String) jrpcParams.get("fileId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new FileDeleteParams(
                parsedFileId, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
