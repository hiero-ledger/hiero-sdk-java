// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AssociateDisassociateTokenParams extends JSONRPC2Param {
    private Optional<String> accountId;
    private Optional<List<String>> tokenIds;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {

        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedTokenIds = Optional.ofNullable((List<String>) jrpcParams.get("tokenIds"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new AssociateDisassociateTokenParams(
                parsedAccountId,
                parsedTokenIds,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
