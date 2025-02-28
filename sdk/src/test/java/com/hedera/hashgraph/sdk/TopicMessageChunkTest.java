// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.ConsensusMessageChunkInfo;
import com.hedera.hashgraph.sdk.proto.Timestamp;
import com.hedera.hashgraph.sdk.proto.mirror.ConsensusTopicResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class TopicMessageChunkTest {

    private static final Instant testTimestamp = Instant.ofEpochSecond(1554158542);
    private static final byte[] testContents = new byte[] {0x01, 0x02, 0x03};
    private static final byte[] testRunningHash = new byte[] {0x04, 0x05, 0x06};
    private static final long testSequenceNumber = 7L;
    private static final TransactionId testTransactionId = new TransactionId(new AccountId(0, 0, 1), testTimestamp);

    @Test
    void constructWithArgs() {
        var consensusTopicResponse = ConsensusTopicResponse.newBuilder()
                .setConsensusTimestamp(Timestamp.newBuilder().setSeconds(testTimestamp.getEpochSecond()))
                .setMessage(ByteString.copyFrom(testContents))
                .setRunningHash(ByteString.copyFrom(testRunningHash))
                .setSequenceNumber(testSequenceNumber)
                .setChunkInfo(ConsensusMessageChunkInfo.newBuilder()
                        .setInitialTransactionID(testTransactionId.toProtobuf())
                        .build())
                .build();

        TopicMessageChunk topicMessageChunk = new TopicMessageChunk(consensusTopicResponse);

        assertThat(topicMessageChunk.consensusTimestamp).isEqualTo(testTimestamp);
        assertThat(topicMessageChunk.contentSize).isEqualTo(testContents.length);
        assertThat(topicMessageChunk.runningHash).isEqualTo(testRunningHash);
        assertThat(topicMessageChunk.sequenceNumber).isEqualTo(testSequenceNumber);
    }
}
