// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.proto.ConsensusCreateTopicTransactionBody;
import com.hedera.hashgraph.sdk.proto.SchedulableTransactionBody;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TopicCreateTransactionTest {
    private static final PrivateKey unusedPrivateKey = PrivateKey.fromString(
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10");

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
        var tx = new TopicCreateTransaction();
        var tx2 = Transaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    private TopicCreateTransaction spawnTestTransaction() {
        return new TopicCreateTransaction()
                .setNodeAccountIds(Arrays.asList(AccountId.fromString("0.0.5005"), AccountId.fromString("0.0.5006")))
                .setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), validStart))
                .setSubmitKey(unusedPrivateKey)
                .setAdminKey(unusedPrivateKey)
                .setAutoRenewAccountId(AccountId.fromString("0.0.5007"))
                .setAutoRenewPeriod(Duration.ofHours(24))
                .setMaxTransactionFee(Hbar.fromTinybars(100_000))
                .setTopicMemo("hello memo")
                .freeze()
                .sign(unusedPrivateKey);
    }

    @Test
    void shouldBytes() throws Exception {
        var tx = spawnTestTransaction();
        var tx2 = TopicCreateTransaction.fromBytes(tx.toBytes());
        assertThat(tx2.toString()).isEqualTo(tx.toString());
    }

    @Test
    void fromScheduledTransaction() {
        var transactionBody = SchedulableTransactionBody.newBuilder()
                .setConsensusCreateTopic(
                        ConsensusCreateTopicTransactionBody.newBuilder().build())
                .build();

        var tx = Transaction.fromScheduledTransaction(transactionBody);

        assertThat(tx).isInstanceOf(TopicCreateTransaction.class);
    }

    @Test
    void shouldSetFeeScheduleKey() {
        PrivateKey feeScheduleKey = PrivateKey.generateECDSA();

        TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction();
        topicCreateTransaction.setFeeScheduleKey(feeScheduleKey);

        assertThat(topicCreateTransaction.getFeeScheduleKey().toString()).isEqualTo(feeScheduleKey.toString());
    }

    @Test
    void shouldSetFeeExemptKeys() {
        Key feeExemptKey1 = PrivateKey.generateECDSA();
        Key feeExemptKey2 = PrivateKey.generateECDSA();
        List<Key> feeExemptKeys = Arrays.asList(feeExemptKey1, feeExemptKey2);

        TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction();
        topicCreateTransaction.setFeeExemptKeys(feeExemptKeys);

        List<Key> retrievedKeys = topicCreateTransaction.getFeeExemptKeys();
        for (int i = 0; i < feeExemptKeys.size(); i++) {
            assertThat(retrievedKeys.get(i).toString())
                    .isEqualTo(feeExemptKeys.get(i).toString());
        }
    }

    @Test
    void shouldAddFeeExemptKeyToEmptyList() {
        TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction();

        PrivateKey feeExemptKeyToBeAdded = PrivateKey.generateECDSA();
        topicCreateTransaction.addFeeExemptKey(feeExemptKeyToBeAdded);

        assertThat(topicCreateTransaction.getFeeExemptKeys()).hasSize(1).containsExactly(feeExemptKeyToBeAdded);
    }

    @Test
    void shouldAddFeeExemptKeyToList() {
        PrivateKey feeExemptKey = PrivateKey.generateECDSA();
        TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction();
        topicCreateTransaction.setFeeExemptKeys(new ArrayList<>(List.of(feeExemptKey)));

        Key feeExemptKeyToBeAdded = PrivateKey.generateECDSA();
        topicCreateTransaction.addFeeExemptKey(feeExemptKeyToBeAdded);

        assertThat(topicCreateTransaction.getFeeExemptKeys())
                .hasSize(2)
                .containsExactly(feeExemptKey, feeExemptKeyToBeAdded);
    }

    @Test
    void shouldSetTopicCustomFees() {
        List<CustomFixedFee> customFixedFees = new ArrayList<>(List.of(
                new CustomFixedFee().setAmount(1).setDenominatingTokenId(new TokenId(0, 0, 0)),
                new CustomFixedFee().setAmount(2).setDenominatingTokenId(new TokenId(0, 0, 1)),
                new CustomFixedFee().setAmount(3).setDenominatingTokenId(new TokenId(0, 0, 2))));

        TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction();
        topicCreateTransaction.setCustomFees(customFixedFees);

        assertThat(topicCreateTransaction.getCustomFees())
                .hasSize(customFixedFees.size())
                .containsExactlyElementsOf(customFixedFees);
    }

    @Test
    void shouldAddTopicCustomFeeToList() {
        List<CustomFixedFee> customFixedFees = new ArrayList<>(List.of(
                new CustomFixedFee().setAmount(1).setDenominatingTokenId(new TokenId(0, 0, 0)),
                new CustomFixedFee().setAmount(2).setDenominatingTokenId(new TokenId(0, 0, 1)),
                new CustomFixedFee().setAmount(3).setDenominatingTokenId(new TokenId(0, 0, 2))));

        CustomFixedFee customFixedFeeToBeAdded =
                new CustomFixedFee().setAmount(4).setDenominatingTokenId(new TokenId(0, 0, 3));

        List<CustomFixedFee> expectedCustomFees = new ArrayList<>(customFixedFees);
        expectedCustomFees.add(customFixedFeeToBeAdded);

        TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction();
        topicCreateTransaction.setCustomFees(customFixedFees);
        topicCreateTransaction.addCustomFee(customFixedFeeToBeAdded);

        assertThat(topicCreateTransaction.getCustomFees())
                .hasSize(expectedCustomFees.size())
                .containsExactlyElementsOf(expectedCustomFees);
    }

    @Test
    void shouldAddTopicCustomFeeToEmptyList() {
        CustomFixedFee customFixedFeeToBeAdded =
                new CustomFixedFee().setAmount(4).setDenominatingTokenId(new TokenId(0, 0, 3));

        TopicCreateTransaction topicCreateTransaction = new TopicCreateTransaction();
        topicCreateTransaction.addCustomFee(customFixedFeeToBeAdded);

        assertThat(topicCreateTransaction.getCustomFees()).hasSize(1).containsExactly(customFixedFeeToBeAdded);
    }
}
