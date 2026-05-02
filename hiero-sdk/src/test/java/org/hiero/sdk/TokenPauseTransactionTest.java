// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenPauseTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenPauseTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final TokenId testTokenId = TokenId.fromString("4.2.0");
    final Instant validStart = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    TokenPauseTransaction spawnTestTransaction() {
        return new TokenPauseTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTokenId(testTokenId)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytesNft() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenPauseTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenPause(TokenPauseTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenPauseTransaction.class);
    }

    @Test
    void constructTokenPauseTransactionFromTransactionBodyProtobuf() {
        var transactionBody = TokenPauseTransactionBody.newBuilder()
                .setToken(testTokenId.toProtobuf())
                .build();

        var tx = TransactionBody.newBuilder().setTokenPause(transactionBody).build();
        var tokenPauseTransaction = new TokenPauseTransaction(tx);

        assertThat(tokenPauseTransaction.getTokenId()).isEqualTo(testTokenId);
    }

    @Test
    void getSetTokenId() {
        var tokenPauseTransaction = new TokenPauseTransaction().setTokenId(testTokenId);
        assertThat(tokenPauseTransaction.getTokenId()).isEqualTo(testTokenId);
    }

    @Test
    void getSetTokenIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setTokenId(testTokenId));
    }
}
