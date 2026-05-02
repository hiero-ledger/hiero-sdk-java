// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Instant;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;

/**
 * A query that returns information about the current state of a scheduled
 * transaction on a Hedera network.
 * <p>
 * See <a href="https://docs.hedera.com/guides/docs/sdks/schedule-transaction/get-schedule-info">Hedera Documentation</a>
 */
public final class ScheduleInfo {
    /**
     * The ID of the schedule transaction
     */
    public final ScheduleId scheduleId;
    /**
     * The Hedera account that created the schedule transaction in x.y.z format
     */
    public final AccountId creatorAccountId;
    /**
     * The Hedera account paying for the execution of the schedule transaction
     * in x.y.z format
     */
    public final AccountId payerAccountId;
    /**
     * The signatories that have provided signatures so far for the schedule
     * transaction
     */
    public final KeyList signatories;

    /**
     * The Key which is able to delete the schedule transaction if set
     */
    @Nullable
    public final Key adminKey;

    /**
     * The scheduled transaction
     */
    @Nullable
    public final TransactionId scheduledTransactionId;

    /**
     * Publicly visible information about the Schedule entity, up to
     * 100 bytes. No guarantee of uniqueness.
     */
    public final String memo;

    /**
     * The date and time the schedule transaction will expire
     */
    @Nullable
    public final Instant expirationTime;

    /**
     * The time the schedule transaction was executed. If the schedule
     * transaction has not executed this field will be left null.
     */
    @Nullable
    public final Instant executedAt;

    /**
     * The consensus time the schedule transaction was deleted. If the
     * schedule transaction was not deleted, this field will be left null.
     */
    @Nullable
    public final Instant deletedAt;

    /**
     * The scheduled transaction (inner transaction).
     */
    final SchedulableTransactionBody transactionBody;

    /**
     * The ledger ID the response was returned from; please see <a href="https://github.com/hashgraph/hedera-improvement-proposal/blob/master/HIP/hip-198.md">HIP-198</a> for the network-specific IDs.
     */
    @Nullable
    public final LedgerId ledgerId;

    /**
     * When set to true, the transaction will be evaluated for execution at expiration_time instead
     * of when all required signatures are received.
     * When set to false, the transaction will execute immediately after sufficient signatures are received
     * to sign the contained transaction. During the initial ScheduleCreate transaction or via ScheduleSign transactions.
     *
     * Note: this field is unused until Long Term Scheduled Transactions are enabled.
     */
    public final boolean waitForExpiry;

    /**
     * Constructor.
     *
     * @param scheduleId                the schedule id
     * @param creatorAccountId          the creator account id
     * @param payerAccountId            the payer account id
     * @param transactionBody           the transaction body
     * @param signers                   the signers key list
     * @param adminKey                  the admin key
     * @param scheduledTransactionId    the transaction id
     * @param memo                      the memo 100 bytes max
     * @param expirationTime            the expiration time
     * @param executed                  the time transaction was executed
     * @param deleted                   the time it was deleted
     * @param ledgerId                  the ledger id
     * @param waitForExpiry             the wait for expiry field
     */
    ScheduleInfo(
            ScheduleId scheduleId,
            AccountId creatorAccountId,
            AccountId payerAccountId,
            SchedulableTransactionBody transactionBody,
            KeyList signers,
            @Nullable Key adminKey,
            @Nullable TransactionId scheduledTransactionId,
            String memo,
            @Nullable Instant expirationTime,
            @Nullable Instant executed,
            @Nullable Instant deleted,
            LedgerId ledgerId,
            boolean waitForExpiry) {
        this.scheduleId = scheduleId;
        this.creatorAccountId = creatorAccountId;
        this.payerAccountId = payerAccountId;
        this.signatories = signers;
        this.adminKey = adminKey;
        this.transactionBody = transactionBody;
        this.scheduledTransactionId = scheduledTransactionId;
        this.memo = memo;
        this.expirationTime = expirationTime;
        this.executedAt = executed;
        this.deletedAt = deleted;
        this.ledgerId = ledgerId;
        this.waitForExpiry = waitForExpiry;
    }

    /**
     * Create a schedule info object from a protobuf.
     *
     * @param info              the protobuf
     * @return                          the new schedule info object
     */
    static ScheduleInfo fromProtobuf(org.hiero.sdk.proto.ScheduleInfo info) {
        var scheduleId = ScheduleId.fromProtobuf(info.getScheduleID());
        var creatorAccountId = AccountId.fromProtobuf(info.getCreatorAccountID());
        var payerAccountId = AccountId.fromProtobuf(info.getPayerAccountID());
        var adminKey = info.hasAdminKey() ? Key.fromProtobufKey(info.getAdminKey()) : null;
        var scheduledTransactionId =
                info.hasScheduledTransactionID() ? TransactionId.fromProtobuf(info.getScheduledTransactionID()) : null;

        return new ScheduleInfo(
                scheduleId,
                creatorAccountId,
                payerAccountId,
                info.getScheduledTransactionBody(),
                KeyList.fromProtobuf(info.getSigners(), null),
                adminKey,
                scheduledTransactionId,
                info.getMemo(),
                info.hasExpirationTime() ? InstantConverter.fromProtobuf(info.getExpirationTime()) : null,
                info.hasExecutionTime() ? InstantConverter.fromProtobuf(info.getExecutionTime()) : null,
                info.hasDeletionTime() ? InstantConverter.fromProtobuf(info.getDeletionTime()) : null,
                LedgerId.fromByteString(info.getLedgerId()),
                info.getWaitForExpiry());
    }

    /**
     * Create a schedule info object from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new schedule info object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static ScheduleInfo fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(
                org.hiero.sdk.proto.ScheduleInfo.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    org.hiero.sdk.proto.ScheduleInfo toProtobuf() {
        var info = org.hiero.sdk.proto.ScheduleInfo.newBuilder();

        if (adminKey != null) {
            info.setAdminKey(adminKey.toProtobufKey());
        }

        if (scheduledTransactionId != null) {
            info.setScheduledTransactionID(scheduledTransactionId.toProtobuf());
        }

        if (expirationTime != null) {
            info.setExpirationTime(InstantConverter.toProtobuf(expirationTime));
        }

        if (executedAt != null) {
            info.setExecutionTime(InstantConverter.toProtobuf(executedAt));
        }

        if (deletedAt != null) {
            info.setDeletionTime(InstantConverter.toProtobuf(deletedAt));
        }

        return info.setScheduleID(scheduleId.toProtobuf())
                .setCreatorAccountID(creatorAccountId.toProtobuf())
                .setScheduledTransactionBody(transactionBody)
                .setPayerAccountID(payerAccountId.toProtobuf())
                .setSigners(signatories.toProtobuf())
                .setMemo(memo)
                .setLedgerId(ledgerId.toByteString())
                .setWaitForExpiry(waitForExpiry)
                .build();
    }

    /**
     * Extract the transaction.
     *
     * @return                          the transaction
     */
    public Transaction<?> getScheduledTransaction() {
        return Transaction.fromScheduledTransaction(transactionBody);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("scheduleId", scheduleId)
                .add("scheduledTransactionId", scheduledTransactionId)
                .add("creatorAccountId", creatorAccountId)
                .add("payerAccountId", payerAccountId)
                .add("signatories", signatories)
                .add("adminKey", adminKey)
                .add("expirationTime", expirationTime)
                .add("memo", memo)
                .add("executedAt", executedAt)
                .add("deletedAt", deletedAt)
                .add("ledgerId", ledgerId)
                .add("waitForExpiry", waitForExpiry)
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
