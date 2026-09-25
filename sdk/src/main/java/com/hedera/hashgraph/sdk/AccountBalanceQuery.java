// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.CryptoGetAccountBalanceQuery;
import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.QueryHeader;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get the balance of a Hedera™ crypto-currency account. This returns only the balance, so it is a
 * smaller and faster reply than {@link AccountInfoQuery}.
 *
 * <p>This query is free.
 *
 * @deprecated AccountBalanceQuery is no longer supported. Use {@link MirrorNodeAccountBalanceQuery}
 *             or the mirror node REST API ({@code GET /api/v1/accounts/{id}}) to retrieve account
 *             balances.
 */
@Deprecated
public final class AccountBalanceQuery extends Query<AccountBalance, AccountBalanceQuery> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountBalanceQuery.class);

    static final String DEPRECATION_MESSAGE =
            "Deprecated: AccountBalanceQuery is no longer supported. Use MirrorNodeAccountBalanceQuery or "
                    + "the mirror node REST API (GET /api/v1/accounts/{id}) to retrieve account balances.";

    @Nullable
    private AccountId accountId = null;

    @Nullable
    private ContractId contractId = null;

    /**
     * Constructor.
     *
     * @deprecated see {@link AccountBalanceQuery}
     */
    @Deprecated
    public AccountBalanceQuery() {
        LOGGER.warn(DEPRECATION_MESSAGE);
    }

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

    /**
     * @deprecated see {@link AccountBalanceQuery}
     * @param client the client with which this would have been executed
     * @param timeout ignored
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Deprecated
    @Override
    public AccountBalance execute(Client client, Duration timeout) {
        throw new UnsupportedOperationException(DEPRECATION_MESSAGE);
    }

    /**
     * @deprecated see {@link AccountBalanceQuery}
     * @param client the client with which this would have been executed
     * @param timeout ignored
     * @return a future that has already failed with {@link UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public CompletableFuture<AccountBalance> executeAsync(Client client, Duration timeout) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException(DEPRECATION_MESSAGE));
    }

    /**
     * @deprecated see {@link AccountBalanceQuery}
     * @param client the client with which this would have been executed
     * @param timeout ignored
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Deprecated
    @Override
    public Hbar getCost(Client client, Duration timeout) {
        throw new UnsupportedOperationException(DEPRECATION_MESSAGE);
    }

    /**
     * @deprecated see {@link AccountBalanceQuery}
     * @param client the client with which this would have been executed
     * @param timeout ignored
     * @return a future that has already failed with {@link UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public CompletableFuture<Hbar> getCostAsync(Client client, Duration timeout) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException(DEPRECATION_MESSAGE));
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
    void onMakeRequest(com.hedera.hashgraph.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
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
    AccountBalance mapResponse(Response response, AccountId nodeId, com.hedera.hashgraph.sdk.proto.Query request) {
        return AccountBalance.fromProtobuf(response.getCryptogetAccountBalance());
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getCryptogetAccountBalance().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(com.hedera.hashgraph.sdk.proto.Query request) {
        return request.getCryptogetAccountBalance().getHeader();
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Query, Response> getMethodDescriptor() {
        return CryptoServiceGrpc.getCryptoGetBalanceMethod();
    }
}
