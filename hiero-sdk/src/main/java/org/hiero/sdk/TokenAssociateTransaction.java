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
import org.hiero.sdk.proto.TokenAssociateTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Associate a Hedera Token Service (HTS) token and an account.
 *
 * An association MUST exist between an account and a token before that
 * account may transfer or receive that token.<br/>
 * If the identified account is not found,
 * the transaction SHALL return `INVALID_ACCOUNT_ID`.<br/>
 * If the identified account has been deleted,
 * the transaction SHALL return `ACCOUNT_DELETED`.<br/>
 * If any of the identified tokens is not found,
 * the transaction SHALL return `INVALID_TOKEN_REF`.<br/>
 * If any of the identified tokens has been deleted,
 * the transaction SHALL return `TOKEN_WAS_DELETED`.<br/>
 * If an association already exists for any of the identified tokens,
 * the transaction SHALL return `TOKEN_ALREADY_ASSOCIATED_TO_ACCOUNT`.<br/>
 * The identified account MUST sign this transaction.
 *
 * ### Block Stream Effects
 * None
 */
public class TokenAssociateTransaction extends Transaction<TokenAssociateTransaction> {
    @Nullable
    private AccountId accountId = null;

    private List<TokenId> tokenIds = new ArrayList<>();

    /**
     * Constructor.
     */
    public TokenAssociateTransaction() {
        defaultMaxTransactionFee = new Hbar(5);
    }

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenAssociateTransaction(
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
    TokenAssociateTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
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
     * The identified account SHALL be associated to each of the
     * tokens identified in the `tokens` field.<br/>
     * This field is REQUIRED and MUST be a valid account identifier.<br/>
     * The identified account MUST exist in state.<br/>
     * The identified account MUST NOT be deleted.<br/>
     * The identified account MUST NOT be expired.
     *
     * @param accountId                 the account id
     * @return {@code this}
     */
    public TokenAssociateTransaction setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.accountId = accountId;
        return this;
    }

    /**
     * Extract the list of token id's.
     *
     * @return                          the list of token id's
     */
    public List<TokenId> getTokenIds() {
        return new ArrayList<>(tokenIds);
    }

    /**
     * A list of token identifiers.
     * <p>
     * Each token identified in this list SHALL be separately associated with
     * the account identified in the `account` field.<br/>
     * This list MUST NOT be empty.
     * Each entry in this list MUST be a valid token identifier.<br/>
     * Each entry in this list MUST NOT be currently associated to the
     * account identified in `account`.<br/>
     * Each entry in this list MUST NOT be expired.<br/>
     * Each entry in this list MUST NOT be deleted.
     *
     * @param tokens                    the list of token id's
     * @return {@code this}
     */
    public TokenAssociateTransaction setTokenIds(List<TokenId> tokens) {
        Objects.requireNonNull(tokens);
        requireNotFrozen();
        this.tokenIds = new ArrayList<>(tokens);
        return this;
    }

    /**
     * Build the transaction body.
     *
     * @return {@link org.hiero.sdk.proto.TokenAssociateTransactionBody}
     */
    TokenAssociateTransactionBody.Builder build() {
        var builder = TokenAssociateTransactionBody.newBuilder();
        if (accountId != null) {
            builder.setAccount(accountId.toProtobuf());
        }

        for (var token : tokenIds) {
            if (token != null) {
                builder.addTokens(token.toProtobuf());
            }
        }

        return builder;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenAssociate();
        if (body.hasAccount()) {
            accountId = AccountId.fromProtobuf(body.getAccount());
        }

        for (var token : body.getTokensList()) {
            tokenIds.add(TokenId.fromProtobuf(token));
        }
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        Objects.requireNonNull(client);
        if (accountId != null) {
            accountId.validateChecksum(client);
        }

        for (var token : tokenIds) {
            if (token != null) {
                token.validateChecksum(client);
            }
        }
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getAssociateTokensMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenAssociate(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenAssociate(build());
    }
}
