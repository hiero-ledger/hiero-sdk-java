// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenBurnTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.Transaction;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Burns tokens from the Token's treasury Account.
 *
 * The token MUST have a `supply_key` set and that key MUST NOT
 * be an empty `KeyList`.<br/>
 * The token `supply_key` MUST sign this transaction.<br/>
 * This operation SHALL decrease the total supply for the token type by
 * the number of tokens "burned".<br/>
 * The total supply for the token type MUST NOT be reduced below zero (`0`)
 * by this transaction.<br/>
 * The tokens to burn SHALL be deducted from the token treasury account.<br/>
 * If the token is a fungible/common type, the amount MUST be specified.<br/>
 * If the token is a non-fungible/unique type, the specific serial numbers
 * MUST be specified.<br/>
 * The global batch size limit (`tokens.nfts.maxBatchSizeBurn`) SHALL set
 * the maximum number of individual NFT serial numbers permitted in a single
 * `tokenBurn` transaction.
 *
 * ### Block Stream Effects
 * None
 */
public class TokenBurnTransaction extends org.hiero.sdk.Transaction<TokenBurnTransaction> {

    @Nullable
    private TokenId tokenId = null;

    private long amount = 0;

    private List<Long> serials = new ArrayList<>();

    /**
     * Constructor.
     */
    public TokenBurnTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenBurnTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TokenBurnTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
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
     * This SHALL identify the token type to "burn".<br/>
     * The identified token MUST exist, and MUST NOT be deleted.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public TokenBurnTransaction setTokenId(TokenId tokenId) {
        Objects.requireNonNull(tokenId);
        requireNotFrozen();
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Extract the amount of tokens to burn.
     *
     * @return                          the amount of tokens to burn
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Assign the amount of tokens to burn.
     *
     * The amount provided must be in the lowest denomination possible.
     *
     * Example: Token A has 2 decimals. In order to burn 100 tokens, one must
     * provide an amount of 10000. In order to burn 100.55 tokens, one must
     * provide an amount of 10055.
     *
     * See <a href="https://docs.hedera.com/guides/docs/sdks/tokens/burn-a-token">Hedera Documentation</a>
     *
     * @param amount                    the amount of tokens to burn
     * @return {@code this}
     */
    public TokenBurnTransaction setAmount(@Nonnegative long amount) {
        requireNotFrozen();
        this.amount = amount;
        return this;
    }

    /**
     * Extract the of token serials.
     *
     * @return                          list of token serials
     */
    public List<Long> getSerials() {
        return new ArrayList<>(serials);
    }

    /**
     * A list of serial numbers to burn from the Treasury Account.
     * <p>
     * This list MUST NOT contain more entries than the current limit set by
     * the network configuration value `tokens.nfts.maxBatchSizeBurn`.<br/>
     * The treasury account for the token MUST hold each unique token
     * identified in this list.<br/>
     * If this list is not empty, the token MUST be a
     * non-fungible/unique type.<br/>
     * If this list is empty, the token MUST be a fungible/common type.
     *
     * @param serials                   list of token serials
     * @return {@code this}
     */
    public TokenBurnTransaction setSerials(List<Long> serials) {
        requireNotFrozen();
        Objects.requireNonNull(serials);
        this.serials = new ArrayList<>(serials);
        return this;
    }

    /**
     * Add a serial number to the list of serials.
     *
     * @param serial                    the serial number to add
     * @return {@code this}
     */
    public TokenBurnTransaction addSerial(@Nonnegative long serial) {
        requireNotFrozen();
        serials.add(serial);
        return this;
    }

    /**
     * Build the transaction body.
     *
     * @return {@link org.hiero.sdk.proto.TokenBurnTransactionBody}
     */
    TokenBurnTransactionBody.Builder build() {
        var builder = TokenBurnTransactionBody.newBuilder();
        if (tokenId != null) {
            builder.setToken(tokenId.toProtobuf());
        }
        builder.setAmount(amount);

        for (var serial : serials) {
            builder.addSerialNumbers(serial);
        }

        return builder;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenBurn();
        if (body.hasToken()) {
            tokenId = TokenId.fromProtobuf(body.getToken());
        }
        amount = body.getAmount();
        serials = body.getSerialNumbersList();
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (tokenId != null) {
            tokenId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getBurnTokenMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenBurn(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenBurn(build());
    }
}
