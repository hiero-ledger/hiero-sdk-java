// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenBurnTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenBurnTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final TokenId testTokenId = TokenId.fromString("4.2.0");
    private static final long testAmount = 69L;
    private static final List<Long> testSerials = Collections.singletonList(420L);
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
    void shouldSerializeFungible() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    private TokenBurnTransaction spawnTestTransaction() {
        return new TokenBurnTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTokenId(testTokenId)
                .setAmount(testAmount)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenBurnTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldSerializeNft() {
        SnapshotMatcher.expect(spawnTestTransactionNft().toString()).toMatchSnapshot();
    }

    private TokenBurnTransaction spawnTestTransactionNft() {
        return new TokenBurnTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTokenId(testTokenId)
                .setSerials(testSerials)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytesFungible() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = TokenBurnTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNft() throws Exception {
        var tx = spawnTestTransactionNft();
        var tx2 = TokenBurnTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenBurn(TokenBurnTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenBurnTransaction.class);
    }

    @Test
    void constructTokenBurnTransactionFromTransactionBodyProtobuf() {
        var transactionBody = TokenBurnTransactionBody.newBuilder()
                .setToken(testTokenId.toProtobuf())
                .setAmount(testAmount)
                .addAllSerialNumbers(testSerials)
                .build();

        var tx = TransactionBody.newBuilder().setTokenBurn(transactionBody).build();
        var tokenBurnTransaction = new TokenBurnTransaction(tx);

        assertThat(tokenBurnTransaction.getTokenId()).isEqualTo(testTokenId);
        assertThat(tokenBurnTransaction.getAmount()).isEqualTo(testAmount);
        assertThat(tokenBurnTransaction.getSerials()).isEqualTo(testSerials);
    }

    @Test
    void getSetTokenId() {
        var tokenBurnTransaction = new TokenBurnTransaction().setTokenId(testTokenId);
        assertThat(tokenBurnTransaction.getTokenId()).isEqualTo(testTokenId);
    }

    @Test
    void getSetTokenIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setTokenId(testTokenId));
    }

    @Test
    void getSetAmount() {
        var tokenBurnTransaction = new TokenBurnTransaction().setAmount(testAmount);
        assertThat(tokenBurnTransaction.getAmount()).isEqualTo(testAmount);
    }

    @Test
    void getSetAmountFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAmount(testAmount));
    }

    @Test
    void getSetSerials() {
        var tokenBurnTransaction = new TokenBurnTransaction().setSerials(testSerials);
        assertThat(tokenBurnTransaction.getSerials()).isEqualTo(testSerials);
    }

    @Test
    void getSetSerialsFrozen() {
        var tx = spawnTestTransactionNft();
        assertThrows(IllegalStateException.class, () -> tx.setSerials(testSerials));
    }
}
