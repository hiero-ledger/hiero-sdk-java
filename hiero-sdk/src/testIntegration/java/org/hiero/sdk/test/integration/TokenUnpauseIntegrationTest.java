// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.AccountDeleteTransaction;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.TokenAssociateTransaction;
import org.hiero.sdk.TokenCreateTransaction;
import org.hiero.sdk.TokenDeleteTransaction;
import org.hiero.sdk.TokenUnpauseTransaction;
import org.hiero.sdk.TokenWipeTransaction;
import org.hiero.sdk.TransferTransaction;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TokenUnpauseIntegrationTest {

    @Test
    @DisplayName("Can execute token unpause transaction")
    void canExecuteTokenUnpauseTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var accountKey = PrivateKey.generateED25519();
            var testTokenAmount = 10;
            var accountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(accountKey)
                    .setInitialBalance(new Hbar(2))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            var tokenId = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setInitialSupply(1000000)
                    .setDecimals(3)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setPauseKey(testEnv.operatorKey)
                    .setWipeKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .tokenId;

            new TokenAssociateTransaction()
                    .setAccountId(accountId)
                    .setTokenIds(Collections.singletonList(tokenId))
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenUnpauseTransaction()
                    .setTokenId(tokenId)
                    .freezeWith(testEnv.client)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TransferTransaction()
                    .addTokenTransfer(tokenId, accountId, testTokenAmount)
                    .addTokenTransfer(tokenId, testEnv.operatorId, -testTokenAmount)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenWipeTransaction()
                    .setTokenId(tokenId)
                    .setAccountId(accountId)
                    .setAmount(testTokenAmount)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new TokenDeleteTransaction()
                    .setTokenId(tokenId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new AccountDeleteTransaction()
                    .setTransferAccountId(testEnv.operatorId)
                    .setAccountId(accountId)
                    .freezeWith(testEnv.client)
                    .sign(accountKey)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Cannot unpause with no token ID")
    void cannotUnpauseWithNoTokenId() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            assertThrows(PrecheckStatusException.class, () -> {
                new TokenUnpauseTransaction().execute(testEnv.client).getReceipt(testEnv.client);
            });
        }
    }
}

