// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.FileID;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

/**
 * The ID for a file on Hedera.
 */
public final class FileId implements Comparable<FileId> {
    /**
     * The public node address book for the current network.
     */
    public static final FileId ADDRESS_BOOK = new FileId(0, 0, 102);
    /**
     * The current fee schedule for the network.
     */
    public static final FileId FEE_SCHEDULE = new FileId(0, 0, 111);
    /**
     * The current exchange rate of HBAR to USD.
     */
    public static final FileId EXCHANGE_RATES = new FileId(0, 0, 112);
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
     * Assign the num portion of the file id.
     *
     * @param num                       the num portion not negative
     *
     * Constructor that uses shard, realm and num should be used instead
     * as shard and realm should not assume 0 value
     */
    @Deprecated
    public FileId(@Nonnegative long num) {
        this(0, 0, num);
    }

    /**
     * Assign the file id.
     *
     * @param shard                     the shard portion
     * @param realm                     the realm portion
     * @param num                       the num portion
     */
    @SuppressWarnings("InconsistentOverloads")
    public FileId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num) {
        this(shard, realm, num, null);
    }

    /**
     * Assign the file id and optional checksum.
     *
     * @param shard                     the shard portion
     * @param realm                     the realm portion
     * @param num                       the num portion
     * @param checksum                  the optional checksum
     */
    @SuppressWarnings("InconsistentOverloads")
    FileId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num, @Nullable String checksum) {
        this.shard = shard;
        this.realm = realm;
        this.num = num;
        this.checksum = checksum;
    }

    /**
     * Get the `FileId` of the Hedera address book for the given realm and shard.
     * @param realm
     * @param shard
     * @return FileId
     */
    public static FileId getAddressBookFileIdFor(long shard, long realm) {
        return new FileId(shard, realm, 102);
    }

    /**
     * Get the `FileId` of the Hedera fee schedule for the given realm and shard.
     * @param realm
     * @param shard
     * @return FileId
     */
    public static FileId getFeeScheduleFileIdFor(long shard, long realm) {
        return new FileId(shard, realm, 111);
    }

    /**
     * Get the `FileId` of the Hedera exchange rates for the given realm and shard.
     * @param realm
     * @param shard
     * @return FileId
     */
    public static FileId getExchangeRatesFileIdFor(long shard, long realm) {
        return new FileId(shard, realm, 112);
    }

    /**
     * Assign the file id from a string.
     *
     * @param id                        the string representation of a file id
     * @return                          the file id object
     */
    public static FileId fromString(String id) {
        return EntityIdHelper.fromString(id, FileId::new);
    }

    /**
     * Assign the file id from a byte array.
     *
     * @param bytes                     the byte array representation of a file id
     * @return                          the file id object
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static FileId fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(FileID.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Create a file id object from a protobuf.
     *
     * @param fileId                    the protobuf
     * @return                          the file id object
     */
    static FileId fromProtobuf(FileID fileId) {
        Objects.requireNonNull(fileId);
        return new FileId(fileId.getShardNum(), fileId.getRealmNum(), fileId.getFileNum());
    }

    /**
     * Create a file id object from a solidity address.
     *
     * @param address                   the solidity address
     * @return                          the file id object
     */
    public static FileId fromSolidityAddress(String address) {
        return EntityIdHelper.fromSolidityAddress(address, FileId::new);
    }

    /**
     * Extract the string representation of file id.
     *
     * @return                          the string representation of file id
     */
    public String toSolidityAddress() {
        return EntityIdHelper.toSolidityAddress(shard, realm, num);
    }

    /**
     * @return                         protobuf representing the file id
     */
    FileID toProtobuf() {
        return FileID.newBuilder()
                .setShardNum(shard)
                .setRealmNum(realm)
                .setFileNum(num)
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
     * Validate that the client is configured correctly.
     *
     * @param client                    the client to validate
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
     * Convert the client to a string representation with a checksum.
     *
     * @param client                    the client to stringify
     * @return                          string representation with checksum
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

        if (!(o instanceof FileId)) {
            return false;
        }

        FileId otherId = (FileId) o;
        return shard == otherId.shard && realm == otherId.realm && num == otherId.num;
    }

    @Override
    public int compareTo(FileId o) {
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
