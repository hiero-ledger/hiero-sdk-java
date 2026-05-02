// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenRelationshipTest {
    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    TokenRelationship spawnTokenRelationshipExample() {
        return new TokenRelationship(TokenId.fromString("1.2.3"), "ABC", 55, true, true, 4, true);
    }

    @Test
    void shouldSerializeTokenRelationship() throws Exception {
        var originalTokenRelationship = spawnTokenRelationshipExample();
        byte[] tokenRelationshipBytes = originalTokenRelationship.toBytes();
        var copyTokenRelationship = TokenRelationship.fromBytes(tokenRelationshipBytes);
        assertThat(copyTokenRelationship.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .isEqualTo(originalTokenRelationship.toString().replaceAll("@[A-Za-z0-9]+", ""));
        SnapshotMatcher.expect(originalTokenRelationship.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }
}
