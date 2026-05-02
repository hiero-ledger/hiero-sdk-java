// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.grpc.MethodDescriptor;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.Query;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Response;
import org.hiero.sdk.proto.ResponseHeader;
import org.hiero.sdk.proto.ScheduleGetInfoQuery;
import org.hiero.sdk.proto.ScheduleServiceGrpc;

/**
 * A query that returns information about the current state of a schedule
 * transaction on a Hedera network.
 *
 * See <a href="https://docs.hedera.com/guides/docs/sdks/schedule-transaction/get-schedule-info">Hedera Documentation</a>
 */
public class ScheduleInfoQuery extends org.hiero.sdk.Query<ScheduleInfo, ScheduleInfoQuery> {
    @Nullable
    private ScheduleId scheduleId = null;

    /**
     * Constructor.
     */
    public ScheduleInfoQuery() {}

    /**
     * Extract the schedule id.
     *
     * @return                          the schedule id
     */
    @Nullable
    public ScheduleId getScheduleId() {
        return scheduleId;
    }

    /**
     * Assign the schedule id.
     *
     * @param scheduleId                the schedule id
     * @return {@code this}
     */
    public ScheduleInfoQuery setScheduleId(ScheduleId scheduleId) {
        Objects.requireNonNull(scheduleId);
        this.scheduleId = scheduleId;
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (scheduleId != null) {
            scheduleId.validateChecksum(client);
        }
    }

    @Override
    void onMakeRequest(org.hiero.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = ScheduleGetInfoQuery.newBuilder();
        if (scheduleId != null) {
            builder.setScheduleID(scheduleId.toProtobuf());
        }

        queryBuilder.setScheduleGetInfo(builder.setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getScheduleGetInfo().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(org.hiero.sdk.proto.Query request) {
        return request.getScheduleGetInfo().getHeader();
    }

    @Override
    ScheduleInfo mapResponse(Response response, AccountId nodeId, Query request) {
        return ScheduleInfo.fromProtobuf(response.getScheduleGetInfo().getScheduleInfo());
    }

    @Override
    MethodDescriptor<Query, Response> getMethodDescriptor() {
        return ScheduleServiceGrpc.getGetScheduleInfoMethod();
    }

    @Override
    public CompletableFuture<Hbar> getCostAsync(Client client) {
        // deleted accounts return a COST_ANSWER of zero which triggers `INSUFFICIENT_TX_FEE`
        // if you set that as the query payment; 25 tinybar seems to be enough to get
        // `Token_DELETED` back instead.
        return super.getCostAsync(client).thenApply((cost) -> Hbar.fromTinybars(Math.max(cost.toTinybars(), 25)));
    }
}
