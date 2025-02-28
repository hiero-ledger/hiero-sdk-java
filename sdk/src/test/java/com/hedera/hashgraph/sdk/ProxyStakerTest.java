// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.ProxyStaker;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ProxyStakerTest {
    private static final ProxyStaker proxyStaker = ProxyStaker.newBuilder()
            .setAccountID(new AccountId(0, 0, 100).toProtobuf())
            .setAmount(10)
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
        SnapshotMatcher.expect(com.hedera.hashgraph.sdk.ProxyStaker.fromProtobuf(proxyStaker)
                        .toString())
                .toMatchSnapshot();
    }
}
