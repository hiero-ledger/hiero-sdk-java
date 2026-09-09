// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.ConsensusGetTopicInfoQuery;
import com.hedera.hashgraph.sdk.proto.ConsensusServiceGrpc;
import com.hedera.hashgraph.sdk.proto.QueryHeader;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.ResponseHeader;
import io.grpc.MethodDescriptor;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Retrieve the latest state of a topic.
 * <p>
 * This method is unrestricted and allowed on any topic by any payer account.
 */
public final class TopicInfoQuery extends Query<TopicInfo, TopicInfoQuery> {
    @Nullable
    TopicId topicId = null;

    /**
     * Constructor.
     */
    public TopicInfoQuery() {}

    /**
     * Extract the topic id.
     *
     * @return                          the topic id
     */
    public @Nullable TopicId getTopicId() {
        return topicId;
    }

    /**
     * Set the topic to retrieve info about (the parameters and running state of).
     *
     * @param topicId The TopicId to be set
     * @return {@code this}
     */
    public TopicInfoQuery setTopicId(TopicId topicId) {
        Objects.requireNonNull(topicId);
        this.topicId = topicId;
        return this;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (topicId != null) {
            topicId.validateChecksum(client);
        }
    }

    @Override
    void onMakeRequest(com.hedera.hashgraph.sdk.proto.Query.Builder queryBuilder, QueryHeader header) {
        var builder = ConsensusGetTopicInfoQuery.newBuilder();
        if (topicId != null) {
            builder.setTopicID(topicId.toProtobuf());
        }

        queryBuilder.setConsensusGetTopicInfo(builder.setHeader(header));
    }

    @Override
    ResponseHeader mapResponseHeader(Response response) {
        return response.getConsensusGetTopicInfo().getHeader();
    }

    @Override
    QueryHeader mapRequestHeader(com.hedera.hashgraph.sdk.proto.Query request) {
        return request.getConsensusGetTopicInfo().getHeader();
    }

    @Override
    TopicInfo mapResponse(Response response, AccountId nodeId, com.hedera.hashgraph.sdk.proto.Query request) {
        return TopicInfo.fromProtobuf(response.getConsensusGetTopicInfo());
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Query, Response> getMethodDescriptor() {
        return ConsensusServiceGrpc.getGetTopicInfoMethod();
    }
}
