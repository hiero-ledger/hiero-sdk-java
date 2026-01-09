// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TransactionIdTest {
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
        SnapshotMatcher.expect(TransactionId.fromString("0.0.23847@1588539964.632521325")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void shouldSerialize2() {
        SnapshotMatcher.expect(TransactionId.fromString("0.0.23847@1588539964.632521325?scheduled/3")
                        .toString())
                .toMatchSnapshot();
    }

    @Test
    void shouldToBytes() {
        var originalId = TransactionId.fromString("0.0.23847@1588539964.632521325");
        var copyId = TransactionId.fromProtobuf(originalId.toProtobuf());
        assertThat(copyId).hasToString(originalId.toString());
    }

    @Test
    void shouldToBytes2() {
        var originalId = TransactionId.fromString("0.0.23847@1588539964.632521325?scheduled/2");
        var copyId = TransactionId.fromProtobuf(originalId.toProtobuf());
        assertThat(copyId).hasToString(originalId.toString());
    }

    @Test
    void shouldFromBytes() throws InvalidProtocolBufferException {
        var originalId = TransactionId.fromString("0.0.23847@1588539964.632521325");
        var copyId = TransactionId.fromBytes(originalId.toProtobuf().toByteArray());
        assertThat(copyId).hasToString(originalId.toString());
    }

    @Test
    void shouldParse() {
        var transactionId = TransactionId.fromString("0.0.23847@1588539964.632521325");
        var accountId = Objects.requireNonNull(transactionId.accountId);
        var validStart = Objects.requireNonNull(transactionId.validStart);
        assertThat(accountId.shard).isEqualTo(0);
        assertThat(accountId.num).isEqualTo(23847);
        assertThat(validStart.getEpochSecond()).isEqualTo(1588539964);
        assertThat(validStart.getNano()).isEqualTo(632521325);
    }

    @Test
    void shouldParseScheduled() {
        var transactionId = TransactionId.fromString("0.0.23847@1588539964.632521325?scheduled");
        var accountId = Objects.requireNonNull(transactionId.accountId);
        var validStart = Objects.requireNonNull(transactionId.validStart);
        assertThat(accountId.shard).isEqualTo(0);
        assertThat(accountId.num).isEqualTo(23847);
        assertThat(validStart.getEpochSecond()).isEqualTo(1588539964);
        assertThat(validStart.getNano()).isEqualTo(632521325);
        assertThat(transactionId.getScheduled()).isTrue();
        assertThat(transactionId.getNonce()).isNull();

        assertThat(transactionId.toString()).isEqualTo("0.0.23847@1588539964.632521325?scheduled");
    }

    @Test
    void shouldParseNonce() {
        var transactionId = TransactionId.fromString("0.0.23847@1588539964.632521325/4");
        var accountId = Objects.requireNonNull(transactionId.accountId);
        var validStart = Objects.requireNonNull(transactionId.validStart);
        assertThat(accountId.shard).isEqualTo(0);
        assertThat(accountId.num).isEqualTo(23847);
        assertThat(validStart.getEpochSecond()).isEqualTo(1588539964);
        assertThat(validStart.getNano()).isEqualTo(632521325);
        assertThat(transactionId.getScheduled()).isFalse();
        assertThat(transactionId.getNonce()).isEqualTo(4);

        assertThat(transactionId.toString()).isEqualTo("0.0.23847@1588539964.632521325/4");
    }

    @Test
    void compare() {
        // Compare when only one of the txs is schedules
        var transactionId1 = TransactionId.fromString("0.0.23847@1588539964.632521325");
        var transactionId2 = TransactionId.fromString("0.0.23847@1588539964.632521325?scheduled");
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(-1);

        transactionId1 = TransactionId.fromString("0.0.23847@1588539964.632521325?scheduled");
        transactionId2 = TransactionId.fromString("0.0.23847@1588539964.632521325");
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(1);

        // Compare when only one of the txs has accountId
        transactionId1 = new TransactionId(null, Instant.ofEpochSecond(1588539964));
        transactionId2 = new TransactionId(AccountId.fromString("0.0.23847"), Instant.ofEpochSecond(1588539964));
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(-1);

        transactionId1 = new TransactionId(AccountId.fromString("0.0.23847"), Instant.ofEpochSecond(1588539964));
        transactionId2 = new TransactionId(null, Instant.ofEpochSecond(1588539964));
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(1);

        // Compare the AccountIds
        transactionId1 = TransactionId.fromString("0.0.23847@1588539964.632521325");
        transactionId2 = TransactionId.fromString("0.0.23847@1588539964.632521325");
        assertThat(transactionId1).isEqualByComparingTo(transactionId2);

        transactionId1 = TransactionId.fromString("0.0.23848@1588539964.632521325");
        transactionId2 = TransactionId.fromString("0.0.23847@1588539964.632521325");
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(1);

        transactionId1 = TransactionId.fromString("0.0.23847@1588539964.632521325");
        transactionId2 = TransactionId.fromString("0.0.23848@1588539964.632521325");
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(-1);

        // Compare when only one of the txs has valid start
        transactionId1 = new TransactionId(null, null);
        transactionId2 = new TransactionId(null, Instant.ofEpochSecond(1588539964));
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(-1);

        transactionId1 = new TransactionId(AccountId.fromString("0.0.23847"), Instant.ofEpochSecond(1588539964));
        transactionId2 = new TransactionId(null, null);
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(1);

        // Compare the validStarts
        transactionId1 = new TransactionId(null, Instant.ofEpochSecond(1588539965));
        transactionId2 = new TransactionId(null, Instant.ofEpochSecond(1588539964));
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(1);

        transactionId1 = new TransactionId(null, Instant.ofEpochSecond(1588539964));
        transactionId2 = new TransactionId(null, Instant.ofEpochSecond(1588539965));
        assertThat(transactionId1.compareTo(transactionId2)).isEqualTo(-1);

        transactionId1 = new TransactionId(null, null);
        transactionId2 = new TransactionId(null, null);
        assertThat(transactionId1).isEqualByComparingTo(transactionId2);
    }

    @Test
    void shouldFail() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> TransactionId.fromString("0.0.23847.1588539964.632521325/4"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> TransactionId.fromString("0.0.23847@1588539964/4"));
    }

    @Test
    void shouldAddTrailingZeroesToNanoseconds() {
        var txIdString = "0.0.4163533@1681876267.054802581";
        var txId = TransactionId.fromString(txIdString);
        assertThat(txId).hasToString(txIdString);
    }

    @Test
    void equalsHashCodeContractWithNonce() {
        AccountId accountId = new AccountId(0, 0, 1000);
        Instant now = Instant.now();

        TransactionId txnId1 = TransactionId.withValidStart(accountId, now);
        TransactionId txnId2 = TransactionId.withValidStart(accountId, now);

        txnId2.setNonce(0);

        assertFalse(
                txnId1.equals(txnId2) && txnId1.hashCode() != txnId2.hashCode(),
                "equals/hashCode contract violation: equal objects must have same hashCode");
    }
}
