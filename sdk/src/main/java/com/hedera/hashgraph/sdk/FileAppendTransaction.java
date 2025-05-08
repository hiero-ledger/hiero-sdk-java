// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.FileAppendTransactionBody;
import com.hedera.hashgraph.sdk.proto.FileServiceGrpc;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionID;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A transaction body for an `appendContent` transaction.<br/>
 * This transaction body provides a mechanism to append content to a "file" in
 * network state. Hedera transactions are limited in size, but there are many
 * uses for in-state byte arrays (e.g. smart contract bytecode) which require
 * more than may fit within a single transaction. The `appendFile` transaction
 * exists to support these requirements. The typical pattern is to create a
 * file, append more data until the full content is stored, verify the file is
 * correct, then update the file entry with any final metadata changes (e.g.
 * adding threshold keys and removing the initial upload key).
 *
 * Each append transaction MUST remain within the total transaction size limit
 * for the network (typically 6144 bytes).<br/>
 * The total size of a file MUST remain within the maximum file size limit for
 * the network (typically 1048576 bytes).
 *
 * #### Signature Requirements
 * Append transactions MUST have signatures from _all_ keys in the `KeyList`
 * assigned to the `keys` field of the file.<br/>
 * See the [File Service](#FileService) specification for a detailed
 * explanation of the signature requirements for all file transactions.
 *
 * ### Block Stream Effects
 * None
 */
public final class FileAppendTransaction extends ChunkedTransaction<FileAppendTransaction> {
    static int DEFAULT_CHUNK_SIZE = 4096;

    @Nullable
    private FileId fileId = null;

    /**
     * Constructor.
     */
    public FileAppendTransaction() {
        super();

        defaultMaxTransactionFee = new Hbar(5);
        setChunkSize(2048);
    }

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    FileAppendTransaction(
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
    FileAppendTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
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
     * This identifies the file to which the `contents` will be appended.
     * <p>
     * This field is REQUIRED.<br/>
     * The identified file MUST exist.<br/>
     * The identified file MUST NOT be larger than the current maximum file
     * size limit.<br/>
     * The identified file MUST NOT be deleted.<br/>
     * The identified file MUST NOT be immutable.
     *
     * @param fileId the ID of the file to append to.
     * @return {@code this}
     */
    public FileAppendTransaction setFileId(FileId fileId) {
        Objects.requireNonNull(fileId);
        requireNotFrozen();
        this.fileId = fileId;
        return this;
    }

    /**
     * Extract the byte string representing the file.
     *
     * @return                          the byte string representing the file
     */
    @Nullable
    public ByteString getContents() {
        return getData();
    }

    /**
     * An array of bytes to append.<br/>
     * <p>
     * This content SHALL be appended to the identified file if this
     * transaction succeeds.<br/>
     * This field is REQUIRED.<br/>
     * This field MUST NOT be empty.
     *
     * @param contents the contents to append to the file.
     * @return {@code this}
     * @see #setContents(String) for an overload which takes String.
     */
    public FileAppendTransaction setContents(byte[] contents) {
        return setData(contents);
    }

    /**
     * <p>Set the contents to append to the file as identified by {@link #setFileId(FileId)}.
     *
     * @param contents the contents to append to the file.
     * @return {@code this}
     * @see #setContents(String) for an overload which takes String.
     */
    public FileAppendTransaction setContents(ByteString contents) {
        return setData(contents);
    }

    /**
     * <p>Encode the given {@link String} as UTF-8 and append it to file as identified by
     * {@link #setFileId(FileId)}.
     *
     * <p>If the whole file is UTF-8 encoded, the string can later be recovered from
     * {@link FileContentsQuery#execute(Client)} via
     * {@link String#String(byte[], java.nio.charset.Charset)} using
     * {@link java.nio.charset.StandardCharsets#UTF_8}.
     *
     * @param text The String to be set as the contents of the file
     * @return {@code this}
     * @see #setContents(byte[]) for appending arbitrary data.
     */
    public FileAppendTransaction setContents(String text) {
        return setData(text);
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (fileId != null) {
            fileId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return FileServiceGrpc.getAppendContentMethod();
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getFileAppend();
        if (body.hasFileID()) {
            fileId = FileId.fromProtobuf(body.getFileID());
        }

        if (!innerSignedTransactions.isEmpty()) {
            try {
                for (var i = 0;
                        i < innerSignedTransactions.size();
                        i += nodeAccountIds.isEmpty() ? 1 : nodeAccountIds.size()) {
                    data = data.concat(TransactionBody.parseFrom(
                                    innerSignedTransactions.get(i).getBodyBytes())
                            .getFileAppend()
                            .getContents());
                }
            } catch (InvalidProtocolBufferException exc) {
                throw new IllegalArgumentException(exc.getMessage());
            }
        } else {
            data = body.getContents();
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link com.hedera.hashgraph.sdk.proto.FileAppendTransactionBody builder}
     */
    FileAppendTransactionBody.Builder build() {
        var builder = FileAppendTransactionBody.newBuilder();
        if (fileId != null) {
            builder.setFileID(fileId.toProtobuf());
        }
        builder.setContents(data);

        return builder;
    }

    @Override
    void onFreezeChunk(
            TransactionBody.Builder body,
            @Nullable TransactionID initialTransactionId,
            int startIndex,
            int endIndex,
            int chunk,
            int total) {
        body.setFileAppend(build().setContents(data.substring(startIndex, endIndex)));
    }

    @Override
    boolean shouldGetReceipt() {
        return true;
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setFileAppend(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setFileAppend(build().setContents(data));
    }

    public ArrayList<Object> bodySizeAllChunks() {
        var list = new ArrayList<>();

        int originalIndex = this.transactionIds.getIndex();

        try {
            // Calculate size for each chunk
            for (int i = 0; i < getRequiredChunks(); i++) {
                this.transactionIds.setIndex(i);
                list.add(super.getTransactionBodySize());
            }
        } finally {
            this.transactionIds.setIndex(originalIndex);
        }

        return list;
    }
}
