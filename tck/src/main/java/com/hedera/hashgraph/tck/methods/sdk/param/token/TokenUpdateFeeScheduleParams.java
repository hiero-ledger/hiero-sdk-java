// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.methods.sdk.param.CustomFee;
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
public class TokenUpdateFeeScheduleParams extends JSONRPC2Param {

    private Optional<String> tokenId;
    private Optional<List<CustomFee>> customFees;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public JSONRPC2Param parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        var parsedCustomFees = JSONRPCParamParser.parseCustomFees(jrpcParams);

        return new TokenUpdateFeeScheduleParams(
                parsedTokenId,
                parsedCustomFees,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
