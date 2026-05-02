// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.CryptoAllowance;
import org.hiero.sdk.proto.GrantedCryptoAllowance;

/**
 * An approved allowance of hbar transfers for a spender.
 *
 * See <a href="https://docs.hedera.com/guides/docs/hedera-api/basic-types/cryptoallowance">Hedera Documentation</a>
 */
public class HbarAllowance {

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
     * The amount of the spender's allowance in tinybars
     */
    @Nullable
    public final Hbar amount;

    /**
     * Constructor.
     * @param ownerAccountId            the owner granting the allowance
     * @param spenderAccountId          the spender
     * @param amount                    the amount of hbar
     */
    HbarAllowance(@Nullable AccountId ownerAccountId, @Nullable AccountId spenderAccountId, @Nullable Hbar amount) {
        this.ownerAccountId = ownerAccountId;
        this.spenderAccountId = spenderAccountId;
        this.amount = amount;
    }

    /**
     * Create a hbar allowance from a crypto allowance protobuf.
     *
     * @param allowanceProto            the crypto allowance protobuf
     * @return                          the new hbar allowance
     */
    static HbarAllowance fromProtobuf(CryptoAllowance allowanceProto) {
        return new HbarAllowance(
                allowanceProto.hasOwner() ? AccountId.fromProtobuf(allowanceProto.getOwner()) : null,
                allowanceProto.hasSpender() ? AccountId.fromProtobuf(allowanceProto.getSpender()) : null,
                Hbar.fromTinybars(allowanceProto.getAmount()));
    }

    /**
     * Create a hbar allowance from a granted crypto allowance protobuf.
     *
     * @param allowanceProto            the granted crypto allowance protobuf
     * @return                          the new hbar allowance
     */
    static HbarAllowance fromProtobuf(GrantedCryptoAllowance allowanceProto) {
        return new HbarAllowance(
                null,
                allowanceProto.hasSpender() ? AccountId.fromProtobuf(allowanceProto.getSpender()) : null,
                Hbar.fromTinybars(allowanceProto.getAmount()));
    }

    /**
     * Create a hbar allowance from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new hbar allowance
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static HbarAllowance fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(CryptoAllowance.parseFrom(Objects.requireNonNull(bytes)));
    }

    /**
     * Validate that the client is configured correctly.
     *
     * @param client                    the client to verify
     * @throws BadEntityIdException     if entity ID is formatted poorly
     */
    void validateChecksums(Client client) throws BadEntityIdException {
        if (ownerAccountId != null) {
            ownerAccountId.validateChecksum(client);
        }
        if (spenderAccountId != null) {
            spenderAccountId.validateChecksum(client);
        }
    }

    /**
     * Convert a crypto allowance into a protobuf.
     *
     * @return                          the protobuf
     */
    CryptoAllowance toProtobuf() {
        var builder = CryptoAllowance.newBuilder().setAmount(amount.toTinybars());
        if (ownerAccountId != null) {
            builder.setOwner(ownerAccountId.toProtobuf());
        }
        if (spenderAccountId != null) {
            builder.setSpender(spenderAccountId.toProtobuf());
        }
        return builder.build();
    }

    /**
     * Convert a crypto allowance into a granted crypto allowance protobuf.
     *
     * @return                          the granted crypto allowance
     */
    GrantedCryptoAllowance toGrantedProtobuf() {
        var builder = GrantedCryptoAllowance.newBuilder().setAmount(amount.toTinybars());
        if (spenderAccountId != null) {
            builder.setSpender(spenderAccountId.toProtobuf());
        }
        return builder.build();
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
        return MoreObjects.toStringHelper(this)
                .add("ownerAccountId", ownerAccountId)
                .add("spenderAccountId", spenderAccountId)
                .add("amount", amount)
                .toString();
    }
}
