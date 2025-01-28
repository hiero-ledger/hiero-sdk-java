// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.ConsensusCreateTopicTransactionBody;
import org.hiero.sdk.proto.ConsensusServiceGrpc;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Create a topic to accept and group consensus messages.
 *
 * If `autoRenewAccount` is specified, that account Key MUST also sign this
 * transaction.<br/>
 * If `adminKey` is set, that Key MUST sign the transaction.<br/>
 * On success, the resulting `TransactionReceipt` SHALL contain the newly
 * created `TopicId`.
 *
 * The `autoRenewPeriod` on a topic MUST be set to a value between
 * `autoRenewPeriod.minDuration` and `autoRenewPeriod.maxDuration`. These
 * values are configurable, typically 30 and 92 days.<br/>
 * This also sets the initial expirationTime of the topic.
 *
 * If no `adminKey` is set on a topic
 *   -`autoRenewAccount` SHALL NOT be set on the topic.
 *   - A `deleteTopic` transaction SHALL fail.
 *   - An `updateTopic` transaction that only extends the expirationTime MAY
 *     succeed.
 *   - Any other `updateTopic` transaction SHALL fail.
 *
 * If the topic expires and is not automatically renewed, the topic SHALL enter
 * the `EXPIRED` state.
 *   - All transactions on the topic SHALL fail with TOPIC_EXPIRED
 *      - Except an updateTopic() call that only extends the expirationTime.
 *   - getTopicInfo() SHALL succeed, and show the topic is expired.
 * The topic SHALL remain in the `EXPIRED` state for a time determined by the
 * `autorenew.gracePeriod` (configurable, originally 7 days).<br/>
 * After the grace period, if the topic's expirationTime is not extended, the
 * topic SHALL be automatically deleted from state entirely, and cannot be
 * recovered or recreated.
 *
 * ### Block Stream Effects
 * None
 */
public final class TopicCreateTransaction extends Transaction<TopicCreateTransaction> {

    @Nullable
    private AccountId autoRenewAccountId = null;

    @Nullable
    private Duration autoRenewPeriod = null;

    private String topicMemo = "";

    @Nullable
    private Key adminKey = null;

    @Nullable
    private Key submitKey = null;

    /**
     * Constructor.
     */
    public TopicCreateTransaction() {
        setAutoRenewPeriod(DEFAULT_AUTO_RENEW_PERIOD);
    }

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TopicCreateTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TopicCreateTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the topic memo.
     *
     * @return                          the topic memo
     */
    public String getTopicMemo() {
        return topicMemo;
    }

    /**
     * Set a short publicly visible memo on the new topic.
     *
     * @param memo The memo to be set
     * @return {@code this}
     */
    public TopicCreateTransaction setTopicMemo(String memo) {
        Objects.requireNonNull(memo);
        requireNotFrozen();
        topicMemo = memo;
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
     * Access control for modification of the topic after it is created.
     * <p>
     * If this field is set, that key MUST sign this transaction.<br/>
     * If this field is set, that key MUST sign each future transaction to
     * update or delete the topic.<br/>
     * An updateTopic transaction that _only_ extends the topic expirationTime
     * (a "manual renewal" transaction) SHALL NOT require admin key
     * signature.<br/>
     * A topic without an admin key SHALL be immutable, except for expiration
     * and renewal.<br/>
     * If adminKey is not set, then `autoRenewAccount` SHALL NOT be set.
     *
     * @param adminKey The Key to be set
     * @return {@code this}
     */
    public TopicCreateTransaction setAdminKey(Key adminKey) {
        Objects.requireNonNull(adminKey);
        requireNotFrozen();
        this.adminKey = adminKey;
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
     * Access control for message submission to the topic.
     * <p>
     * If this field is set, that key MUST sign each consensus submit message
     * for this topic.<br/>
     * If this field is not set then any account may submit a message on the
     * topic, without restriction.
     *
     * @param submitKey The Key to be set
     * @return {@code this}
     */
    public TopicCreateTransaction setSubmitKey(Key submitKey) {
        Objects.requireNonNull(submitKey);
        requireNotFrozen();
        this.submitKey = submitKey;
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

    /**
     * The initial lifetime, in seconds, for the topic.<br/>
     * This is also the number of seconds for which the topic SHALL be
     * automatically renewed upon expiring, if it has a valid auto-renew
     * account.
     * <p>
     * This value MUST be set.<br/>
     * This value MUST be greater than the configured
     * MIN_AUTORENEW_PERIOD.<br/>
     * This value MUST be less than the configured MAX_AUTORENEW_PERIOD.
     *
     * @param autoRenewPeriod The Duration to be set for auto renewal
     * @return {@code this}
     */
    public TopicCreateTransaction setAutoRenewPeriod(Duration autoRenewPeriod) {
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
     * The ID of the account to be charged renewal fees at the topic's
     * expirationTime to extend the lifetime of the topic.
     * <p>
     * The topic lifetime SHALL be extended by the smallest of the following:
     * <ul>
     *   <li>The current `autoRenewPeriod` duration.</li>
     *   <li>The maximum duration that this account has funds to purchase.</li>
     *   <li>The configured MAX_AUTORENEW_PERIOD at the time of automatic
     *       renewal.</li>
     * </ul>
     * If this value is set, the referenced account MUST sign this
     * transaction.<br/>
     * If this value is set, the `adminKey` field MUST also be set (though that
     * key MAY not have any correlation to this account).
     *
     * @param autoRenewAccountId The AccountId to be set for auto renewal
     * @return {@code this}
     */
    public TopicCreateTransaction setAutoRenewAccountId(AccountId autoRenewAccountId) {
        Objects.requireNonNull(autoRenewAccountId);
        requireNotFrozen();
        this.autoRenewAccountId = autoRenewAccountId;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getConsensusCreateTopic();
        if (body.hasAutoRenewAccount()) {
            autoRenewAccountId = AccountId.fromProtobuf(body.getAutoRenewAccount());
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
        topicMemo = body.getMemo();
    }

    /**
     * Build the transaction body.
     *
     * @return {@link
     *         org.hiero.sdk.proto.ConsensusCreateTopicTransactionBody}
     */
    ConsensusCreateTopicTransactionBody.Builder build() {
        var builder = ConsensusCreateTopicTransactionBody.newBuilder();
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
        builder.setMemo(topicMemo);

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (autoRenewAccountId != null) {
            autoRenewAccountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ConsensusServiceGrpc.getCreateTopicMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setConsensusCreateTopic(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setConsensusCreateTopic(build());
    }
}
