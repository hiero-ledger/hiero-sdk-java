// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.time.Instant;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.FreezeServiceGrpc;
import org.hiero.sdk.proto.FreezeTransactionBody;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * A transaction body for all five freeze transactions.
 *
 * Combining five different transactions into a single message, this
 * transaction body MUST support options to schedule a freeze, abort a
 * scheduled freeze, prepare a software upgrade, prepare a telemetry
 * upgrade, or initiate a software upgrade.
 *
 * For a scheduled freeze, at the scheduled time, according to
 * network consensus time
 *   - A freeze (`FREEZE_ONLY`) causes the network nodes to stop creating
 *     events or accepting transactions, and enter a persistent
 *     maintenance state.
 *   - A freeze upgrade (`FREEZE_UPGRADE`) causes the network nodes to stop
 *     creating events or accepting transactions, and upgrade the node software
 *     from a previously prepared upgrade package. The network nodes then
 *     restart and rejoin the network after upgrading.
 *
 * For other freeze types, immediately upon processing the freeze transaction
 *   - A Freeze Abort (`FREEZE_ABORT`) cancels any pending scheduled freeze.
 *   - A prepare upgrade (`PREPARE_UPGRADE`) begins to extract the contents of
 *     the specified upgrade file to the local filesystem.
 *   - A telemetry upgrade (`TELEMETRY_UPGRADE`) causes the network nodes to
 *     extract a telemetry upgrade package to the local filesystem and signal
 *     other software on the machine to upgrade, without impacting the node or
 *     network processing.
 *
 * ### Block Stream Effects
 * Unknown
 */
public final class FreezeTransaction extends Transaction<FreezeTransaction> {
    private int endHour = 0;
    private int endMinute = 0;

    @Nullable
    private Instant startTime = null;

    @Nullable
    private FileId fileId = null;

    private byte[] fileHash = {};
    private FreezeType freezeType = FreezeType.UNKNOWN_FREEZE_TYPE;

    /**
     * Constructor.
     */
    public FreezeTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    FreezeTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    FreezeTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the start time.
     *
     * @return                          the start time
     */
    public Instant getStartTime() {
        return startTime != null ? startTime : Instant.EPOCH;
    }

    /**
     * A start time for the freeze.
     * <p>
     * If this field is REQUIRED for the specified `freeze_type`, then
     * when the network consensus time reaches this instant<ol>
     *   <li>The network SHALL stop accepting transactions.</li>
     *   <li>The network SHALL gossip a freeze state.</li>
     *   <li>The nodes SHALL, in coordinated order, disconnect and
     *       shut down.</li>
     *   <li>The nodes SHALL halt or perform a software upgrade, depending
     *       on `freeze_type`.</li>
     *   <li>If the `freeze_type` is `FREEZE_UPGRADE`, the nodes SHALL
     *       restart and rejoin the network upon completion of the
     *       software upgrade.</li>
     * </ol>
     * <blockquote>
     * If the `freeze_type` is `TELEMETRY_UPGRADE`, the start time is required,
     * but the network SHALL NOT stop, halt, or interrupt transaction
     * processing. The required field is an historical anomaly and SHOULD
     * change in a future release.</blockquote>
     *
     * @param startTime                 the start time
     * @return {@code this}
     */
    public FreezeTransaction setStartTime(Instant startTime) {
        requireNotFrozen();
        Objects.requireNonNull(startTime);
        this.startTime = startTime;
        return this;
    }

    /**
     * @deprecated Use {@link #setStartTime(Instant)} instead.
     * @param hour   The hour to be set
     * @param minute The minute to be set
     * @return {@code this}
     */
    @Deprecated
    public FreezeTransaction setStartTime(int hour, int minute) {
        return setStartTime(Instant.ofEpochMilli(((long) hour * 60 * 60 + (long) minute * 60) * 1000));
    }

    /**
     * @deprecated with no replacement
     * @return the end time
     */
    @Deprecated
    @SuppressWarnings("FromTemporalAccessor")
    public Instant getEndTime() {
        return Instant.from(OffsetTime.of(endHour, endMinute, 0, 0, ZoneOffset.UTC));
    }

    /**
     * Sets the end time (in UTC).
     *
     * @deprecated with no replacement
     * @param hour   The hour to be set
     * @param minute The minute to be set
     * @return {@code this}
     */
    @Deprecated
    public FreezeTransaction setEndTime(int hour, int minute) {
        requireNotFrozen();

        endHour = hour;
        endMinute = minute;

        return this;
    }

    /**
     * @deprecated Use {@link #getFileId()} instead.
     * @return the fileId
     */
    @Deprecated
    @Nullable
    public FileId getUpdateFileId() {
        return fileId;
    }

    /**
     * @deprecated Use {@link #setFileId(FileId)} instead.
     * @param updateFileId the new fileId
     * @return {@code this}
     */
    @Deprecated
    public FreezeTransaction setUpdateFileId(FileId updateFileId) {
        return setFileId(updateFileId);
    }

    /**
     * @deprecated Use {@link #getFileHash()} instead.
     * @return the fileHash
     */
    @Deprecated
    public byte[] getUpdateFileHash() {
        return Arrays.copyOf(fileHash, fileHash.length);
    }

    /**
     * @deprecated Use {@link #setFileHash(byte[])} instead.
     * @param updateFileHash fileHash to set
     * @return {@code this}
     */
    @Deprecated
    public FreezeTransaction setUpdateFileHash(byte[] updateFileHash) {
        return setFileHash(updateFileHash);
    }

    /**
     * Extract the file id.
     *
     * @return                          the file id
     */
    @Nullable
    public FileId getFileId() {
        return fileId;
    }

    /**
     * Assign the file id.
     *
     * @param fileId                    the file id
     * @return {@code this}
     */
    public FreezeTransaction setFileId(FileId fileId) {
        requireNotFrozen();
        Objects.requireNonNull(fileId);
        this.fileId = fileId;
        return this;
    }

    /**
     * The expected hash of the contents of the update file (used to verify the update)
     *
     * @return                          the file's hash
     */
    public byte[] getFileHash() {
        return Arrays.copyOf(fileHash, fileHash.length);
    }

    /**
     * A SHA384 hash of file content.<br/>
     * This is a hash of the file identified by `update_file`.
     * <p>
     * This MUST be set if `update_file` is set, and MUST match the
     * SHA384 hash of the contents of that file.
     *
     * @param fileHash the fileHash to set
     * @return {@code this}
     */
    public FreezeTransaction setFileHash(byte[] fileHash) {
        requireNotFrozen();
        Objects.requireNonNull(fileHash);
        this.fileHash = Arrays.copyOf(fileHash, fileHash.length);
        return this;
    }

    /**
     * Extract the freeze type.
     *
     * @return                          the freeze type
     */
    public FreezeType getFreezeType() {
        return freezeType;
    }

    /**
     * The type of freeze.
     * <p>
     * This REQUIRED field effectively selects between five quite different
     * transactions in the same transaction body. Depending on this value
     * the service may schedule a freeze, prepare upgrades, perform upgrades,
     * or even abort a previously scheduled freeze.
     *
     * {@link org.hiero.sdk.FreezeTransaction}
     *
     * @param freezeType                the freeze type
     * @return {@code this}
     */
    public FreezeTransaction setFreezeType(FreezeType freezeType) {
        requireNotFrozen();
        Objects.requireNonNull(freezeType);
        this.freezeType = freezeType;
        return this;
    }

    @Override
    void validateChecksums(Client client) {}

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return FreezeServiceGrpc.getFreezeMethod();
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getFreeze();
        freezeType = FreezeType.valueOf(body.getFreezeType());
        if (body.hasUpdateFile()) {
            fileId = FileId.fromProtobuf(body.getUpdateFile());
        }
        fileHash = body.getFileHash().toByteArray();
        if (body.hasStartTime()) {
            startTime = InstantConverter.fromProtobuf(body.getStartTime());
        }
    }

    /**
     * Build the correct transaction body.
     *
     * @return {@link org.hiero.sdk.proto.FreezeTransactionBody builder }
     */
    FreezeTransactionBody.Builder build() {
        var builder = FreezeTransactionBody.newBuilder();
        builder.setFreezeType(freezeType.code);
        if (fileId != null) {
            builder.setUpdateFile(fileId.toProtobuf());
        }
        builder.setFileHash(ByteString.copyFrom(fileHash));
        if (startTime != null) {
            builder.setStartTime(InstantConverter.toProtobuf(startTime));
        }
        return builder;
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setFreeze(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setFreeze(build());
    }
}
