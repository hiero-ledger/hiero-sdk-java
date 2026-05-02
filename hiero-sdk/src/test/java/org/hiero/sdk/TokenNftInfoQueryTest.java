// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.hiero.sdk.proto.QueryHeader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenNftInfoQueryTest {
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
        new TokenNftInfoQuery()
                .setNftId(TokenId.fromString("0.0.5005").nft(101))
                .setMaxQueryPayment(Hbar.fromTinybars(100_000))
                .onMakeRequest(builder, QueryHeader.newBuilder().build());
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void propertiesTest() {
        var tokenId = TokenId.fromString("0.0.5005");
        var query = new TokenNftInfoQuery()
                .byAccountId(AccountId.fromString("0.0.123"))
                .byTokenId(tokenId)
                .setStart(5)
                .setEnd(8)
                .setNftId(tokenId.nft(101))
                .setMaxQueryPayment(Hbar.fromTinybars(100_000));

        assertThat(query.getNftId()).hasToString("0.0.5005/101");
        assertThat(query.getTokenId()).isEqualTo(tokenId);
        assertThat(query.getAccountId()).hasToString("0.0.123");
        assertThat(query.getStart()).isEqualTo(5);
        assertThat(query.getEnd()).isEqualTo(8);
    }
}
