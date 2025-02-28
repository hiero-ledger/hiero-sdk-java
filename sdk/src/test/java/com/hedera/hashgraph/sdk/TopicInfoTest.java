// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.ConsensusGetTopicInfoResponse;
import com.hedera.hashgraph.sdk.proto.ConsensusTopicInfo;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TopicInfoTest {
    private static final PrivateKey privateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final PrivateKey feeScheduleKey = PrivateKey.fromString(
            "302e020100300506032b657004220420aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899");

    private static final List<PrivateKey> feeExemptKeys = List.of(
            PrivateKey.fromString(
                    "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10"),
            PrivateKey.fromString(
                    "302e020100300506032b657004220420aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899"));

    private static final byte[] hash = {2};

    private static final List<CustomFixedFee> customFees =
            List.of(new CustomFixedFee().setAmount(100).setDenominatingTokenId(new TokenId(0, 0, 0)));

    private static final ConsensusGetTopicInfoResponse info = ConsensusGetTopicInfoResponse.newBuilder()
            .setTopicInfo(ConsensusTopicInfo.newBuilder()
                    .setMemo("1")
                    .setRunningHash(ByteString.copyFrom(hash))
                    .setSequenceNumber(3)
                    .setExpirationTime(InstantConverter.toProtobuf(Instant.ofEpochMilli(4)))
                    .setAutoRenewPeriod(DurationConverter.toProtobuf(Duration.ofDays(5)))
                    .setAdminKey(privateKey.getPublicKey().toProtobufKey())
                    .setSubmitKey(privateKey.getPublicKey().toProtobufKey())
                    .setFeeScheduleKey(feeScheduleKey.getPublicKey().toProtobufKey())
                    .addAllFeeExemptKeyList(feeExemptKeys.stream()
                            .map(k -> k.getPublicKey().toProtobufKey())
                            .toList())
                    .addAllCustomFees(customFees.stream()
                            .map(CustomFixedFee::toTopicFeeProtobuf)
                            .toList())
                    .setAutoRenewAccount(new AccountId(0, 0, 4).toProtobuf())
                    .setLedgerId(LedgerId.TESTNET.toByteString()))
            .build();

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void fromProtobuf() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(TopicInfo.fromProtobuf(info).toString()).toMatchSnapshot();
    }

    @Test
    void toProtobuf() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(TopicInfo.fromProtobuf(info).toProtobuf().toString())
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(TopicInfo.fromBytes(info.toByteArray()).toString())
                .toMatchSnapshot();
    }

    @Test
    void toBytes() {
        SnapshotMatcher.expect(Hex.toHexString(TopicInfo.fromProtobuf(info).toBytes()))
                .toMatchSnapshot();
    }
}
