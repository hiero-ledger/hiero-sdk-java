// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.ScheduleServiceGrpc;
import org.hiero.sdk.proto.ScheduleSignTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * A transaction that appends signatures to a schedule transaction.
 * You will need to know the schedule ID to reference the schedule
 * transaction to submit signatures to. A record will be generated
 * for each ScheduleSign transaction that is successful and the schedule
 * entity will subsequently update with the public keys that have signed
 * the schedule transaction. To view the keys that have signed the
 * schedule transaction, you can query the network for the schedule info.
 * Once a schedule transaction receives the last required signature, the
 * schedule transaction executes.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/schedule-transaction/sign-a-schedule-transaction">Hedera Documentation</a>
 */
public final class ScheduleSignTransaction extends Transaction<ScheduleSignTransaction> {

    @Nullable
    private ScheduleId scheduleId = null;

    /**
     * Constructor.
     */
    public ScheduleSignTransaction() {
        defaultMaxTransactionFee = new Hbar(5);
    }

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    ScheduleSignTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Extract the schedule id.
     *
     * @return                          the schedule id
     */
    @Nullable
    public ScheduleId getScheduleId() {
        return scheduleId;
    }

    /**
     * A schedule identifier.
     * <p>
     * This MUST identify the schedule which SHALL be deleted.
     *
     * @param scheduleId                the schedule id
     * @return {@code this}
     */
    public ScheduleSignTransaction setScheduleId(ScheduleId scheduleId) {
        Objects.requireNonNull(scheduleId);
        requireNotFrozen();
        this.scheduleId = scheduleId;
        return this;
    }

    /**
     * Clears the schedule id
     *
     * @return {@code this}
     */
    @Deprecated
    public ScheduleSignTransaction clearScheduleId() {
        requireNotFrozen();
        this.scheduleId = null;
        return this;
    }

    /**
     * Build the correct transaction body.
     *
     * @return {@link
     *         org.hiero.sdk.proto.ScheduleSignTransactionBody
     *         builder }
     */
    ScheduleSignTransactionBody.Builder build() {
        var builder = ScheduleSignTransactionBody.newBuilder();
        if (scheduleId != null) {
            builder.setScheduleID(scheduleId.toProtobuf());
        }

        return builder;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getScheduleSign();
        if (body.hasScheduleID()) {
            scheduleId = ScheduleId.fromProtobuf(body.getScheduleID());
        }
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (scheduleId != null) {
            scheduleId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ScheduleServiceGrpc.getSignScheduleMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setScheduleSign(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException("cannot schedule ScheduleSignTransaction");
    }
}
