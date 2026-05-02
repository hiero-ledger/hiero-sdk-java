// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.token;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.util.JSONRPCParamParser;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NftInfoQueryParams extends JSONRPC2Param {
    private String nftId;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedNftId = (String) jrpcParams.get("nftId");
        return new NftInfoQueryParams(parsedNftId, JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
