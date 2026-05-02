// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Collections;
import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.CustomFixedFee;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.hiero.sdk.TokenAssociateTransaction;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenGrantKycTransaction;
import org.hiero.sdk.TransactionResponse;
import org.hiero.sdk.TransferTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenTransferIntegrationTest {
    @Test
    @DisplayName("Can transfer tokens")
    void tokenTransferTest() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var key = PrivateKey.generateED25519();

            TransactionResponse response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client);

            var accountId = response.getReceipt(testEnv.client).accountId;
            assertThat(accountId).isNotNull();

            response = new TokenCreateTransaction()
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
                    .setFreezeDefault(false)
                    .execute(testEnv.client);

            var tokenId = response.getReceipt(testEnv.client).tokenId;
            assertThat(tokenId).isNotNull();

            new TokenAssociateTransaction()
                    .setAccountId(accountId)
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenGrantKycTransaction()
                    .setAccountId(accountId)
                    .setTokenId(tokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TransferTransaction()
                    .addTokenTransfer(tokenId, testEnv.operatorId, -10)
                    .addTokenTransfer(tokenId, accountId, 10)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot transfer tokens if balance is insufficient to pay fee")
    void insufficientBalanceForFee() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            PrivateKey key1 = PrivateKey.generateED25519();
            PrivateKey key2 = PrivateKey.generateED25519();
            var accountId1 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key1)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;
            var accountId2 = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key2)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var tokenId = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setInitialSupply(1)
                    .setCustomFees(Collections.singletonList(
                            new CustomFixedFee().setAmount(5000_000_000L).setFeeCollectorAccountId(testEnv.operatorId)))
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setFeeScheduleKey(testEnv.operatorKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            new TokenAssociateTransaction()
                    .setAccountId(accountId1)
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .sign(key1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenAssociateTransaction()
                    .setAccountId(accountId2)
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .sign(key2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TransferTransaction()
                    .addTokenTransfer(tokenId, testEnv.operatorId, -1)
                    .addTokenTransfer(tokenId, accountId1, 1)
                    .freezeWith(testEnv.client)
                    .sign(key1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TransferTransaction()
                                .addTokenTransfer(tokenId, accountId1, -1)
                                .addTokenTransfer(tokenId, accountId2, 1)
                                .freezeWith(testEnv.client)
                                .sign(key1)
                                .sign(key2)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .satisfies(error -> assertThat(error.getMessage())
                            .containsAnyOf(
                                    Status.INSUFFICIENT_SENDER_ACCOUNT_BALANCE_FOR_CUSTOM_FEE.toString(),
                                    Status.INSUFFICIENT_PAYER_BALANCE_FOR_CUSTOM_FEE.toString()));
        }
    }
}
