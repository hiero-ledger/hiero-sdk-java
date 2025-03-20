package com.hedera.hashgraph.tck.methods.sdk.param.transfer;


import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Contains the parameters of a Hbar transfer.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class HbarTransferParams {
    private Optional<String> accountId;
    private Optional<String> evmAddress;
    private Optional<String> amount;

    public static HbarTransferParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedEvmAddress = Optional.ofNullable((String) jrpcParams.get("evmAddress"));
        var parsedAmount = Optional.ofNullable((String) jrpcParams.get("amount"));

        if ((parsedAccountId.isPresent() && parsedEvmAddress.isPresent())
            || (parsedAccountId.isEmpty() && parsedEvmAddress.isEmpty())) {
            throw new IllegalArgumentException("invalid parameters: only one of accountId or evmAddress SHALL be provided.");
        }

        return new HbarTransferParams(parsedAccountId, parsedEvmAddress, parsedAmount);
    }
}
