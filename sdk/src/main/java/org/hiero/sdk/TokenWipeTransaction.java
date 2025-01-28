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
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.TokenWipeAccountTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Wipe (administratively burn) tokens held by a non-treasury account.<br/>
 * On success, the requested tokens will be removed from the identified account
 * and the token supply will be reduced by the amount "wiped".
 *
 * This transaction MUST be signed by the token `wipe_key`.<br/>
 * The identified token MUST exist, MUST NOT be deleted,
 * and MUST NOT be paused.<br/>
 * The identified token MUST have a valid `Key` set for the `wipe_key` field,
 * and that key MUST NOT be an empty `KeyList`.<br/>
 * The identified account MUST exist, MUST NOT be deleted, MUST be
 * associated to the identified token, MUST NOT be frozen for the identified
 * token, MUST NOT be the token `treasury`, and MUST hold a balance for the
 * token or the specific serial numbers provided.<br/>
 * This transaction SHOULD provide a value for `amount` or `serialNumbers`,
 * but MUST NOT set both fields.
 *
 * ### Block Stream Effects
 * The new total supply for the wiped token type SHALL be recorded.
 */
public class TokenWipeTransaction extends org.hiero.sdk.Transaction<TokenWipeTransaction> {

    @Nullable
    private TokenId tokenId = null;

    @Nullable
    private AccountId accountId = null;

    private long amount = 0;

    private List<Long> serials = new ArrayList<>();

    /**
     * Constructor.
     */
    public TokenWipeTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TokenWipeTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TokenWipeTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
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
     * This field is REQUIRED.<br/>
     * The identified token MUST exist, MUST NOT be paused, MUST NOT be
     * deleted, and MUST NOT be expired.
     *
     * @param tokenId                   the token id
     * @return {@code this}
     */
    public TokenWipeTransaction setTokenId(TokenId tokenId) {
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
     * An account identifier.<br/>
     * This identifies the account from which tokens will be wiped.
     * <p>
     * This field is REQUIRED.<br/>
     * The identified account MUST NOT be deleted or expired.<br/>
     * If the identified token `kyc_key` is set to a valid key, the
     * identified account MUST have "KYC" granted.<br/>
     * The identified account MUST NOT be the `treasury` account for the
     * identified token.
     *
     * @param accountId                 the account id
     * @return {@code this}
     */
    public TokenWipeTransaction setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.accountId = accountId;
        return this;
    }

    /**
     * Extract the amount.
     *
     * @return                          the amount
     */
    public long getAmount() {
        return amount;
    }

    /**
     * An amount of fungible/common tokens to wipe.
     * <p>
     * If the identified token is a non-fungible/unique token type,
     * this value MUST be exactly zero(`0`).<br/>
     * If the identified token type is fungible/common:
     * <ul>
     *   <li>This value SHALL be specified in units of the smallest
     *       denomination possible for the identified token
     *       (<tt>10<sup>-decimals</sup></tt> whole tokens).</li>
     *   <li>This value MUST be strictly less than `Long.MAX_VALUE`.</li>
     *   <li>This value MUST be less than or equal to the current total
     *       supply for the identified token.</li>
     *   <li>This value MUST be less than or equal to the current balance
     *       held by the identified account.</li>
     *   <li>This value MAY be zero(`0`).</li>
     * </ul>
     *
     * @param amount                    the amount
     * @return {@code this}
     */
    public TokenWipeTransaction setAmount(@Nonnegative long amount) {
        requireNotFrozen();
        this.amount = amount;
        return this;
    }

    /**
     * Extract the list of serial numbers.
     *
     * @return                          the list of serial numbers
     */
    public List<Long> getSerials() {
        return new ArrayList<>(serials);
    }

    /**
     * A list of serial numbers to wipe.<br/>
     * The non-fungible/unique tokens with these serial numbers will be
     * destroyed and cannot be recovered or reused.
     * <p>
     * If the identified token type is a fungible/common type, this
     * list MUST be empty.<br/>
     * If the identified token type is non-fungible/unique:
     * <ul>
     *   <li>This list MUST contain at least one entry if the identified token
     *       type is non-fungible/unique.>/li>
     *   <li>This list MUST NOT contain more entries than the current total
     *       supply for the identified token.</li>
     *   <li>Every entry in this list MUST be a valid serial number for the
     *       identified token (i.e. "collection").</li>
     *   <li>Every entry in this list MUST be owned by the
     *       identified account</li>
     *   <li></li>
     * </ul>
     * This list MUST NOT contain more entries than the network configuration
     * value for batch size limit, typically ten(`10`).
     *
     * @param serials                   the list of serial numbers
     * @return {@code this}
     */
    public TokenWipeTransaction setSerials(List<Long> serials) {
        requireNotFrozen();
        Objects.requireNonNull(serials);
        this.serials = new ArrayList<>(serials);
        return this;
    }

    /**
     * Add a serial number to the list of serial numbers.
     *
     * @param serial                    the serial number to add
     * @return {@code this}
     */
    public TokenWipeTransaction addSerial(@Nonnegative long serial) {
        requireNotFrozen();
        serials.add(serial);
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getTokenWipe();
        if (body.hasToken()) {
            tokenId = TokenId.fromProtobuf(body.getToken());
        }

        if (body.hasAccount()) {
            accountId = AccountId.fromProtobuf(body.getAccount());
        }
        amount = body.getAmount();
        serials = body.getSerialNumbersList();
    }

    /**
     * Build the transaction body.
     *
     * @return {@link
     *         org.hiero.sdk.proto.TokenWipeAccountTransactionBody}
     */
    TokenWipeAccountTransactionBody.Builder build() {
        var builder = TokenWipeAccountTransactionBody.newBuilder();
        if (tokenId != null) {
            builder.setToken(tokenId.toProtobuf());
        }

        if (accountId != null) {
            builder.setAccount(accountId.toProtobuf());
        }
        builder.setAmount(amount);
        for (var serial : serials) {
            builder.addSerialNumbers(serial);
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
        return TokenServiceGrpc.getWipeTokenAccountMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenWipe(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setTokenWipe(build());
    }
}
