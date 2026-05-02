// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The fee schedule for a specific hedera functionality and the time period this fee schedule will expire.
 *
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/basic-types/feeschedule">Hedera Documentation</a>
 */
public class FeeSchedule implements Cloneable {
    private List<TransactionFeeSchedule> transactionFeeSchedules = new ArrayList<>();

    @Nullable
    private Instant expirationTime;

    /**
     * Constructor.
     */
    public FeeSchedule() {}

    /**
     * Create a fee schedule from a protobuf.
     *
     * @param feeSchedule               the protobuf
     * @return                          the fee schedule
     */
    static FeeSchedule fromProtobuf(org.hiero.sdk.proto.FeeSchedule feeSchedule) {
        FeeSchedule returnFeeSchedule = new FeeSchedule()
                .setExpirationTime(
                        feeSchedule.hasExpiryTime()
                                ? InstantConverter.fromProtobuf(feeSchedule.getExpiryTime())
                                : null);
        for (var transactionFeeSchedule : feeSchedule.getTransactionFeeScheduleList()) {
            returnFeeSchedule.addTransactionFeeSchedule(TransactionFeeSchedule.fromProtobuf(transactionFeeSchedule));
        }
        return returnFeeSchedule;
    }

    /**
     * Create a fee schedule from byte array.
     *
     * @param bytes                     the bye array
     * @return                          the fee schedule
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static FeeSchedule fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(
                org.hiero.sdk.proto.FeeSchedule.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Extract the of transaction fee schedules.
     *
     * @return                          list of transaction fee schedules
     */
    public List<TransactionFeeSchedule> getTransactionFeeSchedules() {
        return Collections.unmodifiableList(cloneTransactionFeeSchedules(transactionFeeSchedules));
    }

    static List<TransactionFeeSchedule> cloneTransactionFeeSchedules(List<TransactionFeeSchedule> schedules) {
        List<TransactionFeeSchedule> cloneSchedules = new ArrayList<>(schedules.size());
        for (var schedule : schedules) {
            cloneSchedules.add(schedule.clone());
        }
        return cloneSchedules;
    }

    /**
     * Assign the list of transaction fee schedules.
     *
     * @param transactionFeeSchedules   list of transaction fee schedules
     * @return {@code this}
     */
    public FeeSchedule setTransactionFeeSchedules(List<TransactionFeeSchedule> transactionFeeSchedules) {
        this.transactionFeeSchedules = cloneTransactionFeeSchedules(Objects.requireNonNull(transactionFeeSchedules));
        return this;
    }

    /**
     * Add a transaction fee schedule.
     *
     * @param transactionFeeSchedule    transaction fee schedule to add
     * @return {@code this}
     */
    public FeeSchedule addTransactionFeeSchedule(TransactionFeeSchedule transactionFeeSchedule) {
        transactionFeeSchedules.add(Objects.requireNonNull(transactionFeeSchedule));
        return this;
    }

    /**
     * Extract the expiration time.
     *
     * @return                          the expiration time
     */
    @Nullable
    public Instant getExpirationTime() {
        return expirationTime;
    }

    /**
     * Assign the expiration time.
     *
     * @param expirationTime            the expiration time
     * @return {@code this}
     */
    public FeeSchedule setExpirationTime(@Nullable Instant expirationTime) {
        this.expirationTime = expirationTime;
        return this;
    }

    /**
     * Convert to a protobuf.
     *
     * @return                          the protobuf
     */
    org.hiero.sdk.proto.FeeSchedule toProtobuf() {
        var returnBuilder = org.hiero.sdk.proto.FeeSchedule.newBuilder();
        if (expirationTime != null) {
            returnBuilder.setExpiryTime(InstantConverter.toSecondsProtobuf(expirationTime));
        }
        for (TransactionFeeSchedule tFeeSchedule : getTransactionFeeSchedules()) {
            returnBuilder.addTransactionFeeSchedule(tFeeSchedule.toProtobuf());
        }
        return returnBuilder.build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("transactionFeeSchedules", getTransactionFeeSchedules())
                .add("expirationTime", getExpirationTime())
                .toString();
    }

    /**
     * Create the byte array.
     *
     * @return                          a byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public FeeSchedule clone() {
        try {
            FeeSchedule clone = (FeeSchedule) super.clone();
            clone.transactionFeeSchedules = cloneTransactionFeeSchedules(transactionFeeSchedules);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
