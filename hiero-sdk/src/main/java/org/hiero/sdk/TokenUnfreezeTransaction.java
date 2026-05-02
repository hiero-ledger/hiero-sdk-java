// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.TokenUnfreezeAccountTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Resume transfers of a token type for an account.<br/>
 * This releases previously frozen assets of one account with respect to
 * one token type. Once unfrozen, that account can once again send or
 * receive tokens of the identified type.
 *
 * The token MUST have a `freeze_key` set and that key MUST NOT
 * be an empty `KeyList`.<br/>
 * The token `freeze_key` MUST sign this transaction.<br/>
 * The identified token MUST exist, MUST NOT be deleted, MUST NOT be paused,
 * and MUST NOT be expired.<br/>
 * The identified account MUST exist, MUST NOT be deleted, and
 * MUST NOT be expired.<br/>
 * If the identified account is not frozen with respect to the identified
 * token, the transaction SHALL succeed, but no change SHALL be made.<br/>
 * An association between the identified account and the identified
 * token MUST exist.
 *
 * ### Block Stream Effects
 * None
 */
public class TokenUnfreezeTransaction extends org.hiero.sdk.Transaction<TokenUnfreezeTransaction> {
    @Nullable
    private TokenId tokenId = null;

    @Nullable
    private AccountId accountId = null;

    /**
     * Constructor.
     */
    public TokenUnfreezeTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenUnfreezeTransaction(
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
    TokenUnfreezeTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
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
     * This SHALL identify the token type to "unfreeze".<br/>
     * The identified token MUST exist, MUST NOT be deleted, and MUST be
     * associated to the identified account.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public TokenUnfreezeTransaction setTokenId(TokenId tokenId) {
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
     * This shall identify the account to "unfreeze".<br/>
     * The identified account MUST exist, MUST NOT be deleted, MUST NOT be
     * expired, and MUST be associated to the identified token.<br/>
     * The identified account SHOULD be "frozen" with respect to the
     * identified token.
     *
     * @param accountId                 the account id
     * @return {@code this}
     */
    public TokenUnfreezeTransaction setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.accountId = accountId;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenUnfreeze();
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
     * @return {@code @link
     *         org.hiero.sdk.proto.TokenUnfreezeAccountTransactionBody}
     */
    TokenUnfreezeAccountTransactionBody.Builder build() {
        var builder = TokenUnfreezeAccountTransactionBody.newBuilder();
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
        bodyBuilder.setTokenUnfreeze(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenUnfreeze(build());
    }
}
