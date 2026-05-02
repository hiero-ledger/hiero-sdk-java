// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import org.hiero.sdk.proto.ProxyStaker;
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
        SnapshotMatcher.expect(org.hiero.sdk.ProxyStaker.fromProtobuf(proxyStaker)
                        .toString())
                .toMatchSnapshot();
    }
}

