// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.hedera.hashgraph.sdk.proto.*;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Update a topic.
 * <p>
 * If there is no adminKey, the only authorized update (available to anyone) is to extend the expirationTime.
 * Otherwise transaction must be signed by the adminKey.
 * <p>
 * If an adminKey is updated, the transaction must be signed by the pre-update adminKey and post-update adminKey.
 * <p>
 * If a new autoRenewAccount is specified (not just being removed), that account must also sign the transaction.
 */
public final class TopicUpdateTransaction extends Transaction<TopicUpdateTransaction> {

    @Nullable
    private TopicId topicId = null;

    @Nullable
    private AccountId autoRenewAccountId = null;

    @Nullable
    private String topicMemo = null;

    @Nullable
    private Key adminKey = null;

    @Nullable
    private Key submitKey = null;

    @Nullable
    private Duration autoRenewPeriod = null;

    @Nullable
    private Instant expirationTime = null;

    private Key feeScheduleKey = null;

    private List<Key> feeExemptKeys = null;

    private List<CustomFixedFee> customFees = null;

    /**
     * Constructor.
     */
    public TopicUpdateTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TopicUpdateTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TopicUpdateTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the topic id.
     *
     * @return                          the topic id
     */
    @Nullable
    public TopicId getTopicId() {
        return topicId;
    }

    /**
     * The topic ID specifying the topic to update.
     * <p>
     * A topic with this ID MUST exist and MUST NOT be deleted.<br/>
     * This value is REQUIRED.
     *
     * @param topicId The TopicId to be set
     * @return {@code this}
     */
    public TopicUpdateTransaction setTopicId(TopicId topicId) {
        Objects.requireNonNull(topicId);
        requireNotFrozen();
        this.topicId = topicId;
        return this;
    }

    /**
     * Extract the topic memo.
     *
     * @return                          the topic memo
     */
    @Nullable
    public String getTopicMemo() {
        return topicMemo;
    }

    /**
     * An updated memo to be associated with this topic.
     * <p>
     * If this value is set, the current `adminKey` for the topic MUST sign
     * this transaction.<br/>
     * This value, if set, SHALL be encoded UTF-8 and SHALL NOT exceed
     * 100 bytes when so encoded.
     *
     * @param memo The memo to be set
     * @return {@code this}
     */
    public TopicUpdateTransaction setTopicMemo(String memo) {
        Objects.requireNonNull(memo);
        requireNotFrozen();
        topicMemo = memo;
        return this;
    }

    /**
     * Clear the memo for this topic.
     *
     * @return {@code this}
     */
    public TopicUpdateTransaction clearTopicMemo() {
        requireNotFrozen();
        topicMemo = "";
        return this;
    }

    /**
     * Extract the admin key.
     *
     * @return                          the admin key
     */
    @Nullable
    public Key getAdminKey() {
        return adminKey;
    }

    /**
     * Updated access control for modification of the topic.
     * <p>
     * If this field is set, that key and the previously set key MUST both
     * sign this transaction.<br/>
     * If this value is an empty `KeyList`, the prior key MUST sign this
     * transaction, and the topic SHALL be immutable after this transaction
     * completes, except for expiration and renewal.
     *
     * @param adminKey The Key to be set
     * @return {@code this}
     */
    public TopicUpdateTransaction setAdminKey(Key adminKey) {
        Objects.requireNonNull(adminKey);
        requireNotFrozen();
        this.adminKey = adminKey;
        return this;
    }

    /**
     * Clear the admin key for this topic.
     *
     * @return {@code this}
     */
    public TopicUpdateTransaction clearAdminKey() {
        requireNotFrozen();
        adminKey = new KeyList();
        return this;
    }

    /**
     * Extract the submit key.
     *
     * @return                          the submit key
     */
    @Nullable
    public Key getSubmitKey() {
        return submitKey;
    }

    /**
     * Updated access control for message submission to the topic.
     * <p>
     * If this value is set, the current `adminKey` for the topic MUST sign
     * this transaction.<br/>
     * If this value is set to an empty `KeyList`, the `submitKey` for the
     * topic will be unset after this transaction completes. When the
     * `submitKey` is unset, any account may submit a message on the topic,
     * without restriction.
     *
     * @param submitKey The Key to be set
     * @return {@code this}
     */
    public TopicUpdateTransaction setSubmitKey(Key submitKey) {
        Objects.requireNonNull(submitKey);
        requireNotFrozen();
        this.submitKey = submitKey;
        return this;
    }

    /**
     * Clear the submit key for this topic.
     *
     * @return {@code this}
     */
    public TopicUpdateTransaction clearSubmitKey() {
        requireNotFrozen();
        submitKey = new KeyList();
        return this;
    }

    /**
     * Extract the auto renew period.
     *
     * @return                          the auto renew period
     */
    @Nullable
    public Duration getAutoRenewPeriod() {
        return autoRenewPeriod;
    }

    /*
     * An updated value for the number of seconds by which the topic expiration
     * will be automatically extended upon expiration, if it has a valid
     * auto-renew account.
     * <p>
     * If this value is set, the current `adminKey` for the topic MUST sign
     * this transaction.<br/>
     * This value, if set, MUST be greater than the
     * configured MIN_AUTORENEW_PERIOD.<br/>
     * This value, if set, MUST be less than the
     * configured MAX_AUTORENEW_PERIOD.
     *
     * @param autoRenewPeriod The Duration to be set for auto renewal
     * @return {@code this}
     */
    public TopicUpdateTransaction setAutoRenewPeriod(Duration autoRenewPeriod) {
        Objects.requireNonNull(autoRenewPeriod);
        requireNotFrozen();
        this.autoRenewPeriod = autoRenewPeriod;
        return this;
    }

    /**
     * Extract the auto renew account id.
     *
     * @return                          the auto renew account id
     */
    @Nullable
    public AccountId getAutoRenewAccountId() {
        return autoRenewAccountId;
    }

    /**
     * An updated ID for the account to be charged renewal fees at the topic's
     * `expirationTime` to extend the lifetime of the topic.
     * <p>
     * If this value is set and not the "sentinel account", the referenced
     * account MUST sign this transaction.<br/>
     * If this value is set, the current `adminKey` for the topic MUST sign
     * this transaction.<br/>
     * If this value is set to the "sentinel account", which is `0.0.0`, the
     * `autoRenewAccount` SHALL be removed from the topic.
     *
     * @param autoRenewAccountId The AccountId to be set for auto renewal
     * @return {@code this}
     */
    public TopicUpdateTransaction setAutoRenewAccountId(AccountId autoRenewAccountId) {
        Objects.requireNonNull(autoRenewAccountId);
        requireNotFrozen();
        this.autoRenewAccountId = autoRenewAccountId;
        return this;
    }

    /**
     * @param autoRenewAccountId The AccountId to be cleared for auto renewal
     * @return {@code this}
     * @deprecated Use {@link #clearAutoRenewAccountId()}
     * <p>
     * Clear the auto renew account ID for this topic.
     */
    @SuppressWarnings("MissingSummary")
    @Deprecated
    public TopicUpdateTransaction clearAutoRenewAccountId(AccountId autoRenewAccountId) {
        return clearAutoRenewAccountId();
    }

    /**
     * Clear the auto renew account ID for this topic.
     *
     * @return {@code this}
     */
    public TopicUpdateTransaction clearAutoRenewAccountId() {
        requireNotFrozen();
        autoRenewAccountId = new AccountId(0, 0, 0);
        return this;
    }

    /**
     * @return Expiration time
     */
    @Nullable
    public Instant getExpirationTime() {
        return expirationTime;
    }

    /**
     * Sets the effective consensus timestamp at (and after) which all consensus transactions and queries will fail.
     * The expirationTime may be no longer than MAX_AUTORENEW_PERIOD (8000001 seconds) from the consensus timestamp of
     * this transaction.
     * On topics with no adminKey, extending the expirationTime is the only updateTopic option allowed on the topic.
     * @param expirationTime the new expiration time
     * @return {@code this}
     */
    public TopicUpdateTransaction setExpirationTime(Instant expirationTime) {
        requireNotFrozen();
        this.expirationTime = Objects.requireNonNull(expirationTime);
        return this;
    }

    /**
     * Returns the key which allows updates to the new topic’s fees.
     * @return feeScheduleKey
     */
    public Key getFeeScheduleKey() {
        return feeScheduleKey;
    }

    /**
     * Sets the key which allows updates to the new topic’s fees.
     * @param feeScheduleKey the feeScheduleKey
     * @return {@code this}
     */
    public TopicUpdateTransaction setFeeScheduleKey(Key feeScheduleKey) {
        requireNotFrozen();
        this.feeScheduleKey = feeScheduleKey;
        return this;
    }

    public TopicUpdateTransaction clearFeeScheduleKey() {
        requireNotFrozen();
        this.feeScheduleKey = new KeyList();
        return this;
    }

    /**
     * Returns the keys that will be exempt from paying fees.
     * @return {List of feeExemptKeys}
     */
    public List<Key> getFeeExemptKeys() {
        return feeExemptKeys;
    }

    /**
     * Sets the keys that will be exempt from paying fees.
     * @param feeExemptKeys List of feeExemptKeys
     * @return {@code this}
     */
    public TopicUpdateTransaction setFeeExemptKeys(List<Key> feeExemptKeys) {
        Objects.requireNonNull(feeExemptKeys);
        requireNotFrozen();
        this.feeExemptKeys = new ArrayList<>(feeExemptKeys);
        return this;
    }

    /**
     * Clears all keys that will be exempt from paying fees.
     * @return {@code this}
     */
    public TopicUpdateTransaction clearFeeExemptKeys() {
        requireNotFrozen();
        this.feeExemptKeys = new ArrayList<>();
        return this;
    }

    /**
     * Adds a key that will be exempt from paying fees.
     * @param feeExemptKey key
     * @return {@code this}
     */
    public TopicUpdateTransaction addFeeExemptKey(Key feeExemptKey) {
        Objects.requireNonNull(feeExemptKey);
        if (feeExemptKeys != null) {
            feeExemptKeys.add(feeExemptKey);
        } else {
            feeExemptKeys = new ArrayList<>();
            feeExemptKeys.add(feeExemptKey);
        }
        requireNotFrozen();
        return this;
    }

    /**
     * Returns the fixed fees to assess when a message is submitted to the new topic.
     * @return {List of CustomFixedFee}
     */
    public List<CustomFixedFee> getCustomFees() {
        return customFees;
    }

    /**
     * Sets the fixed fees to assess when a message is submitted to the new topic.
     *
     * @param customFees List of CustomFixedFee customFees
     * @return {@code this}
     */
    public TopicUpdateTransaction setCustomFees(List<CustomFixedFee> customFees) {
        Objects.requireNonNull(customFees);
        requireNotFrozen();
        this.customFees = new ArrayList<>(customFees);
        return this;
    }

    /**
     * Clears fixed fees.
     *
     * @return {@code this}
     */
    public TopicUpdateTransaction clearCustomFees() {
        requireNotFrozen();
        //        if (customFees != null) {
        //        }
        customFees = new ArrayList<>();
        return this;
    }

    /**
     * Adds fixed fee to assess when a message is submitted to the new topic.
     *
     * @param customFixedFee {CustomFixedFee} customFee
     * @return {@code this}
     */
    public TopicUpdateTransaction addCustomFee(CustomFixedFee customFixedFee) {
        Objects.requireNonNull(customFixedFee);
        if (customFees != null) {
            customFees.add(customFixedFee);
        } else {
            customFees = new ArrayList<>();
            customFees.add(customFixedFee);
        }
        requireNotFrozen();
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getConsensusUpdateTopic();
        if (body.hasTopicID()) {
            topicId = TopicId.fromProtobuf(body.getTopicID());
        }
        if (body.hasAdminKey()) {
            adminKey = Key.fromProtobufKey(body.getAdminKey());
        }
        if (body.hasSubmitKey()) {
            submitKey = Key.fromProtobufKey(body.getSubmitKey());
        }
        if (body.hasAutoRenewPeriod()) {
            autoRenewPeriod = DurationConverter.fromProtobuf(body.getAutoRenewPeriod());
        }
        if (body.hasAutoRenewAccount()) {
            autoRenewAccountId = AccountId.fromProtobuf(body.getAutoRenewAccount());
        }
        if (body.hasMemo()) {
            topicMemo = body.getMemo().getValue();
        }
        if (body.hasExpirationTime()) {
            expirationTime = InstantConverter.fromProtobuf(body.getExpirationTime());
        }
        if (body.hasFeeScheduleKey()) {
            feeScheduleKey = Key.fromProtobufKey(body.getFeeScheduleKey());
        }
        if (body.hasFeeExemptKeyList()) {
            feeExemptKeys = body.getFeeExemptKeyList().getKeysList().stream()
                    .map(Key::fromProtobufKey)
                    .collect(Collectors.toList());
        }
        if (body.hasCustomFees()) {
            customFees = body.getCustomFees().getFeesList().stream()
                    .map(x -> CustomFixedFee.fromProtobuf(x.getFixedFee()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link
     *         com.hedera.hashgraph.sdk.proto.ConsensusUpdateTopicTransactionBody}
     */
    ConsensusUpdateTopicTransactionBody.Builder build() {
        var builder = ConsensusUpdateTopicTransactionBody.newBuilder();
        if (topicId != null) {
            builder.setTopicID(topicId.toProtobuf());
        }
        if (autoRenewAccountId != null) {
            builder.setAutoRenewAccount(autoRenewAccountId.toProtobuf());
        }
        if (adminKey != null) {
            builder.setAdminKey(adminKey.toProtobufKey());
        }
        if (submitKey != null) {
            builder.setSubmitKey(submitKey.toProtobufKey());
        }
        if (autoRenewPeriod != null) {
            builder.setAutoRenewPeriod(DurationConverter.toProtobuf(autoRenewPeriod));
        }
        if (topicMemo != null) {
            builder.setMemo(StringValue.of(topicMemo));
        }
        if (expirationTime != null) {
            builder.setExpirationTime(InstantConverter.toProtobuf(expirationTime));
        }
        if (feeScheduleKey != null) {
            builder.setFeeScheduleKey(feeScheduleKey.toProtobufKey());
        }
        var feeExemptKeyList = FeeExemptKeyList.newBuilder();
        if (feeExemptKeys != null) {
            for (var feeExemptKey : feeExemptKeys) {
                feeExemptKeyList.addKeys(feeExemptKey.toProtobufKey());
            }
            builder.setFeeExemptKeyList(feeExemptKeyList);
        }

        var protoCustomFeeList = FixedCustomFeeList.newBuilder();
        if (customFees != null) {
            for (CustomFixedFee customFee : customFees) {
                protoCustomFeeList.addFees(customFee.toTopicFeeProtobuf());
            }
            builder.setCustomFees(protoCustomFeeList);
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (topicId != null) {
            topicId.validateChecksum(client);
        }

        if ((autoRenewAccountId != null) && !autoRenewAccountId.equals(new AccountId(0, 0, 0))) {
            autoRenewAccountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ConsensusServiceGrpc.getUpdateTopicMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setConsensusUpdateTopic(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setConsensusUpdateTopic(build());
    }
}
