// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.file;

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
 * FileUpdateParams for file update method
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileUpdateParams extends JSONRPC2Param {
    private Optional<String> fileId;
    private Optional<List<String>> keys;
    private Optional<String> contents;
    private Optional<String> expirationTime;
    private Optional<String> memo;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public FileUpdateParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedFileId = Optional.ofNullable((String) jrpcParams.get("fileId"));
        var parsedKeys = parseStringList(jrpcParams, "keys");
        var parsedContents = Optional.ofNullable((String) jrpcParams.get("contents"));
        var parsedExpirationTime = Optional.ofNullable((String) jrpcParams.get("expirationTime"));
        var parsedMemo = Optional.ofNullable((String) jrpcParams.get("memo"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new FileUpdateParams(
                parsedFileId,
                parsedKeys,
                parsedContents,
                parsedExpirationTime,
                parsedMemo,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }

    @SuppressWarnings("unchecked")
    private Optional<List<String>> parseStringList(Map<String, Object> params, String key) {
        if (!params.containsKey(key)) {
            return Optional.empty();
        }
        Object value = params.get(key);
        if (value instanceof List) {
            return Optional.of((List<String>) value);
        }
        return Optional.empty();
    }
}
