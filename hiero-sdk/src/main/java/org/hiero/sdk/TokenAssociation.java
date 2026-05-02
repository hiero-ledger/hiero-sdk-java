// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Associates the provided Hedera account with the provided Hedera token(s).
 * Hedera accounts must be associated with a fungible or non-fungible token
 * first before you can transfer tokens to that account. In the case of
 * NON_FUNGIBLE Type, once an account is associated, it can hold any number
 * of NFTs (serial numbers) of that token type. The Hedera account that is
 * being associated with a token is required to sign the transaction.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/associate-tokens-to-an-account">Hedera Documentation</a>
 */
public class TokenAssociation {

    /**
     * The token involved in the association
     */
    public final TokenId tokenId;

    /**
     * The account involved in the association
     */
    public final AccountId accountId;

    /**
     * Constructor.
     *
     * @param tokenId                   the token id
     * @param accountId                 the account id
     */
    TokenAssociation(TokenId tokenId, AccountId accountId) {
        this.tokenId = tokenId;
        this.accountId = accountId;
    }

    /**
     * Create a token association from a protobuf.
     *
     * @param tokenAssociation          the protobuf
     * @return                          the new token association
     */
    static TokenAssociation fromProtobuf(org.hiero.sdk.proto.TokenAssociation tokenAssociation) {
        return new TokenAssociation(
                tokenAssociation.hasTokenId()
                        ? TokenId.fromProtobuf(tokenAssociation.getTokenId())
                        : new TokenId(0, 0, 0),
                tokenAssociation.hasAccountId()
                        ? AccountId.fromProtobuf(tokenAssociation.getAccountId())
                        : new AccountId(0, 0, 0));
    }

    /**
     * Create a token association from a byte array.
     *
     * @param bytes                     the byte array
     * @return                          the new token association
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    public static TokenAssociation fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(org.hiero.sdk.proto.TokenAssociation.parseFrom(bytes));
    }

    /**
     * Create the protobuf.
     *
     * @return                          the protobuf representation
     */
    org.hiero.sdk.proto.TokenAssociation toProtobuf() {
        return org.hiero.sdk.proto.TokenAssociation.newBuilder()
                .setTokenId(tokenId.toProtobuf())
                .setAccountId(accountId.toProtobuf())
                .build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tokenId", tokenId)
                .add("accountId", accountId)
                .toString();
    }

    /**
     * Create the byte array.
     *
     * @return                          the byte array representation
     */
    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }
}
