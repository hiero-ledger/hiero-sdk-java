// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressBookQueryParams implements JSONRPC2Param {
    private String fileId;
    private Long limit;
    private String sessionId;

    public static AddressBookQueryParams parse(Map<String, Object> jrpcParams) throws Exception {
        Objects.requireNonNull(jrpcParams, "jrpcParams must not be null");

        var parsedFileId = (String) jrpcParams.get("fileId");
        var parsedLimit = (Long) jrpcParams.get("limit");

        return new AddressBookQueryParams(parsedFileId, parsedLimit, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
