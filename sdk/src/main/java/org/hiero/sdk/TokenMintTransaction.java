// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenMintTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.Transaction;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Mint tokens and deliver the new tokens to the token treasury account.
 *
 * The token MUST have a `supply_key` set and that key MUST NOT
 * be an empty `KeyList`.<br/>
 * The token `supply_key` MUST sign this transaction.<br/>
 * This operation SHALL increase the total supply for the token type by
 * the number of tokens "minted".<br/>
 * The total supply for the token type MUST NOT be increased above the
 * maximum supply limit (2^63-1) by this transaction.<br/>
 * The tokens minted SHALL be credited to the token treasury account.<br/>
 * If the token is a fungible/common type, the amount MUST be specified.<br/>
 * If the token is a non-fungible/unique type, the metadata bytes for each
 * unique token MUST be specified in the `metadata` list.<br/>
 * Each unique metadata MUST not exceed the global metadata size limit defined
 * by the network configuration value `tokens.maxMetadataBytes`.<br/>
 * The global batch size limit (`tokens.nfts.maxBatchSizeMint`) SHALL set
 * the maximum number of individual NFT metadata permitted in a single
 * `tokenMint` transaction.
 *
 * ### Block Stream Effects
 * None
 */
public class TokenMintTransaction extends org.hiero.sdk.Transaction<TokenMintTransaction> {
    @Nullable
    private TokenId tokenId = null;

    private List<byte[]> metadataList = new ArrayList<>();

    private long amount = 0;

    /**
     * Constructor.
     */
    public TokenMintTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenMintTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TokenMintTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the token id.
     *
     * @return                          the token id
     */
    @Nullable
    public TokenId getTokenId() {
        return tokenId;
    }

    /**
     * A token identifier.
     * <p>
     * This SHALL identify the token type to "mint".<br/>
     * The identified token MUST exist, and MUST NOT be deleted.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public TokenMintTransaction setTokenId(TokenId tokenId) {
        Objects.requireNonNull(tokenId);
        requireNotFrozen();
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Extract the amount.
     *
     * @return                          the amount to mint
     */
    public long getAmount() {
        return amount;
    }

    /**
     * An amount to mint to the Treasury Account.
     * <p>
     * This is interpreted as an amount in the smallest possible denomination
     * for the token (10<sup>-decimals</sup> whole tokens).<br/>
     * The balance for the token treasury account SHALL receive the newly
     * minted tokens.<br/>
     * If this value is equal to zero (`0`), the token SHOULD be a
     * non-fungible/unique type.<br/>
     * If this value is non-zero, the token MUST be a fungible/common type.
     *
     * @param amount                    the amount to mint
     * @return {@code this}
     */
    public TokenMintTransaction setAmount(@Nonnegative long amount) {
        requireNotFrozen();
        this.amount = amount;
        return this;
    }

    /**
     * Add to the metadata list.
     *
     * @param metadata                  the metadata 100 bytes max
     * @return {@code this}
     */
    public TokenMintTransaction addMetadata(byte[] metadata) {
        requireNotFrozen();
        Objects.requireNonNull(metadata);
        metadataList.add(metadata);
        return this;
    }

    /**
     * Extract the list of metadata byte array records.
     *
     * @return                          the metadata list
     */
    public List<byte[]> getMetadata() {
        return new ArrayList<>(metadataList);
    }

    /**
     * A list of metadata bytes.<br/>
     * <p>
     * One non-fungible/unique token SHALL be minted for each entry
     * in this list.<br/>
     * Each entry in this list MUST NOT be larger than the limit set by the
     * current network configuration value `tokens.maxMetadataBytes`.<br/>
     * This list MUST NOT contain more entries than the current limit set by
     * the network configuration value `tokens.nfts.maxBatchSizeMint`.<br/>
     * If this list is not empty, the token MUST be a
     * non-fungible/unique type.<br/>
     * If this list is empty, the token MUST be a fungible/common type.
     *
     * @param metadataList              the metadata list
     * @return {@code this}
     */
    public TokenMintTransaction setMetadata(List<byte[]> metadataList) {
        requireNotFrozen();
        this.metadataList = new ArrayList<>(metadataList);
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenMint();
        if (body.hasToken()) {
            tokenId = TokenId.fromProtobuf(body.getToken());
        }
        amount = body.getAmount();
        for (var metadata : body.getMetadataList()) {
            metadataList.add(metadata.toByteArray());
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link
     *         org.hiero.sdk.proto.TokenMintTransactionBody}
     */
    TokenMintTransactionBody.Builder build() {
        var builder = TokenMintTransactionBody.newBuilder();
        if (tokenId != null) {
            builder.setToken(tokenId.toProtobuf());
        }
        builder.setAmount(amount);
        for (var metadata : metadataList) {
            builder.addMetadata(ByteString.copyFrom(metadata));
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (tokenId != null) {
            tokenId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getMintTokenMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenMint(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenMint(build());
    }
}
