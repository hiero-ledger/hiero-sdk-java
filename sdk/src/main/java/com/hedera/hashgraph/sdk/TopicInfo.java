// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ConsensusGetTopicInfoResponse;
import com.hedera.hashgraph.sdk.proto.ConsensusTopicInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Current state of a topic.
 */
public final class TopicInfo {
    /**
     * The ID of the topic for which information is requested.
     */
    public final TopicId topicId;

    /**
     * Short publicly visible memo about the topic. No guarantee of uniqueness.
     */
    public final String topicMemo;

    /**
     * SHA-384 running hash of (previousRunningHash, topicId, consensusTimestamp, sequenceNumber, message).
     */
    public final ByteString runningHash;

    /**
     * Sequence number (starting at 1 for the first submitMessage) of messages on the topic.
     */
    public final long sequenceNumber;

    /**
     * Effective consensus timestamp at (and after) which submitMessage calls will no longer succeed on the topic.
     */
    public final Instant expirationTime;

    /**
     * Access control for update/delete of the topic. Null if there is no key.
     */
    @Nullable
    public final Key adminKey;

    /**
     * Access control for ConsensusService.submitMessage. Null if there is no key.
     */
    @Nullable
    public final Key submitKey;

    /**
     * If an auto-renew account is specified, when the topic expires, its lifetime will be extended
     * by up to this duration (depending on the solvency of the auto-renew account). If the
     * auto-renew account has no funds at all, the topic will be deleted instead.
     */
    public final Duration autoRenewPeriod;

    /**
     * The account, if any, to charge for automatic renewal of the topic's lifetime upon expiry.
     */
    @Nullable
    public final AccountId autoRenewAccountId;

    /**
     * The ledger ID the response was returned from; please see <a href="https://github.com/hashgraph/hedera-improvement-proposal/blob/master/HIP/hip-198.md">HIP-198</a> for the network-specific IDs.
     */
    public final LedgerId ledgerId;

    public final Key feeScheduleKey;

    public final List<Key> feeExemptKeys;

    public final List<CustomFixedFee> customFees;

    private TopicInfo(
            TopicId topicId,
            String topicMemo,
            ByteString runningHash,
            long sequenceNumber,
            Instant expirationTime,
            @Nullable Key adminKey,
            @Nullable Key submitKey,
            Duration autoRenewPeriod,
            @Nullable AccountId autoRenewAccountId,
            LedgerId ledgerId,
            Key feeScheduleKey,
            List<Key> feeExemptKeys,
            List<CustomFixedFee> customFees) {
        this.topicId = topicId;
        this.topicMemo = topicMemo;
        this.runningHash = runningHash;
        this.sequenceNumber = sequenceNumber;
        this.expirationTime = expirationTime;
        this.adminKey = adminKey;
        this.submitKey = submitKey;
        this.autoRenewPeriod = autoRenewPeriod;
        this.autoRenewAccountId = autoRenewAccountId;
        this.ledgerId = ledgerId;
        this.feeScheduleKey = feeScheduleKey;
        this.feeExemptKeys = feeExemptKeys;
        this.customFees = customFees;
    }

    /**
     * Create a topic info object from a protobuf.
     *
     * @param topicInfoResponse         the protobuf
     * @return                          the new topic info object
     */
    static TopicInfo fromProtobuf(ConsensusGetTopicInfoResponse topicInfoResponse) {
        var topicInfo = topicInfoResponse.getTopicInfo();

        var adminKey = topicInfo.hasAdminKey() ? Key.fromProtobufKey(topicInfo.getAdminKey()) : null;

        var submitKey = topicInfo.hasSubmitKey() ? Key.fromProtobufKey(topicInfo.getSubmitKey()) : null;

        var autoRenewAccountId =
                topicInfo.hasAutoRenewAccount() ? AccountId.fromProtobuf(topicInfo.getAutoRenewAccount()) : null;

        var feeScheduleKey = topicInfo.hasFeeScheduleKey() ? Key.fromProtobufKey(topicInfo.getFeeScheduleKey()) : null;

        var feeExemptKeys = topicInfo.getFeeExemptKeyListList() != null
                ? topicInfo.getFeeExemptKeyListList().stream()
                        .map(Key::fromProtobufKey)
                        .toList()
                : null;

        var customFees = topicInfo.getCustomFeesList() != null
                ? topicInfo.getCustomFeesList().stream()
                        .map(CustomFixedFee::fromProtobuf)
                        .toList()
                : null;

        return new TopicInfo(
                TopicId.fromProtobuf(topicInfoResponse.getTopicID()),
                topicInfo.getMemo(),
                topicInfo.getRunningHash(),
                topicInfo.getSequenceNumber(),
                InstantConverter.fromProtobuf(topicInfo.getExpirationTime()),
                adminKey,
                submitKey,
                DurationConverter.fromProtobuf(topicInfo.getAutoRenewPeriod()),
                autoRenewAccountId,
                LedgerId.fromByteString(topicInfo.getLedgerId()),
                feeScheduleKey,
                feeExemptKeys,
                customFees);
    }

    /**
     * Create a topic info object from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new topic info object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static TopicInfo fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(
                ConsensusGetTopicInfoResponse.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    ConsensusGetTopicInfoResponse toProtobuf() {
        var topicInfoResponseBuilder =
                ConsensusGetTopicInfoResponse.newBuilder().setTopicID(topicId.toProtobuf());

        var topicInfoBuilder = ConsensusTopicInfo.newBuilder()
                .setMemo(topicMemo)
                .setRunningHash(runningHash)
                .setSequenceNumber(sequenceNumber)
                .setExpirationTime(InstantConverter.toProtobuf(expirationTime))
                .setAutoRenewPeriod(DurationConverter.toProtobuf(autoRenewPeriod))
                .setLedgerId(ledgerId.toByteString());

        if (adminKey != null) {
            topicInfoBuilder.setAdminKey(adminKey.toProtobufKey());
        }

        if (submitKey != null) {
            topicInfoBuilder.setSubmitKey(submitKey.toProtobufKey());
        }

        if (autoRenewAccountId != null) {
            topicInfoBuilder.setAutoRenewAccount(autoRenewAccountId.toProtobuf());
        }

        if (feeScheduleKey != null) {
            topicInfoBuilder.setFeeScheduleKey(feeScheduleKey.toProtobufKey());
        }

        if (feeExemptKeys != null) {
            for (Key feeExemptKey : feeExemptKeys) {
                topicInfoBuilder.addFeeExemptKeyList(feeExemptKey.toProtobufKey());
            }
        }

        if (customFees != null) {
            for (CustomFixedFee customFee : customFees) {
                topicInfoBuilder.addCustomFees(customFee.toTopicFeeProtobuf());
            }
        }

        return topicInfoResponseBuilder.setTopicInfo(topicInfoBuilder).build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("topicId", topicId)
                .add("topicMemo", topicMemo)
                .add("runningHash", runningHash.toByteArray())
                .add("sequenceNumber", sequenceNumber)
                .add("expirationTime", expirationTime)
                .add("adminKey", adminKey)
                .add("submitKey", submitKey)
                .add("autoRenewPeriod", autoRenewPeriod)
                .add("autoRenewAccountId", autoRenewAccountId)
                .add("ledgerId", ledgerId)
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
