// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.FileDeleteTransactionBody;
import org.hiero.sdk.proto.FileServiceGrpc;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * <p>A transaction to delete a file on the Hedera network.
 *
 * <p>When deleted, a file's contents are truncated to zero length and it can no longer be updated
 * or appended to, or its expiration time extended. {@link FileContentsQuery} and {@link FileInfoQuery}
 * will throw {@link PrecheckStatusException} with a status of {@link Status#FILE_DELETED}.
 *
 * <p>Only one of the file's keys needs to sign to delete the file, unless the key you have is part
 * of a {@link org.hiero.sdk.KeyList}.
 */
public final class FileDeleteTransaction extends Transaction<FileDeleteTransaction> {

    @Nullable
    private FileId fileId = null;

    /**
     * Constructor.
     */
    public FileDeleteTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    FileDeleteTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    FileDeleteTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
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
     * A file identifier.<br/>
     * This identifies the file to delete.
     * <p>
     * The identified file MUST NOT be a "system" file.<br/>
     * This field is REQUIRED.
     *
     * @param fileId the ID of the file to delete.
     * @return {@code this}
     */
    public FileDeleteTransaction setFileId(FileId fileId) {
        Objects.requireNonNull(fileId);
        requireNotFrozen();
        this.fileId = fileId;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getFileDelete();
        if (body.hasFileID()) {
            fileId = FileId.fromProtobuf(body.getFileID());
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link org.hiero.sdk.proto.FileDeleteTransactionBody builder}
     */
    FileDeleteTransactionBody.Builder build() {
        var builder = FileDeleteTransactionBody.newBuilder();
        if (fileId != null) {
            builder.setFileID(fileId.toProtobuf());
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (fileId != null) {
            fileId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return FileServiceGrpc.getDeleteFileMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setFileDelete(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setFileDelete(build());
    }
}
