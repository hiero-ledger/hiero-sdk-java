// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NftInfoQueryParams implements JSONRPC2Param {
    private String nftId;
    private String sessionId;

    public static NftInfoQueryParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedNftId = (String) jrpcParams.get("nftId");
        return new NftInfoQueryParams(parsedNftId, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
