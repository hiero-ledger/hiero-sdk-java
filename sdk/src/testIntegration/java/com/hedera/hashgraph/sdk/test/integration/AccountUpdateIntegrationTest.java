// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.*;
import java.time.Duration;
import java.util.Objects;
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

    // HIP-1340: EOA Code Delegation

    @Test
    @DisplayName("Can update account with delegation address")
    void canUpdateAccountWithDelegationAddress() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            var delegationAddr = "0x1111111111111111111111111111111111111111";
            var expectedBytes = EvmAddress.fromString(delegationAddr).toBytes();

            var createResponse = new AccountCreateTransaction().setKey(key).execute(testEnv.client);

            var accountId = Objects.requireNonNull(createResponse.getReceipt(testEnv.client).accountId);

            new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .setDelegationAddress(delegationAddr)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);

            assertThat(info.accountId).isEqualTo(accountId);
            assertThat(info.delegationAddress).isNotNull();
            assertThat(info.delegationAddress.toBytes()).isEqualTo(expectedBytes);
        }
    }

    @Test
    @DisplayName("Can clear delegation address by setting to null")
    void canClearDelegationAddress() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var key = PrivateKey.generateED25519();
            var delegationAddr = "0x2222222222222222222222222222222222222222";

            var createResponse = new AccountCreateTransaction()
                    .setKey(key)
                    .setDelegationAddress(delegationAddr)
                    .execute(testEnv.client);

            var accountId = Objects.requireNonNull(createResponse.getReceipt(testEnv.client).accountId);

            // Verify delegation address is set
            var info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);
            assertThat(info.delegationAddress).isNotNull();

            new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .setDelegationAddress((String) null)
                    .freezeWith(testEnv.client)
                    .sign(key)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // Verify delegation address is cleared
            info = new AccountInfoQuery().setAccountId(accountId).execute(testEnv.client);
            assertThat(info.delegationAddress).isNull();
        }
    }
}
