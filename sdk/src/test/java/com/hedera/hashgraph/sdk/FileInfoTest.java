// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.FileGetInfoResponse;
import com.hedera.hashgraph.sdk.proto.KeyList;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FileInfoTest {
    private static final PrivateKey privateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final FileGetInfoResponse.FileInfo info = FileGetInfoResponse.FileInfo.newBuilder()
            .setFileID(new FileId(0, 0, 1).toProtobuf())
            .setSize(2)
            .setExpirationTime(InstantConverter.toProtobuf(Instant.ofEpochMilli(3)))
            .setDeleted(true)
            .setKeys(KeyList.newBuilder().addKeys(privateKey.getPublicKey().toProtobufKey()))
            .setLedgerId(LedgerId.MAINNET.toByteString())
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
        SnapshotMatcher.expect(FileInfo.fromProtobuf(info).toString()).toMatchSnapshot();
    }

    @Test
    void toProtobuf() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(FileInfo.fromProtobuf(info).toProtobuf().toString())
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(FileInfo.fromBytes(info.toByteArray()).toString())
                .toMatchSnapshot();
    }

    @Test
    void toBytes() {
        SnapshotMatcher.expect(Hex.toHexString(FileInfo.fromProtobuf(info).toBytes()))
                .toMatchSnapshot();
    }
}
