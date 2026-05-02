// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import com.google.protobuf.ByteString;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.hiero.sdk.proto.QueryHeader;
import org.hiero.sdk.proto.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NetworkVersionInfoQueryTest {
    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerialize() {
        var builder = org.hiero.sdk.proto.Query.newBuilder();
        new NetworkVersionInfoQuery()
                .setMaxQueryPayment(Hbar.fromTinybars(100_000))
                .onMakeRequest(
                        builder,
                        QueryHeader.newBuilder()
                                .setPayment(Transaction.newBuilder()
                                        .setSignedTransactionBytes(ByteString.fromHex("deadbeef"))
                                        .build())
                                .build());
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }
}
