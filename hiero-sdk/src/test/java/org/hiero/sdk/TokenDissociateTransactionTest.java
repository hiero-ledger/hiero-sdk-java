// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenDissociateTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenDissociateTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final AccountId testAccountId = AccountId.fromString("6.9.0");

    private static final List<TokenId> testTokenIds =
            Arrays.asList(TokenId.fromString("4.2.0"), TokenId.fromString("4.2.1"), TokenId.fromString("4.2.2"));

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
        var tx = new TokenDissociateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private TokenDissociateTransaction spawnTestTransaction() {
        return new TokenDissociateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setAccountId(testAccountId)
                .setTokenIds(testTokenIds)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = TokenDissociateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenDissociate(TokenDissociateTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenDissociateTransaction.class);
    }

    @Test
    void constructTokenDissociateTransactionFromTransactionBodyProtobuf() {
        var transactionBody = TokenDissociateTransactionBody.newBuilder()
                .setAccount(testAccountId.toProtobuf())
                .addAllTokens(testTokenIds.stream().map(TokenId::toProtobuf).toList())
                .build();

        var tx =
                TransactionBody.newBuilder().setTokenDissociate(transactionBody).build();
        var tokenDissociateTransaction = new TokenDissociateTransaction(tx);

        assertThat(tokenDissociateTransaction.getAccountId()).isEqualTo(testAccountId);
        assertThat(tokenDissociateTransaction.getTokenIds().size()).isEqualTo(testTokenIds.size());
    }

    @Test
    void getSetAccountId() {
        var tokenDissociateTransaction = new TokenDissociateTransaction().setAccountId(testAccountId);
        assertThat(tokenDissociateTransaction.getAccountId()).isEqualTo(testAccountId);
    }

    @Test
    void getSetAccountIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setAccountId(testAccountId));
    }

    @Test
    void getSetTokenIds() {
        var tokenDissociateTransaction = new TokenDissociateTransaction().setTokenIds(testTokenIds);
        assertThat(tokenDissociateTransaction.getTokenIds()).isEqualTo(testTokenIds);
    }

    @Test
    void getSetTokenIdsFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setTokenIds(testTokenIds));
    }
}
