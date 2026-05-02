// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenClaimAirdropTransactionBody;
import org.hiero.sdk.proto.TokenServiceGrpc;
import org.hiero.sdk.proto.TransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenClaimAirdropTransactionTest {

    private static final PrivateKey privateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    final Instant validStart = Instant.ofEpochSecond(1554158542);
    private TokenClaimAirdropTransaction transaction;

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    private TokenClaimAirdropTransaction spawnTestTransaction() {
        List<PendingAirdropId> pendingAirdropIds = new ArrayList<>();
        pendingAirdropIds.add(
                new PendingAirdropId(new AccountId(0, 0, 457), new AccountId(0, 0, 456), new TokenId(0, 0, 123)));
        pendingAirdropIds.add(new PendingAirdropId(
                new AccountId(0, 0, 457), new AccountId(0, 0, 456), new NftId(new TokenId(0, 0, 1234), 123)));

        return new TokenClaimAirdropTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setPendingAirdropIds(pendingAirdropIds)
                .freeze()
                .sign(privateKey);
    }

    @Test
    void shouldSerialize() {
        SnapshotMatcher.expect(spawnTestTransaction().toString()).toMatchSnapshot();
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenClaimAirdropTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @BeforeEach
    public void setUp() {
        transaction = new TokenClaimAirdropTransaction();
    }

    @Test
    void testConstructorSetsDefaultMaxTransactionFee() {
        Assertions.assertEquals(Hbar.from(1), transaction.getDefaultMaxTransactionFee());
    }

    @Test
    void testGetAndSetPendingAirdropIds() {
        List<PendingAirdropId> pendingAirdropIds = new ArrayList<>();
        pendingAirdropIds.add(
                new PendingAirdropId(new AccountId(0, 0, 457), new AccountId(0, 0, 456), new TokenId(0, 0, 123)));
        pendingAirdropIds.add(new PendingAirdropId(
                new AccountId(0, 0, 457), new AccountId(0, 0, 456), new NftId(new TokenId(0, 0, 1234), 123)));

        transaction.setPendingAirdropIds(pendingAirdropIds);

        Assertions.assertEquals(pendingAirdropIds, transaction.getPendingAirdropIds());
    }

    @Test
    void testSetPendingAirdropIdsNullThrowsException() {
        Assertions.assertThrows(NullPointerException.class, () -> transaction.setPendingAirdropIds(null));
    }

    @Test
    void testClearPendingAirdropIds() {
        List<PendingAirdropId> pendingAirdropIds = new ArrayList<>();
        PendingAirdropId pendingAirdropId =
                new PendingAirdropId(new AccountId(0, 0, 457), new AccountId(0, 0, 456), new TokenId(0, 0, 123));
        pendingAirdropIds.add(pendingAirdropId);

        transaction.setPendingAirdropIds(pendingAirdropIds);
        transaction.clearPendingAirdropIds();

        Assertions.assertTrue(transaction.getPendingAirdropIds().isEmpty());
    }

    @Test
    void testAddAllPendingAirdrops() {
        PendingAirdropId pendingAirdropId1 =
                new PendingAirdropId(new AccountId(0, 0, 457), new AccountId(0, 0, 456), new TokenId(0, 0, 123));
        PendingAirdropId pendingAirdropId2 =
                new PendingAirdropId(new AccountId(0, 0, 458), new AccountId(0, 0, 459), new TokenId(0, 0, 123));

        transaction.addPendingAirdrop(pendingAirdropId1);
        transaction.addPendingAirdrop(pendingAirdropId2);

        Assertions.assertEquals(2, transaction.getPendingAirdropIds().size());
        Assertions.assertTrue(transaction.getPendingAirdropIds().contains(pendingAirdropId1));
        Assertions.assertTrue(transaction.getPendingAirdropIds().contains(pendingAirdropId2));
    }

    @Test
    void testAddAllPendingAirdropsNullThrowsException() {
        Assertions.assertThrows(NullPointerException.class, () -> transaction.addPendingAirdrop(null));
    }

    @Test
    void testBuildTransactionBody() {
        PendingAirdropId pendingAirdropId = new PendingAirdropId(
                new AccountId(0, 0, 457), new AccountId(0, 0, 456), new NftId(new TokenId(0, 0, 1234), 123));
        transaction.addPendingAirdrop(pendingAirdropId);

        TokenClaimAirdropTransactionBody.Builder builder = transaction.build();
        Assertions.assertEquals(1, builder.getPendingAirdropsCount());
        Assertions.assertEquals(pendingAirdropId.toProtobuf(), builder.getPendingAirdrops(0));
    }

    @Test
    void testGetMethodDescriptor() {
        Assertions.assertEquals(TokenServiceGrpc.getClaimAirdropMethod(), transaction.getMethodDescriptor());
    }

    @Test
    void testOnFreeze() {
        var bodyBuilder = TransactionBody.newBuilder();
        transaction.onFreeze(bodyBuilder);

        Assertions.assertTrue(bodyBuilder.hasTokenClaimAirdrop());
    }

    @Test
    void testOnScheduled() {
        SchedulableTransactionBody.Builder scheduled = SchedulableTransactionBody.newBuilder();
        transaction.onScheduled(scheduled);

        Assertions.assertTrue(scheduled.hasTokenClaimAirdrop());
    }

    @Test
    void testFromScheduledTransaction() {
        var pending = new PendingAirdropId(new AccountId(0, 0, 457), new AccountId(0, 0, 456), new TokenId(0, 0, 123));

        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenClaimAirdrop(TokenClaimAirdropTransactionBody.newBuilder()
                        .addPendingAirdrops(pending.toProtobuf())
                        .build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenClaimAirdropTransaction.class);
        assertThat(((TokenClaimAirdropTransaction) tx).getPendingAirdropIds()).isNotEmpty();
        assertThat(((TokenClaimAirdropTransaction) tx).getPendingAirdropIds()).hasSize(1);
    }
}

