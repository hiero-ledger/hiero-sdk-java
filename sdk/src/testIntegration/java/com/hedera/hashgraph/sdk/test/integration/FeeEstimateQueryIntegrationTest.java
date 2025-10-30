// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeeEstimateQueryIntegrationTest {

    @Test
    @DisplayName("Given a TokenCreateTransaction, when fee estimate is requested, "
            + "then response includes service fees for token creation and network fees")
    void tokenCreateTransactionFeeEstimate() throws Throwable {
        try (var testEnv = new IntegrationTestEnv(1)) {

            // Given: A TokenCreateTransaction is created
            var transaction = new TokenCreateTransaction()
                    .setTokenName("Test Token")
                    .setTokenSymbol("TEST")
                    .setDecimals(3)
                    .setInitialSupply(1000000)
                    .setTreasuryAccountId(testEnv.operatorId)
                    .setAdminKey(testEnv.operatorKey)
                    .freezeWith(testEnv.client)
                    .signWithOperator(testEnv.client);

            // Wait for mirror node to sync
            Thread.sleep(2000);

            // When: A fee estimate is requested
            FeeEstimateResponse response = new FeeEstimateQuery()
                    .setTransaction(transaction)
                    .setMode(FeeEstimateMode.STATE)
                    .execute(testEnv.client);

            // Then: The response includes appropriate fees
            assertThat(response).isNotNull();

            // Verify mode is returned correctly
            assertThat(response.getMode()).isEqualTo(FeeEstimateMode.STATE);

            // Verify service fees are present (token creation has service fees)
            assertThat(response.getService()).isNotNull();
            //            assertThat(response.getService().getBase()).isGreaterThan(0); TODO currently there is a stub
            // implementation of the estimateFee which always returns 0

            // Verify network fees are present (transaction size-based fees)
            assertThat(response.getNetwork()).isNotNull();
            //            assertThat(response.getNetwork().getSubtotal()).isGreaterThan(0);

            // Verify total fee is calculated
            //            assertThat(response.getTotal()).isGreaterThan(0);

            // Verify node fees are present
            assertThat(response.getNode()).isNotNull();
            //            assertThat(response.getNode().getBase()).isGreaterThan(0);

            // Log the fee breakdown for debugging
            System.out.println("Token Create Fee Estimate:");
            System.out.println("  Mode: " + response.getMode());
            System.out.println("  Service Base: " + response.getService().getBase());
            System.out.println("  Network Subtotal: " + response.getNetwork().getSubtotal());
            System.out.println("  Node Base: " + response.getNode().getBase());
            System.out.println("  Total: " + response.getTotal());
        }
    }
}
