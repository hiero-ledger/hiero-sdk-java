// SPDX-License-Identifier: Apache-2.0
package org.hiero.tck.methods.sdk.param.contract;

import org.hiero.tck.methods.JSONRPC2Param;
import org.hiero.tck.methods.sdk.param.CommonTransactionParams;
import org.hiero.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DeleteContractParams for contract delete method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteContractParams extends JSONRPC2Param {
    private Optional<String> contractId;
    private Optional<String> transferAccountId;
    private Optional<String> transferContractId;
    private Optional<Boolean> permanentRemoval;
    private Optional<CommonTransactionParams> commonTransactionParams;
    private String sessionId;

    @Override
    public DeleteContractParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedContractId = Optional.ofNullable((String) jrpcParams.get("contractId"));
        var parsedTransferAccountId = Optional.ofNullable((String) jrpcParams.get("transferAccountId"));
        var parsedTransferContractId = Optional.ofNullable((String) jrpcParams.get("transferContractId"));
        var parsedPermanentRemoval = Optional.ofNullable((Boolean) jrpcParams.get("permanentRemoval"));

        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new DeleteContractParams(
                parsedContractId,
                parsedTransferAccountId,
                parsedTransferContractId,
                parsedPermanentRemoval,
                parsedCommonTransactionParams,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}

