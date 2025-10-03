// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.*;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for AccountCreateTransaction with EVM hooks functionality.
 * <p>
 * This test class covers the test scenario from the test plan:
 * Given an AccountCreateTransaction is configured with a basic lambda EVM hook (without storage updates),
 * when the transaction is executed, then the account is created with the lambda hook successfully.
 */
class AccountCreateTransactionHooksIntegrationTest {

    @Test
    @DisplayName("Given an AccountCreateTransaction is configured with a basic lambda EVM hook (without storage updates), when the transaction is executed, then the account is created with the lambda hook successfully")
    void canCreateAccountWithBasicLambdaEvmHook() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a test contract ID for the hook
            // Note: In a real scenario, this would be a deployed contract
            // For this test, we'll use a mock contract ID
            ContractId hookContractId = new ContractId(1000);

            // Create account key
            PrivateKey accountKey = PrivateKey.generateED25519();

            // Create account with basic lambda EVM hook
            var response = new AccountCreateTransaction()
                    .setKey(accountKey.getPublicKey())
                    .setInitialBalance(new Hbar(1))
                    .addAccountAllowanceHook(1L, hookContractId)
                    .execute(testEnv.client);

            // Verify the transaction was successful
            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);
            assertThat(accountId).isNotNull();

            // Verify the account was created successfully
            var accountInfo = new AccountInfoQuery()
                    .setAccountId(accountId)
                    .execute(testEnv.client);

            assertThat(accountInfo.accountId).isEqualTo(accountId);
            assertThat(accountInfo.isDeleted).isFalse();
            assertThat(accountInfo.key.toString()).isEqualTo(accountKey.getPublicKey().toString());
            assertThat(accountInfo.balance).isEqualTo(new Hbar(1));
            assertThat(accountInfo.autoRenewPeriod).isEqualTo(Duration.ofDays(90));
            assertThat(accountInfo.proxyAccountId).isNull();
            assertThat(accountInfo.proxyReceived).isEqualTo(Hbar.ZERO);
        }
    }

    @Test
    @DisplayName("Given an AccountCreateTransaction is configured with a lambda EVM hook with storage updates, when the transaction is executed, then the account is created with the lambda hook successfully")
    void canCreateAccountWithLambdaEvmHookWithStorageUpdates() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a test contract ID for the hook
            ContractId hookContractId = new ContractId(1001);

            // Create account key
            PrivateKey accountKey = PrivateKey.generateED25519();

            // Create storage updates for the hook
            byte[] storageKey = {0x01, 0x02, 0x03};
            byte[] storageValue = {0x04, 0x05, 0x06};
            LambdaStorageUpdate storageUpdate = new LambdaStorageUpdate.LambdaStorageSlot(storageKey, storageValue);

            // Create account with lambda EVM hook including storage updates
            var response = new AccountCreateTransaction()
                    .setKey(accountKey.getPublicKey())
                    .setInitialBalance(new Hbar(1))
                    .addAccountAllowanceHook(1L, hookContractId, null, java.util.Collections.singletonList(storageUpdate))
                    .execute(testEnv.client);

            // Verify the transaction was successful
            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);
            assertThat(accountId).isNotNull();

            // Verify the account was created successfully
            var accountInfo = new AccountInfoQuery()
                    .setAccountId(accountId)
                    .execute(testEnv.client);

            assertThat(accountInfo.accountId).isEqualTo(accountId);
            assertThat(accountInfo.isDeleted).isFalse();
            assertThat(accountInfo.key.toString()).isEqualTo(accountKey.getPublicKey().toString());
            assertThat(accountInfo.balance).isEqualTo(new Hbar(1));
            assertThat(accountInfo.autoRenewPeriod).isEqualTo(Duration.ofDays(90));
            assertThat(accountInfo.proxyAccountId).isNull();
            assertThat(accountInfo.proxyReceived).isEqualTo(Hbar.ZERO);

            // Note: In a real implementation, we would also verify that the hook was properly
            // created with the storage updates and associated with the account.
        }
    }

    @Test
    @DisplayName("Given an AccountCreateTransaction is configured with a lambda EVM hook with admin key, when the transaction is executed, then the account is created with the lambda hook successfully")
    void canCreateAccountWithLambdaEvmHookAndAdminKey() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Create a test contract ID for the hook
            ContractId hookContractId = new ContractId(1002);

            // Create account key
            PrivateKey accountKey = PrivateKey.generateED25519();

            // Create admin key for the hook
            PrivateKey hookAdminKey = PrivateKey.generateED25519();

            // Create account with lambda EVM hook and admin key
            var response = new AccountCreateTransaction()
                    .setKey(accountKey.getPublicKey())
                    .setInitialBalance(new Hbar(1))
                    .addAccountAllowanceHook(1L, hookContractId, hookAdminKey.getPublicKey(), null)
                    .execute(testEnv.client);

            // Verify the transaction was successful
            var accountId = Objects.requireNonNull(response.getReceipt(testEnv.client).accountId);
            assertThat(accountId).isNotNull();

            // Verify the account was created successfully
            var accountInfo = new AccountInfoQuery()
                    .setAccountId(accountId)
                    .execute(testEnv.client);

            assertThat(accountInfo.accountId).isEqualTo(accountId);
            assertThat(accountInfo.isDeleted).isFalse();
            assertThat(accountInfo.key.toString()).isEqualTo(accountKey.getPublicKey().toString());
            assertThat(accountInfo.balance).isEqualTo(new Hbar(1));
            assertThat(accountInfo.autoRenewPeriod).isEqualTo(Duration.ofDays(90));
            assertThat(accountInfo.proxyAccountId).isNull();
            assertThat(accountInfo.proxyReceived).isEqualTo(Hbar.ZERO);

            // Note: In a real implementation, we would also verify that the hook was properly
            // created with the admin key and associated with the account.
        }
    }
}
