// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import javax.annotation.Nonnegative;
import org.hiero.sdk.proto.NftID;

/**
 * The (non-fungible) token of which this NFT is an instance
 */
public class NftId implements Comparable<NftId> {
    /**
     * The (non-fungible) token of which this NFT is an instance
     */
    public final TokenId tokenId;

    /**
     * The unique identifier of this instance
     */
    @Nonnegative
    public final long serial;

    /**
     * Constructor.
     *
     * @param tokenId                   the token id
     * @param serial                    the serial number
     */
    public NftId(TokenId tokenId, @Nonnegative long serial) {
        this.tokenId = Objects.requireNonNull(tokenId);
        this.serial = serial;
    }

    /**
     * Create a new nft id from a string.
     *
     * @param id                        the string representation
     * @return                          the new nft id
     */
    public static NftId fromString(String id) {
        @SuppressWarnings("StringSplitter")
        var parts = id.split("[/@]");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expecting {shardNum}.{realmNum}.{idNum}-{checksum}/{serialNum}");
        }
        return new NftId(TokenId.fromString(parts[0]), Long.parseLong(parts[1]));
    }

    /**
     * Create a new ntf id from a protobuf.
     *
     * @param nftId                     the protobuf representation
     * @return                          the new nft id
     */
    static NftId fromProtobuf(NftID nftId) {
        Objects.requireNonNull(nftId);
        var tokenId = nftId.getTokenID();
        return new NftId(TokenId.fromProtobuf(tokenId), nftId.getSerialNumber());
    }

    /**
     * Create a new nft id from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new nft id
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static NftId fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(NftID.parseFrom(bytes).toBuilder().build());
    }

    /**
     * Create the protobuf.
     *
     * @return                          a protobuf representation
     */
    NftID toProtobuf() {
        return NftID.newBuilder()
                .setTokenID(tokenId.toProtobuf())
                .setSerialNumber(serial)
                .build();
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
    public String toString() {
        return tokenId.toString() + "/" + serial;
    }

    /**
     * Generate a string representation with checksum.
     *
     * @param client                    the configured client
     * @return                          the string representation with checksum
     */
    public String toStringWithChecksum(Client client) {
        return tokenId.toStringWithChecksum(client) + "/" + serial;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenId.shard, tokenId.realm, tokenId.num, serial);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof NftId)) {
            return false;
        }

        NftId otherId = (NftId) o;
        return tokenId.equals(otherId.tokenId) && serial == otherId.serial;
    }

    @Override
    public int compareTo(NftId o) {
        Objects.requireNonNull(o);
        int tokenComparison = tokenId.compareTo(o.tokenId);
        if (tokenComparison != 0) {
            return tokenComparison;
        }
        return Long.compare(serial, o.serial);
    }
}
