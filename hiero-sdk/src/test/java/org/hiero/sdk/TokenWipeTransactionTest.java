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
import org.hiero.sdk.proto.TokenWipeAccountTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenWipeTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final AccountId testAccountId = AccountId.fromString("0.6.9");
    private static final TokenId testTokenId = TokenId.fromString("4.2.0");
    private static final long testAmount = 4L;
    private static final List<Long> testSerialNumbers = Arrays.asList(8L, 9L, 10L);
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

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenWipeTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private TokenWipeTransaction spawnTestTransaction() {
        return new TokenWipeTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTokenId(TokenId.fromString("0.0.111"))
                .setAccountId(testAccountId)
                .setAmount(testAmount)
                .setSerials(testSerialNumbers)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldSerializeNft() {
        SnapshotMatcher.expect(spawnTestTransactionNft().toString()).toMatchSnapshot();
    }

    private TokenWipeTransaction spawnTestTransactionNft() {
        return new TokenWipeTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTokenId(TokenId.fromString("0.0.111"))
                .setAccountId(testAccountId)
                .setSerials(Collections.singletonList(444L))
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytesFungible() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = TokenWipeTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytesNft() throws Exception {
        var tx = spawnTestTransactionNft();
        var tx2 = TokenWipeTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenWipe(TokenWipeAccountTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenWipeTransaction.class);
    }

    @Test
    void constructTokenWipeTransactionFromTransactionBodyProtobuf() {
        var transactionBody = TokenWipeAccountTransactionBody.newBuilder()
                .setToken(testTokenId.toProtobuf())
                .setAccount(testAccountId.toProtobuf())
                .setAmount(testAmount)
                .addAllSerialNumbers(testSerialNumbers)
                .build();

        var txBody = TransactionBody.newBuilder().setTokenWipe(transactionBody).build();
        var tokenWipeTransaction = new TokenWipeTransaction(txBody);

        assertThat(tokenWipeTransaction.getTokenId()).isEqualTo(testTokenId);
        assertThat(tokenWipeTransaction.getAccountId()).isEqualTo(testAccountId);
        assertThat(tokenWipeTransaction.getAmount()).isEqualTo(testAmount);
        assertThat(tokenWipeTransaction.getSerials()).isEqualTo(testSerialNumbers);
    }

    @Test
    void getSetTokenId() {
        var tokenWipeTransaction = new TokenWipeTransaction().setTokenId(testTokenId);
        assertThat(tokenWipeTransaction.getTokenId()).isEqualTo(testTokenId);
    }

    @Test
    void getSetTokenIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setTokenId(testTokenId));
    }

    @Test
    void getSetAccountId() {
        var tx = spawnTestTransaction();
        assertThat(tx.getAccountId()).isEqualTo(testAccountId);
    }

    @Test
    void getSetAccountIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAccountId(testAccountId));
    }

    @Test
    void getSetAmount() {
        var tx = spawnTestTransaction();
        assertThat(tx.getAmount()).isEqualTo(testAmount);
    }

    @Test
    void getSetAmountFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAmount(testAmount));
    }

    @Test
    void getSetSerialNumbers() {
        var tx = spawnTestTransaction();
        assertThat(tx.getSerials()).isEqualTo(testSerialNumbers);
    }

    @Test
    void getSetSerialNumbersFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setSerials(testSerialNumbers));
    }
}
