// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenReference;
import org.hiero.sdk.proto.TokenRejectTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Reject undesired token(s).<br/>
 * Transfer one or more token balances held by the requesting account to the
 * treasury for each token type.
 *
 * Each transfer SHALL be one of the following
 * - A single non-fungible/unique token.
 * - The full balance held for a fungible/common token.
 * A single `tokenReject` transaction SHALL support a maximum
 * of 10 transfers.<br/>
 * A token that is `pause`d MUST NOT be rejected.<br/>
 * If the `owner` account is `frozen` with respect to the identified token(s)
 * the token(s) MUST NOT be rejected.<br/>
 * The `payer` for this transaction, and `owner` if set, SHALL NOT be charged
 * any custom fees or other fees beyond the `tokenReject` transaction fee.
 *
 * ### Block Stream Effects
 * - Each successful transfer from `payer` to `treasury` SHALL be recorded in
 *   the `token_transfer_list` for the transaction record.
 */
public class TokenRejectTransaction extends Transaction<TokenRejectTransaction> {

    @Nullable
    private AccountId ownerId = null;

    private List<TokenId> tokenIds = new ArrayList<>();

    private List<NftId> nftIds = new ArrayList<>();

    /**
     * Constructor
     */
    public TokenRejectTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    TokenRejectTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TokenRejectTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the Account ID of the Owner.
     * @return the Account ID of the Owner.
     */
    @Nullable
    public AccountId getOwnerId() {
        return ownerId;
    }

    /**
     * An account identifier.<br/>
     * This OPTIONAL field identifies the account holding the
     * tokens to be rejected.
     * <p>
     * If set, this account MUST sign this transaction.
     * If not set, the `payer` for this transaction SHALL be the effective
     * `owner` for this transaction.
     *
     * @param ownerId the Account ID of the Owner.
     * @return {@code this}
     */
    public TokenRejectTransaction setOwnerId(AccountId ownerId) {
        Objects.requireNonNull(ownerId);
        requireNotFrozen();
        this.ownerId = ownerId;
        return this;
    }

    /**
     * Extract the list of tokenIds.
     * @return the list of tokenIds.
     */
    public List<TokenId> getTokenIds() {
        return tokenIds;
    }

    /**
     * A list of one or more token rejections (a fungible/common token type).
     *
     * @param tokenIds the list of tokenIds.
     * @return {@code this}
     */
    public TokenRejectTransaction setTokenIds(List<TokenId> tokenIds) {
        requireNotFrozen();
        Objects.requireNonNull(tokenIds);
        this.tokenIds = new ArrayList<>(tokenIds);
        return this;
    }

    /**
     * Add a token to the list of tokens.
     * @param tokenId token to add.
     * @return {@code this}
     */
    public TokenRejectTransaction addTokenId(TokenId tokenId) {
        requireNotFrozen();
        tokenIds.add(tokenId);
        return this;
    }

    /**
     * Extract the list of nftIds.
     * @return the list of nftIds.
     */
    public List<NftId> getNftIds() {
        return nftIds;
    }

    /**
     * A list of one or more token rejections (a single specific serialized non-fungible/unique token).
     *
     * @param nftIds the list of nftIds.
     * @return {@code this}
     */
    public TokenRejectTransaction setNftIds(List<NftId> nftIds) {
        requireNotFrozen();
        Objects.requireNonNull(nftIds);
        this.nftIds = new ArrayList<>(nftIds);
        return this;
    }

    /**
     * Add a nft to the list of nfts.
     * @param nftId nft to add.
     * @return {@code this}
     */
    public TokenRejectTransaction addNftId(NftId nftId) {
        requireNotFrozen();
        nftIds.add(nftId);
        return this;
    }

    /**
     * Build the transaction body.
     *
     * @return {@link org.hiero.sdk.proto.TokenRejectTransactionBody}
     */
    TokenRejectTransactionBody.Builder build() {
        var builder = TokenRejectTransactionBody.newBuilder();

        if (ownerId != null) {
            builder.setOwner(ownerId.toProtobuf());
        }

        for (TokenId tokenId : tokenIds) {
            builder.addRejections(TokenReference.newBuilder()
                    .setFungibleToken(tokenId.toProtobuf())
                    .build());
        }

        for (NftId nftId : nftIds) {
            builder.addRejections(
                    TokenReference.newBuilder().setNft(nftId.toProtobuf()).build());
        }

        return builder;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenReject();
        if (body.hasOwner()) {
            ownerId = AccountId.fromProtobuf(body.getOwner());
        }

        for (TokenReference tokenReference : body.getRejectionsList()) {
            if (tokenReference.hasFungibleToken()) {
                tokenIds.add(TokenId.fromProtobuf(tokenReference.getFungibleToken()));
            } else if (tokenReference.hasNft()) {
                nftIds.add(NftId.fromProtobuf(tokenReference.getNft()));
            }
        }
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (ownerId != null) {
            ownerId.validateChecksum(client);
        }

        for (var token : tokenIds) {
            if (token != null) {
                token.validateChecksum(client);
            }
        }

        for (var nftId : nftIds) {
            nftId.tokenId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getRejectTokenMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenReject(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenReject(build());
    }
}
