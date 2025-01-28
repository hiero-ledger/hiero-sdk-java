// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenRevokeKycTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Revoke "Know Your Customer"(KYC) from one account for a single token.
 *
 * This transaction MUST be signed by the `kyc_key` for the token.<br/>
 * The identified token MUST have a `kyc_key` set to a valid `Key` value.<br/>
 * The token `kyc_key` MUST NOT be an empty `KeyList`.<br/>
 * The identified token MUST exist and MUST NOT be deleted.<br/>
 * The identified account MUST exist and MUST NOT be deleted.<br/>
 * The identified account MUST have an association to the identified token.<br/>
 * On success the association between the identified account and the identified
 * token SHALL NOT be marked as "KYC granted".
 *
 * ### Block Stream Effects
 * None
 */
public class TokenRevokeKycTransaction extends org.hiero.sdk.Transaction<TokenRevokeKycTransaction> {

    @Nullable
    private TokenId tokenId = null;

    @Nullable
    private AccountId accountId = null;

    /**
     * Constructor.
     */
    public TokenRevokeKycTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenRevokeKycTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TokenRevokeKycTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
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
     * The identified token SHALL revoke "KYC" for the account
     * identified by the `account` field.<br/>
     * The identified token MUST be associated to the account identified
     * by the `account` field.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public TokenRevokeKycTransaction setTokenId(TokenId tokenId) {
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
    @Nullable
    public AccountId getAccountId() {
        return accountId;
    }

    /**
     * An account identifier.
     * <p>
     * The token identified by the `token` field SHALL revoke "KYC" for the
     * identified account.<br/>
     * This account MUST be associated to the token identified
     * by the `token` field.
     *
     * @param accountId                 the account id
     * @return {@code this}
     */
    public TokenRevokeKycTransaction setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.accountId = accountId;
        return this;
    }

    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenRevokeKyc();
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
     *         org.hiero.sdk.proto.TokenRevokeKycTransactionBody}
     */
    TokenRevokeKycTransactionBody.Builder build() {
        var builder = TokenRevokeKycTransactionBody.newBuilder();
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
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getFreezeTokenAccountMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenRevokeKyc(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenRevokeKyc(build());
    }
}
