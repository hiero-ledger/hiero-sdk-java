// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class TopicCreateIntegrationTest {
    @Test
    @DisplayName("Can create topic")
    void canCreateTopic() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            new TopicCreateTransaction()
                    .setTopicMemo("[e2e::TopicCreateTransaction]")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setTopicMemo("[e2e::TopicCreateTransaction]")
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            new TopicDeleteTransaction()
                    .setTopicId(topicId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can create topic with no field set")
    void canCreateTopicWithNoFieldsSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new TopicCreateTransaction().execute(testEnv.client);
            assertThat(response.getReceipt(testEnv.client).topicId).isNotNull();
        }
    }

    @Test
    @DisplayName("Creates and updates revenue-generating topic")
    void createsAndUpdatesRevenueGeneratingTopic() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            List<Key> feeExemptKeys = new ArrayList<>(List.of(PrivateKey.generateECDSA(), PrivateKey.generateECDSA()));

            var denominatingTokenId1 = createToken(testEnv);
            var amount1 = 1;

            var denominatingTokenId2 = createToken(testEnv);
            var amount2 = 2;

            var customFixedFees = List.of(
                    new CustomFixedFee()
                            .setFeeCollectorAccountId(testEnv.operatorId)
                            .setDenominatingTokenId(denominatingTokenId1)
                            .setAmount(amount1),
                    new CustomFixedFee()
                            .setFeeCollectorAccountId(testEnv.operatorId)
                            .setDenominatingTokenId(denominatingTokenId2)
                            .setAmount(amount2));

            // Create revenue-generating topic
            var response = new TopicCreateTransaction()
                    .setFeeScheduleKey(testEnv.operatorKey)
                    .setSubmitKey(testEnv.operatorKey)
                    .setAdminKey(testEnv.operatorKey)
                    .setFeeExemptKeys(feeExemptKeys)
                    .setCustomFees(customFixedFees)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            // Get Topic Info
            var info = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(info.feeScheduleKey).isEqualTo(testEnv.operatorKey);

            // Validate fee exempt keys
            for (int i = 0; i < feeExemptKeys.size(); i++) {
                var key = (PrivateKey) feeExemptKeys.get(i);
                PublicKey publicKey = key.getPublicKey();
                assertThat(info.feeExemptKeys.get(i)).isEqualTo(publicKey);
            }

            // Validate custom fees
            for (int i = 0; i < customFixedFees.size(); i++) {
                assertThat(info.customFees.get(i).getAmount())
                        .isEqualTo(customFixedFees.get(i).getAmount());
                assertThat(info.customFees.get(i).getDenominatingTokenId())
                        .isEqualTo(customFixedFees.get(i).getDenominatingTokenId());
            }

            // Update the revenue-generating topic
            List<Key> newFeeExemptKeys = List.of(PrivateKey.generateECDSA(), PrivateKey.generateECDSA());

            var newFeeScheduleKey = PrivateKey.generateECDSA();

            var newAmount1 = 3;
            var newDenominatingTokenId1 = createToken(testEnv);
            var newAmount2 = 4;
            var newDenominatingTokenId2 = createToken(testEnv);

            var newCustomFixedFees = new ArrayList<>(List.of(
                    new CustomFixedFee()
                            .setFeeCollectorAccountId(testEnv.operatorId)
                            .setAmount(newAmount1)
                            .setDenominatingTokenId(newDenominatingTokenId1),
                    new CustomFixedFee()
                            .setFeeCollectorAccountId(testEnv.operatorId)
                            .setAmount(newAmount2)
                            .setDenominatingTokenId(newDenominatingTokenId2)));

            var updateResponse = new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setFeeExemptKeys(newFeeExemptKeys)
                    .setFeeScheduleKey(newFeeScheduleKey.getPublicKey())
                    .setCustomFees(newCustomFixedFees)
                    .execute(testEnv.client);

            updateResponse.getReceipt(testEnv.client);

            var updatedInfo = new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client);

            assertThat(updatedInfo.feeScheduleKey).isEqualTo(newFeeScheduleKey.getPublicKey());

            for (int i = 0; i < newFeeExemptKeys.size(); i++) {
                var key = (PrivateKey) newFeeExemptKeys.get(i);
                PublicKey publicKey = key.getPublicKey();
                assertThat(updatedInfo.feeExemptKeys.get(i)).isEqualTo(publicKey);
            }

            for (int i = 0; i < newCustomFixedFees.size(); i++) {
                assertThat(updatedInfo.customFees.get(i).getAmount())
                        .isEqualTo(newCustomFixedFees.get(i).getAmount());
                assertThat(updatedInfo.customFees.get(i).getDenominatingTokenId())
                        .isEqualTo(newCustomFixedFees.get(i).getDenominatingTokenId());
            }
        }
    }

    @Test
    @DisplayName("Fails to create revenue-generating topic with invalid fee exempt key")
    void failsToCreateRevenueGeneratingTopicWithInvalidFeeExemptKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var feeExemptKey = PrivateKey.generateECDSA();

            List<Key> feeExemptKeyListWithDuplicates = List.of(feeExemptKey, feeExemptKey);

            Executable duplicatesExecutable = () -> new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setFeeExemptKeys(feeExemptKeyListWithDuplicates)
                    .execute(testEnv.client);

            // Expect failure due to duplicated fee exempt keys
            assertThrows(
                    PrecheckStatusException.class,
                    duplicatesExecutable,
                    ResponseCodeEnum.FEE_EXEMPT_KEY_LIST_CONTAINS_DUPLICATED_KEYS.name());

            var invalidKey = PublicKey.fromString("000000000000000000000000000000000000000000000000000000000000000000");

            Executable invalidKeyExecutable = () -> new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setFeeExemptKeys(new ArrayList<>(List.of(invalidKey)))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Expect failure due to invalid fee exempt key
            assertThrows(
                    ReceiptStatusException.class,
                    invalidKeyExecutable,
                    ResponseCodeEnum.INVALID_KEY_IN_FEE_EXEMPT_KEY_LIST.name());

            // Create 11 keys (exceeding the limit of 10)
            List<Key> feeExemptKeyListExceedingLimit = List.of(
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA(),
                    PrivateKey.generateECDSA());

            Executable exceedKeyListLimitExecutable = () -> new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setFeeExemptKeys(feeExemptKeyListExceedingLimit)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Expect failure due to exceeding fee exempt key list limit
            assertThrows(
                    ReceiptStatusException.class,
                    exceedKeyListLimitExecutable,
                    ResponseCodeEnum.MAX_ENTRIES_FOR_FEE_EXEMPT_KEY_LIST_EXCEEDED.name());
        }
    }

    @Test
    @DisplayName("Fails to update fee schedule key without permissions")
    void failsToUpdateFeeScheduleKeyWithoutPermissions() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            var newFeeScheduleKey = PrivateKey.generateECDSA();

            Executable updateExecutable = () -> new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setFeeScheduleKey(newFeeScheduleKey.getPublicKey())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThrows(
                    ReceiptStatusException.class,
                    updateExecutable,
                    ResponseCodeEnum.FEE_SCHEDULE_KEY_CANNOT_BE_UPDATED.name());
        }
    }

    @Test
    @DisplayName("Fails to update custom fees without a fee schedule key")
    void failsToUpdateCustomFeesWithoutFeeScheduleKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            var denominatingTokenId1 = createToken(testEnv);
            var denominatingTokenId2 = createToken(testEnv);

            var customFees = List.of(
                    new CustomFixedFee()
                            .setFeeCollectorAccountId(testEnv.operatorId)
                            .setDenominatingTokenId(denominatingTokenId1)
                            .setAmount(1),
                    new CustomFixedFee()
                            .setFeeCollectorAccountId(testEnv.operatorId)
                            .setDenominatingTokenId(denominatingTokenId2)
                            .setAmount(2));

            Executable updateExecutable = () -> new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setCustomFees(customFees)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThrows(
                    ReceiptStatusException.class, updateExecutable, ResponseCodeEnum.FEE_SCHEDULE_KEY_NOT_SET.name());
        }
    }

    @Test
    @DisplayName("Charges HBAR fees with limits applied")
    void chargesHbarFeesWithLimitsApplied() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var hbarAmount = 100_000_000L;
            var privateKey = PrivateKey.generateECDSA();

            var customFixedFee = new CustomFixedFee()
                    .setFeeCollectorAccountId(testEnv.operatorId)
                    .setAmount(hbarAmount / 2);

            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setFeeScheduleKey(testEnv.operatorKey)
                    .addCustomFee(customFixedFee)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            var accountId = createAccount(testEnv, new Hbar(1), privateKey);

            clientSetOperator(testEnv, accountId, privateKey);

            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Hedera HBAR Fee Test")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            clientSetOperator(testEnv, accountId);

            var balance = new AccountBalanceQuery().setAccountId(accountId).execute(testEnv.client).hbars;

            assertThat(balance.toTinybars()).isLessThan(hbarAmount / 2);
        }
    }

    @Test
    @DisplayName("Exempts fee-exempt keys from HBAR fees")
    void exemptsFeeExemptKeysFromHbarFees() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var hbarAmount = 100_000_000L;
            var feeExemptKey1 = PrivateKey.generateECDSA();
            var feeExemptKey2 = PrivateKey.generateECDSA();

            var customFixedFee = new CustomFixedFee()
                    .setFeeCollectorAccountId(testEnv.operatorId)
                    .setAmount(hbarAmount / 2);

            var response = new TopicCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setFeeScheduleKey(testEnv.operatorKey)
                    .setFeeExemptKeys(List.of(feeExemptKey1.getPublicKey(), feeExemptKey2.getPublicKey()))
                    .addCustomFee(customFixedFee)
                    .execute(testEnv.client);

            var topicId = Objects.requireNonNull(response.getReceipt(testEnv.client).topicId);

            var payerAccountId = createAccount(testEnv, new Hbar(1), feeExemptKey1);

            clientSetOperator(testEnv, payerAccountId, feeExemptKey1);

            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Hedera Fee Exemption Test")
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            clientSetOperator(testEnv, payerAccountId);

            var balance = new AccountBalanceQuery().setAccountId(payerAccountId).execute(testEnv.client).hbars;

            assertThat(balance.toTinybars()).isGreaterThan(hbarAmount / 2);
        }
    }

    @Test
    @DisplayName("Should assign autoRenewAccountId to the topic creator")
    @Disabled("This will be reintroduced once all networks (previewnet, testnet, mainnet) are on version 0.60.")
    void createTopicTransactionShouldAssignAutomaticallyAutoRenewAccountId() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var topicId = new TopicCreateTransaction().execute(testEnv.client).getReceipt(testEnv.client).topicId;

            var autoRenewAccountId =
                    new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client).autoRenewAccountId;

            assertThat(autoRenewAccountId).isNotNull();
        }
    }

    @Test
    @DisplayName("Should assign autoRenewAccountId to the transactionId accountId")
    @Disabled("This will be reintroduced once all networks (previewnet, testnet, mainnet) are on version 0.60.")
    void createTopicTransactionWithTransactionIdShouldAssignAutoRenewAccountIdToTransactionIdAccountId()
            throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var privateKey = PrivateKey.generateECDSA();
            var publicKey = privateKey.getPublicKey();

            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(publicKey)
                    .setInitialBalance(Hbar.from(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var topicId = new TopicCreateTransaction()
                    .setTransactionId(TransactionId.generate(accountId))
                    .freezeWith(testEnv.client)
                    .sign(privateKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .topicId;

            var autoRenewAccountId =
                    new TopicInfoQuery().setTopicId(topicId).execute(testEnv.client).autoRenewAccountId;

            assertThat(autoRenewAccountId).isEqualTo(accountId);
        }
    }

    private AccountId createAccount(IntegrationTestEnv testEnv, Hbar initialBalance, PrivateKey key) throws Exception {
        return new AccountCreateTransaction()
                .setInitialBalance(initialBalance)
                .setKeyWithoutAlias(key)
                .execute(testEnv.client)
                .getReceipt(testEnv.client)
                .accountId;
    }

    private void clientSetOperator(IntegrationTestEnv testEnv, AccountId accountId) {
        testEnv.client.setOperator(accountId, PrivateKey.generateECDSA());
    }

    private void clientSetOperator(IntegrationTestEnv testEnv, AccountId accountId, PrivateKey key) {
        testEnv.client.setOperator(accountId, key);
    }

    private TokenId createToken(IntegrationTestEnv testEnv) throws Exception {
        var tokenCreateResponse = new TokenCreateTransaction()
                .setTokenName("Test Token")
                .setTokenSymbol("TT")
                .setTreasuryAccountId(testEnv.operatorId)
                .setInitialSupply(1_000_000)
                .setDecimals(2)
                .setAdminKey(testEnv.operatorKey)
                .setSupplyKey(testEnv.operatorKey)
                .execute(testEnv.client);

        return Objects.requireNonNull(tokenCreateResponse.getReceipt(testEnv.client).tokenId);
    }
}
