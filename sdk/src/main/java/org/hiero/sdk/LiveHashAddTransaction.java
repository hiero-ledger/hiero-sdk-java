// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.CryptoAddLiveHashTransactionBody;
import org.hiero.sdk.proto.CryptoServiceGrpc;
import org.hiero.sdk.proto.LiveHash;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * A Live Hash value associating some item of content to an account.
 *
 * This message represents a desired entry in the ledger for a SHA-384
 * hash of some content, an associated specific account, a list of authorized
 * keys, and a duration the live hash is "valid".
 */
public final class LiveHashAddTransaction extends Transaction<LiveHashAddTransaction> {
    @Nullable
    private AccountId accountId = null;

    private byte[] hash = {};

    @Nullable
    private KeyList keys = null;

    @Nullable
    private Duration duration = null;

    /**
     * Constructor.
     */
    public LiveHashAddTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    LiveHashAddTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
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
     * The account to which the livehash is attached
     *
     * @param accountId The AccountId to be set
     * @return {@code this}
     */
    public LiveHashAddTransaction setAccountId(AccountId accountId) {
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
     * The SHA-384 hash of a credential or certificate.
     *
     * @param hash The array of bytes to be set as the hash
     * @return {@code this}
     */
    public LiveHashAddTransaction setHash(byte[] hash) {
        requireNotFrozen();
        Objects.requireNonNull(hash);
        this.hash = Arrays.copyOf(hash, hash.length);
        return this;
    }

    /**
     * The SHA-384 hash of a credential or certificate.
     *
     * @param hash The array of bytes to be set as the hash
     * @return {@code this}
     */
    public LiveHashAddTransaction setHash(ByteString hash) {
        Objects.requireNonNull(hash);
        return setHash(hash.toByteArray());
    }

    /**
     * Extract the key / key list.
     *
     * @return                          the key / key list
     */
    @Nullable
    public Collection<Key> getKeys() {
        return keys != null ? Collections.unmodifiableCollection(keys) : null;
    }

    /**
     * A list of keys (primitive or threshold), all of which must sign to attach the livehash to an
     * account, and any one of which can later delete it.
     *
     * @param keys The Key or Keys to be set
     * @return {@code this}
     */
    public LiveHashAddTransaction setKeys(Key... keys) {
        requireNotFrozen();

        this.keys = KeyList.of(keys);

        return this;
    }

    /**
     * Extract the duration.
     *
     * @return                          the duration
     */
    @Nullable
    public Duration getDuration() {
        return duration;
    }

    /**
     * The duration for which the livehash will remain valid
     *
     * @param duration The Duration to be set
     * @return {@code this}
     */
    public LiveHashAddTransaction setDuration(Duration duration) {
        requireNotFrozen();
        Objects.requireNonNull(duration);
        this.duration = duration;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getCryptoAddLiveHash();
        var hashBody = body.getLiveHash();

        if (hashBody.hasAccountId()) {
            accountId = AccountId.fromProtobuf(hashBody.getAccountId());
        }
        hash = hashBody.getHash().toByteArray();
        if (hashBody.hasKeys()) {
            keys = KeyList.fromProtobuf(hashBody.getKeys(), null);
        }
        if (hashBody.hasDuration()) {
            duration = DurationConverter.fromProtobuf(hashBody.getDuration());
        }
    }

    /**
     * Build the correct transaction body.
     *
     * @return {@link org.hiero.sdk.proto.CryptoAddLiveHashTransactionBody}
     */
    CryptoAddLiveHashTransactionBody.Builder build() {
        var builder = CryptoAddLiveHashTransactionBody.newBuilder();
        var hashBuilder = LiveHash.newBuilder();
        if (accountId != null) {
            hashBuilder.setAccountId(accountId.toProtobuf());
        }
        hashBuilder.setHash(ByteString.copyFrom(hash));
        if (keys != null) {
            hashBuilder.setKeys(keys.toProtobuf());
        }
        if (duration != null) {
            hashBuilder.setDuration(DurationConverter.toProtobuf(duration));
        }

        return builder.setLiveHash(hashBuilder);
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (accountId != null) {
            accountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return CryptoServiceGrpc.getAddLiveHashMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setCryptoAddLiveHash(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException("Cannot schedule LiveHashAddTransaction");
    }
}
