// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TokenGrantKycTransactionBody;
import com.hedera.hashgraph.sdk.proto.TokenServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Grant "Know Your Customer"(KYC) for one account for a single token.
 *
 * This transaction MUST be signed by the `kyc_key` for the token.<br/>
 * The identified token MUST have a `kyc_key` set to a valid `Key` value.<br/>
 * The token `kyc_key` MUST NOT be an empty `KeyList`.<br/>
 * The identified token MUST exist and MUST NOT be deleted.<br/>
 * The identified account MUST exist and MUST NOT be deleted.<br/>
 * The identified account MUST have an association to the identified token.<br/>
 * On success the association between the identified account and the identified
 * token SHALL be marked as "KYC granted".
 *
 * ### Block Stream Effects
 * None
 */
public class TokenGrantKycTransaction extends com.hedera.hashgraph.sdk.Transaction<TokenGrantKycTransaction> {
    private @Nullable TokenId tokenId = null;

    private @Nullable AccountId accountId = null;

    /**
     * Configure.
     */
    public TokenGrantKycTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenGrantKycTransaction(
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
    TokenGrantKycTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
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
     * The identified token SHALL grant "KYC" for the account
     * identified by the `account` field.<br/>
     * The identified token MUST be associated to the account identified
     * by the `account` field.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public TokenGrantKycTransaction setTokenId(TokenId tokenId) {
        Objects.requireNonNull(tokenId);
        requireNotFrozen();
        this.tokenId = tokenId;
        return this;
    }

    /**
     * Extract the account id.
     *
     * @return                          the account id
     */
    public @Nullable AccountId getAccountId() {
        return accountId;
    }

    /**
     * An account identifier.
     * <p>
     * The token identified by the `token` field SHALL grant "KYC" for the
     * identified account.<br/>
     * This account MUST be associated to the token identified
     * by the `token` field.
     *
     * @param accountId                 the account id
     * @return {@code this}
     */
    public TokenGrantKycTransaction setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.accountId = accountId;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenGrantKyc();
        if (body.hasToken()) {
            tokenId = TokenId.fromProtobuf(body.getToken());
        }

        if (body.hasAccount()) {
            accountId = AccountId.fromProtobuf(body.getAccount());
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link
     *         com.hedera.hashgraph.sdk.proto.TokenGrantKycTransactionBody}
     */
    TokenGrantKycTransactionBody.Builder build() {
        var builder = TokenGrantKycTransactionBody.newBuilder();
        if (tokenId != null) {
            builder.setToken(tokenId.toProtobuf());
        }

        if (accountId != null) {
            builder.setAccount(accountId.toProtobuf());
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (tokenId != null) {
            tokenId.validateChecksum(client);
        }

        if (accountId != null) {
            accountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getFreezeTokenAccountMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenGrantKyc(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenGrantKyc(build());
    }
}
