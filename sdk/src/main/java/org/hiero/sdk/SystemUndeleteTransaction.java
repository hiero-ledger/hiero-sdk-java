// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.FileServiceGrpc;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.SmartContractServiceGrpc;
import org.hiero.sdk.proto.SystemUndeleteTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Recover a file or contract bytecode deleted from the Hedera File
 * System (HFS) by a `systemDelete` transaction.
 *
 * > Note
 * >> A system delete/undelete for a `contractID` is not supported and
 * >> SHALL return `INVALID_FILE_ID` or `MISSING_ENTITY_ID`.
 *
 * This transaction can _only_ recover a file removed with the `systemDelete`
 * transaction. A file deleted via `fileDelete` SHALL be irrecoverable.<br/>
 * This transaction MUST be signed by an Hedera administrative ("system")
 * account.
 *
 * ### What is a "system" file
 * A "system" file is any file with a file number less than or equal to the
 * current configuration value for `ledger.numReservedSystemEntities`,
 * typically `750`.
 *
 * ### Block Stream Effects
 * None
 */
public final class SystemUndeleteTransaction extends Transaction<SystemUndeleteTransaction> {
    @Nullable
    private FileId fileId;

    @Nullable
    private ContractId contractId;

    /**
     * Constructor.
     */
    public SystemUndeleteTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    SystemUndeleteTransaction(
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
    SystemUndeleteTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
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
     * A file identifier.
     * <p>
     * The identified file MUST exist in the HFS.<br/>
     * The identified file MUST be deleted.<br/>
     * The identified file deletion MUST be a result of a
     * `systemDelete` transaction.<br/>
     * The identified file MUST NOT be a "system" file.<br/>
     * This field is REQUIRED.
     *
     * Mutually exclusive with {@link #setContractId(ContractId)}.
     *
     * @param fileId The FileId to be set
     * @return {@code this}
     */
    public SystemUndeleteTransaction setFileId(FileId fileId) {
        Objects.requireNonNull(fileId);
        requireNotFrozen();
        this.fileId = fileId;
        return this;
    }

    /**
     * The contract ID instance to undelete, in the format used in transactions
     *
     * @return the contractId
     */
    @Nullable
    public ContractId getContractId() {
        return contractId;
    }

    /**
     * A contract identifier.
     * <p>
     * The identified contract MUST exist in network state.<br/>
     * The identified contract bytecode MUST be deleted.<br/>
     * The identified contract deletion MUST be a result of a
     * `systemDelete` transaction.
     * <p>
     * @param contractId The ContractId to be set
     * @return {@code this}
     */
    public SystemUndeleteTransaction setContractId(ContractId contractId) {
        Objects.requireNonNull(contractId);
        requireNotFrozen();
        this.contractId = contractId;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getSystemUndelete();
        if (body.hasFileID()) {
            fileId = FileId.fromProtobuf(body.getFileID());
        }
        if (body.hasContractID()) {
            contractId = ContractId.fromProtobuf(body.getContractID());
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link org.hiero.sdk.proto.SystemUndeleteTransactionBody}
     */
    SystemUndeleteTransactionBody.Builder build() {
        var builder = SystemUndeleteTransactionBody.newBuilder();
        if (fileId != null) {
            builder.setFileID(fileId.toProtobuf());
        }
        if (contractId != null) {
            builder.setContractID(contractId.toProtobuf());
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (fileId != null) {
            fileId.validateChecksum(client);
        }

        if (contractId != null) {
            contractId.validateChecksum(client);
        }
    }

    @Override
    CompletableFuture<Void> onExecuteAsync(Client client) {
        int modesEnabled = (fileId != null ? 1 : 0) + (contractId != null ? 1 : 0);
        if (modesEnabled != 1) {
            throw new IllegalStateException(
                    "SystemDeleteTransaction must have exactly 1 of the following fields set: contractId, fileId");
        }
        return super.onExecuteAsync(client);
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        if (fileId != null) {
            return FileServiceGrpc.getSystemUndeleteMethod();
        } else {
            return SmartContractServiceGrpc.getSystemUndeleteMethod();
        }
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setSystemUndelete(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setSystemUndelete(build());
    }
}
