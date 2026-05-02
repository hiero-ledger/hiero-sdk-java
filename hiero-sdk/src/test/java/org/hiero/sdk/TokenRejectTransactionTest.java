// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenReference;
import org.hiero.sdk.proto.TokenRejectTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenRejectTransactionTest {

    private static final PrivateKey TEST_PRIVATE_KEY = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

    private static final AccountId TEST_OWNER_ID = AccountId.fromString("0.6.9");

    private static final List<TokenId> TEST_TOKEN_IDS =
            List.of(TokenId.fromString("1.2.3"), TokenId.fromString("4.5.6"), TokenId.fromString("7.8.9"));

    private static final List<NftId> TEST_NFT_IDS =
            List.of(new NftId(TokenId.fromString("4.5.6"), 2), new NftId(TokenId.fromString("7.8.9"), 3));

    final Instant TEST_VALID_START = Instant.ofEpochSecond(1554158542);

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

    private TokenRejectTransaction spawnTestTransaction() {
        return new TokenRejectTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), TEST_VALID_START))
                .setOwnerId(TEST_OWNER_ID)
                .setTokenIds(TEST_TOKEN_IDS)
                .setNftIds(TEST_NFT_IDS)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(TEST_PRIVATE_KEY);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenRejectTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = TokenUpdateNftsTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setTokenReject(TokenRejectTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenRejectTransaction.class);
    }

    @Test
    void constructTokenRejectTransactionFromTransactionBodyProtobuf() {
        var transactionBodyBuilder = TokenRejectTransactionBody.newBuilder();

        transactionBodyBuilder.setOwner(TEST_OWNER_ID.toProtobuf());

        for (TokenId tokenId : TEST_TOKEN_IDS) {
            transactionBodyBuilder.addRejections(TokenReference.newBuilder()
                    .setFungibleToken(tokenId.toProtobuf())
                    .build());
        }

        for (NftId nftId : TEST_NFT_IDS) {
            transactionBodyBuilder.addRejections(
                    TokenReference.newBuilder().setNft(nftId.toProtobuf()).build());
        }

        var tx = TransactionBody.newBuilder()
                .setTokenReject(transactionBodyBuilder.build())
                .build();
        var tokenRejectTransaction = new TokenRejectTransaction(tx);

        assertThat(tokenRejectTransaction.getOwnerId()).isEqualTo(TEST_OWNER_ID);
        assertThat(tokenRejectTransaction.getTokenIds()).hasSize(TEST_TOKEN_IDS.size());
        assertThat(tokenRejectTransaction.getNftIds()).hasSize(TEST_NFT_IDS.size());
    }

    @Test
    void getSetOwnerId() {
        var transaction = new TokenRejectTransaction().setOwnerId(TEST_OWNER_ID);
        assertThat(transaction.getOwnerId()).isEqualTo(TEST_OWNER_ID);
    }

    @Test
    void getSetOwnerIdFrozen() {
        var transaction = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> transaction.setOwnerId(TEST_OWNER_ID));
    }

    @Test
    void getSetTokenIds() {
        var transaction = new TokenRejectTransaction().setTokenIds(TEST_TOKEN_IDS);
        assertThat(transaction.getTokenIds()).isEqualTo(TEST_TOKEN_IDS);
    }

    @Test
    void getSetTokenIdFrozen() {
        var transaction = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> transaction.setTokenIds(TEST_TOKEN_IDS));
    }

    @Test
    void getSetNftIds() {
        var transaction = new TokenRejectTransaction().setNftIds(TEST_NFT_IDS);
        assertThat(transaction.getNftIds()).isEqualTo(TEST_NFT_IDS);
    }

    @Test
    void getSetNftIdFrozen() {
        var transaction = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> transaction.setNftIds(TEST_NFT_IDS));
    }
}
