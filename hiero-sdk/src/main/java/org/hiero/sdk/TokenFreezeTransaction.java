// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenFreezeAccountTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Block transfers of a token type for an account.<br/>
 * This, effectively, freezes assets of one account with respect to
 * one token type. While frozen, that account cannot send or receive tokens
 * of the identified type.
 *
 * The token MUST have a `freeze_key` set and that key MUST NOT
 * be an empty `KeyList`.<br/>
 * The token `freeze_key` MUST sign this transaction.<br/>
 * The identified token MUST exist, MUST NOT be deleted, MUST NOT be paused,
 * and MUST NOT be expired.<br/>
 * The identified account MUST exist, MUST NOT be deleted, and
 * MUST NOT be expired.<br/>
 * If the identified account is already frozen with respect to the identified
 * token, the transaction SHALL succeed, but no change SHALL be made.<br/>
 * An association between the identified account and the identified
 * token MUST exist.
 *
 * ### Block Stream Effects
 * None
 */
public class TokenFreezeTransaction extends org.hiero.sdk.Transaction<TokenFreezeTransaction> {
    @Nullable
    private TokenId tokenId = null;

    @Nullable
    private AccountId accountId = null;

    /**
     * Constructor.
     */
    public TokenFreezeTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenFreezeTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TokenFreezeTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
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
     * This SHALL identify the token type to "freeze".<br/>
     * The identified token MUST exist, MUST NOT be deleted, and MUST be
     * associated to the identified account.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public TokenFreezeTransaction setTokenId(TokenId tokenId) {
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
     * This shall identify the account to "freeze".<br/>
     * The identified account MUST exist, MUST NOT be deleted, MUST NOT be
     * expired, and MUST be associated to the identified token.<br/>
     * The identified account SHOULD NOT be "frozen" with respect to the
     * identified token.
     *
     * @param accountId                 the account id
     * @return {@code this}
     */
    public TokenFreezeTransaction setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.accountId = accountId;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenFreeze();
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
     *         org.hiero.sdk.proto.TokenFreezeAccountTransactionBody}
     */
    TokenFreezeAccountTransactionBody.Builder build() {
        var builder = TokenFreezeAccountTransactionBody.newBuilder();
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
        bodyBuilder.setTokenFreeze(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenFreeze(build());
    }
}
