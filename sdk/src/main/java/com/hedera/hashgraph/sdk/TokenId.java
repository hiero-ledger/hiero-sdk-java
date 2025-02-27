// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.TokenID;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

/**
 * Constructs a TokenId.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/token-id">Hedera Documentation</a>
 */
public class TokenId implements Comparable<TokenId> {
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
    public TokenId(@Nonnegative long num) {
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
    public TokenId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num) {
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
    TokenId(@Nonnegative long shard, @Nonnegative long realm, @Nonnegative long num, @Nullable String checksum) {
        this.shard = shard;
        this.realm = realm;
        this.num = num;
        this.checksum = checksum;
    }

    /**
     * Create a token id from a string.
     *
     * @param id                        the string representation
     * @return                          the new token id
     */
    public static TokenId fromString(String id) {
        return EntityIdHelper.fromString(id, TokenId::new);
    }

    /**
     * Create a token id from a protobuf.
     *
     * @param tokenId                   the protobuf
     * @return                          the new token id
     */
    static TokenId fromProtobuf(TokenID tokenId) {
        Objects.requireNonNull(tokenId);
        return new TokenId(tokenId.getShardNum(), tokenId.getRealmNum(), tokenId.getTokenNum());
    }

    /**
     * Create a token id from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new token id
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static TokenId fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(TokenID.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Create a token id from a solidity address.
     *
     * @param address                   the solidity address as a string
     * @return                          the new token id
     */
    public static TokenId fromSolidityAddress(String address) {
        return EntityIdHelper.fromSolidityAddress(address, TokenId::new);
    }

    /**
     * Create an nft id.
     *
     * @param serial                    the serial number
     * @return                          the new nft id
     */
    public NftId nft(@Nonnegative long serial) {
        return new NftId(this, serial);
    }

    /**
     * Extract the solidity address as a string.
     *
     * @return                         the solidity address as a string
     */
    public String toSolidityAddress() {
        return EntityIdHelper.toSolidityAddress(shard, realm, num);
    }

    /**
     * Create the protobuf.
     *
     * @return                          a protobuf representation
     */
    TokenID toProtobuf() {
        return TokenID.newBuilder()
                .setShardNum(shard)
                .setRealmNum(realm)
                .setTokenNum(num)
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
     * Create a string representation with checksum.
     *
     * @param client                    the configured client
     * @return                          the string representation with checksum
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

        if (!(o instanceof TokenId)) {
            return false;
        }

        TokenId otherId = (TokenId) o;
        return shard == otherId.shard && realm == otherId.realm && num == otherId.num;
    }

    @Override
    public int compareTo(TokenId o) {
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
