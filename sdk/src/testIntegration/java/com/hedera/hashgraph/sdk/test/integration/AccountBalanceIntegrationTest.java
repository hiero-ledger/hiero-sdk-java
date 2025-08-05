// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountBalanceIntegrationTest {
    @Test
    @DisplayName("can connect to previewnwet with TLS")
    void canConnectToPreviewnetWithTLS() throws Exception {
        var client = Client.forPreviewnet().setTransportSecurity(true);

        boolean succeededAtLeastOnce = false;

        for (var entry : client.getNetwork().entrySet()) {
            assertThat(entry.getKey().endsWith(":50212")).isTrue();

            try {
                new AccountBalanceQuery()
                        .setMaxAttempts(1)
                        .setNodeAccountIds(Collections.singletonList(entry.getValue()))
                        .setAccountId(entry.getValue())
                        .execute(client);
                System.out.println("succeeded for " + entry);
                succeededAtLeastOnce = true;
            } catch (Throwable error) {
                System.out.println("failed for " + entry);
            }
        }

        client.close();
        assertThat(succeededAtLeastOnce).isTrue();
    }

    @Test
    @DisplayName("can connect to testnet with TLS")
    void canConnectToTestnetWithTLS() throws Exception {
        var client = Client.forTestnet().setTransportSecurity(true);

        boolean succeededAtLeastOnce = false;

        for (var entry : client.getNetwork().entrySet()) {
            assertThat(entry.getKey().endsWith(":50212")).isTrue();

            try {
                new AccountBalanceQuery()
                        .setMaxAttempts(1)
                        .setNodeAccountIds(Collections.singletonList(entry.getValue()))
                        .setAccountId(entry.getValue())
                        .execute(client);
                System.out.println("succeeded for " + entry);
                succeededAtLeastOnce = true;
            } catch (Throwable error) {
                System.out.println("failed for " + entry);
            }
        }

        client.close();
        assertThat(succeededAtLeastOnce).isTrue();
    }

    @Test
    @Disabled
    @DisplayName("can connect to mainnet with TLS")
    void canConnectToMainnetWithTLS() throws Exception {
        var client = Client.forMainnet().setTransportSecurity(true);

        boolean succeededAtLeastOnce = false;

        for (var entry : client.getNetwork().entrySet()) {
            assertThat(entry.getKey().endsWith(":50212")).isTrue();

            try {
                new AccountBalanceQuery()
                        .setMaxAttempts(1)
                        .setNodeAccountIds(Collections.singletonList(entry.getValue()))
                        .setAccountId(entry.getValue())
                        .execute(client);
                System.out.println("succeeded for " + entry);
                succeededAtLeastOnce = true;
            } catch (Throwable error) {
                System.out.println("failed for " + entry);
                System.out.println(error);
            }
        }

        client.close();
        assertThat(succeededAtLeastOnce).isTrue();
    }

    @Test
    @DisplayName("can connect to previewnet with certificate verification off")
    void cannotConnectToPreviewnetWhenNetworkNameIsNullAndCertificateVerificationIsEnabled() throws Exception {
        var client = Client.forPreviewnet()
                .setTransportSecurity(true)
                .setVerifyCertificates(true)
                .setNetworkName(null);

        assertThat(client.getNetwork().isEmpty()).isFalse();

        for (var entry : client.getNetwork().entrySet()) {
            assertThat(entry.getKey().endsWith(":50212")).isTrue();

            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                new AccountBalanceQuery()
                        .setNodeAccountIds(Collections.singletonList(entry.getValue()))
                        .setAccountId(entry.getValue())
                        .execute(client);
            });
        }

        client.close();
    }

    @Test
    @DisplayName("Can fetch balance for client operator")
    void canFetchBalanceForClientOperator() throws Exception {
        try (IntegrationTestEnv testEnv = new IntegrationTestEnv(1)) {

            var balance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).execute(testEnv.client);

            assertThat(balance.hbars.toTinybars() > 0).isTrue();
        }
    }

    @Test
    @DisplayName("Can fetch cost for the query")
    void getCostBalanceForClientOperator() throws Exception {
        try (IntegrationTestEnv testEnv = new IntegrationTestEnv(1)) {

            var balance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).setMaxQueryPayment(new Hbar(1));

            var cost = balance.getCost(testEnv.client);

            var accBalance = balance.setQueryPayment(cost).execute(testEnv.client);

            assertThat(accBalance.hbars.toTinybars() > 0).isTrue();
            assertThat(cost.toTinybars()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("Can fetch cost for the query, big max set")
    void getCostBigMaxBalanceForClientOperator() throws Exception {
        try (IntegrationTestEnv testEnv = new IntegrationTestEnv(1)) {

            var balance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).setMaxQueryPayment(new Hbar(1000000));

            var cost = balance.getCost(testEnv.client);

            var accBalance = balance.setQueryPayment(cost).execute(testEnv.client);

            assertThat(accBalance.hbars.toTinybars() > 0).isTrue();
        }
    }

    @Test
    @DisplayName("Can fetch cost for the query, very small max set")
    void getCostSmallMaxBalanceForClientOperator() throws Exception {
        try (IntegrationTestEnv testEnv = new IntegrationTestEnv(1)) {

            var balance =
                    new AccountBalanceQuery().setAccountId(testEnv.operatorId).setMaxQueryPayment(Hbar.fromTinybars(1));

            var cost = balance.getCost(testEnv.client);

            var accBalance = balance.setQueryPayment(cost).execute(testEnv.client);

            assertThat(accBalance.hbars.toTinybars() > 0).isTrue();
        }
    }

    @Test
    @DisplayName("Cannot fetch balance for invalid account ID")
    void canNotFetchBalanceForInvalidAccountId() throws Exception {
        try (IntegrationTestEnv testEnv = new IntegrationTestEnv(1)) {

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> {
                        new AccountBalanceQuery()
                                .setAccountId(AccountId.fromString("1.0.3"))
                                .execute(testEnv.client);
                    })
                    .withMessageContaining(Status.INVALID_ACCOUNT_ID.toString());
        }
    }

    @Test
    @DisplayName("Can fetch token balances for client operator")
    void canFetchTokenBalancesForClientOperator() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1).useThrowawayAccount()) {

            var response = new TokenCreateTransaction()
                    .setTokenName("ffff")
                    .setTokenSymbol("F")
                    .setInitialSupply(10000)
                    .setDecimals(50)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setFreezeDefault(false)
                    .execute(testEnv.client);

            var tokenId = Objects.requireNonNull(response.getReceipt(testEnv.client).tokenId);

            var query = new AccountBalanceQuery();
            var balance = query.setAccountId(testEnv.operatorId).execute(testEnv.client);

            assertThat(balance.tokens.get(tokenId)).isEqualTo(10000);
            assertThat(balance.tokenDecimals.get(tokenId)).isEqualTo(50);
            assertThat(query.toString()).isNotEmpty();
            assertThat(query.getPaymentTransactionId()).isNull();
        }
    }
}
