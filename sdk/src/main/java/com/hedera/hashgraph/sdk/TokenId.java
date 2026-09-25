// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.TokenID;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Constructs a TokenId.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/token-id">Hedera Documentation</a>
 */
public class TokenId implements Comparable<TokenId> {
    /**
     * The shard number. Always non-negative.
     */
    public final long shard;

    /**
     * The realm number. Always non-negative.
     */
    public final long realm;

    /**
     * The id number. Always non-negative.
     */
    public final long num;

    private final @Nullable String checksum;

    /**
     * Constructor.
     *
     * @param num                       the num part, must be non-negative
     *
     * Constructor that uses shard, realm and num should be used instead
     * as shard and realm should not assume 0 value
     */
    @Deprecated
    public TokenId(long num) {
        this(0, 0, num);
    }

    /**
     * Constructor.
     *
     * @param shard                     the shard part, must be non-negative
     * @param realm                     the realm part, must be non-negative
     * @param num                       the num part, must be non-negative
     */
    @SuppressWarnings("InconsistentOverloads")
    public TokenId(long shard, long realm, long num) {
        this(shard, realm, num, null);
    }

    /**
     * Constructor.
     *
     * @param shard                     the shard part, must be non-negative
     * @param realm                     the realm part, must be non-negative
     * @param num                       the num part, must be non-negative
     * @param checksum                  the checksum
     */
    @SuppressWarnings("InconsistentOverloads")
    TokenId(long shard, long realm, long num, @Nullable String checksum) {
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
     * Retrieve the token id from a solidity address.
     *
     * @param address                   a string representing the address
     * @return                          the token id object
     * @deprecated This method is deprecated. Use {@link #fromEvmAddress(long, long, String)} instead.
     */
    @Deprecated
    public static TokenId fromSolidityAddress(String address) {
        return EntityIdHelper.fromSolidityAddress(address, TokenId::new);
    }

    /**
     * Constructs a TokenID from shard, realm, and EVM address.
     * The EVM address must be a "long zero address" (first 12 bytes are zero).
     *
     * @param shard      the shard number, must be non-negative
     * @param realm      the realm number, must be non-negative
     * @param evmAddress the EVM address as a hex string
     * @return           the TokenID object
     * @throws IllegalArgumentException if the EVM address is not a valid long zero address
     */
    public static TokenId fromEvmAddress(long shard, long realm, String evmAddress) {
        byte[] addressBytes = EntityIdHelper.decodeEvmAddress(evmAddress);

        if (!EntityIdHelper.isLongZeroAddress(addressBytes)) {
            throw new IllegalArgumentException("EVM address is not a correct long zero address");
        }

        ByteBuffer buf = ByteBuffer.wrap(addressBytes);
        buf.getInt();
        buf.getLong();
        long tokenNum = buf.getLong();

        return new TokenId(shard, realm, tokenNum);
    }

    /**
     * Converts this TokenId to an EVM address string.
     * Creates a solidity address using shard=0, realm=0, and the file number.
     *
     * @return the EVM address as a hex string
     */
    public String toEvmAddress() {
        return EntityIdHelper.toSolidityAddress(0, 0, this.num);
    }

    /**
     * Create an nft id.
     *
     * @param serial                    the serial number, must be non-negative
     * @return                          the new nft id
     */
    public NftId nft(long serial) {
        return new NftId(this, serial);
    }

    /**
     * Extract the solidity address.
     *
     * @return                          the solidity address as a string
     * @deprecated This method is deprecated. Use {@link #toEvmAddress()} instead.
     */
    @Deprecated
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
    public @Nullable String getChecksum() {
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
