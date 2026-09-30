// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.transfer;

import java.util.Map;
import java.util.Optional;

/**
 * Contains the parameters of a token transfer.
 */
public record TokenTransferParams(
        Optional<String> accountId, Optional<String> tokenId, Optional<String> amount, Optional<Long> decimals) {
    public static TokenTransferParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var parsedAmount = Optional.ofNullable((String) jrpcParams.get("amount"));
        var parsedDecimals = Optional.ofNullable((Long) jrpcParams.get("decimals"));

        return new TokenTransferParams(parsedAccountId, parsedTokenId, parsedAmount, parsedDecimals);
    }
}
