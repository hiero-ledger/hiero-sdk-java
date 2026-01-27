// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ContractCallTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.SmartContractServiceGrpc;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Call a function of the given smart contract instance, giving it parameters as its inputs.
 * <p>
 * It can use the given amount of gas, and any unspent gas will be refunded to the paying account.
 * <p>
 * If this function stores information, it is charged gas to store it.
 * There is a fee in hbars to maintain that storage until the expiration time, and that fee is
 * added as part of the transaction fee.
 */
public final class ContractExecuteTransaction extends Transaction<ContractExecuteTransaction> {

    @Nullable
    private ContractId contractId = null;

    private long gas = 0;
    private Hbar payableAmount = new Hbar(0);
    private byte[] functionParameters = {};

    /**
     * Constructor.
     */
    public ContractExecuteTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    ContractExecuteTransaction(
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
    ContractExecuteTransaction(com.hedera.hashgraph.sdk.proto.TransactionBody txBody) {
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
     * Sets the contract instance to call.
     *
     * @param contractId The ContractId to be set
     * @return {@code this}
     */
    public ContractExecuteTransaction setContractId(ContractId contractId) {
        Objects.requireNonNull(contractId);
        requireNotFrozen();
        this.contractId = contractId;
        return this;
    }

    /**
     * Extract the gas.
     *
     * @return                          the gas
     */
    public long getGas() {
        return gas;
    }

    /**
     * A maximum limit to the amount of gas to use for this call.
     * <p>
     * The network SHALL charge the greater of the following, but
     * SHALL NOT charge more than the value of this field.
     * <ol>
     *   <li>The actual gas consumed by the smart contract call.</li>
     *   <li>`80%` of this value.</li>
     * </ol>
     * The `80%` factor encourages reasonable estimation, while allowing for
     * some overage to ensure successful execution.
     *
     * @param gas The long to be set as gas
     * @return {@code this}
     */
    public ContractExecuteTransaction setGas(long gas) {
        requireNotFrozen();
        if (gas < 0) {
            throw new IllegalArgumentException("Gas must be non-negative");
        }
        this.gas = gas;
        return this;
    }

    /**
     * Extract the payable amount.
     *
     * @return                          the payable amount in hbar
     */
    public Hbar getPayableAmount() {
        return payableAmount;
    }

    /**
     * Sets the number of hbars sent with this function call.
     *
     * @param amount The Hbar to be set
     * @return {@code this}
     */
    public ContractExecuteTransaction setPayableAmount(Hbar amount) {
        Objects.requireNonNull(amount);
        requireNotFrozen();
        payableAmount = amount;
        return this;
    }

    /**
     * Extract the function parameters.
     *
     * @return                          the function parameters
     */
    public ByteString getFunctionParameters() {
        return ByteString.copyFrom(functionParameters);
    }

    /**
     * Sets the function parameters as their raw bytes.
     * <p>
     * Use this instead of {@link #setFunction(String, ContractFunctionParameters)} if you have already
     * pre-encoded a solidity function call.
     *
     * This MUST contain The application binary interface (ABI) encoding of the
     * function call per the Ethereum contract ABI standard, giving the
     * function signature and arguments being passed to the function.
     *
     * @param functionParameters The function parameters to be set
     * @return {@code this}
     */
    public ContractExecuteTransaction setFunctionParameters(ByteString functionParameters) {
        Objects.requireNonNull(functionParameters);
        requireNotFrozen();
        this.functionParameters = functionParameters.toByteArray();
        return this;
    }

    /**
     * Sets the function name to call.
     * <p>
     * The function will be called with no parameters. Use {@link #setFunction(String, ContractFunctionParameters)}
     * to call a function with parameters.
     *
     * @param name The String to be set as the function name
     * @return {@code this}
     */
    public ContractExecuteTransaction setFunction(String name) {
        return setFunction(name, new ContractFunctionParameters());
    }

    /**
     * Sets the function to call, and the parameters to pass to the function.
     *
     * @param name   The String to be set as the function name
     * @param params The function parameters to be set
     * @return {@code this}
     */
    public ContractExecuteTransaction setFunction(String name, ContractFunctionParameters params) {
        Objects.requireNonNull(params);
        return setFunctionParameters(params.toBytes(name));
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getContractCall();
        if (body.hasContractID()) {
            contractId = ContractId.fromProtobuf(body.getContractID());
        }
        gas = body.getGas();
        payableAmount = Hbar.fromTinybars(body.getAmount());
        functionParameters = body.getFunctionParameters().toByteArray();
    }

    /**
     * Build the transaction body.
     *
     * @return {@link ContractCallTransactionBody}
     */
    ContractCallTransactionBody.Builder build() {
        var builder = ContractCallTransactionBody.newBuilder();
        if (contractId != null) {
            builder.setContractID(contractId.toProtobuf());
        }
        builder.setGas(gas);
        builder.setAmount(payableAmount.toTinybars());
        builder.setFunctionParameters(ByteString.copyFrom(functionParameters));

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (contractId != null) {
            contractId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return SmartContractServiceGrpc.getContractCallMethodMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setContractCall(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setContractCall(build());
    }
}
