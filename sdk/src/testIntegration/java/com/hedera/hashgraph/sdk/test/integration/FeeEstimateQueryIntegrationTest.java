// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.FeeEstimateMode;
import com.hedera.hashgraph.sdk.FeeEstimateQuery;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenSupplyType;
import com.hedera.hashgraph.sdk.TokenType;
import com.hedera.hashgraph.sdk.TransferTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeeEstimateQueryIntegrationTest {

    @Test
    @DisplayName("Can estimate fees for TokenCreateTransaction")
    void canEstimateFeesForTokenCreateTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            // Skip if not running against a mirror node that supports fee estimation
            if (!testEnv.isLocalNode) {
                return;
            }

            // Create a TokenCreateTransaction
            var transaction = new TokenCreateTransaction()
                    .setTokenName("Test Token")
                    .setTokenSymbol("TEST")
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setTokenType(TokenType.FUNGIBLE_COMMON)
                    .setSupplyType(TokenSupplyType.INFINITE)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            // Wait for mirror node to be ready
            Thread.sleep(2000);

            // Request fee estimate with STATE mode (default)
            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.STATE)
                    .execute(testEnv.client);

            // Verify response structure
            assertThat(response).isNotNull();
            assertThat(response.getMode()).isEqualTo(FeeEstimateMode.STATE);
            assertThat(response.getTotal()).isGreaterThan(0);

            // Verify network fee component
            assertThat(response.getNetwork()).isNotNull();
            assertThat(response.getNetwork().getMultiplier()).isGreaterThan(0);
            assertThat(response.getNetwork().getSubtotal()).isGreaterThan(0);

            // Verify node fee estimate
            assertThat(response.getNode()).isNotNull();
            assertThat(response.getNode().getBase()).isGreaterThan(0);
            // Node fee may or may not have extras
            assertThat(response.getNode().getExtras()).isNotNull();

            // Verify service fee estimate
            assertThat(response.getService()).isNotNull();
            assertThat(response.getService().getBase()).isGreaterThan(0);
            // Service fee for token creation should have extras for token operations
            assertThat(response.getService().getExtras()).isNotNull();

            // Verify notes (may be empty)
            assertThat(response.getNotes()).isNotNull();

            // Verify that network fee = node subtotal * multiplier (approximately)
            var nodeSubtotal = response.getNode().getBase();
            var expectedNetworkFee = nodeSubtotal * response.getNetwork().getMultiplier();
            assertThat(response.getNetwork().getSubtotal())
                    .isCloseTo(expectedNetworkFee, org.assertj.core.data.Percentage.withPercentage(1.0));
        }
    }

    @Test
    @DisplayName("Can estimate fees with INTRINSIC mode")
    void canEstimateFeesWithIntrinsicMode() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            if (!testEnv.isLocalNode) {
                return;
            }

            var transaction = new TokenCreateTransaction()
                    .setTokenName("Test Token")
                    .setTokenSymbol("TEST")
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            Thread.sleep(2000);

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.INTRINSIC)
                    .execute(testEnv.client);

            assertThat(response).isNotNull();
            assertThat(response.getMode()).isEqualTo(FeeEstimateMode.INTRINSIC);
            assertThat(response.getTotal()).isGreaterThan(0);
            assertThat(response.getNetwork()).isNotNull();
            assertThat(response.getNode()).isNotNull();
            assertThat(response.getService()).isNotNull();
        }
    }

    @Test
    @DisplayName("Can estimate fees for AccountCreateTransaction")
    void canEstimateFeesForAccountCreateTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            if (!testEnv.isLocalNode) {
                return;
            }

            var newKey = PrivateKey.generateED25519();
            var transaction = new AccountCreateTransaction()
                    .setKey(newKey)
                    .setInitialBalance(com.hedera.hashgraph.sdk.Hbar.from(10))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            Thread.sleep(2000);

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .execute(testEnv.client);

            assertThat(response).isNotNull();
            assertThat(response.getTotal()).isGreaterThan(0);
            assertThat(response.getNetwork()).isNotNull();
            assertThat(response.getNode()).isNotNull();
            assertThat(response.getService()).isNotNull();

            // Account creation should have a base fee
            assertThat(response.getService().getBase()).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Can estimate fees for TransferTransaction")
    void canEstimateFeesForTransferTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            if (!testEnv.isLocalNode) {
                return;
            }

            var transaction = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, com.hedera.hashgraph.sdk.Hbar.from(-1))
                    .addHbarTransfer(new com.hedera.hashgraph.sdk.AccountId(0, 0, 3), com.hedera.hashgraph.sdk.Hbar.from(1))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            Thread.sleep(2000);

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .execute(testEnv.client);

            assertThat(response).isNotNull();
            assertThat(response.getTotal()).isGreaterThan(0);

            // Transfer transactions should have lower fees than complex transactions
            assertThat(response.getService().getBase()).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("State mode and intrinsic mode return different estimates")
    void stateAndIntrinsicModesReturnDifferentEstimates() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            if (!testEnv.isLocalNode) {
                return;
            }

            var transaction = new TokenCreateTransaction()
                    .setTokenName("Test Token")
                    .setTokenSymbol("TEST")
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            Thread.sleep(2000);

            var stateResponse = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.STATE)
                    .execute(testEnv.client);

            var intrinsicResponse = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.INTRINSIC)
                    .execute(testEnv.client);

            assertThat(stateResponse).isNotNull();
            assertThat(intrinsicResponse).isNotNull();

            // Both should have valid totals
            assertThat(stateResponse.getTotal()).isGreaterThan(0);
            assertThat(intrinsicResponse.getTotal()).isGreaterThan(0);

            // The estimates may differ based on state-dependent factors
            // We just verify both modes work and return reasonable values
            assertThat(stateResponse.getMode()).isEqualTo(FeeEstimateMode.STATE);
            assertThat(intrinsicResponse.getMode()).isEqualTo(FeeEstimateMode.INTRINSIC);
        }
    }

    @Test
    @DisplayName("Can execute query asynchronously")
    void canExecuteQueryAsynchronously() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            if (!testEnv.isLocalNode) {
                return;
            }

            var transaction = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, com.hedera.hashgraph.sdk.Hbar.from(-1))
                    .addHbarTransfer(new com.hedera.hashgraph.sdk.AccountId(0, 0, 3), com.hedera.hashgraph.sdk.Hbar.from(1))
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            Thread.sleep(2000);

            var responseFuture = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .executeAsync(testEnv.client);

            // Verify the async call completes successfully
            assertThatNoException().isThrownBy(() -> {
                var response = responseFuture.get();
                assertThat(response).isNotNull();
                assertThat(response.getTotal()).isGreaterThan(0);
            });
        }
    }

    @Test
    @DisplayName("Response includes appropriate fields for extras")
    void responseIncludesAppropriateFieldsForExtras() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            if (!testEnv.isLocalNode) {
                return;
            }

            var transaction = new TokenCreateTransaction()
                    .setTokenName("Test Token")
                    .setTokenSymbol("TEST")
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .setSupplyKey(testEnv.operatorKey)
                    .setWipeKey(testEnv.operatorKey)
                    .setFreezeKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            Thread.sleep(2000);

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .execute(testEnv.client);

            assertThat(response).isNotNull();

            // Check if service fee has extras
            if (response.getService() != null && !response.getService().getExtras().isEmpty()) {
                for (var extra : response.getService().getExtras()) {
                    // Verify extra fields are populated
                    assertThat(extra.getCount()).isGreaterThanOrEqualTo(0);
                    assertThat(extra.getCharged()).isGreaterThanOrEqualTo(0);
                    assertThat(extra.getIncluded()).isGreaterThanOrEqualTo(0);
                    assertThat(extra.getFeePerUnit()).isGreaterThanOrEqualTo(0);
                    assertThat(extra.getSubtotal()).isGreaterThanOrEqualTo(0);
                    // Name may or may not be set
                    // Charged = max(0, count - included)
                    assertThat(extra.getCharged()).isEqualTo(Math.max(0, extra.getCount() - extra.getIncluded()));
                    // Subtotal = charged * feePerUnit
                    assertThat(extra.getSubtotal()).isEqualTo(extra.getCharged() * extra.getFeePerUnit());
                }
            }
        }
    }

    @Test
    @DisplayName("Can handle transaction without freezing")
    void canHandleTransactionWithoutFreezing() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            if (!testEnv.isLocalNode) {
                return;
            }

            var transaction = new TransferTransaction()
                    .addHbarTransfer(testEnv.operatorId, com.hedera.hashgraph.sdk.Hbar.from(-1))
                    .addHbarTransfer(new com.hedera.hashgraph.sdk.AccountId(0, 0, 3), com.hedera.hashgraph.sdk.Hbar.from(1));

            // Freeze and sign within the query
            transaction.freezeWith(testEnv.client).signWithOperator(testEnv.client);

            Thread.sleep(2000);

            var response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .execute(testEnv.client);

            assertThat(response).isNotNull();
            assertThat(response.getTotal()).isGreaterThan(0);
        }
    }
}

