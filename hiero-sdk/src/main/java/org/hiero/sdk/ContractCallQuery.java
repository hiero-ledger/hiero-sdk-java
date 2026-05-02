// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import io.grpc.MethodDescriptor;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.ContractCallLocalQuery;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseHeader;
import org.hiero.sdk.proto.SmartContractServiceGrpc;

/**
 * Call a function of the given smart contract instance, giving it functionParameters as its inputs.
 * It will consume the entire given amount of gas.
 * <p>
 * This is performed locally on the particular node that the client is communicating with.
 * It cannot change the state of the contract instance (and so, cannot spend
 * anything from the instance's cryptocurrency account).
 * <p>
 * It will not have a consensus timestamp. It cannot generate a record or a receipt.
 * The response will contain the output returned by the function call.
 * This is useful for calling getter functions, which purely read the state and don't change it.
 * It is faster and cheaper than a normal call, because it is purely local to a single  node.
 */
public final class ContractCallQuery extends Query<ContractFunctionResult, ContractCallQuery> {
    @Nullable
    private ContractId contractId = null;

    private long gas = 0;
    private byte[] functionParameters = {};
    private long maxResultSize = 0;

    @Nullable
    private AccountId senderAccountId = null;

    /**
     * Constructor.
     */
    public ContractCallQuery() {}

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
     * Sets the contract instance to call, in the format used in transactions.
     *
     * @param contractId The ContractId to be set
     * @return {@code this}
     */
    public ContractCallQuery setContractId(ContractId contractId) {
        Objects.requireNonNull(contractId);
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
     * Sets the amount of gas to use for the call.
     * <p>
     * All of the gas offered will be charged for.
     *
     * @param gas The long to be set as gas
     * @return {@code this}
     */
    public ContractCallQuery setGas(long gas) {
        this.gas = gas;
        return this;
    }

    @Override
    public CompletableFuture<Hbar> getCostAsync(Client client) {
        // network bug: ContractCallLocal cost estimate is too low
        return super.getCostAsync(client).thenApply(cost -> Hbar.fromTinybars((long) (cost.toTinybars() * 1.1)));
    }

    /**
     * Extract the function parameters.
     *
     * @return                          the byte string representation
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
     * @param functionParameters The function parameters to be set
     * @return {@code this}
     */
    public ContractCallQuery setFunctionParameters(byte[] functionParameters) {
        this.functionParameters = Arrays.copyOf(functionParameters, functionParameters.length);
        return this;
    }

    /**
     * Sets the function name to call.
     * <p>
     * The function will be called with no parameters. Use {@link #setFunction(String, ContractFunctionParameters)}
     * to call a function with parameters.
     *
     * @param name The function name to be set
     * @return {@code this}
     */
    public ContractCallQuery setFunction(String name) {
        return setFunction(name, new ContractFunctionParameters());
    }

    /**
     * Sets the function to call, and the parameters to pass to the function.
     *
     * @param name   The function name to be set
     * @param params The parameters to pass
     * @return {@code this}
     */
    public ContractCallQuery setFunction(String name, ContractFunctionParameters params) {
        Objects.requireNonNull(params);
        setFunctionParameters(params.toBytes(name).toByteArray());
        return this;
    }

    /**
     * Get the max number of bytes that the result might include
     *
     * @deprecated with no replacement
     * @return the max number of byes
     */
    @Deprecated
    public long getMaxResultSize() {
        return maxResultSize;
    }

    /**
     * @deprecated with no replacement
     * <br>
     * Sets the max number of bytes that the result might include.
     * The run will fail if it had returned more than this number of bytes.
     *
     * @param size The long to be set as size
     * @return {@code this}
     */
    @Deprecated
    public ContractCallQuery setMaxResultSize(long size) {
        maxResultSize = size;
        return this;
    }

    /**
     * Get the sender account ID
     * @return the account that is the "sender"
     */
    @Nullable
    public AccountId getSenderAccountId() {
        return senderAccountId;
    }

    /**
     * Sets the account that is the "sender." If not present it is the accountId from the transactionId.
     * Typically a different value than specified in the transactionId requires a valid signature
     * over either the hedera transaction or foreign transaction data.
     *
     * @param senderAccountId the account that is the "sender"
     * @return {@code this}
     */
    public ContractCallQuery setSenderAccountId(AccountId senderAccountId) {
        Objects.requireNonNull(senderAccountId);
        this.senderAccountId = senderAccountId;
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (contractId != null) {
            contractId.validateChecksum(client);
        }
    }

    @Override
    void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = ContractCallLocalQuery.newBuilder();
        if (contractId != null) {
            builder.setContractID(contractId.toProtobuf());
        }
        builder.setGas(gas);
        builder.setFunctionParameters(ByteString.copyFrom(functionParameters));
        if (senderAccountId != null) {
            builder.setSenderId(senderAccountId.toProtobuf());
        }

        queryBuilder.setContractCallLocal(builder.setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getContractCallLocal().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
        return request.getContractCallLocal().getHeader();
    }

    @Override
    ContractFunctionResult mapResponse(Response response, AccountId nodeId, org.hiero.sdk.proto.Query request) {
        return new ContractFunctionResult(response.getContractCallLocal().getFunctionResult());
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Query, Response> getMethodDescriptor() {
        return SmartContractServiceGrpc.getContractCallLocalMethodMethod();
    }
}
