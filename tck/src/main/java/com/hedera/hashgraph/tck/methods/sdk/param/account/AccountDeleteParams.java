// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.account;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;

/**
 * AccountDeleteParams for account delete method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record AccountDeleteParams(
        Optional<String> deleteAccountId,
        Optional<String> transferAccountId,
        Optional<CommonTransactionParams> commonTransactionParams,
        String sessionId)
        implements JSONRPC2Param {
    public static AccountDeleteParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedDeleteAccountId = Optional.ofNullable((String) jrpcParams.get("deleteAccountId"));
        var parsedTransferAccountId = Optional.ofNullable((String) jrpcParams.get("transferAccountId"));

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new AccountDeleteParams(
                parsedDeleteAccountId,
                parsedTransferAccountId,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
