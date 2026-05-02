// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.GrantedTokenAllowance;

/**
 * An approved allowance of token transfers for a spender.
 *
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/basic-types/tokenallowance">Hedera Documentation</a>
 */
public class TokenAllowance {
    /**
     * The token that the allowance pertains to
     */
    @Nullable
    public final TokenId tokenId;
    /**
     * The account ID of the hbar owner (ie. the grantor of the allowance)
     */
    @Nullable
    public final AccountId ownerAccountId;
    /**
     * The account ID of the spender of the hbar allowance
     */
    @Nullable
    public final AccountId spenderAccountId;
    /**
     * The amount of the spender's token allowance
     */
    public final long amount;

    /**
     * Constructor.
     *
     * @param tokenId                   the token id
     * @param ownerAccountId            the grantor account id
     * @param spenderAccountId          the spender account id
     * @param amount                    the token allowance
     */
    TokenAllowance(
            @Nullable TokenId tokenId,
            @Nullable AccountId ownerAccountId,
            @Nullable AccountId spenderAccountId,
            long amount) {
        this.tokenId = tokenId;
        this.ownerAccountId = ownerAccountId;
        this.spenderAccountId = spenderAccountId;
        this.amount = amount;
    }

    /**
     * Create a token allowance from a protobuf.
     *
     * @param allowanceProto            the protobuf
     * @return                          the new token allowance
     */
    static TokenAllowance fromProtobuf(org.hiero.sdk.proto.TokenAllowance allowanceProto) {
        return new TokenAllowance(
                allowanceProto.hasTokenId() ? TokenId.fromProtobuf(allowanceProto.getTokenId()) : null,
                allowanceProto.hasOwner() ? AccountId.fromProtobuf(allowanceProto.getOwner()) : null,
                allowanceProto.hasSpender() ? AccountId.fromProtobuf(allowanceProto.getSpender()) : null,
                allowanceProto.getAmount());
    }

    /**
     * Create a token allowance from a protobuf.
     *
     * @param allowanceProto            the protobuf
     * @return                          the new token allowance
     */
    static TokenAllowance fromProtobuf(GrantedTokenAllowance allowanceProto) {
        return new TokenAllowance(
                allowanceProto.hasTokenId() ? TokenId.fromProtobuf(allowanceProto.getTokenId()) : null,
                null,
                allowanceProto.hasSpender() ? AccountId.fromProtobuf(allowanceProto.getSpender()) : null,
                allowanceProto.getAmount());
    }

    /**
     * Create a token allowance from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new token allowance
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static TokenAllowance fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(org.hiero.sdk.proto.TokenAllowance.parseFrom(Objects.requireNonNull(bytes)));
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
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    org.hiero.sdk.proto.TokenAllowance toProtobuf() {
        var builder = org.hiero.sdk.proto.TokenAllowance.newBuilder().setAmount(amount);
        if (tokenId != null) {
            builder.setTokenId(tokenId.toProtobuf());
        }
        if (ownerAccountId != null) {
            builder.setOwner(ownerAccountId.toProtobuf());
        }
        if (spenderAccountId != null) {
            builder.setSpender(spenderAccountId.toProtobuf());
        }
        return builder.build();
    }

    /**
     * Create the byte array.
     *
     * @return                          the protobuf representation
     */
    GrantedTokenAllowance toGrantedProtobuf() {
        var builder = GrantedTokenAllowance.newBuilder().setAmount(amount);
        if (tokenId != null) {
            builder.setTokenId(tokenId.toProtobuf());
        }
        if (spenderAccountId != null) {
            builder.setSpender(spenderAccountId.toProtobuf());
        }
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
        return MoreObjects.toStringHelper(this)
                .add("tokenId", tokenId)
                .add("ownerAccountId", ownerAccountId)
                .add("spenderAccountId", spenderAccountId)
                .add("amount", amount)
                .toString();
    }
}
