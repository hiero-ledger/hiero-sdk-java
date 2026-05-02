// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.Objects;
import org.hiero.sdk.CustomFee;
import org.hiero.sdk.CustomFixedFee;
import org.hiero.sdk.CustomFractionalFee;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenFeeScheduleUpdateTransaction;
import org.hiero.sdk.TokenInfoQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenFeeScheduleUpdateIntegrationTest {
    @Test
    @DisplayName("Can update token fees")
    void canUpdateToken() throws Exception {
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
                    .setFreezeDefault(false)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var info = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(info.tokenId).isEqualTo(tokenId);
            assertThat(info.name).isEqualTo("ffff");
            assertThat(info.symbol).isEqualTo("F");
            assertThat(info.decimals).isEqualTo(3);
            assertThat(testEnv.operatorId).isEqualTo(info.treasuryAccountId);
            assertThat(info.adminKey).isNotNull();
            assertThat(info.freezeKey).isNotNull();
            assertThat(info.wipeKey).isNotNull();
            assertThat(info.kycKey).isNotNull();
            assertThat(info.supplyKey).isNotNull();
            assertThat(info.adminKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.freezeKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.wipeKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.kycKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.supplyKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.feeScheduleKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.defaultFreezeStatus).isNotNull();
            assertThat(info.defaultFreezeStatus).isFalse();
            assertThat(info.defaultKycStatus).isNotNull();
            assertThat(info.defaultKycStatus).isFalse();
            assertThat(info.customFees.size()).isEqualTo(0);

            var customFees = new ArrayList<CustomFee>();
            customFees.add(new CustomFixedFee().setAmount(10).setFeeCollectorAccountId(testEnv.operatorId));
            customFees.add(new CustomFractionalFee()
                    .setNumerator(1)
                    .setDenominator(20)
                    .setMin(1)
                    .setMax(10)
                    .setFeeCollectorAccountId(testEnv.operatorId));

            new TokenFeeScheduleUpdateTransaction()
                    .setTokenId(tokenId)
                    .setCustomFees(customFees)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            info = new TokenInfoQuery().setTokenId(tokenId).execute(testEnv.client);

            assertThat(info.tokenId).isEqualTo(tokenId);
            assertThat(info.name).isEqualTo("ffff");
            assertThat(info.symbol).isEqualTo("F");
            assertThat(info.decimals).isEqualTo(3);
            assertThat(info.treasuryAccountId).isEqualTo(testEnv.operatorId);
            assertThat(info.adminKey).isNotNull();
            assertThat(info.freezeKey).isNotNull();
            assertThat(info.wipeKey).isNotNull();
            assertThat(info.kycKey).isNotNull();
            assertThat(info.supplyKey).isNotNull();
            assertThat(info.adminKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.freezeKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.wipeKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.kycKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.supplyKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.feeScheduleKey.toString()).isEqualTo(testEnv.operatorKey.toString());
            assertThat(info.defaultFreezeStatus).isNotNull();
            assertThat(info.defaultFreezeStatus).isFalse();
            assertThat(info.defaultKycStatus).isNotNull();
            assertThat(info.defaultKycStatus).isFalse();

            var fees = info.customFees;
            assertThat(fees.size()).isEqualTo(2);
            int fixedCount = 0;
            int fractionalCount = 0;
            for (var fee : fees) {
                if (fee instanceof CustomFixedFee) {
                    fixedCount++;
                    var fixed = (CustomFixedFee) fee;
                    assertThat(fixed.getAmount()).isEqualTo(10);
                    assertThat(fixed.getFeeCollectorAccountId()).isEqualTo(testEnv.operatorId);
                    assertThat(fixed.getDenominatingTokenId()).isNull();
                } else if (fee instanceof CustomFractionalFee) {
                    fractionalCount++;
                    var fractional = (CustomFractionalFee) fee;
                    assertThat(fractional.getNumerator()).isEqualTo(1);
                    assertThat(fractional.getDenominator()).isEqualTo(20);
                    assertThat(fractional.getMin()).isEqualTo(1);
                    assertThat(fractional.getMax()).isEqualTo(10);
                    assertThat(fractional.getFeeCollectorAccountId()).isEqualTo(testEnv.operatorId);
                }
            }
            assertThat(fixedCount).isEqualTo(1);
            assertThat(fractionalCount).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Cannot update fee schedule with any key other than fee schedule key")
    void cannotUpdateWithAnyOtherKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setFeeScheduleKey(PrivateKey.generate())
                    .setFreezeDefault(false)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var customFees = new ArrayList<CustomFee>();
            customFees.add(new CustomFixedFee().setAmount(10).setFeeCollectorAccountId(testEnv.operatorId));
            customFees.add(new CustomFractionalFee()
                    .setNumerator(1)
                    .setDenominator(20)
                    .setMin(1)
                    .setMax(10)
                    .setFeeCollectorAccountId(testEnv.operatorId));

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenFeeScheduleUpdateTransaction()
                                .setTokenId(tokenId)
                                .setCustomFees(customFees)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());
        }
    }
}
