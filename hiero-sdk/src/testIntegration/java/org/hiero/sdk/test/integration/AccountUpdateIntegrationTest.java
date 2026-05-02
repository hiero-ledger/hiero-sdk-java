// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.util.Objects;
import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.AccountInfoQuery;
import org.hiero.sdk.AccountUpdateTransaction;
import org.hiero.sdk.Hbar;
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountUpdateIntegrationTest {
    @Test
    @DisplayName("Can update account with a new key")
    void canUpdateAccountWithNewKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var key1 = PrivateKey.generateED25519();
            var key2 = PrivateKey.generateED25519();

            var response = new AccountCreateTransaction().setKey(key1).execute(testEnv.client);

            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isEqualTo(accountId);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.key.toString()).isEqualTo(key1.getPublicKey().toString());
            assertThat(info.balance).isEqualTo(new Hbar(0));
            assertThat(info.autoRenewPeriod).isEqualTo(Duration.ofDays(90));
            assertThat(info.proxyAccountId).isNull();
            assertThat(info.proxyReceived).isEqualTo(Hbar.ZERO);

            new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .setKey(key2.getPublicKey())
                    .freezeWith(testEnv.client)
                    .sign(key1)
                    .sign(key2)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isEqualTo(accountId);
            assertThat(info.isDeleted).isFalse();
            assertThat(info.key.toString()).isEqualTo(key2.getPublicKey().toString());
            assertThat(info.balance).isEqualTo(new Hbar(0));
            assertThat(info.autoRenewPeriod).isEqualTo(Duration.ofDays(90));
            assertThat(info.proxyAccountId).isNull();
            assertThat(info.proxyReceived).isEqualTo(Hbar.ZERO);
        }
    }

    @Test
    @DisplayName("Cannot update account when account ID is not set")
    void cannotUpdateAccountWhenAccountIdIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new AccountUpdateTransaction().execute(testEnv.client).getReceipt(testEnv.client);
                    })
                    .withMessageContaining(Status.ACCOUNT_ID_DOES_NOT_EXIST.toString());
        }
    }
}
