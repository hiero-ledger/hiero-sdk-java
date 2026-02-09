// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AddressBookQueryParams extends JSONRPC2Param {
    private Optional<String> fileId;
    private Optional<Long> limit;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedFileId = Optional.ofNullable((String) jrpcParams.get("fileId"));
        var parsedLimit = Optional.ofNullable((Long) jrpcParams.get("limit"));

        return new AddressBookQueryParams(parsedFileId, parsedLimit, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
