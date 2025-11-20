// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * The network fee component which covers the cost of gossip, consensus,
 * signature verifications, fee payment, and storage.
 */
public final class NetworkFee {
    /**
     * Multiplied by the node fee to determine the total network fee.
     */
    private final int multiplier;

    /**
     * The subtotal in tinycents for the network fee component which is calculated by
     * multiplying the node subtotal by the network multiplier.
     */
    private final long subtotal;

    /**
     * Constructor.
     *
     * @param multiplier the network fee multiplier
     * @param subtotal   the network fee subtotal in tinycents
     */
    NetworkFee(int multiplier, long subtotal) {
        this.multiplier = multiplier;
        this.subtotal = subtotal;
    }

    /**
     * Create a NetworkFee from a protobuf.
     *
     * @param networkFee the protobuf
     * @return the new NetworkFee
     */
    static NetworkFee fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.NetworkFee networkFee) {
        return new NetworkFee(networkFee.getMultiplier(), networkFee.getSubtotal());
    }

    /**
     * Create a NetworkFee from a byte array.
     *
     * @param bytes the byte array
     * @return the new NetworkFee
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static NetworkFee fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(com.hedera.hashgraph.sdk.proto.mirror.NetworkFee.parseFrom(bytes).toBuilder()
                .build());
    }

    /**
     * Extract the network fee multiplier.
     *
     * @return the network fee multiplier
     */
    public int getMultiplier() {
        return multiplier;
    }

    /**
     * Extract the network fee subtotal in tinycents.
     *
     * @return the network fee subtotal in tinycents
     */
    public long getSubtotal() {
        return subtotal;
    }

    /**
     * Convert the network fee to a protobuf.
     *
     * @return the protobuf
     */
    com.hedera.hashgraph.sdk.proto.mirror.NetworkFee toProtobuf() {
        return com.hedera.hashgraph.sdk.proto.mirror.NetworkFee.newBuilder()
                .setMultiplier(multiplier)
                .setSubtotal(subtotal)
                .build();
    }

    /**
     * Convert the network fee to a byte array.
     *
     * @return the byte array
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("multiplier", multiplier)
                .add("subtotal", subtotal)
                .toString();
    }
}
