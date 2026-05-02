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
import org.hiero.sdk.proto.TokenAssociateTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenAssociateTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final AccountId accountId = AccountId.fromString("1.2.3");
    private static final List<TokenId> tokenIds =
            List.of(TokenId.fromString("4.5.6"), TokenId.fromString("7.8.9"), TokenId.fromString("10.11.12"));
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
        var tx = new TokenAssociateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private TokenAssociateTransaction spawnTestTransaction() {
        return new TokenAssociateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setAccountId(AccountId.fromString("0.0.222"))
                .setTokenIds(Collections.singletonList(TokenId.fromString("0.0.666")))
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = TokenAssociateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenAssociate(TokenAssociateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenAssociateTransaction.class);
    }

    @Test
    void constructTokenDeleteTransactionFromTransactionBodyProtobuf() {
        var transactionBody = TokenAssociateTransactionBody.newBuilder()
                .addAllTokens(tokenIds.stream().map(TokenId::toProtobuf).toList())
                .setAccount(accountId.toProtobuf())
                .build();
        var txBody =
                TransactionBody.newBuilder().setTokenAssociate(transactionBody).build();
        var tokenAssociateTransaction = new TokenAssociateTransaction(txBody);

        assertThat(tokenAssociateTransaction.getAccountId()).isEqualTo(accountId);
        assertThat(tokenAssociateTransaction.getTokenIds()).hasSize(tokenIds.size());
    }

    @Test
    void getSetAccountId() {
        var transaction = new TokenAssociateTransaction().setAccountId(accountId);
        assertThat(transaction.getAccountId()).isEqualTo(accountId);
    }

    @Test
    void getSetAccountIdFrozen() {
        var transaction = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> transaction.setAccountId(accountId));
    }

    @Test
    void getSetTokenIds() {
        var transaction = new TokenAssociateTransaction().setTokenIds(tokenIds);
        assertThat(transaction.getTokenIds()).isEqualTo(tokenIds);
    }

    @Test
    void getSetTokenIdFrozen() {
        var transaction = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> transaction.setTokenIds(tokenIds));
    }
}
