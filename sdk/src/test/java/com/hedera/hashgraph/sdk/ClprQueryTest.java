// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.QueryHeader;
import com.hedera.hashgraph.sdk.proto.Transaction;
import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ClprQueryTest {
    private static final QueryHeader HEADER = QueryHeader.newBuilder()
            .setPayment(Transaction.newBuilder()
                    .setSignedTransactionBytes(ByteString.fromHex("deadbeef"))
                    .build())
            .build();

    @BeforeAll
    static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void ledgerConfigurationQueryShouldSerialize() {
        var builder = com.hedera.hashgraph.sdk.proto.Query.newBuilder();
        var query = new ClprGetLedgerConfigurationQuery();
        query.onMakeRequest(builder, HEADER);
        assertThat(query.mapRequestHeader(builder.build())).isEqualTo(HEADER);
        assertThat(query.isPaymentRequired()).isTrue();
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void endpointManifestQueryShouldSerialize() {
        var builder = com.hedera.hashgraph.sdk.proto.Query.newBuilder();
        var query = new ClprGetEndpointManifestQuery();
        query.onMakeRequest(builder, HEADER);
        assertThat(query.mapRequestHeader(builder.build())).isEqualTo(HEADER);
        assertThat(query.isPaymentRequired()).isTrue();
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }
}
