// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.ScheduleCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.ScheduleServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Create a new Schedule.
 *
 * #### Requirements
 * This transaction SHALL create a new _schedule_ entity in network state.<br/>
 * The schedule created SHALL contain the `scheduledTransactionBody` to be
 * executed.<br/>
 * If successful the receipt SHALL contain a `scheduleID` with the full
 * identifier of the schedule created.<br/>
 * When a schedule _executes_ successfully, the receipt SHALL include a
 * `scheduledTransactionID` with the `TransactionID` of the transaction that
 * executed.<br/>
 * When a scheduled transaction is executed the network SHALL charge the
 * regular _service_ fee for the transaction to the `payerAccountID` for
 * that schedule, but SHALL NOT charge node or network fees.<br/>
 * If the `payerAccountID` field is not set, the effective `payerAccountID`
 * SHALL be the `payer` for this create transaction.<br/>
 * If an `adminKey` is not specified, or is an empty `KeyList`, the schedule
 * created SHALL be immutable.<br/>
 * An immutable schedule MAY be signed, and MAY execute, but SHALL NOT be
 * deleted.<br/>
 * If two schedules have the same values for all fields except `payerAccountID`
 * then those two schedules SHALL be deemed "identical".<br/>
 * If a `scheduleCreate` requests a new schedule that is identical to an
 * existing schedule, the transaction SHALL fail and SHALL return a status
 * code of `IDENTICAL_SCHEDULE_ALREADY_CREATED` in the receipt.<br/>
 * The receipt for a duplicate schedule SHALL include the `ScheduleID` of the
 * existing schedule and the `TransactionID` of the earlier `scheduleCreate`
 * so that the earlier schedule may be queried and/or referred to in a
 * subsequent `scheduleSign`.
 *
 * #### Signature Requirements
 * A `scheduleSign` transaction SHALL be used to add additional signatures
 * to an existing schedule.<br/>
 * Each signature SHALL "activate" the corresponding cryptographic("primitive")
 * key for that schedule.<br/>
 * Signature requirements SHALL be met when the set of active keys includes
 * all keys required by the scheduled transaction.<br/>
 * A scheduled transaction for a "long term" schedule SHALL NOT execute if
 * the signature requirements for that transaction are not met when the
 * network consensus time reaches the schedule `expiration_time`.<br/>
 * A "short term" schedule SHALL execute immediately once signature
 * requirements are met. This MAY be immediately when created.
 *
 * #### Long Term Schedules
 * A "short term" schedule SHALL have the flag `wait_for_expiry` _unset_.<br/>
 * A "long term" schedule SHALL have the flag  `wait_for_expiry` _set_.<br/>
 * A "long term" schedule SHALL NOT be accepted if the network configuration
 * `scheduling.longTermEnabled` is not enabled.<br/>
 * A "long term" schedule SHALL execute when the current consensus time
 * matches or exceeds the `expiration_time` for that schedule, if the
 * signature requirements for the scheduled transaction
 * are met at that instant.<br/>
 * A "long term" schedule SHALL NOT execute before the current consensus time
 * matches or exceeds the `expiration_time` for that schedule.<br/>
 * A "long term" schedule SHALL expire, and be removed from state, after the
 * network consensus time exceeds the schedule `expiration_time`.<br/>
 * A short term schedule SHALL expire, and be removed from state,
 * after the network consensus time exceeds the current network
 * configuration for `ledger.scheduleTxExpiryTimeSecs`.
 *
 * > Note
 * >> Long term schedules are not (as of release 0.56.0) enabled. Any schedule
 * >> created currently MUST NOT set the `wait_for_expiry` flag.<br/>
 * >> When long term schedules are not enabled, schedules SHALL NOT be
 * >> executed at expiration, and MUST meet signature requirements strictly
 * >> before expiration to be executed.
 *
 * ### Block Stream Effects
 * If the scheduled transaction is executed immediately, the transaction
 * record SHALL include a `scheduleRef` with the schedule identifier of the
 * schedule created.
 */
public final class ScheduleCreateTransaction extends Transaction<ScheduleCreateTransaction> {
    @Nullable
    private AccountId payerAccountId = null;

    @Nullable
    private SchedulableTransactionBody transactionToSchedule = null;

    @Nullable
    private Key adminKey = null;

    private String scheduleMemo = "";

    @Nullable
    private Instant expirationTime = null;

    @Nullable
    private java.time.Duration expirationTimeDuration = null;

    private boolean waitForExpiry = false;

    /**
     * Constructor.
     */
    public ScheduleCreateTransaction() {
        defaultMaxTransactionFee = new Hbar(5);
    }

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    ScheduleCreateTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Get the expiration time
     *
     * @return The expiration time
     */
    @Nullable
    public Instant getExpirationTime() {
        return expirationTime;
    }

    /**
     * An optional timestamp for specifying when the transaction should be evaluated for execution and then expire.
     * Defaults to 30 minutes after the transaction's consensus timestamp.
     * <p>
     * Note: This field is unused and forced to be unset until Long Term Scheduled Transactions are enabled - Transactions will always
     *       expire in 30 minutes if Long Term Scheduled Transactions are not enabled.
     *
     * @param expirationTime The expiration time
     * @return {@code this}
     */
    public ScheduleCreateTransaction setExpirationTime(Instant expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        this.expirationTime = expirationTime;
        this.expirationTimeDuration = null;
        return this;
    }

    /**
     * Overload: set the expiration time using a Duration value.
     *
     * @param expirationTime The duration to be used as expiration time
     * @return {@code this}
     */
    public ScheduleCreateTransaction setExpirationTime(java.time.Duration expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        this.expirationTime = null;
        this.expirationTimeDuration = expirationTime;
        return this;
    }

    /**
     * get the status of the waitForExpiry boolean
     *
     * @return waitForExpiry boolean
     */
    public boolean isWaitForExpiry() {
        return waitForExpiry;
    }

    /**
     * When set to true, the transaction will be evaluated for execution at expiration_time instead
     * of when all required signatures are received.
     * When set to false, the transaction will execute immediately after sufficient signatures are received
     * to sign the contained transaction. During the initial ScheduleCreate transaction or via ScheduleSign transactions.
     * Defaults to false.
     * <p>
     * Setting this to false does not necessarily mean that the transaction will never execute at expiration_time.
     *  <p>
     *  For Example - If the signature requirements for a Scheduled Transaction change via external means (e.g. CryptoUpdate)
     *  such that the Scheduled Transaction would be allowed to execute, it will do so autonomously at expiration_time, unless a
     *  ScheduleSign comes in to “poke” it and force it to go through immediately.
     * <p>
     * Note: This field is unused and forced to be unset until Long Term Scheduled Transactions are enabled. Before Long Term
     *       Scheduled Transactions are enabled, Scheduled Transactions will _never_ execute at expiration  - they will _only_
     *       execute during the initial ScheduleCreate transaction or via ScheduleSign transactions and will _always_
     *       expire at expiration_time.
     *
     * @param waitForExpiry Whether to wait for expiry
     * @return {@code this}
     */
    public ScheduleCreateTransaction setWaitForExpiry(boolean waitForExpiry) {
        this.waitForExpiry = waitForExpiry;
        return this;
    }

    /**
     * Get the payer's account ID.
     *
     * @return The payer's account ID
     */
    @Nullable
    public AccountId getPayerAccountId() {
        return payerAccountId;
    }

    /**
     * An account identifier of a `payer` for the scheduled transaction.
     * <p>
     * This value MAY be unset. If unset, the `payer` for this `scheduleCreate`
     * transaction SHALL be the `payer` for the scheduled transaction.<br/>
     * If this is set, the identified account SHALL be charged the fees
     * required for the scheduled transaction when it is executed.<br/>
     * If the actual `payer` for the _scheduled_ transaction lacks
     * sufficient HBAR balance to pay service fees for the scheduled
     * transaction _when it executes_, the scheduled transaction
     * SHALL fail with `INSUFFICIENT_PAYER_BALANCE`.<br/>
     *
     * @param accountId                 the payer's account ID
     * @return {@code this}
     */
    public ScheduleCreateTransaction setPayerAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.payerAccountId = accountId;
        return this;
    }

    /**
     * Assign the transaction to schedule.
     *
     * @param transaction               the transaction to schedule
     * @return {@code this}
     */
    public ScheduleCreateTransaction setScheduledTransaction(Transaction<?> transaction) {
        requireNotFrozen();
        Objects.requireNonNull(transaction);

        var scheduled = transaction.schedule();
        transactionToSchedule = scheduled.transactionToSchedule;

        return this;
    }

    /**
     * Assign the transaction body to schedule.
     *
     * @param tx                        the transaction body to schedule
     * @return {@code this}
     */
    ScheduleCreateTransaction setScheduledTransactionBody(SchedulableTransactionBody tx) {
        requireNotFrozen();
        Objects.requireNonNull(tx);
        transactionToSchedule = tx;
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
     * A `Key` required to delete this schedule.
     * <p>
     * If this is not set, or is an empty `KeyList`, this schedule SHALL be
     * immutable and SHALL NOT be deleted.
     *
     * @param key                       the admin key
     * @return {@code this}
     */
    public ScheduleCreateTransaction setAdminKey(Key key) {
        requireNotFrozen();
        adminKey = key;
        return this;
    }

    /**
     * Extract the schedule's memo.
     *
     * @return                          the schedule's memo
     */
    public String getScheduleMemo() {
        return scheduleMemo;
    }

    /**
     * A short description of the schedule.
     * <p>
     * This value, if set, MUST NOT exceed `transaction.maxMemoUtf8Bytes`
     * (default 100) bytes when encoded as UTF-8.
     *
     * @param memo                      the schedule's memo
     * @return {@code this}
     */
    public ScheduleCreateTransaction setScheduleMemo(String memo) {
        requireNotFrozen();
        scheduleMemo = memo;
        return this;
    }

    /**
     * Build the correct transaction body.
     *
     * @return {@link com.hedera.hashgraph.sdk.proto.ScheduleCreateTransactionBody builder }
     */
    ScheduleCreateTransactionBody.Builder build() {
        var builder = ScheduleCreateTransactionBody.newBuilder();
        if (payerAccountId != null) {
            builder.setPayerAccountID(payerAccountId.toProtobuf());
        }
        if (transactionToSchedule != null) {
            builder.setScheduledTransactionBody(transactionToSchedule);
        }
        if (adminKey != null) {
            builder.setAdminKey(adminKey.toProtobufKey());
        }
        if (expirationTime != null) {
            builder.setExpirationTime(InstantConverter.toProtobuf(expirationTime));
        } else if (expirationTimeDuration != null) {
            builder.setExpirationTime(InstantConverter.toProtobuf(expirationTimeDuration));
        }
        builder.setMemo(scheduleMemo).setWaitForExpiry(waitForExpiry);
        return builder;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getScheduleCreate();
        if (body.hasPayerAccountID()) {
            payerAccountId = AccountId.fromProtobuf(body.getPayerAccountID());
        }
        if (body.hasScheduledTransactionBody()) {
            transactionToSchedule = body.getScheduledTransactionBody();
        }
        if (body.hasAdminKey()) {
            adminKey = Key.fromProtobufKey(body.getAdminKey());
        }
        if (body.hasExpirationTime()) {
            expirationTime = InstantConverter.fromProtobuf(body.getExpirationTime());
            expirationTimeDuration = null;
        }
        scheduleMemo = body.getMemo();
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (payerAccountId != null) {
            payerAccountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ScheduleServiceGrpc.getCreateScheduleMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setScheduleCreate(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException("Cannot schedule ScheduleCreateTransaction");
    }
}
