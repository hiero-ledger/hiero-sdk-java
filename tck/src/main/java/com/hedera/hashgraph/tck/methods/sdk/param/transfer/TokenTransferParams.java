package com.hedera.hashgraph.tck.methods.sdk.param.transfer;

import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Contains the parameters of a token transfer.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenTransferParams {
    private Optional<String> accountId;
    private Optional<String> tokenId;
    private Optional<String> amount;
    private Optional<Long> decimals;

    public static TokenTransferParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var parsedAmount = Optional.ofNullable((String) jrpcParams.get("amount"));
        var parsedDecimals = Optional.ofNullable((Long) jrpcParams.get("decimals"));

        return new TokenTransferParams(
            parsedAccountId,
            parsedTokenId,
            parsedAmount,
            parsedDecimals);
    }
}
