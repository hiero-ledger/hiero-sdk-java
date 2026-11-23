// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.tck.methods.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.TopicInfo;
import com.hedera.hashgraph.sdk.proto.ConsensusGetTopicInfoResponse;
import com.hedera.hashgraph.sdk.proto.ConsensusTopicInfo;
import com.hedera.hashgraph.sdk.proto.Duration;
import com.hedera.hashgraph.sdk.proto.Timestamp;
import com.hedera.hashgraph.sdk.proto.TopicID;
import org.junit.jupiter.api.Test;

class TopicServiceTest {

    @Test
    void topicInfoRunningHashIsHexEncoded() throws Exception {
        // A running hash is 48 raw SHA-384 bytes, not text.
        byte[] runningHash = new byte[48];
        for (int i = 0; i < runningHash.length; i++) {
            runningHash[i] = (byte) (0xd0 + i);
        }
        var info = ConsensusTopicInfo.newBuilder()
                .setRunningHash(ByteString.copyFrom(runningHash))
                .setSequenceNumber(1)
                .setExpirationTime(Timestamp.newBuilder().setSeconds(1787170588))
                .setAutoRenewPeriod(Duration.newBuilder().setSeconds(7776000))
                .setLedgerId(ByteString.copyFrom(new byte[] {1}));
        var topicInfo = TopicInfo.fromBytes(ConsensusGetTopicInfoResponse.newBuilder()
                .setTopicID(TopicID.newBuilder().setTopicNum(1001))
                .setTopicInfo(info)
                .build()
                .toByteArray());

        var response = TopicService.mapTopicInfoResponse(topicInfo);

        assertEquals(
                "d0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff",
                response.runningHash());
    }
}
