// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The extra fee charged for the transaction.
 * <p>
 * Represents additional fees that apply for specific fee components,
 * such as charges beyond included amounts.
 */
public final class FeeExtra {
    /**
     * The charged count of items as calculated by max(0, count - included).
     */
    private final int charged;

    /**
     * The actual count of items received.
     */
    private final int count;

    /**
     * The fee price per unit in tinycents.
     */
    private final long feePerUnit;

    /**
     * The count of this "extra" that is included for free.
     */
    private final int included;

    /**
     * The unique name of this extra fee as defined in the fee schedule.
     */
    @Nullable
    private final String name;

    /**
     * The subtotal in tinycents for this extra fee.
     * <p>
     * Calculated by multiplying the charged count by the fee_per_unit.
     */
    private final long subtotal;

    /**
     * Constructor.
     *
     * @param charged    the charged count of items
     * @param count      the actual count of items
     * @param feePerUnit the fee price per unit in tinycents
     * @param included   the count included for free
     * @param name       the unique name of this extra fee
     * @param subtotal   the subtotal in tinycents
     */
    FeeExtra(int charged, int count, long feePerUnit, int included, @Nullable String name, long subtotal) {
        this.charged = charged;
        this.count = count;
        this.feePerUnit = feePerUnit;
        this.included = included;
        this.name = name;
        this.subtotal = subtotal;
    }

    /**
     * Create a FeeExtra from a protobuf.
     *
     * @param feeExtra the protobuf
     * @return the new FeeExtra
     */
    static FeeExtra fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.FeeExtra feeExtra) {
        return new FeeExtra(
                feeExtra.getCharged(),
                feeExtra.getCount(),
                feeExtra.getFeePerUnit(),
                feeExtra.getIncluded(),
                feeExtra.getName().isEmpty() ? null : feeExtra.getName(),
                feeExtra.getSubtotal());
    }

    /**
     * Create a FeeExtra from a byte array.
     *
     * @param bytes the byte array
     * @return the new FeeExtra
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static FeeExtra fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.FeeExtra.parseFrom(bytes).toBuilder()
                .build());
    }

    /**
     * Extract the charged count of items.
     *
     * @return the charged count of items
     */
    public int getCharged() {
        return charged;
    }

    /**
     * Extract the actual count of items.
     *
     * @return the actual count of items
     */
    public int getCount() {
        return count;
    }

    /**
     * Extract the fee price per unit in tinycents.
     *
     * @return the fee price per unit in tinycents
     */
    public long getFeePerUnit() {
        return feePerUnit;
    }

    /**
     * Extract the count included for free.
     *
     * @return the count included for free
     */
    public int getIncluded() {
        return included;
    }

    /**
     * Extract the unique name of this extra fee.
     *
     * @return the unique name of this extra fee, or null if not set
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Extract the subtotal in tinycents.
     *
     * @return the subtotal in tinycents
     */
    public long getSubtotal() {
        return subtotal;
    }

    /**
     * Convert the fee extra to a protobuf.
     *
     * @return the protobuf
     */
    com.hedera.hashgraph.sdk.proto.mirror.FeeExtra toProtobuf() {
        var builder = com.hedera.hashgraph.sdk.proto.mirror.FeeExtra.newBuilder()
                .setCharged(charged)
                .setCount(count)
                .setFeePerUnit(feePerUnit)
                .setIncluded(included)
                .setSubtotal(subtotal);

        if (name != null) {
            builder.setName(name);
        }

        return builder.build();
    }

    /**
     * Convert the fee extra to a byte array.
     *
     * @return the byte array
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("charged", charged)
                .add("count", count)
                .add("feePerUnit", feePerUnit)
                .add("included", included)
                .add("name", name)
                .add("subtotal", subtotal)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeeExtra that)) {
            return false;
        }
        return charged == that.charged
                && count == that.count
                && feePerUnit == that.feePerUnit
                && included == that.included
                && subtotal == that.subtotal
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(charged, count, feePerUnit, included, name, subtotal);
    }
}
