// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenDeleteTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenDeleteTransactionTest {
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
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenDeleteTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private TokenDeleteTransaction spawnTestTransaction() {
        return new TokenDeleteTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTokenId(TokenId.fromString("1.2.3"))
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = TokenDeleteTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenDeletion(TokenDeleteTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenDeleteTransaction.class);
    }

    @Test
    void constructTokenDeleteTransaction() {
        var transaction = new TokenDeleteTransaction();

        assertThat(transaction.getTokenId()).isNull();
    }

    @Test
    void ConstructTokenDeleteTransactionFromTransactionBodyProtobuf() {
        var tokenId = TokenId.fromString("1.2.3");

        var transactionBody = TokenDeleteTransactionBody.newBuilder()
                .setToken(tokenId.toProtobuf())
                .build();
        var txBody =
                TransactionBody.newBuilder().setTokenDeletion(transactionBody).build();
        var tokenDeleteTransaction = new TokenDeleteTransaction(txBody);

        assertThat(tokenDeleteTransaction.getTokenId()).isEqualTo(tokenId);
    }

    @Test
    void getSetTokenId() {
        var tokenId = TokenId.fromString("1.2.3");

        var transaction = new TokenDeleteTransaction().setTokenId(tokenId);

        assertThat(transaction.getTokenId()).isEqualTo(tokenId);
    }

    @Test
    void getSetTokenIdFrozen() {
        var tokenId = TokenId.fromString("1.2.3");

        var tx = spawnTestTransaction();

        assertThrows(IllegalStateException.class, () -> tx.setTokenId(tokenId));
    }
}
