// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.transfer;

import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Contains the parameters of an NFT transfer.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NftTransferParams {
    private Optional<String> senderAccountId;
    private Optional<String> receiverAccountId;
    private Optional<String> tokenId;
    private Optional<String> serialNumber;

    public static NftTransferParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedSenderAccountId = Optional.ofNullable((String) jrpcParams.get("senderAccountId"));
        var parsedReceiverAccountId = Optional.ofNullable((String) jrpcParams.get("receiverAccountId"));
        var parsedTokenId = Optional.ofNullable((String) jrpcParams.get("tokenId"));
        var parsedSerialNumber = Optional.ofNullable((String) jrpcParams.get("serialNumber"));

        return new NftTransferParams(parsedSenderAccountId, parsedReceiverAccountId, parsedTokenId, parsedSerialNumber);
    }
}
