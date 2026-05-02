// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import org.hiero.sdk.proto.CryptoDeleteAllowanceTransactionBody;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AccountAllowanceDeleteTransactionTest {
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

    AccountAllowanceDeleteTransaction spawnTestTransaction() {
        var ownerId = AccountId.fromString("5.6.7");
        return new AccountAllowanceDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .deleteAllHbarAllowances(ownerId)
                .deleteAllTokenAllowances(TokenId.fromString("2.2.2"), ownerId)
                .deleteAllTokenNftAllowances(TokenId.fromString("4.4.4").nft(123), ownerId)
                .deleteAllTokenNftAllowances(TokenId.fromString("4.4.4").nft(456), ownerId)
                .deleteAllTokenNftAllowances(TokenId.fromString("8.8.8").nft(456), ownerId)
                .deleteAllTokenNftAllowances(TokenId.fromString("4.4.4").nft(789), ownerId)
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = AccountAllowanceDeleteTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new AccountAllowanceDeleteTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setCryptoDeleteAllowance(
                        CryptoDeleteAllowanceTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(AccountAllowanceDeleteTransaction.class);
    }
}
