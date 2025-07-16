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
 * FileDeleteParams for file delete method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileDeleteParams extends JSONRPC2Param {
    private Optional<String> fileId;
    private Optional<CommonTransactionParams> commonTransactionParams;

    @Override
    public FileDeleteParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedFileId = Optional.ofNullable((String) jrpcParams.get("fileId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new FileDeleteParams(parsedFileId, parsedCommonTransactionParams);
    }
}
