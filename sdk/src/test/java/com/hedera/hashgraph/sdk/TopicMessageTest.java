// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.ConsensusMessageChunkInfo;
import com.hedera.hashgraph.sdk.proto.Timestamp;
import com.hedera.hashgraph.sdk.proto.mirror.ConsensusTopicResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TopicMessageTest {

    private static final Instant testTimestamp = Instant.ofEpochSecond(1554158542);
    private static final byte[] testContents = new byte[] {0x01, 0x02, 0x03};
    private static final byte[] testRunningHash = new byte[] {0x04, 0x05, 0x06};
    private static final long testSequenceNumber = 7L;
    private static final TransactionId testTransactionId = new TransactionId(new AccountId(0, 0, 1), testTimestamp);

    @Test
    void constructWithArgs() {
        TopicMessageChunk topicMessageChunk = new TopicMessageChunk(ConsensusTopicResponse.newBuilder()
                .setConsensusTimestamp(Timestamp.newBuilder().setSeconds(testTimestamp.getEpochSecond()))
                .setRunningHash(ByteString.copyFrom(testRunningHash))
                .setSequenceNumber(testSequenceNumber)
                .build());

        TopicMessageChunk[] topicMessageChunkArr = {topicMessageChunk, topicMessageChunk, topicMessageChunk};

        TopicMessage topicMessage = new TopicMessage(
                testTimestamp,
                testContents,
                testRunningHash,
                testSequenceNumber,
                topicMessageChunkArr,
                testTransactionId);

        assertThat(topicMessage.consensusTimestamp).isEqualTo(testTimestamp);
        assertThat(topicMessage.contents).isEqualTo(testContents);
        assertThat(topicMessage.runningHash).isEqualTo(testRunningHash);
        assertThat(topicMessage.sequenceNumber).isEqualTo(testSequenceNumber);
        assertThat(topicMessage.chunks).hasSize(3);
        assertThat(topicMessage.transactionId).isEqualTo(testTransactionId);
    }

    @Test
    void ofSingle() {
        var consensusTopicResponse = ConsensusTopicResponse.newBuilder()
                .setConsensusTimestamp(Timestamp.newBuilder().setSeconds(testTimestamp.getEpochSecond()))
                .setMessage(ByteString.copyFrom(testContents))
                .setRunningHash(ByteString.copyFrom(testRunningHash))
                .setSequenceNumber(testSequenceNumber)
                .setChunkInfo(ConsensusMessageChunkInfo.newBuilder()
                        .setInitialTransactionID(testTransactionId.toProtobuf())
                        .build())
                .build();

        TopicMessage topicMessage = TopicMessage.ofSingle(consensusTopicResponse);

        assertThat(topicMessage.consensusTimestamp).isEqualTo(testTimestamp);
        assertThat(topicMessage.contents).isEqualTo(testContents);
        assertThat(topicMessage.runningHash).isEqualTo(testRunningHash);
        assertThat(topicMessage.sequenceNumber).isEqualTo(testSequenceNumber);
        assertThat(topicMessage.chunks).hasSize(1);
        assertThat(topicMessage.transactionId).isEqualTo(testTransactionId);
    }

    @Test
    void ofMany() {
        var consensusTopicResponse1 = ConsensusTopicResponse.newBuilder()
                .setConsensusTimestamp(Timestamp.newBuilder().setSeconds(testTimestamp.getEpochSecond()))
                .setMessage(ByteString.copyFrom(testContents))
                .setRunningHash(ByteString.copyFrom(testRunningHash))
                .setSequenceNumber(testSequenceNumber)
                .setChunkInfo(ConsensusMessageChunkInfo.newBuilder()
                        .setInitialTransactionID(testTransactionId.toProtobuf())
                        .setNumber(1)
                        .setTotal(2)
                        .build())
                .build();

        var consensusTopicResponse2 = ConsensusTopicResponse.newBuilder()
                .setConsensusTimestamp(Timestamp.newBuilder().setSeconds(testTimestamp.getEpochSecond() + 1))
                .setMessage(ByteString.copyFrom(testContents))
                .setRunningHash(ByteString.copyFrom(testRunningHash))
                .setSequenceNumber(testSequenceNumber + 1L)
                .setChunkInfo(ConsensusMessageChunkInfo.newBuilder()
                        .setNumber(2)
                        .setTotal(2)
                        .build())
                .build();

        TopicMessage topicMessage = TopicMessage.ofMany(List.of(consensusTopicResponse1, consensusTopicResponse2));

        byte[] totalContents = new byte[testContents.length * 2];
        System.arraycopy(testContents, 0, totalContents, 0, testContents.length);
        System.arraycopy(testContents, 0, totalContents, testContents.length, testContents.length);
        assertThat(topicMessage.consensusTimestamp).isEqualTo(testTimestamp.plusSeconds(1));
        assertThat(topicMessage.contents).isEqualTo(totalContents);
        assertThat(topicMessage.runningHash).isEqualTo(testRunningHash);
        assertThat(topicMessage.sequenceNumber).isEqualTo(testSequenceNumber + 1L);
        assertThat(topicMessage.chunks).hasSize(2);
        assertThat(topicMessage.transactionId).isEqualTo(testTransactionId);
    }
}
