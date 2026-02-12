// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.BoolValue;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.NftAllowance;
import com.hedera.hashgraph.sdk.proto.NftRemoveAllowance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Class to encapsulate the nft methods for token allowance's.
 */
public class TokenNftAllowance {

    /**
     * The NFT token type that the allowance pertains to
     */
    @Nullable
    public final TokenId tokenId;

    /**
     * The account ID of the token owner (ie. the grantor of the allowance)
     */
    @Nullable
    public final AccountId ownerAccountId;

    /**
     * The account ID of the token allowance spender
     */
    @Nullable
    public final AccountId spenderAccountId;

    /**
     * The account ID of the spender who is granted approvedForAll allowance and granting
     * approval on an NFT serial to another spender.
     */
    @Nullable
    public final AccountId delegatingSpender;

    /**
     * The list of serial numbers that the spender is permitted to transfer.
     */
    public final List<Long> serialNumbers;

    /**
     * If true, the spender has access to all of the owner's NFT units of type tokenId (currently
     * owned and any in the future).
     */
    @Nullable
    public final Boolean allSerials;

    /**
     * Constructor.
     *
     * @param tokenId                   the token id
     * @param ownerAccountId            the grantor's account id
     * @param spenderAccountId          the spender's account id
     * @param delegatingSpender         the delegating spender's account id
     * @param serialNumbers             the list of serial numbers
     * @param allSerials                grant for all serial's
     */
    TokenNftAllowance(
            @Nullable TokenId tokenId,
            @Nullable AccountId ownerAccountId,
            @Nullable AccountId spenderAccountId,
            @Nullable AccountId delegatingSpender,
            Collection<Long> serialNumbers,
            @Nullable Boolean allSerials) {
        this.tokenId = tokenId;
        this.ownerAccountId = ownerAccountId;
        this.spenderAccountId = spenderAccountId;
        this.delegatingSpender = delegatingSpender;
        this.serialNumbers = new ArrayList<>(serialNumbers);
        this.allSerials = allSerials;
    }

    /**
     * Create a copy of a nft token allowance object.
     *
     * @param allowance                 the nft token allowance to copj
     * @return                          a new copy
     */
    static TokenNftAllowance copyFrom(TokenNftAllowance allowance) {
        return new TokenNftAllowance(
                allowance.tokenId,
                allowance.ownerAccountId,
                allowance.spenderAccountId,
                allowance.delegatingSpender,
                allowance.serialNumbers,
                allowance.allSerials);
    }

    /**
     * Create a nft token allowance from a protobuf.
     *
     * @param allowanceProto            the protobuf
     * @return                          the nft token allowance
     */
    static TokenNftAllowance fromProtobuf(NftAllowance allowanceProto) {
        return new TokenNftAllowance(
                allowanceProto.hasTokenId() ? TokenId.fromProtobuf(allowanceProto.getTokenId()) : null,
                allowanceProto.hasOwner() ? AccountId.fromProtobuf(allowanceProto.getOwner()) : null,
                allowanceProto.hasSpender() ? AccountId.fromProtobuf(allowanceProto.getSpender()) : null,
                allowanceProto.hasDelegatingSpender()
                        ? AccountId.fromProtobuf(allowanceProto.getDelegatingSpender())
                        : null,
                allowanceProto.getSerialNumbersList(),
                allowanceProto.hasApprovedForAll()
                        ? allowanceProto.getApprovedForAll().getValue()
                        : null);
    }

    /**
     * Create a nft token allowance from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the nft token allowance
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static TokenNftAllowance fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(NftAllowance.parseFrom(Objects.requireNonNull(bytes)));
    }

    /**
     * Validate the configured client.
     *
     * @param client                    the configured client
     * @throws BadEntityIdException     if entity ID is formatted poorly
     */
    void validateChecksums(Client client) throws BadEntityIdException {
        if (tokenId != null) {
            tokenId.validateChecksum(client);
        }
        if (ownerAccountId != null) {
            ownerAccountId.validateChecksum(client);
        }
        if (spenderAccountId != null) {
            spenderAccountId.validateChecksum(client);
        }
        if (delegatingSpender != null) {
            delegatingSpender.validateChecksum(client);
        }
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    NftAllowance toProtobuf() {
        var builder = NftAllowance.newBuilder();
        if (tokenId != null) {
            builder.setTokenId(tokenId.toProtobuf());
        }
        if (ownerAccountId != null) {
            builder.setOwner(ownerAccountId.toProtobuf());
        }
        if (spenderAccountId != null) {
            builder.setSpender(spenderAccountId.toProtobuf());
        }
        if (delegatingSpender != null) {
            builder.setDelegatingSpender(delegatingSpender.toProtobuf());
        }
        builder.addAllSerialNumbers(serialNumbers);
        if (allSerials != null) {
            builder.setApprovedForAll(
                    BoolValue.newBuilder().setValue(allSerials).build());
        }
        return builder.build();
    }

    /**
     * Create the protobuf.
     *
     * @return                          the remove protobuf
     */
    NftRemoveAllowance toRemoveProtobuf() {
        var builder = NftRemoveAllowance.newBuilder();
        if (tokenId != null) {
            builder.setTokenId(tokenId.toProtobuf());
        }
        if (ownerAccountId != null) {
            builder.setOwner(ownerAccountId.toProtobuf());
        }
        builder.addAllSerialNumbers(serialNumbers);
        return builder.build();
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
        var stringHelper = MoreObjects.toStringHelper(this)
                .add("tokenId", tokenId)
                .add("ownerAccountId", ownerAccountId)
                .add("spenderAccountId", spenderAccountId)
                .add("delegatingSpender", delegatingSpender);
        if (allSerials != null) {
            stringHelper.add("allSerials", allSerials);
        } else {
            stringHelper.add("serials", serialNumbers);
        }
        return stringHelper.toString();
    }
}
