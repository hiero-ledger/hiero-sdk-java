// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AssessedCustomFeeTest {

    private static final int amount = 1;
    private static final TokenId tokenId = new TokenId(2, 3, 4);
    private static final AccountId feeCollector = new AccountId(5, 6, 7);
    private static final List<AccountId> payerAccountIds =
            List.of(new AccountId(8, 9, 10), new AccountId(11, 12, 13), new AccountId(14, 15, 16));

    private final org.hiero.sdk.proto.AssessedCustomFee fee = org.hiero.sdk.proto.AssessedCustomFee.newBuilder()
            .setAmount(amount)
            .setTokenId(tokenId.toProtobuf())
            .setFeeCollectorAccountId(feeCollector.toProtobuf())
            .addAllEffectivePayerAccountId(
                    payerAccountIds.stream().map(AccountId::toProtobuf).toList())
            .build();

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    AssessedCustomFee spawnAssessedCustomFeeExample() {
        return new AssessedCustomFee(
                201,
                TokenId.fromString("1.2.3"),
                AccountId.fromString("4.5.6"),
                List.of(AccountId.fromString("0.0.1"), AccountId.fromString("0.0.2"), AccountId.fromString("0.0.3")));
    }

    @Test
    void shouldSerialize() throws Exception {
        var originalAssessedCustomFee = spawnAssessedCustomFeeExample();
        byte[] assessedCustomFeeBytes = originalAssessedCustomFee.toBytes();
        var copyAssessedCustomFee = AssessedCustomFee.fromBytes(assessedCustomFeeBytes);
        assertThat(originalAssessedCustomFee.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .isEqualTo(copyAssessedCustomFee.toString().replaceAll("@[A-Za-z0-9]+", ""));
        SnapshotMatcher.expect(originalAssessedCustomFee.toString().replaceAll("@[A-Za-z0-9]+", ""))
                .toMatchSnapshot();
    }

    @Test
    void fromProtobuf() {
        SnapshotMatcher.expect(AssessedCustomFee.fromProtobuf(fee).toString()).toMatchSnapshot();
    }

    @Test
    void toProtobuf() {
        SnapshotMatcher.expect(AssessedCustomFee.fromProtobuf(fee).toProtobuf().toString())
                .toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws Exception {
        var assessedCustomFee = spawnAssessedCustomFeeExample();
        var tx2 = AssessedCustomFee.fromBytes(assessedCustomFee.toBytes());
        assertThat(tx2).hasToString(assessedCustomFee.toString());
    }
}
