// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.TokenUpdateNftsTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Modify the metadata field for an individual non-fungible/unique token (NFT).
 *
 * Updating the metadata of an NFT SHALL NOT affect ownership or
 * the ability to transfer that NFT.<br/>
 * This transaction SHALL affect only the specific serial numbered tokens
 * identified.
 * This transaction SHALL modify individual token metadata.<br/>
 * This transaction MUST be signed by the token `metadata_key`.<br/>
 * The token `metadata_key` MUST be a valid `Key`.<br/>
 * The token `metadata_key` MUST NOT be an empty `KeyList`.
 *
 * ### Block Stream Effects
 * None
 */
public class TokenUpdateNftsTransaction extends Transaction<TokenUpdateNftsTransaction> {

    @Nullable
    private TokenId tokenId = null;

    private List<Long> serials = new ArrayList<>();

    private byte[] metadata = null;

    /**
     * Constructor.
     */
    public TokenUpdateNftsTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException when there is an issue with the protobuf
     */
    TokenUpdateNftsTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    TokenUpdateNftsTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the token id.
     *
     * @return the token id
     */
    @Nullable
    public TokenId getTokenId() {
        return tokenId;
    }

    /**
     * A token identifier.<br/>
     * This is the token type (i.e. collection) for which to update NFTs.
     * <p>
     * This field is REQUIRED.<br/>
     * The identified token MUST exist, MUST NOT be paused, MUST have the type
     * non-fungible/unique, and MUST have a valid `metadata_key`.
     *
     * @param tokenId the token id
     * @return {@code this}
     */
    public TokenUpdateNftsTransaction setTokenId(@Nullable TokenId tokenId) {
        requireNotFrozen();
        Objects.requireNonNull(tokenId);
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Extract the list of serial numbers.
     *
     * @return the list of serial numbers
     */
    public List<Long> getSerials() {
        return serials;
    }

    /**
     * A list of serial numbers to be updated.
     * <p>
     * This field is REQUIRED.<br/>
     * This list MUST have at least one(1) entry.<br/>
     * This list MUST NOT have more than ten(10) entries.
     *
     * @param serials the list of serial numbers
     * @return {@code this}
     */
    public TokenUpdateNftsTransaction setSerials(List<Long> serials) {
        requireNotFrozen();
        Objects.requireNonNull(serials);
        this.serials = new ArrayList<>(serials);
        return this;
    }

    /**
     * Add a serial number to the list of serial numbers.
     *
     * @param serial the serial number to add
     * @return {@code this}
     */
    public TokenUpdateNftsTransaction addSerial(@Nonnegative long serial) {
        requireNotFrozen();
        serials.add(serial);
        return this;
    }

    /**
     * Extract the metadata.
     *
     * @return the metadata
     */
    @Nullable
    public byte[] getMetadata() {
        return metadata;
    }

    /**
     * A new value for the metadata.
     * <p>
     * If this field is not set, the metadata SHALL NOT change.<br/>
     * This value, if set, MUST NOT exceed 100 bytes.
     *
     * @param metadata the metadata
     * @return {@code this}
     */
    public TokenUpdateNftsTransaction setMetadata(byte[] metadata) {
        requireNotFrozen();
        this.metadata = metadata;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenUpdateNfts();
        if (body.hasToken()) {
            tokenId = TokenId.fromProtobuf(body.getToken());
        }
        serials = body.getSerialNumbersList();
        if (body.hasMetadata()) {
            metadata = body.getMetadata().getValue().toByteArray();
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link org.hiero.sdk.proto.TokenUpdateNftsTransactionBody}
     */
    TokenUpdateNftsTransactionBody.Builder build() {
        var builder = TokenUpdateNftsTransactionBody.newBuilder();

        if (tokenId != null) {
            builder.setToken(tokenId.toProtobuf());
        }

        for (var serial : serials) {
            builder.addSerialNumbers(serial);
        }

        if (metadata != null) {
            builder.setMetadata(BytesValue.of(ByteString.copyFrom(metadata)));
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
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getUpdateNftsMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenUpdateNfts(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenUpdateNfts(build());
    }
}
