// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.TopicID;
import java.nio.ByteBuffer;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

/**
 * Unique identifier for a topic (used by the consensus service).
 */
public final class TopicId implements Comparable<TopicId> {
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
     *
     * Constructor that uses shard, realm and num should be used instead
     * as shard and realm should not assume 0 value
     */
    @Deprecated
    public TopicId(@Nonnegative long num) {
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
    public TopicId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num) {
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
    TopicId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num, @Nullable String checksum) {
        this.shard = shard;
        this.realm = realm;
        this.num = num;
        this.checksum = checksum;
    }

    /**
     * Create a topic id from a string.
     *
     * @param id                        the string representation
     * @return                          the new topic id
     */
    public static TopicId fromString(String id) {
        return EntityIdHelper.fromString(id, TopicId::new);
    }

    /**
     * Create a topic id from a solidity address.
     *
     * @param address                   the solidity address
     * @return                          the new topic id
     */
    public static TopicId fromSolidityAddress(String address) {
        return EntityIdHelper.fromSolidityAddress(address, TopicId::new);
    }

    /**
     * Create a topic id from a protobuf.
     *
     * @param topicId                   the protobuf
     * @return                          the new topic id
     */
    @Deprecated
    static TopicId fromProtobuf(TopicID topicId) {
        Objects.requireNonNull(topicId);

        return new TopicId(topicId.getShardNum(), topicId.getRealmNum(), topicId.getTopicNum());
    }

    /**
     * Create a topic id from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new topic id
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static TopicId fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(TopicID.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Extract the solidity address representation.
     *
     * @return                          the solidity address representation
     */
    @Deprecated
    public String toSolidityAddress() {
        return EntityIdHelper.toSolidityAddress(shard, realm, num);
    }

    /**
     * Constructs a TopicId from shard, realm, and EVM address.
     * The EVM address must be a "long zero address" (first 12 bytes are zero).
     *
     * @param shard      the shard number
     * @param realm      the realm number
     * @param evmAddress the EVM address as a hex string
     * @return           the TopicId object
     * @throws IllegalArgumentException if the EVM address is not a valid long zero address
     */
    public static TopicId fromEvmAddress(long shard, long realm, String evmAddress) {
        byte[] addressBytes = EntityIdHelper.decodeEvmAddress(evmAddress);

        if (!EntityIdHelper.isLongZeroAddress(addressBytes)) {
            throw new IllegalArgumentException("EVM address is not a correct long zero address");
        }

        ByteBuffer buf = ByteBuffer.wrap(addressBytes);
        buf.getInt();
        buf.getLong();
        long tokenNum = buf.getLong();

        return new TopicId(shard, realm, tokenNum);
    }

    /**
     * Converts this TopicId to an EVM address string.
     * Creates a solidity address using shard=0, realm=0, and the file number.
     *
     * @return the EVM address as a hex string
     */
    public String toEvmAddress() {
        return EntityIdHelper.toSolidityAddress(0, 0, this.num);
    }
    /**
     * Extracts a protobuf representing the token id.
     *
     * @return                          the protobuf representation
     */
    TopicID toProtobuf() {
        return TopicID.newBuilder()
                .setShardNum(shard)
                .setRealmNum(realm)
                .setTopicNum(num)
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
     * Verify that the client has a valid checksum.
     *
     * @param client                    the client to verify
     * @throws BadEntityIdException     if entity ID is formatted poorly
     */
    public void validateChecksum(Client client) throws BadEntityIdException {
        EntityIdHelper.validate(shard, realm, num, client, checksum);
    }

    /**
     * Extracts the checksum.
     *
     * @return                          the checksum
     */
    @Nullable
    public String getChecksum() {
        return checksum;
    }

    /**
     * Extracts a byte array representation.
     *
     * @return                          the byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }

    @Override
    public String toString() {
        return EntityIdHelper.toString(shard, realm, num);
    }

    /**
     * Create a string representation that includes the checksum.
     *
     * @param client                    the client
     * @return                          the string representation with the checksum
     */
    public String tostringwithchecksum(Client client) {
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

        if (!(o instanceof TopicId)) {
            return false;
        }

        TopicId otherId = (TopicId) o;
        return shard == otherId.shard && realm == otherId.realm && num == otherId.num;
    }

    @Override
    public int compareTo(TopicId o) {
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
