// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import org.hiero.sdk.proto.mirror.ConsensusTopicResponse;

/**
 * Topic message records.
 */
public final class TopicMessage {
    /**
     * The consensus timestamp of the message in seconds.nanoseconds
     */
    public final Instant consensusTimestamp;
    /**
     * The content of the message
     */
    public final byte[] contents;
    /**
     * The new running hash of the topic that received the message
     */
    public final byte[] runningHash;
    /**
     * The sequence number of the message relative to all other messages
     * for the same topic
     */
    public final long sequenceNumber;
    /**
     * Array of topic message chunks.
     */
    @Nullable
    public final TopicMessageChunk[] chunks;
    /**
     * The transaction id
     */
    @Nullable
    public final TransactionId transactionId;

    /**
     * Constructor.
     *
     * @param lastConsensusTimestamp    the last consensus time
     * @param message                   the message
     * @param lastRunningHash           the last running hash
     * @param lastSequenceNumber        the last sequence number
     * @param chunks                    the array of chunks
     * @param transactionId             the transaction id
     */
    TopicMessage(
            Instant lastConsensusTimestamp,
            byte[] message,
            byte[] lastRunningHash,
            long lastSequenceNumber,
            @Nullable TopicMessageChunk[] chunks,
            @Nullable TransactionId transactionId) {
        this.consensusTimestamp = lastConsensusTimestamp;
        this.contents = message;
        this.runningHash = lastRunningHash;
        this.sequenceNumber = lastSequenceNumber;
        this.chunks = chunks;
        this.transactionId = transactionId;
    }

    /**
     * Create a new topic message from a response protobuf.
     *
     * @param response                  the protobuf response
     * @return                          the new topic message
     */
    static TopicMessage ofSingle(ConsensusTopicResponse response) {
        return new TopicMessage(
                InstantConverter.fromProtobuf(response.getConsensusTimestamp()),
                response.getMessage().toByteArray(),
                response.getRunningHash().toByteArray(),
                response.getSequenceNumber(),
                new TopicMessageChunk[] {new TopicMessageChunk(response)},
                response.hasChunkInfo() && response.getChunkInfo().hasInitialTransactionID()
                        ? TransactionId.fromProtobuf(response.getChunkInfo().getInitialTransactionID())
                        : null);
    }

    /**
     * Create a new topic message from a list of response's protobuf.
     *
     * @param responses                 the protobuf response
     * @return                          the new topic message
     */
    static TopicMessage ofMany(List<ConsensusTopicResponse> responses) {
        // response should be in the order of oldest to newest (not chunk order)
        var chunks = new TopicMessageChunk[responses.size()];
        TransactionId transactionId = null;
        var contents = new ByteString[responses.size()];
        long totalSize = 0;

        for (ConsensusTopicResponse r : responses) {
            if (transactionId == null && r.getChunkInfo().hasInitialTransactionID()) {
                transactionId = TransactionId.fromProtobuf(r.getChunkInfo().getInitialTransactionID());
            }

            int index = r.getChunkInfo().getNumber() - 1;

            chunks[index] = new TopicMessageChunk(r);
            contents[index] = r.getMessage();
            totalSize += r.getMessage().size();
        }

        var wholeMessage = ByteBuffer.allocate((int) totalSize);

        for (var content : contents) {
            wholeMessage.put(content.asReadOnlyByteBuffer());
        }

        var lastReceived = responses.get(responses.size() - 1);

        return new TopicMessage(
                InstantConverter.fromProtobuf(lastReceived.getConsensusTimestamp()),
                wholeMessage.array(),
                lastReceived.getRunningHash().toByteArray(),
                lastReceived.getSequenceNumber(),
                chunks,
                transactionId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("consensusTimestamp", consensusTimestamp)
                .add("contents", new String(contents, StandardCharsets.UTF_8))
                .add("runningHash", runningHash)
                .add("sequenceNumber", sequenceNumber)
                .toString();
    }
}
