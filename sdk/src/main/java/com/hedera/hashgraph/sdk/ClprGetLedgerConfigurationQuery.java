// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.QueryHeader;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import io.grpc.MethodDescriptor;

/**
 * Reads the current CLPR ledger configuration with the standard query payment flow.
 * Execution requires an operator, and the cost is looked up unless a payment is set.
 * <p>
 * Configuration fields are live, while the state proof is a signed snapshot.
 * An empty state proof is a successful read; retry later for proof material.
 * CLPR is off by default and must be enabled with {@code clpr.enabled}.
 */
public class ClprGetLedgerConfigurationQuery
        extends Query<ClprLedgerConfigurationInfo, ClprGetLedgerConfigurationQuery> {
    /**
     * Constructor.
     */
    public ClprGetLedgerConfigurationQuery() {}

    @Override
    void onMakeRequest(com.hedera.hashgraph.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        queryBuilder.setClprGetLedgerConfiguration(
                com.hedera.hashgraph.sdk.proto.ClprGetLedgerConfigurationQuery.newBuilder()
                        .setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getClprGetLedgerConfiguration().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(com.hedera.hashgraph.sdk.proto.Query request) {
        return request.getClprGetLedgerConfiguration().getHeader();
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        // no-op
    }

    @Override
    ClprLedgerConfigurationInfo mapResponse(
            Response response, AccountId nodeId, com.hedera.hashgraph.sdk.proto.Query request) {
        return ClprLedgerConfigurationInfo.fromProtobuf(response.getClprGetLedgerConfiguration());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Query, Response> getMethodDescriptor() {
        return ClprServiceGrpc.getGetLedgerConfigurationMethod();
    }
}
