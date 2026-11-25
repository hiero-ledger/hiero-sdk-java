// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;

/**
 * Immutable capacity limits for a CLPR ledger.
 * <p>
 * Use {@link #of} to validate all seven limits before submitting a configuration update.
 * The uint32 limits are held as {@code long} values. {@link #getMaxGasPerMessage()} and
 * {@link #getMaxSyncBytes()} are uint64 values held in a {@code long}; read them with
 * {@link Long#toUnsignedString(long)} and compare them with {@link Long#compareUnsigned(long, long)}.
 * Configuration updates require treasury or system admin authority.
 * CLPR is off by default and must be enabled with {@code clpr.enabled}.
 */
public final class ClprThrottles {
    private static final long MAX_UINT32 = 0xFFFF_FFFFL;
    private static final long SYNC_BYTES_OVERHEAD = 65_536L;

    private final long maxMessagesPerBundle;
    private final long maxMessagePayloadBytes;
    private final long maxGasPerMessage;
    private final long maxQueueDepth;
    private final long maxSyncBytes;
    private final long maxLocalEndpoints;
    private final long maxPeerEndpoints;

    private ClprThrottles(
            long maxMessagesPerBundle,
            long maxMessagePayloadBytes,
            long maxGasPerMessage,
            long maxQueueDepth,
            long maxSyncBytes,
            long maxLocalEndpoints,
            long maxPeerEndpoints) {
        this.maxMessagesPerBundle = maxMessagesPerBundle;
        this.maxMessagePayloadBytes = maxMessagePayloadBytes;
        this.maxGasPerMessage = maxGasPerMessage;
        this.maxQueueDepth = maxQueueDepth;
        this.maxSyncBytes = maxSyncBytes;
        this.maxLocalEndpoints = maxLocalEndpoints;
        this.maxPeerEndpoints = maxPeerEndpoints;
    }

    /**
     * Creates validated throttles. The five uint32 limits must fit in 32 unsigned bits, and the three
     * non-endpoint ones must be positive. The two endpoint limits accept zero for no limit. The two uint64
     * limits must be non-zero, and the sync capacity must cover one maximum message payload plus 65536 bytes.
     *
     * @param maxMessagesPerBundle   maximum messages per bundle, 1..2^32-1
     * @param maxMessagePayloadBytes maximum message payload size, 1..2^32-1
     * @param maxGasPerMessage       maximum gas per message, a non-zero unsigned uint64
     * @param maxQueueDepth          maximum queue depth, 1..2^32-1
     * @param maxSyncBytes           maximum sync payload size, a non-zero unsigned uint64
     * @param maxLocalEndpoints      maximum local endpoints, 0..2^32-1 where 0 is no limit
     * @param maxPeerEndpoints       maximum peer endpoints, 0..2^32-1 where 0 is no limit
     * @return the throttles
     * @throws IllegalArgumentException if a limit is out of range
     */
    public static ClprThrottles of(
            long maxMessagesPerBundle,
            long maxMessagePayloadBytes,
            long maxGasPerMessage,
            long maxQueueDepth,
            long maxSyncBytes,
            long maxLocalEndpoints,
            long maxPeerEndpoints) {
        requireUint32("maxMessagesPerBundle", maxMessagesPerBundle, 1);
        requireUint32("maxMessagePayloadBytes", maxMessagePayloadBytes, 1);
        requireUint32("maxQueueDepth", maxQueueDepth, 1);
        requireUint32("maxLocalEndpoints", maxLocalEndpoints, 0);
        requireUint32("maxPeerEndpoints", maxPeerEndpoints, 0);
        if (maxGasPerMessage == 0) {
            throw new IllegalArgumentException("maxGasPerMessage must be a positive uint64");
        }
        if (maxSyncBytes == 0) {
            throw new IllegalArgumentException("maxSyncBytes must be a positive uint64");
        }
        if (Long.compareUnsigned(maxSyncBytes, maxMessagePayloadBytes + SYNC_BYTES_OVERHEAD) < 0) {
            throw new IllegalArgumentException("maxSyncBytes must be at least maxMessagePayloadBytes + 65536");
        }
        return new ClprThrottles(
                maxMessagesPerBundle,
                maxMessagePayloadBytes,
                maxGasPerMessage,
                maxQueueDepth,
                maxSyncBytes,
                maxLocalEndpoints,
                maxPeerEndpoints);
    }

    private static void requireUint32(String field, long value, long minimum) {
        if (value < minimum || value > MAX_UINT32) {
            throw new IllegalArgumentException(field + " must be an integer from " + minimum + " to 4294967295");
        }
    }

    /**
     * Decodes server values without applying configuration-update validation.
     */
    static ClprThrottles fromProtobuf(com.hedera.hashgraph.sdk.proto.ClprThrottles throttles) {
        return new ClprThrottles(
                Integer.toUnsignedLong(throttles.getMaxMessagesPerBundle()),
                Integer.toUnsignedLong(throttles.getMaxMessagePayloadBytes()),
                throttles.getMaxGasPerMessage(),
                Integer.toUnsignedLong(throttles.getMaxQueueDepth()),
                throttles.getMaxSyncBytes(),
                Integer.toUnsignedLong(throttles.getMaxLocalEndpoints()),
                Integer.toUnsignedLong(throttles.getMaxPeerEndpoints()));
    }

    /**
     * Creates throttles from a protobuf byte array, without validation.
     *
     * @param bytes the byte array
     * @return the throttles
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    public static ClprThrottles fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(com.hedera.hashgraph.sdk.proto.ClprThrottles.parseFrom(bytes));
    }

    com.hedera.hashgraph.sdk.proto.ClprThrottles toProtobuf() {
        return com.hedera.hashgraph.sdk.proto.ClprThrottles.newBuilder()
                .setMaxMessagesPerBundle((int) maxMessagesPerBundle)
                .setMaxMessagePayloadBytes((int) maxMessagePayloadBytes)
                .setMaxGasPerMessage(maxGasPerMessage)
                .setMaxQueueDepth((int) maxQueueDepth)
                .setMaxSyncBytes(maxSyncBytes)
                .setMaxLocalEndpoints((int) maxLocalEndpoints)
                .setMaxPeerEndpoints((int) maxPeerEndpoints)
                .build();
    }

    /**
     * Serializes the throttles to a protobuf byte array.
     *
     * @return the byte array
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    /**
     * @return maximum number of messages in one bundle
     */
    public long getMaxMessagesPerBundle() {
        return maxMessagesPerBundle;
    }

    /**
     * @return maximum size of one message payload, in bytes
     */
    public long getMaxMessagePayloadBytes() {
        return maxMessagePayloadBytes;
    }

    /**
     * @return maximum gas allocated to one message, as an unsigned uint64
     */
    public long getMaxGasPerMessage() {
        return maxGasPerMessage;
    }

    /**
     * @return maximum queue depth for a Channel
     */
    public long getMaxQueueDepth() {
        return maxQueueDepth;
    }

    /**
     * @return maximum serialized sync payload size, in bytes, as an unsigned uint64
     */
    public long getMaxSyncBytes() {
        return maxSyncBytes;
    }

    /**
     * @return maximum number of local endpoints, where 0 is no limit
     */
    public long getMaxLocalEndpoints() {
        return maxLocalEndpoints;
    }

    /**
     * @return maximum number of peer endpoints, where 0 is no limit
     */
    public long getMaxPeerEndpoints() {
        return maxPeerEndpoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClprThrottles that)) {
            return false;
        }
        return maxMessagesPerBundle == that.maxMessagesPerBundle
                && maxMessagePayloadBytes == that.maxMessagePayloadBytes
                && maxGasPerMessage == that.maxGasPerMessage
                && maxQueueDepth == that.maxQueueDepth
                && maxSyncBytes == that.maxSyncBytes
                && maxLocalEndpoints == that.maxLocalEndpoints
                && maxPeerEndpoints == that.maxPeerEndpoints;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                maxMessagesPerBundle,
                maxMessagePayloadBytes,
                maxGasPerMessage,
                maxQueueDepth,
                maxSyncBytes,
                maxLocalEndpoints,
                maxPeerEndpoints);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("maxMessagesPerBundle", maxMessagesPerBundle)
                .add("maxMessagePayloadBytes", maxMessagePayloadBytes)
                .add("maxGasPerMessage", Long.toUnsignedString(maxGasPerMessage))
                .add("maxQueueDepth", maxQueueDepth)
                .add("maxSyncBytes", Long.toUnsignedString(maxSyncBytes))
                .add("maxLocalEndpoints", maxLocalEndpoints)
                .add("maxPeerEndpoints", maxPeerEndpoints)
                .toString();
    }
}
