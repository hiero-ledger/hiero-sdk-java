package com.hedera.hashgraph.sdk.test.integration;

import com.hedera.hashgraph.sdk.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;

public class ScheduleTransactionIntegrationTest {

    @Test
    @DisplayName("Should charge hbars with limit using scheduled transaction")
    void shouldChargeHbarsWithLimitUsingScheduledTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hbar = 100_000_000L; // 1 HBAR in tinybars

            var customFixedFee = new CustomFixedFee()
                .setFeeCollectorAccountId(testEnv.client.getOperatorAccountId())
                .setAmount(hbar / 2);

            // Create a revenue generating topic
            var topicResponse = new TopicCreateTransaction()
                .setAdminKey(testEnv.operatorKey)
                .setFeeScheduleKey(testEnv.operatorKey)
                .setCustomFees(List.of(customFixedFee))
                .execute(testEnv.client);

            var topicId = Objects.requireNonNull(topicResponse.getReceipt(testEnv.client).topicId);

            // Create payer account
            var payerKey = PrivateKey.generateED25519();
            var payerResponse = new AccountCreateTransaction()
                .setKey(payerKey)
                .setInitialBalance(Hbar.fromTinybars(hbar))
                .execute(testEnv.client);

            var payerAccountId = Objects.requireNonNull(payerResponse.getReceipt(testEnv.client).accountId);

            var customFeeLimit = new CustomFeeLimit()
                .setPayerId(payerAccountId)
                .setCustomFees(List.of(customFixedFee));

            // Submit a message to the revenue generating topic with custom fee limit using scheduled transaction
            // Create a new client with the payer account as operator
            var payerClient = Client.forNetwork(testEnv.client.getNetwork());
            payerClient.setMirrorNetwork(testEnv.client.getMirrorNetwork());
            payerClient.setOperator(payerAccountId, payerKey);

            var submitMessageTransaction = new TopicMessageSubmitTransaction()
                .setMessage("Hello, Hedera!")
                .setTopicId(topicId)
                .setCustomFeeLimits(List.of(customFeeLimit));

            var scheduleResponse = submitMessageTransaction
                .schedule()
                .setExpirationTime(Instant.now().plus(Duration.ofDays(1)))
                .execute(payerClient);

            var scheduleId = Objects.requireNonNull(scheduleResponse.getReceipt(payerClient).scheduleId);

            // The scheduled transaction should execute immediately since we have all required signatures
            assertThat(scheduleId).isNotNull();

            payerClient.close();

            // Verify the custom fee was charged
            var accountBalance = new AccountBalanceQuery()
                .setAccountId(payerAccountId)
                .execute(testEnv.client);

            assertThat(accountBalance.hbars.toTinybars()).isLessThan(hbar / 2);

            // Cleanup
            new TopicDeleteTransaction()
                .setTopicId(topicId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Should charge tokens with limit using scheduled transaction")
    void shouldChargeTokensWithLimitUsingScheduledTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a fungible token
            var tokenResponse = new TokenCreateTransaction()
                .setTokenName("Test Token")
                .setTokenSymbol("TT")
                .setDecimals(3)
                .setInitialSupply(1000000)
                .setTreasuryAccountId(testEnv.operatorId)
                .setAdminKey(testEnv.operatorKey)
                .setFreezeKey(testEnv.operatorKey)
                .setWipeKey(testEnv.operatorKey)
                .setKycKey(testEnv.operatorKey)
                .setSupplyKey(testEnv.operatorKey)
                .setFreezeDefault(false)
                .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(tokenResponse.getReceipt(testEnv.client).tokenId);

            var customFixedFee = new CustomFixedFee()
                .setAmount(1)
                .setDenominatingTokenId(tokenId)
                .setFeeCollectorAccountId(testEnv.client.getOperatorAccountId());

            // Create a revenue generating topic
            var topicResponse = new TopicCreateTransaction()
                .setAdminKey(testEnv.operatorKey)
                .setFeeScheduleKey(testEnv.operatorKey)
                .setCustomFees(List.of(customFixedFee))
                .execute(testEnv.client);

            var topicId = Objects.requireNonNull(topicResponse.getReceipt(testEnv.client).topicId);

            // Create payer account with unlimited token associations
            var payerKey = PrivateKey.generateED25519();
            var payerResponse = new AccountCreateTransaction()
                .setKey(payerKey)
                .setInitialBalance(Hbar.fromTinybars(100_000_000))
                .setMaxAutomaticTokenAssociations(-1)
                .execute(testEnv.client);

            var payerAccountId = Objects.requireNonNull(payerResponse.getReceipt(testEnv.client).accountId);

            // Send tokens to payer
            new TransferTransaction()
                .addTokenTransfer(tokenId, testEnv.client.getOperatorAccountId(), -1)
                .addTokenTransfer(tokenId, payerAccountId, 1)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            var customFeeLimit = new CustomFeeLimit()
                .setPayerId(payerAccountId)
                .setCustomFees(List.of(
                    new CustomFixedFee()
                        .setAmount(1)
                        .setDenominatingTokenId(tokenId)
                ));

            // Submit a message to the revenue generating topic with custom fee limit using scheduled transaction
            // Create a new client with the payer account as operator
            var payerClient = Client.forNetwork(testEnv.client.getNetwork());
            payerClient.setMirrorNetwork(testEnv.client.getMirrorNetwork());
            payerClient.setOperator(payerAccountId, payerKey);

            var submitMessageTransaction = new TopicMessageSubmitTransaction()
                .setMessage("Hello, Hedera!")
                .setTopicId(topicId)
                .setCustomFeeLimits(List.of(customFeeLimit));

            var scheduleResponse = submitMessageTransaction
                .schedule()
                .setExpirationTime(Instant.now().plus(Duration.ofDays(1)))
                .execute(payerClient);

            var scheduleId = Objects.requireNonNull(scheduleResponse.getReceipt(payerClient).scheduleId);

            // The scheduled transaction should execute immediately since we have all required signatures
            assertThat(scheduleId).isNotNull();

            payerClient.close();

            // Verify the custom fee was charged
            var accountBalance = new AccountBalanceQuery()
                .setAccountId(payerAccountId)
                .execute(testEnv.client);

            assertThat(accountBalance.tokens.get(tokenId)).isEqualTo(0);

            // Cleanup
            new TopicDeleteTransaction()
                .setTopicId(topicId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Should not charge hbars with lower limit using scheduled transaction")
    void shouldNotChargeHbarsWithLowerLimitUsingScheduledTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hbar = 100_000_000L; // 1 HBAR in tinybars

            var customFixedFee = new CustomFixedFee()
                .setFeeCollectorAccountId(testEnv.client.getOperatorAccountId())
                .setAmount(hbar / 2);

            // Create a revenue generating topic with Hbar custom fee
            var topicResponse = new TopicCreateTransaction()
                .setAdminKey(testEnv.operatorKey)
                .setFeeScheduleKey(testEnv.operatorKey)
                .setCustomFees(List.of(customFixedFee))
                .execute(testEnv.client);

            var topicId = Objects.requireNonNull(topicResponse.getReceipt(testEnv.client).topicId);

            // Create payer account
            var payerKey = PrivateKey.generateED25519();
            var payerResponse = new AccountCreateTransaction()
                .setKey(payerKey)
                .setInitialBalance(Hbar.fromTinybars(hbar))
                .execute(testEnv.client);

            var payerAccountId = Objects.requireNonNull(payerResponse.getReceipt(testEnv.client).accountId);

            // Set custom fee limit with lower amount than the custom fee
            var customFeeLimit = new CustomFeeLimit()
                .setPayerId(payerAccountId)
                .setCustomFees(List.of(new CustomFixedFee().setAmount(hbar / 2 - 1)));

            // Submit a message to the revenue generating topic with custom fee limit using scheduled transaction
            // Create a new client with the payer account as operator
            var payerClient = Client.forNetwork(testEnv.client.getNetwork());
            payerClient.setMirrorNetwork(testEnv.client.getMirrorNetwork());
            payerClient.setOperator(payerAccountId, payerKey);

            var submitMessageTransaction = new TopicMessageSubmitTransaction()
                .setMessage("Hello, Hedera!")
                .setTopicId(topicId)
                .setCustomFeeLimits(List.of(customFeeLimit));

            assertThatExceptionOfType(ReceiptStatusException.class)
                .isThrownBy(() -> {
                    submitMessageTransaction
                        .schedule()
                        .setExpirationTime(Instant.now().plus(Duration.ofDays(1)))
                        .execute(payerClient)
                        .getReceipt(payerClient);
                })
                .withMessageContaining(Status.MAX_CUSTOM_FEE_LIMIT_EXCEEDED.toString());

            payerClient.close();

            // Cleanup
            new TopicDeleteTransaction()
                .setTopicId(topicId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Should not charge tokens with lower limit using scheduled transaction")
    void shouldNotChargeTokensWithLowerLimitUsingScheduledTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a fungible token
            var tokenResponse = new TokenCreateTransaction()
                .setTokenName("Test Token")
                .setTokenSymbol("TT")
                .setDecimals(3)
                .setInitialSupply(1000000)
                .setTreasuryAccountId(testEnv.operatorId)
                .setAdminKey(testEnv.operatorKey)
                .setFreezeKey(testEnv.operatorKey)
                .setWipeKey(testEnv.operatorKey)
                .setKycKey(testEnv.operatorKey)
                .setSupplyKey(testEnv.operatorKey)
                .setFreezeDefault(false)
                .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(tokenResponse.getReceipt(testEnv.client).tokenId);

            var customFixedFee = new CustomFixedFee()
                .setAmount(2)
                .setDenominatingTokenId(tokenId)
                .setFeeCollectorAccountId(testEnv.client.getOperatorAccountId());

            // Create a revenue generating topic
            var topicResponse = new TopicCreateTransaction()
                .setAdminKey(testEnv.operatorKey)
                .setFeeScheduleKey(testEnv.operatorKey)
                .setCustomFees(List.of(customFixedFee))
                .execute(testEnv.client);

            var topicId = Objects.requireNonNull(topicResponse.getReceipt(testEnv.client).topicId);

            // Create payer account with unlimited token associations
            var payerKey = PrivateKey.generateED25519();
            var payerResponse = new AccountCreateTransaction()
                .setKey(payerKey)
                .setInitialBalance(Hbar.fromTinybars(100_000_000))
                .setMaxAutomaticTokenAssociations(-1)
                .execute(testEnv.client);

            var payerAccountId = Objects.requireNonNull(payerResponse.getReceipt(testEnv.client).accountId);

            // Send tokens to payer
            new TransferTransaction()
                .addTokenTransfer(tokenId, testEnv.client.getOperatorAccountId(), -2)
                .addTokenTransfer(tokenId, payerAccountId, 2)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            // Set custom fee limit with lower amount than the custom fee
            var customFeeLimit = new CustomFeeLimit()
                .setPayerId(payerAccountId)
                .setCustomFees(List.of(
                    new CustomFixedFee()
                        .setAmount(1)
                        .setDenominatingTokenId(tokenId)
                ));

            // Submit a message to the revenue generating topic with custom fee limit using scheduled transaction
            // Create a new client with the payer account as operator
            var payerClient = Client.forNetwork(testEnv.client.getNetwork());
            payerClient.setMirrorNetwork(testEnv.client.getMirrorNetwork());
            payerClient.setOperator(payerAccountId, payerKey);

            var submitMessageTransaction = new TopicMessageSubmitTransaction()
                .setMessage("Hello, Hedera!")
                .setTopicId(topicId)
                .setCustomFeeLimits(List.of(customFeeLimit));

            assertThatExceptionOfType(ReceiptStatusException.class)
                .isThrownBy(() -> {
                    submitMessageTransaction
                        .schedule()
                        .setExpirationTime(Instant.now().plus(Duration.ofDays(1)))
                        .execute(payerClient)
                        .getReceipt(payerClient);
                })
                .withMessageContaining(Status.MAX_CUSTOM_FEE_LIMIT_EXCEEDED.toString());

            payerClient.close();

            // Cleanup
            new TopicDeleteTransaction()
                .setTopicId(topicId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Should not execute with invalid custom fee limit using scheduled transaction")
    void shouldNotExecuteWithInvalidCustomFeeLimitUsingScheduledTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var tokenResponse = new TokenCreateTransaction()
                .setTokenName("Test Token")
                .setTokenSymbol("TT")
                .setDecimals(3)
                .setInitialSupply(1000000)
                .setTreasuryAccountId(testEnv.operatorId)
                .setAdminKey(testEnv.operatorKey)
                .setFreezeKey(testEnv.operatorKey)
                .setWipeKey(testEnv.operatorKey)
                .setKycKey(testEnv.operatorKey)
                .setSupplyKey(testEnv.operatorKey)
                .setFreezeDefault(false)
                .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(tokenResponse.getReceipt(testEnv.client).tokenId);

            var customFixedFee = new CustomFixedFee()
                .setAmount(2)
                .setDenominatingTokenId(tokenId)
                .setFeeCollectorAccountId(testEnv.client.getOperatorAccountId());

            // Create a revenue generating topic
            var topicResponse = new TopicCreateTransaction()
                .setAdminKey(testEnv.operatorKey)
                .setFeeScheduleKey(testEnv.operatorKey)
                .setCustomFees(List.of(customFixedFee))
                .execute(testEnv.client);

            var topicId = Objects.requireNonNull(topicResponse.getReceipt(testEnv.client).topicId);

            // Create payer account with unlimited token associations
            var payerKey = PrivateKey.generateED25519();
            var payerResponse = new AccountCreateTransaction()
                .setKey(payerKey)
                .setInitialBalance(Hbar.fromTinybars(100_000_000))
                .setMaxAutomaticTokenAssociations(-1)
                .execute(testEnv.client);

            var payerAccountId = Objects.requireNonNull(payerResponse.getReceipt(testEnv.client).accountId);

            // Send tokens to payer
            new TransferTransaction()
                .addTokenTransfer(tokenId, testEnv.client.getOperatorAccountId(), -2)
                .addTokenTransfer(tokenId, payerAccountId, 2)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            // Create a new client with the payer account as operator
            var payerClient = Client.forNetwork(testEnv.client.getNetwork());
            payerClient.setMirrorNetwork(testEnv.client.getMirrorNetwork());
            payerClient.setOperator(payerAccountId, payerKey);

            // Test 1: Set custom fee limit with invalid token ID (0.0.0)
            var invalidCustomFeeLimit = new CustomFeeLimit()
                .setPayerId(payerAccountId)
                .setCustomFees(List.of(
                    new CustomFixedFee()
                        .setAmount(1)
                        .setDenominatingTokenId(new TokenId(0))
                ));

            var submitMessageTransactionWithInvalidToken = new TopicMessageSubmitTransaction()
                .setMessage("Hello, Hedera!")
                .setTopicId(topicId)
                .setCustomFeeLimits(List.of(invalidCustomFeeLimit));

            assertThatExceptionOfType(ReceiptStatusException.class)
                .isThrownBy(() -> {
                    submitMessageTransactionWithInvalidToken
                        .schedule()
                        .setExpirationTime(Instant.now().plus(Duration.ofDays(1)))
                        .execute(payerClient)
                        .getReceipt(payerClient);
                })
                .withMessageContaining(Status.NO_VALID_MAX_CUSTOM_FEE.toString());

            // Test 2: Set custom fee limit with duplicate denominations
            var duplicateCustomFeeLimit = new CustomFeeLimit()
                .setPayerId(payerAccountId)
                .setCustomFees(List.of(
                    new CustomFixedFee()
                        .setAmount(1)
                        .setDenominatingTokenId(tokenId),
                    new CustomFixedFee()
                        .setAmount(2)
                        .setDenominatingTokenId(tokenId)
                ));

            var submitMessageTransactionWithDuplicates = new TopicMessageSubmitTransaction()
                .setMessage("Hello, Hedera!")
                .setTopicId(topicId)
                .setCustomFeeLimits(List.of(duplicateCustomFeeLimit));

            assertThatExceptionOfType(ReceiptStatusException.class)
                .isThrownBy(() -> {
                    submitMessageTransactionWithDuplicates
                        .schedule()
                        .setExpirationTime(Instant.now().plus(Duration.ofDays(1)))
                        .execute(payerClient)
                        .getReceipt(payerClient);
                })
                .withMessageContaining(Status.DUPLICATE_DENOMINATION_IN_MAX_CUSTOM_FEE_LIST.toString());

            payerClient.close();

            // Cleanup
            new TopicDeleteTransaction()
                .setTopicId(topicId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can submit a topic message with custom fee limits")
    void canSubmitATopicMessageWithCustomFeeLimits() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create sample custom fee limits for testing
            var customFeeLimits = new ArrayList<>(List.of(
                new CustomFeeLimit()
                    .setPayerId(new AccountId(0, 0, 1))
                    .setCustomFees(List.of(
                        new CustomFixedFee().setAmount(1).setDenominatingTokenId(new TokenId(0, 0, 1)))),
                new CustomFeeLimit()
                    .setPayerId(new AccountId(0, 0, 2))
                    .setCustomFees(List.of(
                        new CustomFixedFee().setAmount(1).setDenominatingTokenId(new TokenId(0, 0, 2))))));

            var response = new TopicCreateTransaction()
                .setAdminKey(testEnv.operatorKey)
                .setTopicMemo("[e2e::TopicCreateTransaction]")
                .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            var info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.topicId).isEqualTo(topicId);
            assertThat(info.topicMemo).isEqualTo("[e2e::TopicCreateTransaction]");
            assertThat(info.sequenceNumber).isEqualTo(0);
            assertThat(info.adminKey).isEqualTo(testEnv.operatorKey);

            // Submit message with custom fee limits via scheduled transaction
            new TopicMessageSubmitTransaction()
                .setTopicId(topicId)
                .setMessage("Hello, from HCS!")
                .setCustomFeeLimits(customFeeLimits)
                .schedule()
                .execute(testEnv.client)
                .getReceipt(testEnv.client);

            info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.topicId).isEqualTo(topicId);
            assertThat(info.topicMemo).isEqualTo("[e2e::TopicCreateTransaction]");
            assertThat(info.sequenceNumber).isEqualTo(1);
            assertThat(info.adminKey).isEqualTo(testEnv.operatorKey);

            new TopicDeleteTransaction()
                .setTopicId(topicId)
                .execute(testEnv.client)
                .getReceipt(testEnv.client);
        }
    }
}
