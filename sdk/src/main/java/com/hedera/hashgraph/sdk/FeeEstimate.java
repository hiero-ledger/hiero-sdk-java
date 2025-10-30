// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The fee estimate for a fee component (node or service).
 * <p>
 * Includes the base fee and any extras associated with it.
 */
public final class FeeEstimate {
    /**
     * The base fee price, in tinycents.
     */
    private final long base;

    /**
     * The extra fees that apply for this fee component.
     */
    private final List<FeeExtra> extras;

    /**
     * Constructor.
     *
     * @param base   the base fee price in tinycents
     * @param extras the list of extra fees
     */
    FeeEstimate(long base, List<FeeExtra> extras) {
        this.base = base;
        this.extras = Collections.unmodifiableList(new ArrayList<>(extras));
    }

    /**
     * Create a FeeEstimate from a protobuf.
     *
     * @param feeEstimate the protobuf
     * @return the new FeeEstimate
     */
    static FeeEstimate fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate feeEstimate) {
        List<FeeExtra> extras = new ArrayList<>(feeEstimate.getExtrasCount());
        for (var extraProto : feeEstimate.getExtrasList()) {
            extras.add(FeeExtra.fromProtobuf(extraProto));
        }
        return new FeeEstimate(feeEstimate.getBase(), extras);
    }

    /**
     * Create a FeeEstimate from a byte array.
     *
     * @param bytes the byte array
     * @return the new FeeEstimate
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static FeeEstimate fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate.parseFrom(bytes).toBuilder()
                .build());
    }

    /**
     * Extract the base fee price in tinycents.
     *
     * @return the base fee price in tinycents
     */
    public long getBase() {
        return base;
    }

    /**
     * Extract the list of extra fees.
     *
     * @return an unmodifiable list of extra fees
     */
    public List<FeeExtra> getExtras() {
        return extras;
    }

    /**
     * Convert the fee estimate to a protobuf.
     *
     * @return the protobuf
     */
    com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate toProtobuf() {
        var builder =
                com.hedera.hashgraph.sdk.proto.mirror.FeeEstimate.newBuilder().setBase(base);

        for (var extra : extras) {
            builder.addExtras(extra.toProtobuf());
        }

        return builder.build();
    }

    /**
     * Convert the fee estimate to a byte array.
     *
     * @return the byte array
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("base", base)
                .add("extras", extras)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeeEstimate that)) {
            return false;
        }
        return base == that.base && Objects.equals(extras, that.extras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, extras);
    }
}
