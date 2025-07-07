// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenCreateIntegrationTest {
    private static List<CustomFee> createFixedFeeList(int count, AccountId feeCollector) {
        var feeList = new ArrayList<CustomFee>();
        for (int i = 0; i < count; i++) {
            feeList.add(new CustomFixedFee().setAmount(10).setFeeCollectorAccountId(feeCollector));
        }
        return feeList;
    }

    private static List<CustomFee> createFractionalFeeList(int count, AccountId feeCollector) {
        var feeList = new ArrayList<CustomFee>();
        for (int i = 0; i < count; i++) {
            feeList.add(new CustomFractionalFee()
                    .setNumerator(1)
                    .setDenominator(20)
                    .setMin(1)
                    .setMax(10)
                    .setFeeCollectorAccountId(feeCollector));
        }
        return feeList;
    }

    @Test
    @DisplayName("Can create token with operator as all keys")
    void canCreateTokenWithOperatorAsAllKeys() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setFreezeKey(testEnv.operatorKey)
                    .setWipeKey(testEnv.operatorKey)
                    .setKycKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setFeeScheduleKey(testEnv.operatorKey)
                    .setPauseKey(testEnv.operatorKey)
                    .setMetadataKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client);

            Objects.requireNonNull(response.getReceipt(testEnv.client));
        }
    }

    @Test
    @DisplayName("Can create token with minimal properties set")
    @SuppressWarnings("UnusedVariable")
    void canCreateTokenWithMinimalPropertiesSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot create token when token name is not set")
    void cannotCreateTokenWhenTokenNameIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenSymbol("F")
                                .setTreasuryAccountId(testEnv.operatorId)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.MISSING_TOKEN_NAME.toString());
        }
    }

    @Test
    @DisplayName("Cannot create token when token symbol is not set")
    void cannotCreateTokenWhenTokenSymbolIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenName("ffff")
                                .setTreasuryAccountId(testEnv.operatorId)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.MISSING_TOKEN_SYMBOL.toString());
        }
    }

    @Test
    @DisplayName("Cannot create token when token treasury account ID is not set")
    void cannotCreateTokenWhenTokenTreasuryAccountIDIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenName("ffff")
                                .setTokenSymbol("F")
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_TREASURY_ACCOUNT_FOR_TOKEN.toString());
        }
    }

    @Test
    @DisplayName("Cannot create token when token treasury account ID does not sign transaction")
    void cannotCreateTokenWhenTokenTreasuryAccountIDDoesNotSignTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenName("ffff")
                                .setTokenSymbol("F")
                                .setTreasuryAccountId(AccountId.fromString("0.0.3"))
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @DisplayName("Cannot create token when admin key does not sign transaction")
    void cannotCreateTokenWhenAdminKeyDoesNotSignTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var key = PrivateKey.generateED25519();

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenName("ffff")
                                .setTokenSymbol("F")
                                .setTreasuryAccountId(testEnv.operatorId)
                                .setAdminKey(key)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }

    @Test
    @DisplayName("Can create token with custom fees")
    void canCreateTokenWithCustomFees() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var customFees = new ArrayList<CustomFee>();
            customFees.add(new CustomFixedFee().setAmount(10).setFeeCollectorAccountId(testEnv.operatorId));
            customFees.add(new CustomFractionalFee()
                    .setNumerator(1)
                    .setDenominator(20)
                    .setMin(1)
                    .setMax(10)
                    .setFeeCollectorAccountId(testEnv.operatorId));

            new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setCustomFees(customFees)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot create custom fee list with > 10 entries")
    void cannotCreateMoreThanTenCustomFees() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenName("ffff")
                                .setTokenSymbol("F")
                                .setAdminKey(testEnv.operatorKey)
                                .setTreasuryAccountId(testEnv.operatorId)
                                .setCustomFees(createFixedFeeList(11, testEnv.operatorId))
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.CUSTOM_FEES_LIST_TOO_LONG.toString());
        }
    }

    @Test
    @DisplayName("Can create custom fee list with 10 fixed fees")
    void canCreateTenFixedFees() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setCustomFees(createFixedFeeList(10, testEnv.operatorId))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can create custom fee list with 10 fractional fees")
    void canCreateTenFractionalFees() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setAdminKey(testEnv.operatorKey)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setCustomFees(createFractionalFeeList(10, testEnv.operatorId))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot create a token with a custom fee where min > max")
    void cannotCreateMinGreaterThanMax() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenName("ffff")
                                .setTokenSymbol("F")
                                .setTreasuryAccountId(testEnv.operatorId)
                                .setAdminKey(testEnv.operatorKey)
                                .setCustomFees(Collections.singletonList(new CustomFractionalFee()
                                        .setNumerator(1)
                                        .setDenominator(3)
                                        .setMin(3)
                                        .setMax(2)
                                        .setFeeCollectorAccountId(testEnv.operatorId)))
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.FRACTIONAL_FEE_MAX_AMOUNT_LESS_THAN_MIN_AMOUNT.toString());
        }
    }

    @Test
    @DisplayName("Cannot create a token with invalid fee collector account ID")
    void cannotCreateInvalidFeeCollector() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenName("ffff")
                                .setTokenSymbol("F")
                                .setAdminKey(testEnv.operatorKey)
                                .setTreasuryAccountId(testEnv.operatorId)
                                .setCustomFees(Collections.singletonList(new CustomFixedFee().setAmount(1)))
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_CUSTOM_FEE_COLLECTOR.toString());
        }
    }

    @Test
    @DisplayName("Cannot create a token with a negative custom fee")
    void cannotCreateNegativeFee() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenName("ffff")
                                .setTokenSymbol("F")
                                .setAdminKey(testEnv.operatorKey)
                                .setTreasuryAccountId(testEnv.operatorId)
                                .setCustomFees(Collections.singletonList(new CustomFixedFee()
                                        .setAmount(-1)
                                        .setFeeCollectorAccountId(testEnv.operatorId)))
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.CUSTOM_FEE_MUST_BE_POSITIVE.toString());
        }
    }

    @Test
    @DisplayName("Cannot create custom fee with 0 denominator")
    void cannotCreateZeroDenominator() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenCreateTransaction()
                                .setTokenName("ffff")
                                .setTokenSymbol("F")
                                .setTreasuryAccountId(testEnv.operatorId)
                                .setAdminKey(testEnv.operatorKey)
                                .setCustomFees(Collections.singletonList(new CustomFractionalFee()
                                        .setNumerator(1)
                                        .setDenominator(0)
                                        .setMin(1)
                                        .setMax(10)
                                        .setFeeCollectorAccountId(testEnv.operatorId)))
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.FRACTION_DIVIDES_BY_ZERO.toString());
        }
    }

    @Test
    @DisplayName("Can create NFT")
    void canCreateNfts() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setFreezeKey(testEnv.operatorKey)
                    .setWipeKey(testEnv.operatorKey)
                    .setKycKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client);

            Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);
        }
    }

    @Test
    @DisplayName("Can create NFT with royalty fee")
    void canCreateRoyaltyFee() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setSupplyKey(testEnv.operatorKey)
                    .setAdminKey(testEnv.operatorKey)
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setCustomFees(Collections.singletonList(new CustomRoyaltyFee()
                            .setNumerator(1)
                            .setDenominator(10)
                            .setFallbackFee(new CustomFixedFee().setHbarAmount(new Hbar(1)))
                            .setFeeCollectorAccountId(testEnv.operatorId)))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can create token with minimal properties set and autoRenewAccount should be automatically set")
    @SuppressWarnings("UnusedVariable")
    void canCreateTokenWithMinimalPropertiesSetAutoRenewAccountShouldBeAutomaticallySet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var tokenId = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            var autoRenewAccount = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client).autoRenewAccount;

            assertThat(autoRenewAccount).isNotNull();
            assertThat(autoRenewAccount).isEqualByComparingTo(testEnv.operatorId);
        }
    }

    @Test
    @DisplayName("Can set auto-renew period when creating token")
    void canSetAutoRenewPeriod() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var autoRenewPeriod = Duration.ofSeconds(7890000);
            var expirationTime = Instant.now().plus(autoRenewPeriod);

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setAutoRenewPeriod(autoRenewPeriod)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .execute(testEnv.client);

            var tokenId = response.getReceipt(testEnv.client).tokenId;
            var tokenInfo = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfo.autoRenewAccount).isEqualTo(testEnv.operatorId);
            assertThat(tokenInfo.autoRenewPeriod).isEqualTo(autoRenewPeriod);
            assertThat(tokenInfo.expirationTime.getEpochSecond()).isEqualTo(expirationTime.getEpochSecond());
        }
    }

    @Test
    @DisplayName("Can set expiration time when creating token")
    void canSetExpirationTime() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var expirationTime = Instant.now().plusSeconds(8000001);

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setExpirationTime(expirationTime)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .execute(testEnv.client);

            var tokenId = response.getReceipt(testEnv.client).tokenId;
            var tokenInfo = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfo.expirationTime.getEpochSecond()).isEqualTo(expirationTime.getEpochSecond());
        }
    }

    @Test
    @DisplayName("AutoRenewAccountId should be equal to AccountId")
    void whenTransactionIdIsSetAutoRenewAccountIdShouldBeEqualToAccountId() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var privateKey = PrivateKey.generateECDSA();
            var publicKey = privateKey.getPublicKey();

            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(publicKey)
                    .setInitialBalance(Hbar.from(10))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var tokenId = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTransactionId(TransactionId.generate(accountId))
                    .setTreasuryAccountId(accountId)
                    .freezeWith(testEnv.client)
                    .sign(privateKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            var tokenInfo = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfo.autoRenewAccount).isEqualTo(accountId);
        }
    }

    @Test
    @DisplayName("Can create token with decimal adjustment for supply values")
    void canCreateTokenWithDecimalAdjustmentForSupplyValues() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            int decimals = 3;
            long userInputInitialSupply = 1000;
            long userInputMaxSupply = 10000;
            long expectedInitialSupply = userInputInitialSupply * 1000;
            long expectedMaxSupply = userInputMaxSupply * 1000;

            var response = new TokenCreateTransaction()
                    .setTokenName("DecimalTest")
                    .setTokenSymbol("DT")
                    .setDecimals(decimals)
                    .setInitialSupply(expectedInitialSupply)
                    .setMaxSupply(expectedMaxSupply)
                    .setSupplyType(TokenSupplyType.FINITE)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            var tokenId = response.getReceipt(testEnv.client).tokenId;
            var tokenInfo = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfo.decimals).isEqualTo(decimals);
            assertThat(tokenInfo.totalSupply).isEqualTo(expectedInitialSupply);
            assertThat(tokenInfo.maxSupply).isEqualTo(expectedMaxSupply);
        }
    }

    @Test
    @DisplayName("Can create NFT with zero decimals and zero initial supply")
    void canCreateNftWithZeroDecimalsAndZeroInitialSupply() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            var response = new TokenCreateTransaction()
                    .setTokenName("NFTTest")
                    .setTokenSymbol("NFT")
                    .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                    .setDecimals(0)
                    .setInitialSupply(0)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            var tokenId = response.getReceipt(testEnv.client).tokenId;
            var tokenInfo = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(tokenInfo.tokenType).isEqualTo(TokenType.NON_FUNGIBLE_UNIQUE);
            assertThat(tokenInfo.decimals).isEqualTo(0);
            assertThat(tokenInfo.totalSupply).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("Can create token with different decimal precision values")
    void canCreateTokenWithDifferentDecimalPrecisionValues() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            int[] decimalValues = {0, 1, 2, 6, 8, 18};

            for (int decimals : decimalValues) {
                long userInputSupply = 100;
                long expectedSupply = userInputSupply * (long) Math.pow(10, decimals);

                var response = new TokenCreateTransaction()
                        .setTokenName("DecimalTest" + decimals)
                        .setTokenSymbol("DT" + decimals)
                        .setDecimals(decimals)
                        .setInitialSupply(expectedSupply)
                        .setTreasuryAccountId(testEnv.operatorId)
                        .setAdminKey(testEnv.operatorKey)
                        .execute(testEnv.client);

                var tokenId = response.getReceipt(testEnv.client).tokenId;
                var tokenInfo = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

                assertThat(tokenInfo.decimals).isEqualTo(decimals);
                assertThat(tokenInfo.totalSupply).isEqualTo(expectedSupply);
            }
        }
    }

    @Test
    @DisplayName("Can create token when autoRenewPeriod is null")
    void canCreateTokenWhenAutoRenewPeriodIsNull() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {
            // Calculate expiration time 90 days from now
            var expirationTime = Instant.now().plusSeconds(90 * 24 * 60 * 60);

            var response = new TokenCreateTransaction()
                    .setTokenName("TEST")
                    .setTokenSymbol("TEST")
                    .setTokenType(TokenType.FUNGIBLE_COMMON)
                    .setSupplyType(TokenSupplyType.INFINITE)
                    .setAutoRenewAccountId(testEnv.operatorId)
                    .setInitialSupply(1)
                    .setMaxTransactionFee(new Hbar(100))
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setExpirationTime(expirationTime)
                    .setDecimals(0)
                    // Note: autoRenewPeriod is intentionally NOT set (null)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);

            assertThat(receipt.status).isEqualTo(Status.SUCCESS);

            var tokenId = receipt.tokenId;
            assertThat(tokenId).isNotNull();

            var tokenInfo = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);
            assertThat(tokenInfo.name).isEqualTo("TEST");
            assertThat(tokenInfo.symbol).isEqualTo("TEST");
            assertThat(tokenInfo.tokenType).isEqualTo(TokenType.FUNGIBLE_COMMON);
            assertThat(tokenInfo.supplyType).isEqualTo(TokenSupplyType.INFINITE);
            assertThat(tokenInfo.autoRenewAccount).isEqualTo(testEnv.operatorId);
            assertThat(tokenInfo.expirationTime.getEpochSecond()).isEqualTo(expirationTime.getEpochSecond());
        }
    }
}
