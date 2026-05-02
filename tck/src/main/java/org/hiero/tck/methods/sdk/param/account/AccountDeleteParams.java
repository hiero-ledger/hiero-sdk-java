// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.account;

import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.methods.sdk.param.CommonTransactionParams;
import org.hiero.tck.util.JSONRPCParamParser;

/**
 * AccountDeleteParams for account delete method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDeleteParams extends JSONRPC2Param {
    private Optional<String> deleteAccountId;
    private Optional<String> transferAccountId;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public AccountDeleteParams parse(Map<String, Object> jrpcParams) throws Exception {
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
