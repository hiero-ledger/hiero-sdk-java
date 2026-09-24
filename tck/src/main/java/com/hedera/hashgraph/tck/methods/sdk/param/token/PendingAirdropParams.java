// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.param.token;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PendingAirdropParams(
        Optional<String> tokenId,
        Optional<String> senderAccountId,
        Optional<String> receiverAccountId,
        Optional<List<String>> serialNumbers) {
    public static PendingAirdropParams parse(Map<String, Object> params) throws Exception {
        var parsedTokenId = Optional.ofNullable((String) params.get("tokenId"));
        var parsedSenderAccountId = Optional.ofNullable((String) params.get("senderAccountId"));
        var parsedReceiverAccountId = Optional.ofNullable((String) params.get("receiverAccountId"));
        var parsedSerialNumbers = Optional.ofNullable((List<String>) params.get("serialNumbers"));

        return new PendingAirdropParams(
                parsedTokenId, parsedSenderAccountId, parsedReceiverAccountId, parsedSerialNumbers);
    }
}
