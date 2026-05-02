// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.MethodDescriptor;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.CryptoGetInfoQuery;
import org.hiero.sdk.proto.CryptoServiceGrpc;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseHeader;

/**
 * Get all the information about an account, including the balance.
 * This does not get the list of account records.
 */
public final class AccountInfoQuery extends Query<AccountInfo, AccountInfoQuery> {

    @Nullable
    private AccountId accountId = null;

    /**
     * Constructor.
     */
    public AccountInfoQuery() {}

    /**
     * Extract the account id.
     *
     * @return                          the account id
     */
    @Nullable
    public AccountId getAccountId() {
        return accountId;
    }

    /**
     * Sets the account ID for which information is requested.
     *
     * @param accountId The AccountId to be set
     * @return {@code this}
     */
    public AccountInfoQuery setAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId);
        this.accountId = accountId;
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (accountId != null) {
            accountId.validateChecksum(client);
        }
    }

    @Override
    void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = CryptoGetInfoQuery.newBuilder();

        if (accountId != null) {
            builder.setAccountID(accountId.toProtobuf());
        }

        queryBuilder.setCryptoGetInfo(builder.setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getCryptoGetInfo().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
        return request.getCryptoGetInfo().getHeader();
    }

    @Override
    AccountInfo mapResponse(Response response, AccountId nodeId, org.hiero.sdk.proto.Query request) {
        return AccountInfo.fromProtobuf(response.getCryptoGetInfo().getAccountInfo());
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Query, Response> getMethodDescriptor() {
        return CryptoServiceGrpc.getGetAccountInfoMethod();
    }

    @Override
    public CompletableFuture<Hbar> getCostAsync(Client client) {
        // deleted accounts return a COST_ANSWER of zero which triggers `INSUFFICIENT_TX_FEE`
        // if you set that as the query payment; 25 tinybar seems to be enough to get
        // `ACCOUNT_DELETED` back instead.
        return super.getCostAsync(client).thenApply((cost) -> Hbar.fromTinybars(Math.max(cost.toTinybars(), 25)));
    }
}
