// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Objects;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.Status;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenDeleteTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenDeleteIntegrationTest {
    @Test
    @DisplayName("Can delete token")
    void canDeleteToken() throws Exception {
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
                    .setFreezeDefault(false)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            new TokenDeleteTransaction()
                    .setTokenId(tokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Can delete token with only admin key set")
    void canDeleteTokenWithOnlyAdminKeySet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .execute(testEnv.client);

            Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);
        }
    }

    @Test
    @DisplayName("Cannot delete token when admin key does not sign transaction")
    void cannotDeleteTokenWhenAdminKeyDoesNotSignTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var key = PrivateKey.generateED25519();

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(key)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        new TokenDeleteTransaction()
                                .setTokenId(tokenId)
                                .execute(testEnv.client)
                                .getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_SIGNATURE.toString());

            new TokenDeleteTransaction()
                    .setTokenId(tokenId)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot delete token when token ID is not set")
    void cannotDeleteTokenWhenTokenIDIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new TokenDeleteTransaction().execute(testEnv.client).getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_TOKEN_ID.toString());
        }
    }
}
