// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.file;

import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.methods.sdk.param.CommonTransactionParams;
import org.hiero.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FileDeleteParams for file delete method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileDeleteParams extends JSONRPC2Param {
    private Optional<String> fileId;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public FileDeleteParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedFileId = Optional.ofNullable((String) jrpcParams.get("fileId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new FileDeleteParams(
                parsedFileId, parsedCommonTransactionParams, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}

