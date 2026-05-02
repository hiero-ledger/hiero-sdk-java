// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.MethodDescriptor;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.ContractGetInfoQuery;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseHeader;
import org.hiero.sdk.proto.SmartContractServiceGrpc;

/**
 * Get information about a smart contract instance.
 * <p>
 * This includes the account that it uses, the file containing its bytecode,
 * and the time when it will expire.
 */
public final class ContractInfoQuery extends Query<ContractInfo, ContractInfoQuery> {
    @Nullable
    private ContractId contractId = null;

    /**
     * Constructor.
     */
    public ContractInfoQuery() {}

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
     * Sets the contract ID for which information is requested.
     *
     * @param contractId The ContractId to be set
     * @return {@code this}
     */
    public ContractInfoQuery setContractId(ContractId contractId) {
        Objects.requireNonNull(contractId);
        this.contractId = contractId;
        return this;
    }

    @Override
    public CompletableFuture<Hbar> getCostAsync(Client client) {
        // deleted accounts return a COST_ANSWER of zero which triggers `INSUFFICIENT_TX_FEE`
        // if you set that as the query payment; 25 tinybar seems to be enough to get
        // `CONTRACT_DELETED` back instead.
        return super.getCostAsync(client).thenApply((cost) -> Hbar.fromTinybars(Math.max(cost.toTinybars(), 25)));
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (contractId != null) {
            contractId.validateChecksum(client);
        }
    }

    @Override
    void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = ContractGetInfoQuery.newBuilder();
        if (contractId != null) {
            builder.setContractID(contractId.toProtobuf());
        }

        queryBuilder.setContractGetInfo(builder.setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getContractGetInfo().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
        return request.getContractGetInfo().getHeader();
    }

    @Override
    ContractInfo mapResponse(Response response, AccountId nodeId, org.hiero.sdk.proto.Query request) {
        return ContractInfo.fromProtobuf(response.getContractGetInfo().getContractInfo());
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Query, Response> getMethodDescriptor() {
        return SmartContractServiceGrpc.getGetContractInfoMethod();
    }
}
