// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.FileCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.FileServiceGrpc;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Create a new file.
 *
 * If successful, the new file SHALL contain the (possibly empty) content
 * provided in the `contents` field.<br/>
 * When the current consensus time exceeds the `expirationTime` value, the
 * network SHALL expire the file, and MAY archive the state entry.
 *
 * #### Signature Requirements
 * The HFS manages file authorization in a manner that can be confusing.
 * The core element of file authorization is the `keys` field,
 * which is a `KeyList`; a list of individual `Key` messages, each of which
 * may represent a simple or complex key.<br/>
 * The file service transactions treat this list differently.<br/>
 * A `fileCreate`, `fileAppend`, or `fileUpdate` MUST have a valid signature
 * from _each_ key in the list.<br/>
 * A `fileDelete` MUST have a valid signature from _at least one_ key in
 * the list. This is different, and allows a file "owned" by many entities
 * to be deleted by any one of those entities. A deleted file cannot be
 * restored, so it is important to consider this when assigning keys for
 * a file.<br/>
 * If any of the keys in a `KeyList` are complex, the full requirements of
 * each complex key must be met to count as a "valid signature" for that key.
 * A complex key structure (i.e. a `ThresholdKey`, or `KeyList`, possibly
 * including additional `ThresholdKey` or `KeyList` descendants) may be
 * assigned as the sole entry in a file `keys` field to ensure all transactions
 * have the same signature requirements.
 *
 * If the `keys` field is an empty `KeyList`, then the file SHALL be immutable
 * and the only transaction permitted to modify that file SHALL be a
 * `fileUpdate` transaction with _only_ the `expirationTime` set.
 *
 * #### Shard and Realm
 * The current API ignores shardID and realmID. All files are created in
 * shard 0 and realm 0. Future versions of the API may support multiple
 * realms and multiple shards.
 *
 * ### Block Stream Effects
 * After the file is created, the FileID for it SHALL be returned in the
 * transaction receipt, and SHALL be recorded in the transaction record.
 */
public final class FileCreateTransaction extends Transaction<FileCreateTransaction> {

    @Nullable
    private Instant expirationTime = null;

    private Duration expirationTimeDuration = null;

    @Nullable
    private KeyList keys = null;

    private byte[] contents = {};
    private String fileMemo = "";

    /**
     * Constructor.
     */
    public FileCreateTransaction() {
        setExpirationTime(Instant.now().plus(DEFAULT_AUTO_RENEW_PERIOD));
        defaultMaxTransactionFee = new Hbar(5);
    }

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    FileCreateTransaction(
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
    FileCreateTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the time.
     *
     * @return                          expiration time
     */
    @Nullable
    public Instant getExpirationTime() {
        return expirationTime;
    }

    /**
     * <p>Set the instant at which this file will expire, after which its contents will no longer be
     * available.
     *
     * <p>Defaults to 1/4 of a Julian year from the instant {@link #FileCreateTransaction()}
     * was invoked.
     *
     * <p>May be extended using {@link FileUpdateTransaction#setExpirationTime(Instant)}.
     *
     * @param expirationTime the {@link Instant} at which this file should expire.
     * @return {@code this}
     */
    public FileCreateTransaction setExpirationTime(Instant expirationTime) {
        requireNotFrozen();
        Objects.requireNonNull(expirationTime);
        this.expirationTime = expirationTime;
        return this;
    }

    public FileCreateTransaction setExpirationTime(Duration expirationTime) {
        Objects.requireNonNull(expirationTime);
        requireNotFrozen();
        this.expirationTime = null;
        this.expirationTimeDuration = expirationTime;
        return this;
    }

    /**
     * Extract the of keys.
     *
     * @return                          list of keys
     */
    @Nullable
    public Collection<Key> getKeys() {
        return keys != null ? Collections.unmodifiableCollection(keys) : null;
    }

    /**
     * <p>Set the keys which must sign any transactions modifying this file. Required.
     *
     * <p>All keys must sign to modify the file's contents or keys. No key is required
     * to sign for extending the expiration time (except the one for the operator account
     * paying for the transaction). Only one key must sign to delete the file, however.
     *
     * <p>To require more than one key to sign to delete a file, add them to a
     * {@link com.hedera.hashgraph.sdk.KeyList} and pass that here.
     *
     * <p>The network currently requires a file to have at least one key (or key list or threshold key)
     * but this requirement may be lifted in the future.
     *
     * @param keys The Key or Keys to be set
     * @return {@code this}
     */
    public FileCreateTransaction setKeys(Key... keys) {
        requireNotFrozen();
        this.keys = KeyList.of(keys);
        return this;
    }

    /**
     * Create the byte string.
     *
     * @return                          byte string representation
     */
    public ByteString getContents() {
        return ByteString.copyFrom(contents);
    }

    /**
     * <p>Set the given byte array as the file's contents.
     *
     * <p>This may be omitted to create an empty file.
     *
     * <p>Note that total size for a given transaction is limited to 6KiB (as of March 2020) by the
     * network; if you exceed this you may receive a {@link PrecheckStatusException}
     * with {@link com.hedera.hashgraph.sdk.Status#TRANSACTION_OVERSIZE}.
     *
     * <p>In this case, you can use {@link FileAppendTransaction}, which automatically breaks the contents
     * into chunks for you, to append contents of arbitrary size.
     *
     * @param bytes the contents of the file.
     * @return {@code this}
     */
    public FileCreateTransaction setContents(byte[] bytes) {
        requireNotFrozen();
        Objects.requireNonNull(bytes);
        contents = Arrays.copyOf(bytes, bytes.length);
        return this;
    }

    /**
     * <p>Encode the given {@link String} as UTF-8 and set it as the file's contents.
     *
     * <p>This may be omitted to create an empty file.
     *
     * <p>The string can later be recovered from {@link FileContentsQuery#execute(Client)}
     * via {@link String#String(byte[], java.nio.charset.Charset)} using
     * {@link java.nio.charset.StandardCharsets#UTF_8}.
     *
     * <p>Note that total size for a given transaction is limited to 6KiB (as of March 2020) by the
     * network; if you exceed this you may receive a {@link PrecheckStatusException}
     * with {@link com.hedera.hashgraph.sdk.Status#TRANSACTION_OVERSIZE}.
     *
     * <p>In this case, you can use {@link FileAppendTransaction}, which automatically breaks the contents
     * into chunks for you, to append contents of arbitrary size.
     *
     * @param text the contents of the file.
     * @return {@code this}
     */
    public FileCreateTransaction setContents(String text) {
        requireNotFrozen();
        Objects.requireNonNull(text);
        contents = text.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    /**
     * Extract the file's memo field.
     *
     * @return                          the file's memo field
     */
    public String getFileMemo() {
        return fileMemo;
    }

    /**
     * Assign a memo to the file (100 bytes max).
     *
     * @param memo                      memo string
     * @return {@code this}
     */
    public FileCreateTransaction setFileMemo(String memo) {
        requireNotFrozen();
        Objects.requireNonNull(memo);
        fileMemo = memo;
        return this;
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return FileServiceGrpc.getCreateFileMethod();
    }

    @Override
    void validateChecksums(Client client) {
        // do nothing
    }

    /**
     * Initialize from transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getFileCreate();
        if (body.hasExpirationTime()) {
            expirationTime = InstantConverter.fromProtobuf(body.getExpirationTime());
        }
        if (body.hasKeys()) {
            keys = KeyList.fromProtobuf(body.getKeys(), null);
        }
        contents = body.getContents().toByteArray();
        fileMemo = body.getMemo();
    }

    /**
     * Build the transaction body.
     *
     * @return {@link com.hedera.hashgraph.sdk.proto.FileCreateTransactionBody builder}
     */
    FileCreateTransactionBody.Builder build() {
        var builder = FileCreateTransactionBody.newBuilder();

        if (expirationTime != null) {
            builder.setExpirationTime(InstantConverter.toProtobuf(expirationTime));
        }
        if (expirationTimeDuration != null) {
            builder.setExpirationTime(InstantConverter.toProtobuf(expirationTimeDuration));
        }
        if (keys != null) {
            builder.setKeys(keys.toProtobuf());
        }
        builder.setContents(ByteString.copyFrom(contents));
        builder.setMemo(fileMemo);

        return builder;
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setFileCreate(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setFileCreate(build());
    }
}
