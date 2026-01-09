// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.EthereumTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.SmartContractServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A transaction in Ethereum format.<br/>
 * Make an Ethereum transaction "call" with all data in Ethereum formats,
 * including the contract alias. Call data may be in the transaction, or
 * stored within an Hedera File.
 *
 * The caller MAY offer additional gas above what is offered in the call data,
 * but MAY be charged up to 80% of that value if the amount required is less
 * than this "floor" amount.
 *
 * ### Block Stream Effects
 * An `EthereumOutput` message SHALL be emitted for each transaction.
 */
public class EthereumTransaction extends Transaction<EthereumTransaction> {
    private byte[] ethereumData = new byte[0];
    private FileId callDataFileId = null;
    private Hbar maxGasAllowanceHbar = Hbar.ZERO;

    /**
     * Constructor
     */
    public EthereumTransaction() {}

    EthereumTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    EthereumTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Gets the raw Ethereum transaction
     *
     * @return the raw Ethereum transaction
     */
    public byte[] getEthereumData() {
        return Arrays.copyOf(ethereumData, ethereumData.length);
    }

    /**
     * Sets the raw Ethereum transaction (RLP encoded type 0, 1, 2 and 4). Complete
     * unless the callDataFileId is set.
     *
     * @param ethereumData raw ethereum transaction bytes
     * @return {@code this}
     */
    public EthereumTransaction setEthereumData(byte[] ethereumData) {
        Objects.requireNonNull(ethereumData);
        requireNotFrozen();
        this.ethereumData = Arrays.copyOf(ethereumData, ethereumData.length);
        return this;
    }

    /**
     * Gets the FileId of the call data
     *
     * @return the FileId of the call data
     */
    @Nullable
    public FileId getCallDataFileId() {
        return callDataFileId;
    }

    /**
     * For large transactions (for example contract create) this should be used to
     * set the FileId of an HFS file containing the callData
     * of the ethereumData. The data in the ethereumData will be re-written with
     * the callData element as a zero length string with the original contents in
     * the referenced file at time of execution. The ethereumData will need to be
     * "rehydrated" with the callData for signature validation to pass.
     *
     * @param fileId File ID of an HFS file containing the callData
     * @return {@code this}
     */
    public EthereumTransaction setCallDataFileId(FileId fileId) {
        Objects.requireNonNull(fileId);
        requireNotFrozen();
        callDataFileId = fileId;
        return this;
    }

    /**
     * Gets the maximum amount that the payer of the hedera transaction
     * is willing to pay to complete the transaction.
     *
     * @return the max gas allowance
     */
    public Hbar getMaxGasAllowanceHbar() {
        return maxGasAllowanceHbar;
    }

    /**
     * Sets the maximum amount that the payer of the hedera transaction
     * is willing to pay to complete the transaction.
     * <br>
     * Ordinarily the account with the ECDSA alias corresponding to the public
     * key that is extracted from the ethereum_data signature is responsible for
     * fees that result from the execution of the transaction. If that amount of
     * authorized fees is not sufficient then the payer of the transaction can be
     * charged, up to but not exceeding this amount. If the ethereum_data
     * transaction authorized an amount that was insufficient then the payer will
     * only be charged the amount needed to make up the difference. If the gas
     * price in the transaction was set to zero then the payer will be assessed
     * the entire fee.
     *
     * @param maxGasAllowanceHbar the maximum gas allowance
     * @return {@code this}
     */
    public EthereumTransaction setMaxGasAllowanceHbar(Hbar maxGasAllowanceHbar) {
        Objects.requireNonNull(maxGasAllowanceHbar);
        requireNotFrozen();
        this.maxGasAllowanceHbar = maxGasAllowanceHbar;
        return this;
    }

    private void initFromTransactionBody() {
        var body = sourceTransactionBody.getEthereumTransaction();

        ethereumData = body.getEthereumData().toByteArray();
        if (body.hasCallData()) {
            callDataFileId = FileId.fromProtobuf(body.getCallData());
        }
        maxGasAllowanceHbar = Hbar.fromTinybars(body.getMaxGasAllowance());
    }

    private EthereumTransactionBody.Builder build() {
        var builder = EthereumTransactionBody.newBuilder()
                .setEthereumData(ByteString.copyFrom(ethereumData))
                .setMaxGasAllowance(maxGasAllowanceHbar.toTinybars());
        if (callDataFileId != null) {
            builder.setCallData(callDataFileId.toProtobuf());
        }
        return builder;
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return SmartContractServiceGrpc.getCallEthereumMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setEthereumTransaction(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        throw new UnsupportedOperationException("Cannot schedule EthereumTransaction");
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (callDataFileId != null) {
            callDataFileId.validateChecksum(client);
        }
    }
}
