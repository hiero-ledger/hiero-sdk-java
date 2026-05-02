// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.Iterables;
import com.google.protobuf.ByteString;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenMintTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenMintTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final TokenId testTokenId = TokenId.fromString("4.2.0");
    private static final Long testAmount = 10L;
    private static final List<byte[]> testMetadataList = List.of(new byte[] {1, 2, 3, 4, 5});
    private static final ByteString testMetadataByteString = ByteString.copyFrom(new byte[] {1, 2, 3, 4, 5});
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
    void shouldSerializeMetadata() {
        SnapshotMatcher.expect(spawnMetadataTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenMintTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private TokenMintTransaction spawnTestTransaction() {
        return new TokenMintTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTokenId(testTokenId)
                .setAmount(testAmount)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    private TokenMintTransaction spawnMetadataTestTransaction() {
        return new TokenMintTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTokenId(TokenId.fromString("1.2.3"))
                .setMetadata(testMetadataList)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = TokenUpdateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesMetadata() throws Exception {
        var tx = spawnMetadataTestTransaction();
        var tx2 = TokenUpdateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenMint(TokenMintTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenMintTransaction.class);
    }

    @Test
    void constructTokenMintTransactionFromTransactionBodyProtobuf() {
        var transactionBody = TokenMintTransactionBody.newBuilder()
                .setToken(testTokenId.toProtobuf())
                .setAmount(testAmount)
                .addMetadata(testMetadataByteString)
                .build();

        var tx = TransactionBody.newBuilder().setTokenMint(transactionBody).build();
        var tokenMintTransaction = new TokenMintTransaction(tx);

        assertThat(tokenMintTransaction.getTokenId()).isEqualTo(testTokenId);
        assertThat(tokenMintTransaction.getAmount()).isEqualTo(testAmount);
        assertThat(Iterables.getLast(tokenMintTransaction.getMetadata()))
                .isEqualTo(testMetadataByteString.toByteArray());
    }

    @Test
    void getSetTokenId() {
        var tokenMintTransaction = new TokenMintTransaction().setTokenId(testTokenId);
        assertThat(tokenMintTransaction.getTokenId()).isEqualTo(testTokenId);
    }

    @Test
    void getSetTokenIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setTokenId(testTokenId));
    }

    @Test
    void getSetAmount() {
        var tokenMintTransaction = new TokenMintTransaction().setAmount(testAmount);
        assertThat(tokenMintTransaction.getAmount()).isEqualTo(testAmount);
    }

    @Test
    void getSetAmountFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAmount(testAmount));
    }

    @Test
    void getSetMetadata() {
        var tokenMintTransaction = new TokenMintTransaction().setMetadata(testMetadataList);
        assertThat(tokenMintTransaction.getMetadata()).isEqualTo(testMetadataList);
    }

    @Test
    void getSetMetadataFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setMetadata(testMetadataList));
    }

    @Test
    void addMetadata() {
        var tokenMintTransaction = new TokenMintTransaction().addMetadata(Iterables.getLast(testMetadataList));
        assertThat(Iterables.getLast(tokenMintTransaction.getMetadata()))
                .isEqualTo(Iterables.getLast(testMetadataList));
    }
}
