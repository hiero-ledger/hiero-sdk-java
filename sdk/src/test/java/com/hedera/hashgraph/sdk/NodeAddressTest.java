// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NodeAddressTest {
    private final com.hedera.hashgraph.sdk.proto.NodeAddress nodeAddress =
            com.hedera.hashgraph.sdk.proto.NodeAddress.newBuilder()
                    .setNodeAccountId(AccountId.fromString("0.0.100").toProtobuf())
                    .setRSAPubKey("0x7f3a9c2e8b1d4f605a7e9c3b2d8f1046")
                    .setNodeId(1)
                    .setNodeCertHash(
                            ByteString.copyFrom("0x8b2f6d1a9c4e7f305b8d2a6c1e9f43708d5b2a6c4e1f9037b8d5a2c6e9f10438"
                                    .getBytes(StandardCharsets.UTF_8)))
                    .setDescription("Unit test node")
                    .addServiceEndpoint(new Endpoint()
                            .setAddress(new byte[] {127, 0, 0, 1})
                            .setPort(8080)
                            .toProtobuf())
                    .addServiceEndpoint(new Endpoint()
                            .setDomainName("test.node.com")
                            .setPort(8080)
                            .toProtobuf())
                    .setStake(1)
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
        SnapshotMatcher.expect(NodeAddress.fromProtobuf(nodeAddress).toString()).toMatchSnapshot();
    }

    @Test
    void toProtobuf() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(
                        NodeAddress.fromProtobuf(nodeAddress).toProtobuf().toString())
                .toMatchSnapshot();
    }
}
