// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RegisteredNodeTest {
    private static final PrivateKey privateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private final BlockNodeServiceEndpoint serviceEndpoint = new BlockNodeServiceEndpoint()
            .addEndpointApi(BlockNodeApi.STATUS)
            .setPort(443)
            .setDomainName("test.block.com")
            .setRequiresTls(true);

    private final com.hedera.hashgraph.sdk.proto.RegisteredNode registeredNode =
            com.hedera.hashgraph.sdk.proto.RegisteredNode.newBuilder()
                    .setRegisteredNodeId(1)
                    .setDescription("Unit test registered node")
                    .setAdminKey(privateKey.getPublicKey().toProtobufKey())
                    .addServiceEndpoint(serviceEndpoint.toProtobuf())
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
    void fromProtobuf() {
        SnapshotMatcher.expect(RegisteredNode.fromProtobuf(registeredNode).toString())
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        RegisteredNode.fromBytes(registeredNode.toByteArray()).toString())
                .toMatchSnapshot();
    }

    @Test
    void toBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        RegisteredNode.fromBytes(registeredNode.toByteArray()).toBytes())
                .toMatchSnapshot();
    }


    @Test
    void toProtobuf() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        RegisteredNode.fromProtobuf(registeredNode).toProtobuf().toString())
                .toMatchSnapshot();
    }
}
