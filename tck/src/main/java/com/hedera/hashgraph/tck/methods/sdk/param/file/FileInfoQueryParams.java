package com.hedera.hashgraph.tck.methods.sdk.param.file;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Optional;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoQueryParams extends JSONRPC2Param {
    private String fileId;
    private Optional<String> queryPayment;
    private Optional<String> maxQueryPayment;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedFileId = (String) jrpcParams.get("fileId");
        var parsedQueryPayment = Optional.ofNullable((String) jrpcParams.get("queryPayment"));
        var parseMaxQueryPayment = Optional.ofNullable((String) jrpcParams.get("maxQueryPayment"));

        return new FileInfoQueryParams(
            parsedFileId, parsedQueryPayment, parseMaxQueryPayment, JSONRPCParamParser.parseSessionId(jrpcParams)
        );
    }
}
