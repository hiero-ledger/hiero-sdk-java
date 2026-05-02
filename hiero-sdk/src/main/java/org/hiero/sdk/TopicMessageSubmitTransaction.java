// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hiero.sdk.proto.ConsensusMessageChunkInfo;
import org.hiero.sdk.proto.ConsensusServiceGrpc;
import org.hiero.sdk.proto.ConsensusSubmitMessageTransactionBody;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.hiero.sdk.proto.TransactionID;
import org.hiero.sdk.proto.TransactionResponse;
import io.grpc.MethodDescriptor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Submit a message for consensus.
 * <p>
 * Valid and authorized messages on valid topics will be ordered by the consensus service, gossipped to the
 * mirror net, and published (in order) to all subscribers (from the mirror net) on this topic.
 * <p>
 * The submitKey (if any) must sign this transaction.
 * <p>
 * On success, the resulting TransactionReceipt contains the topic's updated topicSequenceNumber and
 * topicRunningHash.
 */
public final class TopicMessageSubmitTransaction extends ChunkedTransaction<TopicMessageSubmitTransaction> {
    @Nullable
    private TopicId topicId = null;

    /**
     * Constructor.
     */
    public TopicMessageSubmitTransaction() {}

    /**
     * Constructor.
     *
     * @param txs Compound list of transaction id's list of (AccountId, Transaction)
     *            records
     * @throws InvalidProtocolBufferException       when there is an issue with the protobuf
     */
    TopicMessageSubmitTransaction(
            LinkedHashMap<TransactionId, LinkedHashMap<AccountId, org.hiero.sdk.proto.Transaction>> txs)
            throws InvalidProtocolBufferException {
        super(txs);
        initFromTransactionBody();
    }

    /**
     * Constructor.
     *
     * @param txBody protobuf TransactionBody
     */
    TopicMessageSubmitTransaction(org.hiero.sdk.proto.TransactionBody txBody) {
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
     * Assign the topic id.
     *
     * @param topicId                   the topic id
     * @return {@code this}
     */
    public TopicMessageSubmitTransaction setTopicId(TopicId topicId) {
        Objects.requireNonNull(topicId);
        requireNotFrozen();
        this.topicId = topicId;
        return this;
    }

    /**
     * Extract the message.
     *
     * @return                          the message
     */
    public ByteString getMessage() {
        return getData();
    }

    /**
     * Assign the message from a byte string.
     *
     * @param message                   the byte string
     * @return                          the message
     */
    public TopicMessageSubmitTransaction setMessage(ByteString message) {
        return setData(message);
    }

    /**
     * Assign the message from a byte array.
     *
     * @param message                   the byte array
     * @return                          the message
     */
    public TopicMessageSubmitTransaction setMessage(byte[] message) {
        return setData(message);
    }

    /**
     * Assign the message from a string.
     *
     * @param message                   the string
     * @return                          the message
     */
    public TopicMessageSubmitTransaction setMessage(String message) {
        return setData(message);
    }

    /**
     * Extract the custom fee limits of the transaction
     * @return the custom fee limits of the transaction
     */
    public List<CustomFeeLimit> getCustomFeeLimits() {
        return this.customFeeLimits;
    }

    /**
     * The maximum custom fee that the user is willing to pay for the message. If left empty, the user is willing to pay any custom fee.
     * If used with a transaction type that does not support custom fee limits, the transaction will fail.
     */
    public TopicMessageSubmitTransaction setCustomFeeLimits(List<CustomFeeLimit> customFeeLimits) {
        Objects.requireNonNull(customFeeLimits);
        requireNotFrozen();
        this.customFeeLimits = customFeeLimits;
        return this;
    }

    /**
     * Adds a custom fee limit
     * @param customFeeLimit
     * @return {@code this}
     */
    public TopicMessageSubmitTransaction addCustomFeeLimit(CustomFeeLimit customFeeLimit) {
        Objects.requireNonNull(customFeeLimit);
        requireNotFrozen();
        if (this.customFeeLimits != null) {
            this.customFeeLimits.add(customFeeLimit);
        }
        return this;
    }

    /**
     * Clears all custom fee limits.
     * @return {@code this}
     */
    public TopicMessageSubmitTransaction clearCustomFeeLimits() {
        requireNotFrozen();
        if (this.customFeeLimits != null) {
            this.customFeeLimits.clear();
        }
        return this;
    }

    /**
     * Initialize from the transaction body.
     */
    void initFromTransactionBody() {
        var body = sourceTransactionBody.getConsensusSubmitMessage();
        if (body.hasTopicID()) {
            topicId = TopicId.fromProtobuf(body.getTopicID());
        }

        if (!innerSignedTransactions.isEmpty()) {
            try {
                for (var i = 0;
                        i < innerSignedTransactions.size();
                        i += nodeAccountIds.isEmpty() ? 1 : nodeAccountIds.size()) {
                    data = data.concat(TransactionBody.parseFrom(
                                    innerSignedTransactions.get(i).getBodyBytes())
                            .getConsensusSubmitMessage()
                            .getMessage());
                }
            } catch (InvalidProtocolBufferException exc) {
                throw new IllegalArgumentException(exc.getMessage());
            }
        } else {
            data = body.getMessage();
        }
    }

    /**
     * Build the transaction body.
     *
     * @return {@link
     *         org.hiero.sdk.proto.ConsensusSubmitMessageTransactionBody}
     */
    ConsensusSubmitMessageTransactionBody.Builder build() {
        var builder = ConsensusSubmitMessageTransactionBody.newBuilder();
        if (topicId != null) {
            builder.setTopicID(topicId.toProtobuf());
        }
        builder.setMessage(data);

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
        return ConsensusServiceGrpc.getSubmitMessageMethod();
    }

    @Override
    void onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setConsensusSubmitMessage(build());
    }

    @Override
    void onFreezeChunk(
            TransactionBody.Builder body,
            @Nullable TransactionID initialTransactionId,
            int startIndex,
            int endIndex,
            int chunk,
            int total) {
        if (total == 1) {
            body.setConsensusSubmitMessage(build().setMessage(data.substring(startIndex, endIndex)));
        } else {
            body.setConsensusSubmitMessage(build().setMessage(data.substring(startIndex, endIndex))
                    .setChunkInfo(ConsensusMessageChunkInfo.newBuilder()
                            .setInitialTransactionID(Objects.requireNonNull(initialTransactionId))
                            .setNumber(chunk + 1)
                            .setTotal(total)));
        }
    }

    @Override
    void onScheduled(SchedulableTransactionBody.Builder scheduled) {
        scheduled.setConsensusSubmitMessage(build().setMessage(data));
    }
}

