// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ScheduleID;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

/**
 * The entity ID of a schedule transaction.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/schedule-transaction/schedule-id">Hedera Documentation</a>
 */
public final class ScheduleId implements Comparable<ScheduleId> {
    /**
     * The shard number
     */
    @Nonnegative
    public final long shard;

    /**
     * The realm number
     */
    @Nonnegative
    public final long realm;

    /**
     * The id number
     */
    @Nonnegative
    public final long num;

    @Nullable
    private final String checksum;

    /**
     * Constructor.
     *
     * @param num                       the num part
     */
    @Deprecated
    public ScheduleId(@Nonnegative long num) {
        this(0, 0, num);
    }

    /**
     * Constructor.
     *
     * @param shard                     the shard part
     * @param realm                     the realm part
     * @param num                       the num part
     */
    @SuppressWarnings("InconsistentOverloads")
    public ScheduleId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num) {
        this(shard, realm, num, null);
    }

    /**
     * Constructor.
     *
     * @param shard                     the shard part
     * @param realm                     the realm part
     * @param num                       the num part
     * @param checksum                  the checksum
     */
    @SuppressWarnings("InconsistentOverloads")
    ScheduleId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num, @Nullable String checksum) {
        this.shard = shard;
        this.realm = realm;
        this.num = num;
        this.checksum = checksum;
    }

    /**
     * Create a schedule id from a string.
     *
     * @param id                        the string representing the schedule id
     * @return                          the new schedule id
     */
    public static ScheduleId fromString(String id) {
        return EntityIdHelper.fromString(id, ScheduleId::new);
    }

    /**
     * Create a schedule id from a protobuf.
     *
     * @param scheduleId                the protobuf
     * @return                          the new schedule id
     */
    static ScheduleId fromProtobuf(ScheduleID scheduleId) {
        Objects.requireNonNull(scheduleId);
        return new ScheduleId(scheduleId.getShardNum(), scheduleId.getRealmNum(), scheduleId.getScheduleNum());
    }

    /**
     * Create a schedule id from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new schedule id
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static ScheduleId fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(ScheduleID.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representing the schedule id
     */
    ScheduleID toProtobuf() {
        return ScheduleID.newBuilder()
                .setShardNum(shard)
                .setRealmNum(realm)
                .setScheduleNum(num)
                .build();
    }

    /**
     * @param client to validate against
     * @throws BadEntityIdException if entity ID is formatted poorly
     * @deprecated Use {@link #validateChecksum(Client)} instead.
     */
    @Deprecated
    public void validate(Client client) throws BadEntityIdException {
        validateChecksum(client);
    }

    /**
     * Validate the configured client.
     *
     * @param client                    the configured client
     * @throws BadEntityIdException     if entity ID is formatted poorly
     */
    public void validateChecksum(Client client) throws BadEntityIdException {
        EntityIdHelper.validate(shard, realm, num, client, checksum);
    }

    /**
     * Extract the checksum.
     *
     * @return                          the checksum
     */
    @Nullable
    public String getChecksum() {
        return checksum;
    }

    /**
     * Create the byte array.
     *
     * @return                          byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public String toString() {
        return EntityIdHelper.toString(shard, realm, num);
    }

    /**
     * Convert the schedule id into a string with checksum.
     *
     * @param client                    the configured client
     * @return                          the string representation
     */
    public String toStringWithChecksum(Client client) {
        return EntityIdHelper.toStringWithChecksum(shard, realm, num, client, checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shard, realm, num);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ScheduleId)) {
            return false;
        }

        ScheduleId otherId = (ScheduleId) o;
        return shard == otherId.shard && realm == otherId.realm && num == otherId.num;
    }

    @Override
    public int compareTo(ScheduleId o) {
        Objects.requireNonNull(o);
        int shardComparison = Long.compare(shard, o.shard);
        if (shardComparison != 0) {
            return shardComparison;
        }
        int realmComparison = Long.compare(realm, o.realm);
        if (realmComparison != 0) {
            return realmComparison;
        }
        return Long.compare(num, o.num);
    }
}
