// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AddressBookQueryParams extends JSONRPC2Param {
    private String fileId;
    private Long limit;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        if (jrpcParams == null) {
            throw new IllegalArgumentException("jrpcParams cannot be null");
        }

        var parsedFileId = (String) jrpcParams.get("fileId");
        var parsedLimit = (Long) jrpcParams.get("limit");

        return new AddressBookQueryParams(parsedFileId, parsedLimit, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
