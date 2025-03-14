// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TokenFeeScheduleUpdateTransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenFeeScheduleUpdateTransactionTest {
    final Instant validStart = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    private TokenFeeScheduleUpdateTransaction spawnTestTransaction() {
        var customFees = new ArrayList<CustomFee>();
        customFees.add(new CustomFixedFee()
                .setFeeCollectorAccountId(new AccountId(0, 0, 4322))
                .setDenominatingTokenId(new TokenId(0, 0, 483902))
                .setAmount(10));
        customFees.add(new CustomFractionalFee()
                .setFeeCollectorAccountId(new AccountId(0, 0, 389042))
                .setNumerator(3)
                .setDenominator(7)
                .setMin(3)
                .setMax(100)
                .setAssessmentMethod(FeeAssessmentMethod.EXCLUSIVE));

        return new TokenFeeScheduleUpdateTransaction()
                .setTokenId(new TokenId(0, 0, 8798))
                .setCustomFees(customFees)
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .freeze();
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenFeeScheduleUpdateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldSerialize() throws InvalidProtocolBufferException {
        var originalUpdate = spawnTestTransaction();
        byte[] updateBytes = originalUpdate.toBytes();
        var copyUpdate = TokenFeeScheduleUpdateTransaction.fromBytes(updateBytes);
        assertThat(copyUpdate.toString()).isEqualTo(originalUpdate.toString());
        SnapshotMatcher.expect(originalUpdate.toString()).toMatchSnapshot();
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenFeeScheduleUpdate(
                        TokenFeeScheduleUpdateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenFeeScheduleUpdateTransaction.class);
    }
}
