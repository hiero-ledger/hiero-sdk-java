// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Represent transactionReceipt response.
 *
 * @param status the status of the transaction
 * @param accountId the account ID of a newly created account
 * @param fileId the file ID of a newly created file
 * @param contractId the contract ID of a newly created contract
 * @param topicId the topic ID of a newly created topic
 * @param tokenId the token ID of a newly created token
 * @param scheduleId the schedule ID of a newly created schedule
 * @param exchangeRate the exchange rate at the time of the transaction
 * @param topicSequenceNumber the sequence number for a consensus service topic message
 * @param topicRunningHash the running hash for a consensus service topic
 * @param totalSupply the total supply of a token after a mint/burn operation
 * @param scheduledTransactionId the transaction ID of the scheduled transaction
 * @param serials the serial numbers of newly created NFTs
 * @param duplicates list of duplicate transaction receipts
 * @param children list of child transaction receipts
 * @param nodeId the node ID affected by a node related transaction
 */
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

    /**
     * Represent exchangeRate response.
     *
     * @param hbars the number of hbars in the exchange rate
     * @param cents the number of cents (USD) in the exchange rate
     * @param expirationTime the expiration time of the exchange rate
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExchangeRate(Long hbars, Long cents, String expirationTime) {}
}
