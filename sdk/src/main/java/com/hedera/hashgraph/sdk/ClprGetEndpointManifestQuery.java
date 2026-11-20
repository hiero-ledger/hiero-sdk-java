// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.ClprServiceGrpc;
import com.hedera.hashgraph.sdk.proto.QueryHeader;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import io.grpc.MethodDescriptor;

/**
 * Reads the current CLPR endpoint manifest with the standard query payment flow.
 * Execution requires an operator, and the cost is looked up unless a payment is set.
 * <p>
 * The manifest and its state proof are paired and should be kept together.
 * An empty state proof is a successful read; retry later for proof material.
 * CLPR is off by default and must be enabled with {@code clpr.enabled}.
 */
public class ClprGetEndpointManifestQuery extends Query<ClprEndpointManifestInfo, ClprGetEndpointManifestQuery> {
    /**
     * Constructor.
     */
    public ClprGetEndpointManifestQuery() {}

    @Override
    void onMakeRequest(com.hedera.hashgraph.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        queryBuilder.setClprGetEndpointManifest(com.hedera.hashgraph.sdk.proto.ClprGetEndpointManifestQuery.newBuilder()
                .setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getClprGetEndpointManifest().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(com.hedera.hashgraph.sdk.proto.Query request) {
        return request.getClprGetEndpointManifest().getHeader();
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        // no-op
    }

    @Override
    ClprEndpointManifestInfo mapResponse(
            Response response, AccountId nodeId, com.hedera.hashgraph.sdk.proto.Query request) {
        return ClprEndpointManifestInfo.fromProtobuf(response.getClprGetEndpointManifest());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Query, Response> getMethodDescriptor() {
        return ClprServiceGrpc.getGetEndpointManifestMethod();
    }
}
