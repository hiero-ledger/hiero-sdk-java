// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NetworkVersionInfoTest {
    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    NetworkVersionInfo spawnNetworkVerionInfoExample() {
        return new NetworkVersionInfo(new SemanticVersion(1, 2, 3), new SemanticVersion(4, 5, 6));
    }

    @Test
    void shouldSerialize() throws Exception {
        var originalNetworkVersionInfo = spawnNetworkVerionInfoExample();
        byte[] networkVersionInfoBytes = originalNetworkVersionInfo.toBytes();
        var copyNetworkVersionInfo = NetworkVersionInfo.fromBytes(networkVersionInfoBytes);
        assertThat(originalNetworkVersionInfo.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .isEqualTo(copyNetworkVersionInfo.toString().replaceAll("@[A-Za-z0-9]+", ""));
        SnapshotMatcher.expect(originalNetworkVersionInfo.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }
}
