// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.MethodDescriptor;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.CryptoGetAccountBalanceQuery;
import org.hiero.sdk.proto.CryptoServiceGrpc;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseHeader;

/**
 * Get the balance of a Hedera™ crypto-currency account. This returns only the balance, so it is a
 * smaller and faster reply than {@link AccountInfoQuery}.
 *
 * <p>This query is free.
 */
public final class AccountBalanceQuery extends Query<AccountBalance, AccountBalanceQuery> {
    @Nullable
    private AccountId accountId = null;

    @Nullable
    private ContractId contractId = null;

    /**
     * Constructor.
     */
    public AccountBalanceQuery() {}

    /**
     * Return the account's id.
     *
     * @return {@code accountId}
     */
    @Nullable
    public AccountId getAccountId() {
        return accountId;
    }

    /**
     * The account ID for which the balance is being requested.
     * <p>
     * This is mutually exclusive with {@link #setContractId(ContractId)}.
     *
     * @param accountId The AccountId to set
     * @return {@code this}
     */
    public AccountBalanceQuery setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        this.accountId = accountId;
        return this;
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
     * The contract ID for which the balance is being requested.
     * <p>
     * This is mutually exclusive with {@link #setAccountId(AccountId)}.
     *
     * @param contractId The ContractId to set
     * @return {@code this}
     */
    public AccountBalanceQuery setContractId(ContractId contractId) {
        Objects.requireNonNull(contractId);
        this.contractId = contractId;
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (accountId != null) {
            accountId.validateChecksum(client);
        }

        if (contractId != null) {
            contractId.validateChecksum(client);
        }
    }

    @Override
    boolean isPaymentRequired() {
        return false;
    }

    @Override
    void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = CryptoGetAccountBalanceQuery.newBuilder();
        if (accountId != null) {
            builder.setAccountID(accountId.toProtobuf());
        }

        if (contractId != null) {
            builder.setContractID(contractId.toProtobuf());
        }

        queryBuilder.setCryptogetAccountBalance(builder.setHeader(header));
    }

    @Override
    AccountBalance mapResponse(Response response, AccountId nodeId, org.hiero.sdk.proto.Query request) {
        return AccountBalance.fromProtobuf(response.getCryptogetAccountBalance());
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getCryptogetAccountBalance().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
        return request.getCryptogetAccountBalance().getHeader();
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Query, Response> getMethodDescriptor() {
        return CryptoServiceGrpc.getCryptoGetBalanceMethod();
    }
}
