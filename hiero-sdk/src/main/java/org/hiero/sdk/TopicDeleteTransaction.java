// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.ConsensusDeleteTopicTransactionBody;
import org.hiero.sdk.proto.ConsensusServiceGrpc;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionResponse;

/**
 * Delete a topic.
 * <p>
 * No more transactions or queries on the topic will succeed.
 * <p>
 * If an {@code adminKey} is set, this transaction must be signed by that key.
 * If there is no {@code adminKey}, this transaction will fail with {@link Status#UNAUTHORIZED}.
 */
public final class TopicDeleteTransaction extends Transaction<TopicDeleteTransaction> {
    @Nullable
    private TopicId topicId = null;

    /**
     * Constructor.
     */
    public TopicDeleteTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TopicDeleteTransaction(LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TopicDeleteTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
        super(txBody);
        initFromTransactionBody();
    }

    /**
     * Extract the topic id.
     *
     * @return                          the topic id
     */
    @Nullable
    public TopicId getTopicId() {
        return topicId;
    }

    /**
     * Set the topic ID to delete.
     *
     * @param topicId The TopicId to be set
     * @return {@code this}
     */
    public TopicDeleteTransaction setTopicId(TopicId topicId) {
        Objects.requireNonNull(topicId);
        requireNotFrozen();
        this.topicId = topicId;
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getConsensusDeleteTopic();
        if (body.hasTopicID()) {
            topicId = TopicId.fromProtobuf(body.getTopicID());
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link
     *         org.hiero.sdk.proto.ConsensusDeleteTopicTransactionBody}
     */
    ConsensusDeleteTopicTransactionBody.Builder build() {
        var builder = ConsensusDeleteTopicTransactionBody.newBuilder();
        if (topicId != null) {
            builder.setTopicID(topicId.toProtobuf());
        }

        return builder;
    }

    @Override
    void validateChecksums(Client client) throws BadEntityIdException {
        if (topicId != null) {
            topicId.validateChecksum(client);
        }
    }

    @Override
    MethodDescriptor<org.hiero.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return ConsensusServiceGrpc.getDeleteTopicMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setConsensusDeleteTopic(build());
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setConsensusDeleteTopic(build());
    }
}
