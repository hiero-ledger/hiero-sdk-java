// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.*;

import com.hedera.hashgraph.sdk.*;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

            var customFeeLimit = new CustomFeeLimit().setPayerId(payerAccountId).setCustomFees(List.of(customFixedFee));

            // Submit a message to the revenue generating topic with custom fee limit using scheduled transaction
            // Create a new client with the payer account as operator
            var payerClient = Client.forNetwork(testEnv.client.getNetwork());
            payerClient.setMirrorNetwork(testEnv.client.getMirrorNetwork());
            payerClient.setOperator(payerAccountId, payerKey);

            var submitMessageTransaction = new TopicMessageSubmitTransaction()
                    .setMessage("hello!")
                    .setTopicId(topicId)
                    .setCustomFeeLimits(List.of(customFeeLimit));

            var scheduleResponse = submitMessageTransaction.schedule().execute(payerClient);

            var scheduleId = Objects.requireNonNull(scheduleResponse.getReceipt(payerClient).scheduleId);

            // The scheduled transaction should execute immediately since we have all required signatures
            assertThat(scheduleId).isNotNull();

            payerClient.close();

            var accountBalance =
                    new AccountBalanceQuery().setAccountId(payerAccountId).execute(testEnv.client);

            assertThat(accountBalance.hbars.toTinybars()).isLessThan(hbar / 2);

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

            new TopicMessageSubmitTransaction()
                    .setMessage("Hello")
                    .setTopicId(topicId)
                    .setCustomFeeLimits(List.of(customFeeLimit))
                    .schedule()
                    .execute(payerClient)
                    .getReceipt(payerClient);

            var accountBalance =
                    new AccountBalanceQuery().setAccountId(payerAccountId).execute(testEnv.client);

            assertThat(accountBalance.hbars.toTinybars()).isGreaterThan(hbar / 2);

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
                    .setCustomFees(List.of(new CustomFixedFee().setAmount(1).setDenominatingTokenId(tokenId)));

            // Submit a message to the revenue generating topic with custom fee limit using scheduled transaction
            // Create a new client with the payer account as operator
            testEnv.client.setOperator(payerAccountId, payerKey);

            new TopicMessageSubmitTransaction()
                    .setMessage("Hello!")
                    .setTopicId(topicId)
                    .setCustomFeeLimits(List.of(customFeeLimit))
                    .schedule()
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var accountBalance =
                    new AccountBalanceQuery().setAccountId(payerAccountId).execute(testEnv.client);

            assertThat(accountBalance.tokens.get(tokenId)).isEqualTo(2);
        }
    }
}
