// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionReceiptResponse {
    private final String status;

    @Nullable
    private final String accountId;

    @Nullable
    private final String fileId;

    @Nullable
    private final String contractId;

    @Nullable
    private final String topicId;

    @Nullable
    private final String tokenId;

    @Nullable
    private final String scheduleId;

    @Nullable
    private final ExchangeRate exchangeRate;

    @Nullable
    private final String topicSequenceNumber;

    @Nullable
    private final String topicRunningHash;

    @Nullable
    private final String totalSupply;

    @Nullable
    private final String scheduledTransactionId;

    private final List<Long> serials;
    private final List<TransactionReceiptResponse> duplicates;
    private final List<TransactionReceiptResponse> children;

    @Nullable
    private final String nodeId;

    @Data
    public static class ExchangeRate {
        private final Long hbars;
        private final Long cents;
        private final String expirationTime;
    }
}
