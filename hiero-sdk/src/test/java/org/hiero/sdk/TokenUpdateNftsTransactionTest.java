// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.hiero.sdk.proto.SchedulableTransactionBody;
import org.hiero.sdk.proto.TokenUpdateNftsTransactionBody;
import org.hiero.sdk.proto.TransactionBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TokenUpdateNftsTransactionTest {
    private static final PrivateKey testMetadataKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final TokenId testTokenId = TokenId.fromString("4.2.0");
    private static final List<Long> testSerialNumbers = Arrays.asList(8L, 9L, 10L);
    private static final byte[] testMetadata = new byte[] {1, 2, 3, 4, 5};
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

    private TokenUpdateNftsTransaction spawnTestTransaction() {
        return new TokenUpdateNftsTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTokenId(testTokenId)
                .setMetadata(testMetadata)
                .setSerials(testSerialNumbers)
                .setMaxTransactionFee(new Hbar(1))
                .freeze()
                .sign(testMetadataKey);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TokenUpdateNftsTransaction();
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
                .setTokenUpdateNfts(TokenUpdateNftsTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TokenUpdateNftsTransaction.class);
    }

    @Test
    void constructTokenUpdateTransactionFromTransactionBodyProtobuf() {
        var transactionBody = TokenUpdateNftsTransactionBody.newBuilder()
                .setToken(testTokenId.toProtobuf())
                .setMetadata(BytesValue.of(ByteString.copyFrom(testMetadata)))
                .addAllSerialNumbers(testSerialNumbers)
                .build();

        var tx =
                TransactionBody.newBuilder().setTokenUpdateNfts(transactionBody).build();
        var tokenUpdateNftsTransaction = new TokenUpdateNftsTransaction(tx);

        assertThat(tokenUpdateNftsTransaction.getTokenId()).isEqualTo(testTokenId);
        assertThat(tokenUpdateNftsTransaction.getMetadata()).isEqualTo(testMetadata);
        assertThat(tokenUpdateNftsTransaction.getSerials()).isEqualTo(testSerialNumbers);
    }

    @Test
    void getSetTokenId() {
        var tokenUpdateNftsTransaction = new TokenUpdateNftsTransaction().setTokenId(testTokenId);
        assertThat(tokenUpdateNftsTransaction.getTokenId()).isEqualTo(testTokenId);
    }

    @Test
    void getSetTokenIdFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setTokenId(testTokenId));
    }

    @Test
    void getSetMetadata() {
        var tx = spawnTestTransaction();
        assertThat(tx.getMetadata()).isEqualTo(testMetadata);
    }

    @Test
    void getSetMetadataFrozen() {
        var tx = spawnTestTransaction();
        assertThrows(IllegalStateException.class, () -> tx.setMetadata(testMetadata));
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
