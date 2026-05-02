// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.hiero.sdk.TokenAssociateTransaction;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenFreezeTransaction;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenFreezeIntegrationTest {
    @Test
    @DisplayName("Can freeze account with token")
    void canFreezeAccountWithToken() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var key = PrivateKey.generateED25519();

            var response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client);

            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);

            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
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
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            new TokenAssociateTransaction()
                    .setAccountId(accountId)
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenFreezeTransaction()
                    .setAccountId(accountId)
                    .setTokenId(tokenId)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot freeze account on token when token ID is not set")
    void cannotFreezeAccountOnTokenWhenTokenIDIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var key = PrivateKey.generateED25519();

            var response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client);

            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenFreezeTransaction()
                                .setAccountId(accountId)
                                .freezeWith(testEnv.client)
                                .sign(key)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_TOKEN_ID.toString());
        }
    }

    @Test
    @DisplayName("Cannot freeze account on token when account ID is not set")
    void cannotFreezeAccountOnTokenWhenAccountIDIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var key = PrivateKey.generateED25519();

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
                    .setFreezeDefault(false)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenFreezeTransaction()
                                .setTokenId(tokenId)
                                .freezeWith(testEnv.client)
                                .sign(key)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_ACCOUNT_ID.toString());
        }
    }

    @Test
    @DisplayName("Cannot freeze account on token when account was not associated with")
    void cannotFreezeAccountOnTokenWhenAccountWasNotAssociatedWith() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var key = PrivateKey.generateED25519();

            var response = new AccountCreateTransaction()
                    .setKeyWithoutAlias(key)
                    .setInitialBalance(new Hbar(1))
                    .execute(testEnv.client);

            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);

            var tokenId = Objects.requireNonNull(new TokenCreateTransaction()
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
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenFreezeTransaction()
                                .setAccountId(accountId)
                                .setTokenId(tokenId)
                                .freezeWith(testEnv.client)
                                .sign(key)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.TOKEN_NOT_ASSOCIATED_TO_ACCOUNT.toString());
        }
    }
}

