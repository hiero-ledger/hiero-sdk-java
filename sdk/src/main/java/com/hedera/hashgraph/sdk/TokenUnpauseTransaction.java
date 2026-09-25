// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TokenServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TokenUnpauseTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Resume transaction activity for a token.
 *
 * This transaction MUST be signed by the Token `pause_key`.<br/>
 * The `token` identified MUST exist, and MUST NOT be deleted.<br/>
 * The `token` identified MAY not be paused; if the token is not paused,
 * this transaction SHALL have no effect.
 * The `token` identified MUST have a `pause_key` set, the `pause_key` MUST be
 * a valid `Key`, and the `pause_key` MUST NOT be an empty `KeyList`.<br/>
 * An `unpaused` token MAY be transferred or otherwise modified.
 *
 * ### Block Stream Effects
 * None
 */
public class TokenUnpauseTransaction extends Transaction<TokenUnpauseTransaction> {
    private @Nullable TokenId tokenId = null;

    /**
     * Constructor
     */
    public TokenUnpauseTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenUnpauseTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TokenUnpauseTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the token id.
     *
     * @return                          the token id
     */
    public @Nullable TokenId getTokenId() {
        return tokenId;
    }

    /**
     * A token identifier.
     * <p>
     * The identified token SHALL be "unpaused". Subsequent transactions
     * involving that token MAY succeed.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public TokenUnpauseTransaction setTokenId(TokenId tokenId) {
        Objects.requireNonNull(tokenId);
        requireNotFrozen();
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenUnpause();
        if (body.hasToken()) {
            tokenId = TokenId.fromProtobuf(body.getToken());
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link
     *         com.hedera.hashgraph.sdk.proto.TokenUnpauseTransactionBody}
     */
    TokenUnpauseTransactionBody.Builder build() {
        var builder = TokenUnpauseTransactionBody.newBuilder();
        if (tokenId != null) {
            builder.setToken(tokenId.toProtobuf());
        }

        return builder;
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getUnpauseTokenMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenUnpause(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenUnpause(build());
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (tokenId != null) {
            tokenId.validateChecksum(client);
        }
    }
}
