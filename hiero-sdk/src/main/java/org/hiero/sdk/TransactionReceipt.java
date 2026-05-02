// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bouncycastle.util.encoders.Hex;
import org.hiero.sdk.proto.ExchangeRateSet;
import org.hiero.sdk.proto.TimestampSeconds;

/**
 * The consensus result for a transaction, which might not be currently
 * known, or may succeed or fail.
 */
public final class TransactionReceipt {

    /**
     * The transaction's ID
     */
    @Nullable
    public final TransactionId transactionId;
    /**
     * Whether the transaction succeeded or failed (or is unknown).
     */
    public final Status status;

    /**
     * The exchange rate of Hbars to cents (USD).
     */
    public final ExchangeRate exchangeRate;

    /**
     * Next exchange rate which will take effect when current rate expires
     */
    public final ExchangeRate nextExchangeRate;

    /**
     * The account ID, if a new account was created.
     */
    @Nullable
    public final AccountId accountId;

    /**
     * The file ID, if a new file was created.
     */
    @Nullable
    public final FileId fileId;

    /**
     * The contract ID, if a new contract was created.
     */
    @Nullable
    public final ContractId contractId;

    /**
     * The topic ID, if a new topic was created.
     */
    @Nullable
    public final TopicId topicId;

    /**
     * The token ID, if a new token was created.
     */
    @Nullable
    public final TokenId tokenId;

    /**
     * Updated sequence number for a consensus service topic.
     * Set for {@link TopicMessageSubmitTransaction}.
     */
    @Nullable
    public final Long topicSequenceNumber;

    /**
     * Updated running hash for a consensus service topic.
     * Set for {@link TopicMessageSubmitTransaction}.
     */
    @Nullable
    public final ByteString topicRunningHash;

    /**
     * In the receipt of TokenMint, TokenWipe, TokenBurn, For fungible tokens - the current total
     * supply of this token. For non fungible tokens - the total number of NFTs issued for a given
     * tokenID
     */
    public final Long totalSupply;

    /**
     * In the receipt of a ScheduleCreate, the id of the newly created Scheduled Entity
     */
    @Nullable
    public final ScheduleId scheduleId;

    /**
     * In the receipt of a ScheduleCreate or ScheduleSign that resolves to SUCCESS, the
     * TransactionID that should be used to query for the receipt or record of the relevant
     * scheduled transaction
     */
    @Nullable
    public final TransactionId scheduledTransactionId;

    /**
     * In the receipt of a TokenMint for tokens of type NON_FUNGIBLE_UNIQUE, the serial numbers of
     * the newly created NFTs
     */
    public final List<Long> serials;

    /**
     * In the receipt of a NodeCreate, NodeUpdate, NodeDelete, the id of the newly created node.
     * An affected node identifier.<br/>
     * This value SHALL be set following a `createNode` transaction.<br/>
     * This value SHALL be set following a `updateNode` transaction.<br/>
     * This value SHALL be set following a `deleteNode` transaction.<br/>
     * This value SHALL NOT be set following any other transaction.
     */
    public final long nodeId;

    /**
     * The receipts of processing all transactions with the given id, in consensus time order.
     */
    public final List<TransactionReceipt> duplicates;

    /**
     * The receipts (if any) of all child transactions spawned by the transaction with the
     * given top-level id, in consensus order. Always empty if the top-level status is UNKNOWN.
     */
    public final List<TransactionReceipt> children;

    TransactionReceipt(
            @Nullable TransactionId transactionId,
            Status status,
            ExchangeRate exchangeRate,
            ExchangeRate nextExchangeRate,
            @Nullable AccountId accountId,
            @Nullable FileId fileId,
            @Nullable ContractId contractId,
            @Nullable TopicId topicId,
            @Nullable TokenId tokenId,
            @Nullable Long topicSequenceNumber,
            @Nullable ByteString topicRunningHash,
            Long totalSupply,
            @Nullable ScheduleId scheduleId,
            @Nullable TransactionId scheduledTransactionId,
            List<Long> serials,
            long nodeId,
            List<TransactionReceipt> duplicates,
            List<TransactionReceipt> children) {
        this.transactionId = transactionId;
        this.status = status;
        this.exchangeRate = exchangeRate;
        this.nextExchangeRate = nextExchangeRate;
        this.accountId = accountId;
        this.fileId = fileId;
        this.contractId = contractId;
        this.topicId = topicId;
        this.tokenId = tokenId;
        this.topicSequenceNumber = topicSequenceNumber;
        this.topicRunningHash = topicRunningHash;
        this.totalSupply = totalSupply;
        this.scheduleId = scheduleId;
        this.scheduledTransactionId = scheduledTransactionId;
        this.serials = serials;
        this.nodeId = nodeId;
        this.duplicates = duplicates;
        this.children = children;
    }

    /**
     * Create transaction receipt from protobuf.
     *
     * @param transactionReceipt        the protobuf
     * @param duplicates                list of duplicates
     * @param children                  list of children
     * @return                          the new transaction receipt
     */
    static TransactionReceipt fromProtobuf(
            org.hiero.sdk.proto.TransactionReceipt transactionReceipt,
            List<TransactionReceipt> duplicates,
            List<TransactionReceipt> children,
            @Nullable TransactionId transactionId) {
        var status = Status.valueOf(transactionReceipt.getStatus());

        var rate = transactionReceipt.getExchangeRate();
        var exchangeRate = ExchangeRate.fromProtobuf(rate.getCurrentRate());
        var nextExchangeRate = ExchangeRate.fromProtobuf(rate.getNextRate());

        var accountId =
                transactionReceipt.hasAccountID() ? AccountId.fromProtobuf(transactionReceipt.getAccountID()) : null;

        var fileId = transactionReceipt.hasFileID() ? FileId.fromProtobuf(transactionReceipt.getFileID()) : null;

        var contractId =
                transactionReceipt.hasContractID() ? ContractId.fromProtobuf(transactionReceipt.getContractID()) : null;

        var topicId = transactionReceipt.hasTopicID() ? TopicId.fromProtobuf(transactionReceipt.getTopicID()) : null;

        var tokenId = transactionReceipt.hasTokenID() ? TokenId.fromProtobuf(transactionReceipt.getTokenID()) : null;

        var topicSequenceNumber =
                transactionReceipt.getTopicSequenceNumber() == 0 ? null : transactionReceipt.getTopicSequenceNumber();

        var topicRunningHash =
                transactionReceipt.getTopicRunningHash().isEmpty() ? null : transactionReceipt.getTopicRunningHash();

        var totalSupply = transactionReceipt.getNewTotalSupply();

        var scheduleId =
                transactionReceipt.hasScheduleID() ? ScheduleId.fromProtobuf(transactionReceipt.getScheduleID()) : null;

        var scheduledTransactionId = transactionReceipt.hasScheduledTransactionID()
                ? TransactionId.fromProtobuf(transactionReceipt.getScheduledTransactionID())
                : null;

        var serials = transactionReceipt.getSerialNumbersList();

        var nodeId = transactionReceipt.getNodeId();

        return new TransactionReceipt(
                transactionId,
                status,
                exchangeRate,
                nextExchangeRate,
                accountId,
                fileId,
                contractId,
                topicId,
                tokenId,
                topicSequenceNumber,
                topicRunningHash,
                totalSupply,
                scheduleId,
                scheduledTransactionId,
                serials,
                nodeId,
                duplicates,
                children);
    }

    /**
     * Create a transaction receipt from a protobuf.
     *
     * @param transactionReceipt        the protobuf
     * @return                          the new transaction receipt
     */
    public static TransactionReceipt fromProtobuf(org.hiero.sdk.proto.TransactionReceipt transactionReceipt) {
        return fromProtobuf(transactionReceipt, new ArrayList<>(), new ArrayList<>(), null);
    }

    static TransactionReceipt fromProtobuf(
            org.hiero.sdk.proto.TransactionReceipt transactionReceipt, @Nullable TransactionId transactionId) {
        return fromProtobuf(transactionReceipt, new ArrayList<>(), new ArrayList<>(), transactionId);
    }

    /**
     * Create a transaction receipt from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new transaction receipt
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static TransactionReceipt fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(org.hiero.sdk.proto.TransactionReceipt.parseFrom(bytes).toBuilder()
                .build());
    }

    /**
     * Validate the transaction status in the receipt.
     *
     * @param shouldValidate Whether to perform transaction status validation
     * @return {@code this}
     * @throws ReceiptStatusException when shouldValidate is true and the transaction status is not SUCCESS
     */
    public TransactionReceipt validateStatus(boolean shouldValidate) throws ReceiptStatusException {
        if (shouldValidate && status != Status.SUCCESS && status != Status.FEE_SCHEDULE_FILE_PART_UPLOADED) {
            throw new ReceiptStatusException(transactionId, this);
        }
        return this;
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    org.hiero.sdk.proto.TransactionReceipt toProtobuf() {
        var transactionReceiptBuilder = org.hiero.sdk.proto.TransactionReceipt.newBuilder()
                .setStatus(status.code)
                .setExchangeRate(ExchangeRateSet.newBuilder()
                        .setCurrentRate(org.hiero.sdk.proto.ExchangeRate.newBuilder()
                                .setHbarEquiv(exchangeRate.hbars)
                                .setCentEquiv(exchangeRate.cents)
                                .setExpirationTime(TimestampSeconds.newBuilder()
                                        .setSeconds(exchangeRate.expirationTime.getEpochSecond())))
                        .setNextRate(org.hiero.sdk.proto.ExchangeRate.newBuilder()
                                .setHbarEquiv(nextExchangeRate.hbars)
                                .setCentEquiv(nextExchangeRate.cents)
                                .setExpirationTime(TimestampSeconds.newBuilder()
                                        .setSeconds(nextExchangeRate.expirationTime.getEpochSecond()))))
                .setNewTotalSupply(totalSupply);

        if (accountId != null) {
            transactionReceiptBuilder.setAccountID(accountId.toProtobuf());
        }

        if (fileId != null) {
            transactionReceiptBuilder.setFileID(fileId.toProtobuf());
        }

        if (contractId != null) {
            transactionReceiptBuilder.setContractID(contractId.toProtobuf());
        }

        if (topicId != null) {
            transactionReceiptBuilder.setTopicID(topicId.toProtobuf());
        }

        if (tokenId != null) {
            transactionReceiptBuilder.setTokenID(tokenId.toProtobuf());
        }

        if (topicSequenceNumber != null) {
            transactionReceiptBuilder.setTopicSequenceNumber(topicSequenceNumber);
        }

        if (topicRunningHash != null) {
            transactionReceiptBuilder.setTopicRunningHash(topicRunningHash);
        }

        if (scheduleId != null) {
            transactionReceiptBuilder.setScheduleID(scheduleId.toProtobuf());
        }

        if (scheduledTransactionId != null) {
            transactionReceiptBuilder.setScheduledTransactionID(scheduledTransactionId.toProtobuf());
        }

        for (var serial : serials) {
            transactionReceiptBuilder.addSerialNumbers(serial);
        }

        transactionReceiptBuilder.setNodeId(nodeId);

        return transactionReceiptBuilder.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("transactionId", transactionId)
                .add("status", status)
                .add("exchangeRate", exchangeRate)
                .add("nextExchangeRate", nextExchangeRate)
                .add("accountId", accountId)
                .add("fileId", fileId)
                .add("contractId", contractId)
                .add("topicId", topicId)
                .add("tokenId", tokenId)
                .add("topicSequenceNumber", topicSequenceNumber)
                .add("topicRunningHash", topicRunningHash != null ? Hex.encode(topicRunningHash.toByteArray()) : null)
                .add("totalSupply", totalSupply)
                .add("scheduleId", scheduleId)
                .add("scheduledTransactionId", scheduledTransactionId)
                .add("serials", serials)
                .add("nodeId", nodeId)
                .add("duplicates", duplicates)
                .add("children", children)
                .toString();
    }

    /**
     * Create the byte array.
     *
     * @return                          the byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }
}
