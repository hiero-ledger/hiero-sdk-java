// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to estimate transaction fees using the mirror node's fee estimation service.
 * <p>
 * This example demonstrates:
 * 1. Creating and freezing a transfer transaction
 * 2. Estimating fees with STATE mode (considers current network state)
 * 3. Estimating fees with INTRINSIC mode (only transaction properties)
 * 4. Displaying detailed fee breakdowns
 */
class FeeEstimateQueryExample {

    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * Operator's account ID.
     * Used to sign and pay for operations on Hedera.
     */
    private static final AccountId OPERATOR_ID =
        AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
        PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file.
     * Network can be: localhost, testnet, previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file.
     * Log levels can be: TRACE, DEBUG, INFO, WARN, ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL,
     * for example via VM options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        System.out.println("Fee Estimate Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        // Create a recipient account for the example
        AccountId recipientId = AccountId.fromString("0.0.3");

        /*
         * Step 1:
         * Create and freeze a transfer transaction.
         * The transaction must be frozen before it can be estimated.
         */
        System.out.println("\n=== Creating Transfer Transaction ===");
        Hbar transferAmount = Hbar.from(1);

        TransferTransaction tx = new TransferTransaction()
            .addHbarTransfer(OPERATOR_ID, transferAmount.negated())
            .addHbarTransfer(recipientId, transferAmount)
            .setTransactionMemo("Fee estimate example")
            .freezeWith(client);

        // Sign the transaction (required for accurate fee estimation)
        tx.signWithOperator(client);

        System.out.println("Transaction created: Transfer " + transferAmount + " from "
            + OPERATOR_ID + " to " + recipientId);

        /*
         * Step 2:
         * Estimate fees with STATE mode (default).
         * STATE mode considers the current network state (e.g., whether accounts exist,
         * token associations, etc.) for more accurate fee estimation.
         */
        System.out.println("\n=== Estimating Fees with STATE Mode ===");

        FeeEstimateResponse stateEstimate = new FeeEstimateQuery()
            .setMode(FeeEstimateMode.STATE)
            .setTransaction(tx)
            .execute(client);

        System.out.println("Mode: " + stateEstimate.getMode());

        // Network fee breakdown
        System.out.println("\nNetwork Fee:");
        System.out.println("  Multiplier: " + stateEstimate.getNetwork().getMultiplier());
        System.out.println("  Subtotal: " + stateEstimate.getNetwork().getSubtotal() + " tinycents");

        // Node fee breakdown
        System.out.println("\nNode Fee:");
        System.out.println("  Base: " + stateEstimate.getNode().getBase() + " tinycents");
        long nodeTotal = stateEstimate.getNode().getBase();
        for (FeeExtra extra : stateEstimate.getNode().getExtras()) {
            System.out.println("  Extra - " + extra.getName() + ": " + extra.getSubtotal() + " tinycents");
            nodeTotal += extra.getSubtotal();
        }
        System.out.println("  Node Total: " + nodeTotal + " tinycents");

        // Service fee breakdown
        System.out.println("\nService Fee:");
        System.out.println("  Base: " + stateEstimate.getService().getBase() + " tinycents");
        long serviceTotal = stateEstimate.getService().getBase();
        for (FeeExtra extra : stateEstimate.getService().getExtras()) {
            System.out.println("  Extra - " + extra.getName() + ": " + extra.getSubtotal() + " tinycents");
            serviceTotal += extra.getSubtotal();
        }
        System.out.println("  Service Total: " + serviceTotal + " tinycents");

        // Total fee
        System.out.println("\nTotal Estimated Fee: " + stateEstimate.getTotal() + " tinycents");
        System.out.println("Total Estimated Fee: " + Hbar.fromTinybars(stateEstimate.getTotal() / 100));

        // Display any notes/caveats
        if (!stateEstimate.getNotes().isEmpty()) {
            System.out.println("\nNotes:");
            for (String note : stateEstimate.getNotes()) {
                System.out.println("  - " + note);
            }
        }

        /*
         * Step 3:
         * Estimate fees with INTRINSIC mode.
         * INTRINSIC mode only considers the transaction's inherent properties
         * (size, signatures, keys) and ignores state-dependent factors.
         */
        System.out.println("\n=== Estimating Fees with INTRINSIC Mode ===");

        FeeEstimateResponse intrinsicEstimate = new FeeEstimateQuery()
            .setMode(FeeEstimateMode.INTRINSIC)
            .setTransaction(tx)
            .execute(client);

        System.out.println("Mode: " + intrinsicEstimate.getMode());
        System.out.println("Network Fee Subtotal: " + intrinsicEstimate.getNetwork().getSubtotal() + " tinycents");
        System.out.println("Node Fee Base: " + intrinsicEstimate.getNode().getBase() + " tinycents");
        System.out.println("Service Fee Base: " + intrinsicEstimate.getService().getBase() + " tinycents");
        System.out.println("Total Estimated Fee: " + intrinsicEstimate.getTotal() + " tinycents");
        System.out.println("Total Estimated Fee: " + Hbar.fromTinybars(intrinsicEstimate.getTotal() / 100));

        /*
         * Step 4:
         * Compare STATE vs INTRINSIC mode estimates.
         */
        System.out.println("\n=== Comparison ===");
        System.out.println("STATE mode total: " + stateEstimate.getTotal() + " tinycents");
        System.out.println("INTRINSIC mode total: " + intrinsicEstimate.getTotal() + " tinycents");
        long difference = Math.abs(stateEstimate.getTotal() - intrinsicEstimate.getTotal());
        System.out.println("Difference: " + difference + " tinycents");

        /*
         * Step 5:
         * Demonstrate fee estimation for a token creation transaction.
         */
        System.out.println("\n=== Estimating Token Creation Fees ===");

        TokenCreateTransaction tokenTx = new TokenCreateTransaction()
            .setTokenName("Example Token")
            .setTokenSymbol("EXT")
            .setDecimals(3)
            .setInitialSupply(1000000)
            .setTreasuryAccountId(OPERATOR_ID)
            .setAdminKey(OPERATOR_KEY)
            .freezeWith(client)
            .signWithOperator(client);

        FeeEstimateResponse tokenEstimate = new FeeEstimateQuery()
            .setMode(FeeEstimateMode.STATE)
            .setTransaction(tokenTx)
            .execute(client);

        System.out.println("Token Creation Estimated Fee: " + tokenEstimate.getTotal() + " tinycents");
        System.out.println("Token Creation Estimated Fee: " + Hbar.fromTinybars(tokenEstimate.getTotal() / 100));

        /*
         * Clean up:
         */
        client.close();
        System.out.println("\nExample complete!");
    }
}

