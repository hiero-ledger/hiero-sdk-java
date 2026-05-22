// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionReceiptQueryParams extends JSONRPC2Param {
    private String transactionId;
    private Boolean includeDuplicates;
    private Boolean includeChildren;
    private Boolean validateStatus;
    private String sessionId;

    @Override
    public TransactionReceiptQueryParams parse(Map<String, Object> jrpcParams) throws Exception {
        Objects.requireNonNull(jrpcParams, "jrpcParams must not be null");

        String parsedTransactionId = (String) jrpcParams.get("transactionId");
        Boolean parsedIncludeDuplicates = (Boolean) jrpcParams.get("includeDuplicates");
        Boolean parsedIncludeChildren = (Boolean) jrpcParams.get("includeChildren");
        Boolean parsedValidateStatus = (Boolean) jrpcParams.get("validateStatus");

        return new TransactionReceiptQueryParams(
                parsedTransactionId,
                parsedIncludeDuplicates != null ? parsedIncludeDuplicates : false,
                parsedIncludeChildren != null ? parsedIncludeChildren : false,
                parsedValidateStatus != null ? parsedValidateStatus : true,
                JSONRPCParamParser.parseSessionId(jrpcParams));
    }
}
