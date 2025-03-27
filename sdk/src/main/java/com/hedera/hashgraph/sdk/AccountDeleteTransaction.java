// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.CryptoDeleteTransactionBody;
import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Delete an account.<br/>
 * This will mark an account deleted, and transfer all tokens to a "sweep"
 * account.
 *
 * A deleted account SHALL NOT hold a balance in any token type.<br/>
 * A deleted account SHALL remain in state until it expires.<br/>
 * Transfers that would increase the balance of a deleted account
 * SHALL fail.<br/>
 * A deleted account MAY be subject of a `cryptoUpdate` transaction to extend
 * its expiration.<br/>
 * When a deleted account expires it SHALL be removed entirely, and SHALL NOT
 * be archived.
 *
 * ### Block Stream Effects
 * None
 */
public final class AccountDeleteTransaction extends Transaction<AccountDeleteTransaction> {
    @Nullable
    private AccountId accountId = null;

    @Nullable
    private AccountId transferAccountId = null;

    /**
     * Constructor.
     */
    public AccountDeleteTransaction() {}

    /**
     * Constructor.
     *
     * @param txs                                   Compound list of transaction id's list of (AccountId, Transaction) records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    AccountDeleteTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody                    protobuf TransactionBody
     */
    AccountDeleteTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
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
     * This account SHALL be deleted if this transaction succeeds.<br/>
     * This account SHOULD NOT hold any balance other than HBAR.<br/>
     * If this account _does_ hold balances, the `transferAccountID` value
     * MUST be set to a valid transfer account.<br/>
     * This account MUST sign this transaction.<br/>
     * This field MUST be set to a valid account identifier.
     *
     * @param deleteAccountId The AccountId to be set
     * @return {@code this}
     */
    public AccountDeleteTransaction setAccountId(AccountId deleteAccountId) {
        Objects.requireNonNull(deleteAccountId);
        requireNotFrozen();
        this.accountId = deleteAccountId;
        return this;
    }

    /**
     * Extract the receiving account id.
     *
     * @return                          the account id that receives the hbar
     */
    @Nullable
    public AccountId getTransferAccountId() {
        return transferAccountId;
    }

    /**
     * An account identifier.
     * <p>
     * The identified account SHALL receive all tokens, token balances,
     * and non-fungible/unique from the deleted account.<br/>
     * The identified account MUST sign this transaction.<br/>
     * If not set, the account to be deleted MUST NOT have a balance in any
     * token, a balance in HBAR, or hold any NFT.
     *
     * @param transferAccountId The AccountId to be set
     * @return {@code this}
     */
    public AccountDeleteTransaction setTransferAccountId(AccountId transferAccountId) {
        requireNotFrozen();
        Objects.requireNonNull(transferAccountId);
        this.transferAccountId = transferAccountId;
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (accountId != null) {
            accountId.validateChecksum(client);
        }

        if (transferAccountId != null) {
            transferAccountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return CryptoServiceGrpc.getCryptoDeleteMethod();
    }

    /**
     * Build the transaction body.
     *
     * @return {@link CryptoDeleteTransactionBody}
     */
    CryptoDeleteTransactionBody.Builder build() {
        var builder = CryptoDeleteTransactionBody.newBuilder();

        if (accountId != null) {
            builder.setDeleteAccountID(accountId.toProtobuf());
        }

        if (transferAccountId != null) {
            builder.setTransferAccountID(transferAccountId.toProtobuf());
        }

        return builder;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getCryptoDelete();
        if (body.hasDeleteAccountID()) {
            accountId = AccountId.fromProtobuf(body.getDeleteAccountID());
        }

        if (body.hasTransferAccountID()) {
            transferAccountId = AccountId.fromProtobuf(body.getTransferAccountID());
        }
    }

    AccountDeleteTransaction fromProtobuf() {
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setCryptoDelete(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setCryptoDelete(build());
    }
}
