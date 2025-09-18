// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ContractDeleteTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.SmartContractServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Delete a smart contract, and transfer any remaining HBAR balance to a
 * designated account.
 *
 * If this call succeeds then all subsequent calls to that smart contract
 * SHALL execute the `0x0` opcode, as required for EVM equivalence.
 *
 * ### Requirements
 *  - An account or smart contract MUST be designated to receive all remaining
 *    account balances.
 *  - The smart contract MUST have an admin key set. If the contract does not
 *    have `admin_key` set, then this transaction SHALL fail and response code
 *    `MODIFYING_IMMUTABLE_CONTRACT` SHALL be set.
 *  - If `admin_key` is, or contains, an empty `KeyList` key, it SHALL be
 *    treated the same as an admin key that is not set.
 *  - The `Key` set for `admin_key` on the smart contract MUST have a valid
 *    signature set on this transaction.
 *  - The designated receiving account MAY have `receiver_sig_required` set. If
 *    that field is set, the receiver account MUST also sign this transaction.
 *  - The field `permanent_removal` MUST NOT be set. That field is reserved for
 *    internal system use when purging the smart contract from state. Any user
 *    transaction with that field set SHALL be rejected and a response code
 *    `PERMANENT_REMOVAL_REQUIRES_SYSTEM_INITIATION` SHALL be set.
 *
 * ### Block Stream Effects
 * None
 */
public final class ContractDeleteTransaction extends Transaction<ContractDeleteTransaction> {

    @Nullable
    private ContractId contractId = null;

    @Nullable
    private ContractId transferContractId = null;

    @Nullable
    private AccountId transferAccountId = null;

    @Nullable
    private Boolean permanentRemoval = null;

    /**
     * Constructor.
     */
    public ContractDeleteTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    ContractDeleteTransaction(
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
    ContractDeleteTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the contract id.
     *
     * @return                          the contract id
     */
    @Nullable
    public ContractId getContractId() {
        return contractId;
    }

    /**
     * Sets the contract ID which should be deleted.
     *
     * @param contractId The ContractId to be set
     * @return {@code this}
     */
    public ContractDeleteTransaction setContractId(ContractId contractId) {
        Objects.requireNonNull(contractId);
        requireNotFrozen();
        this.contractId = contractId;
        return this;
    }

    /**
     * Extract the transfer account id.
     *
     * @return                          the account id that will receive the remaining hbars
     */
    @Nullable
    public AccountId getTransferAccountId() {
        return transferAccountId;
    }

    /**
     * Sets the account ID which will receive all remaining hbars.
     * <p>
     * This is mutually exclusive with {@link #setTransferContractId(ContractId)}.
     *
     * @param transferAccountId The AccountId to be set
     * @return {@code this}
     */
    public ContractDeleteTransaction setTransferAccountId(AccountId transferAccountId) {
        Objects.requireNonNull(transferAccountId);
        requireNotFrozen();
        this.transferAccountId = transferAccountId;
        return this;
    }

    /**
     * Extract the transfer contract id.
     *
     * @return                          the contract id that will receive the remaining hbars
     */
    @Nullable
    public ContractId getTransferContractId() {
        return transferContractId;
    }

    /**
     * Sets the contract ID which will receive all remaining hbars.
     * <p>
     * This is mutually exclusive with {@link #setTransferAccountId(AccountId)}.
     *
     * @param transferContractId The ContractId to be set
     * @return {@code this}
     */
    public ContractDeleteTransaction setTransferContractId(ContractId transferContractId) {
        Objects.requireNonNull(transferContractId);
        requireNotFrozen();
        this.transferContractId = transferContractId;
        return this;
    }

    /**
     * Extract the permanent removal flag.
     *
     * @return                          the permanent removal flag
     */
    @Nullable
    public Boolean getPermanentRemoval() {
        return permanentRemoval;
    }

    /**
     * Sets the permanent removal flag.
     * <p>
     * This field is reserved for system use. User transactions setting this field
     * may be rejected by the network with status {@link Status#PERMANENT_REMOVAL_REQUIRES_SYSTEM_INITIATION}.
     *
     * @param permanentRemoval The permanent removal flag to be set
     * @return {@code this}
     */
    public ContractDeleteTransaction setPermanentRemoval(boolean permanentRemoval) {
        requireNotFrozen();
        this.permanentRemoval = permanentRemoval;
        return this;
    }

    /**
     * Validates tha the contract id, transfer contract id and the transfer account id are valid.
     *
     * @param client                    the configured client
     * @throws BadEntityIdException     if entity ID is formatted poorly
     */
    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (contractId != null) {
            contractId.validateChecksum(client);
        }

        if (transferContractId != null) {
            transferContractId.validateChecksum(client);
        }

        if (transferAccountId != null) {
            transferAccountId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return SmartContractServiceGrpc.getDeleteContractMethod();
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getContractDeleteInstance();
        if (body.hasContractID()) {
            contractId = ContractId.fromProtobuf(body.getContractID());
        }
        if (body.hasTransferContractID()) {
            transferContractId = ContractId.fromProtobuf(body.getTransferContractID());
        }
        if (body.hasTransferAccountID()) {
            transferAccountId = AccountId.fromProtobuf(body.getTransferAccountID());
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link ContractDeleteTransactionBody}
     */
    ContractDeleteTransactionBody.Builder build() {
        var builder = ContractDeleteTransactionBody.newBuilder();
        if (contractId != null) {
            builder.setContractID(contractId.toProtobuf());
        }

        if (transferAccountId != null) {
            builder.setTransferAccountID(transferAccountId.toProtobuf());
        }

        if (transferContractId != null) {
            builder.setTransferContractID(transferContractId.toProtobuf());
        }

        if (permanentRemoval != null) {
            builder.setPermanentRemoval(permanentRemoval.booleanValue());
        }

        return builder;
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setContractDeleteInstance(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setContractDeleteInstance(build());
    }
}
