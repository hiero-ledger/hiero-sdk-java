// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.CryptoDeleteLiveHashTransactionBody;
import org.hiero.sdk.proto.CryptoServiceGrpc;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Delete a specific live hash associated to a given account.
 *
 * This transaction MUST be signed by either the key of the associated account,
 * or at least one of the keys listed in the live hash.
 *
 * ### Block Stream Effects
 * None
 */
public final class LiveHashDeleteTransaction extends Transaction<LiveHashDeleteTransaction> {
    @Nullable
    private AccountId accountId = null;

    private byte[] hash = {};

    /**
     * Constructor.
     */
    public LiveHashDeleteTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    LiveHashDeleteTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
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
     * The account owning the livehash
     *
     * @param accountId The AccountId to be set
     * @return {@code this}
     */
    public LiveHashDeleteTransaction setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        requireNotFrozen();
        this.accountId = accountId;
        return this;
    }

    /**
     * Extract the hash.
     *
     * @return                          the hash
     */
    public ByteString getHash() {
        return ByteString.copyFrom(hash);
    }

    /**
     * The SHA-384 livehash to delete from the account
     *
     * @param hash The array of bytes to be set as hash
     * @return {@code this}
     */
    public LiveHashDeleteTransaction setHash(byte[] hash) {
        requireNotFrozen();
        Objects.requireNonNull(hash);
        this.hash = Arrays.copyOf(hash, hash.length);
        return this;
    }

    /**
     * The SHA-384 livehash to delete from the account
     *
     * @param hash The array of bytes to be set as hash
     * @return {@code this}
     */
    public LiveHashDeleteTransaction setHash(ByteString hash) {
        Objects.requireNonNull(hash);
        return setHash(hash.toByteArray());
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getCryptoDeleteLiveHash();
        if (body.hasAccountOfLiveHash()) {
            accountId = AccountId.fromProtobuf(body.getAccountOfLiveHash());
        }
        hash = body.getLiveHashToDelete().toByteArray();
    }

    /**
     * Build the correct transaction body.
     *
     * @return {@link org.hiero.sdk.proto.CryptoAddLiveHashTransactionBody}
     */
    CryptoDeleteLiveHashTransactionBody.Builder build() {
        var builder = CryptoDeleteLiveHashTransactionBody.newBuilder();
        if (accountId != null) {
            builder.setAccountOfLiveHash(accountId.toProtobuf());
        }
        builder.setLiveHashToDelete(ByteString.copyFrom(hash));

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (accountId != null) {
            accountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return CryptoServiceGrpc.getDeleteLiveHashMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setCryptoDeleteLiveHash(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException("Cannot schedule LiveHashDeleteTransaction");
    }
}
