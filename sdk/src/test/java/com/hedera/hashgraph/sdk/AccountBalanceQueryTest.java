// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.proto.QueryHeader;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AccountBalanceQueryTest {
    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    @Test
    void shouldSerializeWithAccountId() {
        var builder = com.hedera.hashgraph.sdk.proto.Query.newBuilder();
        new AccountBalanceQuery()
                .setAccountId(AccountId.fromString("0.0.5005"))
                .onMakeRequest(builder, QueryHeader.newBuilder().build());
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void shouldSerializeWithContractId() {
        var builder = com.hedera.hashgraph.sdk.proto.Query.newBuilder();
        new AccountBalanceQuery()
                .setContractId(ContractId.fromString("0.0.5005"))
                .onMakeRequest(builder, QueryHeader.newBuilder().build());
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void constructorLogsDeprecationWarning() {
        var originalErr = System.err;
        var captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            new AccountBalanceQuery();
        } finally {
            System.setErr(originalErr);
        }

        assertThat(captured.toString(StandardCharsets.UTF_8)).contains(AccountBalanceQuery.DEPRECATION_MESSAGE);
    }
}
