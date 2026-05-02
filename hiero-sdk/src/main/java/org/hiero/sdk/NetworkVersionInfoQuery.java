// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.MethodDescriptor;
import org.hiero.sdk.proto.NetworkGetVersionInfoQuery;
import org.hiero.sdk.proto.NetworkServiceGrpc;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseHeader;

/**
 * Information about the versions of protobuf and hedera.
 */
public class NetworkVersionInfoQuery extends Query<NetworkVersionInfo, NetworkVersionInfoQuery> {
    /**
     * Constructor.
     */
    public NetworkVersionInfoQuery() {}

    @Override
    void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        queryBuilder.setNetworkGetVersionInfo(
                NetworkGetVersionInfoQuery.newBuilder().setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getNetworkGetVersionInfo().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
        return request.getNetworkGetVersionInfo().getHeader();
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        // do nothing
    }

    @Override
    NetworkVersionInfo mapResponse(Response response, AccountId nodeId, org.hiero.sdk.proto.Query request) {
        return NetworkVersionInfo.fromProtobuf(response.getNetworkGetVersionInfo());
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Query, Response> getMethodDescriptor() {
        return NetworkServiceGrpc.getGetVersionInfoMethod();
    }
}
