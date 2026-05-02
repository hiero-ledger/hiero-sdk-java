// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import org.hiero.sdk.proto.QueryHeader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ContractCallQueryTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    final Instant validStart = Instant.ofEpochSecond(1554158542);

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
        new ContractCallQuery()
                .setContractId(ContractId.fromString("0.0.5005"))
                .setGas(1541)
                .setSenderAccountId(AccountId.fromString("1.2.3"))
                .setFunction(
                        "foo",
                        new ContractFunctionParameters().addString("Hello").addString("world!"))
                .onMakeRequest(builder, QueryHeader.newBuilder().build());
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void setFunctionParameters() {
        var builder = org.hiero.sdk.proto.Query.newBuilder();
        new ContractCallQuery()
                .setContractId(ContractId.fromString("0.0.5005"))
                .setGas(1541)
                .setSenderAccountId(AccountId.fromString("1.2.3"))
                .setFunctionParameters(new ContractFunctionParameters()
                        .addString("Hello")
                        .addString("world!")
                        .toBytes(null)
                        .toByteArray())
                .onMakeRequest(builder, QueryHeader.newBuilder().build());
        SnapshotMatcher.expect(builder.build().toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }
}
