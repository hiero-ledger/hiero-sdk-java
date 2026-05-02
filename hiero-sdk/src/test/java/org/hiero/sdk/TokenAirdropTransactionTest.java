// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenAirdropTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenAirdropTransactionTest {
    private static final PrivateKey privateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    final Instant validStart = Instant.ofEpochSecond(1554158542);
    private TokenAirdropTransaction transaction;

    @BeforeEach
    public void setUp() {
        transaction = new TokenAirdropTransaction();
    }

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
        var tx = new TokenAirdropTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private TokenAirdropTransaction spawnTestTransaction() {
        return new TokenAirdropTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .addTokenTransfer(TokenId.fromString("0.0.5"), AccountId.fromString("0.0.5008"), 400)
                .addTokenTransferWithDecimals(TokenId.fromString("0.0.5"), AccountId.fromString("0.0.5006"), -800, 3)
                .addTokenTransferWithDecimals(TokenId.fromString("0.0.5"), AccountId.fromString("0.0.5007"), 400, 3)
                .addTokenTransfer(TokenId.fromString("0.0.4"), AccountId.fromString("0.0.5008"), 1)
                .addTokenTransfer(TokenId.fromString("0.0.4"), AccountId.fromString("0.0.5006"), -1)
                .addNftTransfer(
                        TokenId.fromString("0.0.3").nft(2),
                        AccountId.fromString("0.0.5008"),
                        AccountId.fromString("0.0.5007"))
                .addNftTransfer(
                        TokenId.fromString("0.0.3").nft(1),
                        AccountId.fromString("0.0.5008"),
                        AccountId.fromString("0.0.5007"))
                .addNftTransfer(
                        TokenId.fromString("0.0.3").nft(3),
                        AccountId.fromString("0.0.5008"),
                        AccountId.fromString("0.0.5006"))
                .addNftTransfer(
                        TokenId.fromString("0.0.3").nft(4),
                        AccountId.fromString("0.0.5007"),
                        AccountId.fromString("0.0.5006"))
                .addNftTransfer(
                        TokenId.fromString("0.0.2").nft(4),
                        AccountId.fromString("0.0.5007"),
                        AccountId.fromString("0.0.5006"))
                .addApprovedTokenTransfer(TokenId.fromString("0.0.4"), AccountId.fromString("0.0.5006"), 123)
                .addApprovedNftTransfer(
                        new NftId(TokenId.fromString("0.0.4"), 4),
                        AccountId.fromString("0.0.5005"),
                        AccountId.fromString("0.0.5006"))
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .freeze()
                .sign(privateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = TokenAirdropTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void decimalsMustBeConsistent() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            new TokenAirdropTransaction()
                    .addTokenTransferWithDecimals(TokenId.fromString("0.0.5"), AccountId.fromString("0.0.8"), 100, 2)
                    .addTokenTransferWithDecimals(TokenId.fromString("0.0.5"), AccountId.fromString("0.0.7"), -100, 3);
        });
    }

    @Test
    void canGetDecimals() {
        var tx = new TokenAirdropTransaction();
        assertThat(tx.getTokenIdDecimals().get(TokenId.fromString("0.0.5"))).isNull();
        tx.addTokenTransfer(TokenId.fromString("0.0.5"), AccountId.fromString("0.0.8"), 100);
        assertThat(tx.getTokenIdDecimals().get(TokenId.fromString("0.0.5"))).isNull();
        tx.addTokenTransferWithDecimals(TokenId.fromString("0.0.5"), AccountId.fromString("0.0.7"), -100, 5);
        assertThat(tx.getTokenIdDecimals().get(TokenId.fromString("0.0.5"))).isEqualTo(5);
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenAirdrop(TokenAirdropTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenAirdropTransaction.class);
    }

    @Test
    void testDefaultMaxTransactionFeeIsSet() {
        assertEquals(
                new Hbar(1), transaction.getDefaultMaxTransactionFee(), "Default max transaction fee should be 1 Hbar");
    }

    @Test
    void testAddTokenTransfer() {
        TokenId tokenId = new TokenId(0, 0, 123);
        AccountId accountId = new AccountId(0, 0, 456);
        long value = 1000L;

        transaction.addTokenTransfer(tokenId, accountId, value);

        Map<TokenId, Map<AccountId, Long>> tokenTransfers = transaction.getTokenTransfers();
        assertTrue(tokenTransfers.containsKey(tokenId));
        assertEquals(1, tokenTransfers.get(tokenId).size());
        assertEquals(value, tokenTransfers.get(tokenId).get(accountId));
    }

    @Test
    void testAddApprovedTokenTransfer() {
        TokenId tokenId = new TokenId(0, 0, 123);
        AccountId accountId = new AccountId(0, 0, 456);
        long value = 1000L;

        transaction.addApprovedTokenTransfer(tokenId, accountId, value);

        Map<TokenId, Map<AccountId, Long>> tokenTransfers = transaction.getTokenTransfers();
        assertTrue(tokenTransfers.containsKey(tokenId));
        assertEquals(1, tokenTransfers.get(tokenId).size());
        assertEquals(value, tokenTransfers.get(tokenId).get(accountId));
    }

    @Test
    void testAddNftTransfer() {
        NftId nftId = new NftId(new TokenId(0, 0, 123), 1);
        AccountId sender = new AccountId(0, 0, 456);
        AccountId receiver = new AccountId(0, 0, 789);

        transaction.addNftTransfer(nftId, sender, receiver);

        Map<TokenId, List<TokenNftTransfer>> nftTransfers = transaction.getTokenNftTransfers();
        assertTrue(nftTransfers.containsKey(nftId.tokenId));
        assertEquals(1, nftTransfers.get(nftId.tokenId).size());
        assertEquals(sender, nftTransfers.get(nftId.tokenId).get(0).sender);
        assertEquals(receiver, nftTransfers.get(nftId.tokenId).get(0).receiver);
    }

    @Test
    void testAddApprovedNftTransfer() {
        NftId nftId = new NftId(new TokenId(0, 0, 123), 1);
        AccountId sender = new AccountId(0, 0, 456);
        AccountId receiver = new AccountId(0, 0, 789);

        transaction.addApprovedNftTransfer(nftId, sender, receiver);

        Map<TokenId, List<TokenNftTransfer>> nftTransfers = transaction.getTokenNftTransfers();
        assertTrue(nftTransfers.containsKey(nftId.tokenId));
        assertEquals(1, nftTransfers.get(nftId.tokenId).size());
        assertEquals(sender, nftTransfers.get(nftId.tokenId).get(0).sender);
        assertEquals(receiver, nftTransfers.get(nftId.tokenId).get(0).receiver);
    }

    @Test
    void testGetTokenIdDecimals() {
        TokenId tokenId = new TokenId(0, 0, 123);
        AccountId accountId = new AccountId(0, 0, 456);
        long value = 1000L;
        int decimals = 8;

        transaction.addTokenTransferWithDecimals(tokenId, accountId, value, decimals);

        Map<TokenId, Integer> decimalsMap = transaction.getTokenIdDecimals();
        assertTrue(decimalsMap.containsKey(tokenId));
        assertEquals(decimals, decimalsMap.get(tokenId));
    }

    @Test
    void testBuildTransactionBody() {
        TokenAirdropTransactionBody.Builder builder = spawnTestTransaction().build();

        assertNotNull(builder);
    }

    @Test
    void testGetMethodDescriptor() {
        assertEquals(TokenServiceGrpc.getAirdropTokensMethod(), transaction.getMethodDescriptor());
    }
}
