// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionReceiptResponse(
        String status,
        @Nullable String accountId,
        @Nullable String fileId,
        @Nullable String contractId,
        @Nullable String topicId,
        @Nullable String tokenId,
        @Nullable String scheduleId,
        @Nullable ExchangeRate exchangeRate,
        @Nullable String topicSequenceNumber,
        @Nullable String topicRunningHash,
        @Nullable String totalSupply,
        @Nullable String scheduledTransactionId,
        List<Long> serials,
        List<TransactionReceiptResponse> duplicates,
        List<TransactionReceiptResponse> children,
        @Nullable String nodeId) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExchangeRate(Long hbars, Long cents, String expirationTime) {}
}
