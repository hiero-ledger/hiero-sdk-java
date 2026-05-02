// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.hiero.sdk.proto.TokenSupplyType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenSupplyTypeTest {
    private final TokenSupplyType tokenSupplyTypeInfinite = TokenSupplyType.INFINITE;
    private final TokenSupplyType tokenSupplyTypeFinite = TokenSupplyType.FINITE;

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
        SnapshotMatcher.expect(
                        org.hiero.sdk.TokenSupplyType.valueOf(tokenSupplyTypeInfinite)
                                .toString(),
                        org.hiero.sdk.TokenSupplyType.valueOf(tokenSupplyTypeFinite)
                                .toString())
                .toMatchSnapshot();
    }

    @Test
    void toProtobuf() {
        SnapshotMatcher.expect(
                        org.hiero.sdk.TokenSupplyType.valueOf(tokenSupplyTypeInfinite)
                                .toProtobuf(),
                        org.hiero.sdk.TokenSupplyType.valueOf(tokenSupplyTypeFinite)
                                .toProtobuf())
                .toMatchSnapshot();
    }

    @Test
    void tokenSupplyTestToString() {
        assertThat(org.hiero.sdk.TokenSupplyType.INFINITE).hasToString("INFINITE");
        assertThat(org.hiero.sdk.TokenSupplyType.FINITE).hasToString("FINITE");
    }
}
