// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.proto.ConsensusSubmitMessageTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TopicMessageSubmitTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");
    private static final TopicId testTopicId = new TopicId(0, 6, 9);
    private static final byte[] testMessageBytes = {0x04, 0x05, 0x06};
    private static final Instant validStart = Instant.ofEpochSecond(1554158542);

    @BeforeAll
    public static void beforeAll() {
        SnapshotMatcher.start(Snapshot::asJsonString);
    }

    @AfterAll
    public static void afterAll() {
        SnapshotMatcher.validateSnapshots();
    }

    private TopicMessageSubmitTransaction spawnTestTransactionString() {
        return new TopicMessageSubmitTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTopicId(testTopicId)
                .setMessage(new String(testMessageBytes))
                .freeze()
                .sign(unusedPrivateKey);
    }

    private TopicMessageSubmitTransaction spawnTestTransactionBytes() {
        return new TopicMessageSubmitTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setTopicId(testTopicId)
                .setMessage(testMessageBytes)
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytesNoSetters() throws Exception {
        var tx = new TopicMessageSubmitTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setConsensusSubmitMessage(
                        ConsensusSubmitMessageTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TopicMessageSubmitTransaction.class);
    }

    @Test
    void constructTopicMessageSubmitTransactionFromTransactionBodyProtobuf() {
        var transactionBody = ConsensusSubmitMessageTransactionBody.newBuilder()
                .setTopicID(testTopicId.toProtobuf())
                .setMessage(ByteString.copyFrom(testMessageBytes))
                .build();

        var tx = TransactionBody.newBuilder()
                .setConsensusSubmitMessage(transactionBody)
                .build();

        var topicSubmitMessageTransaction = new TopicMessageSubmitTransaction(tx);
        assertThat(topicSubmitMessageTransaction.getTopicId()).isEqualTo(testTopicId);
    }

    @Test
    void getSetTopicId() {
        var topicSubmitMessageTransaction = new TopicMessageSubmitTransaction().setTopicId(testTopicId);
        assertThat(topicSubmitMessageTransaction.getTopicId()).isEqualTo(testTopicId);
    }

    @Test
    void getSetTopicIdFrozen() {
        var tx = spawnTestTransactionString();
        assertThrows(IllegalStateException.class, () -> tx.setTopicId(testTopicId));
    }

    @Test
    void getSetMessage() {
        var topicSubmitMessageTransactionString =
                new TopicMessageSubmitTransaction().setMessage(new String(testMessageBytes));
        var topicSubmitMessageTransactionBytes = new TopicMessageSubmitTransaction().setMessage(testMessageBytes);
        assertThat(topicSubmitMessageTransactionString.getMessage().toByteArray())
                .isEqualTo(testMessageBytes);
        assertThat(topicSubmitMessageTransactionBytes.getMessage().toByteArray())
                .isEqualTo(testMessageBytes);
    }

    @Test
    void getSetMessageFrozen() {
        var topicSubmitMessageTransactionString = spawnTestTransactionString();
        var topicSubmitMessageTransactionBytes = spawnTestTransactionBytes();
        assertThrows(
                IllegalStateException.class, () -> topicSubmitMessageTransactionString.setMessage(testMessageBytes));
        assertThrows(
                IllegalStateException.class, () -> topicSubmitMessageTransactionBytes.setMessage(testMessageBytes));
    }

    @Test
    void shouldSetCustomFeeLimits() {
        var customFeeLimits = Arrays.asList(
                new CustomFeeLimit()
                        .setPayerId(new AccountId(0, 0, 1))
                        .setCustomFees(List.of(
                                new CustomFixedFee().setAmount(1).setDenominatingTokenId(new TokenId(0, 0, 1)))),
                new CustomFeeLimit()
                        .setPayerId(new AccountId(0, 0, 2))
                        .setCustomFees(List.of(
                                new CustomFixedFee().setAmount(1).setDenominatingTokenId(new TokenId(0, 0, 2)))));

        var topicMessageSubmitTransaction = new TopicMessageSubmitTransaction().setCustomFeeLimits(customFeeLimits);

        assertThat(topicMessageSubmitTransaction.getCustomFeeLimits()).isEqualTo(customFeeLimits);
    }

    @Test
    void shouldAddCustomFeeLimitToList() {
        var customFeeLimits = new ArrayList<>(List.of(
                new CustomFeeLimit()
                        .setPayerId(new AccountId(0, 0, 1))
                        .setCustomFees(List.of(
                                new CustomFixedFee().setAmount(1).setDenominatingTokenId(new TokenId(0, 0, 1)))),
                new CustomFeeLimit()
                        .setPayerId(new AccountId(0, 0, 2))
                        .setCustomFees(List.of(
                                new CustomFixedFee().setAmount(1).setDenominatingTokenId(new TokenId(0, 0, 2))))));

        var customFeeLimitToBeAdded = new CustomFeeLimit()
                .setPayerId(new AccountId(0, 0, 3))
                .setCustomFees(new ArrayList<>(
                        List.of(new CustomFixedFee().setAmount(3).setDenominatingTokenId(new TokenId(0, 0, 3)))));

        var expectedCustomFeeLimits = new ArrayList<>(customFeeLimits);
        expectedCustomFeeLimits.add(customFeeLimitToBeAdded);

        var topicMessageSubmitTransaction = new TopicMessageSubmitTransaction()
                .setCustomFeeLimits(customFeeLimits)
                .addCustomFeeLimit(customFeeLimitToBeAdded);

        assertThat(topicMessageSubmitTransaction.getCustomFeeLimits()).isEqualTo(expectedCustomFeeLimits);
    }

    @Test
    void shouldAddCustomFeeLimitToEmptyList() {
        var customFeeLimitToBeAdded = new CustomFeeLimit()
                .setPayerId(new AccountId(0, 0, 3))
                .setCustomFees(new ArrayList<>(
                        List.of(new CustomFixedFee().setAmount(3).setDenominatingTokenId(new TokenId(0, 0, 3)))));

        var topicMessageSubmitTransaction =
                new TopicMessageSubmitTransaction().addCustomFeeLimit(customFeeLimitToBeAdded);

        assertThat(topicMessageSubmitTransaction.getCustomFeeLimits()).containsExactly(customFeeLimitToBeAdded);
    }
}
