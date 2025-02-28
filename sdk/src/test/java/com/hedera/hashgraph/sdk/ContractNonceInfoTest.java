// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ContractNonceInfoTest {
    private final com.hedera.hashgraph.sdk.proto.ContractNonceInfo info =
            com.hedera.hashgraph.sdk.proto.ContractNonceInfo.newBuilder()
                    .setContractId(new ContractId(0, 0, 1).toProtobuf())
                    .setNonce(2)
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
        SnapshotMatcher.expect(ContractNonceInfo.fromProtobuf(info).toString()).toMatchSnapshot();
    }

    @Test
    void toProtobuf() {
        SnapshotMatcher.expect(ContractNonceInfo.fromProtobuf(info).toProtobuf())
                .toMatchSnapshot();
    }

    @Test
    void toBytes() {
        SnapshotMatcher.expect(
                        Hex.toHexString(ContractNonceInfo.fromProtobuf(info).toBytes()))
                .toMatchSnapshot();
    }

    @Test
    void fromBytes() throws InvalidProtocolBufferException {
        SnapshotMatcher.expect(ContractNonceInfo.fromBytes(info.toByteArray()).toString())
                .toMatchSnapshot();
    }
}
