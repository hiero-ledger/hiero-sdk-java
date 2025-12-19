// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.Objects;

/**
 * How to estimate transaction fees using the mirror node's fee estimation service.
 * <p>
 * This example demonstrates:
 * 1.Creating and freezing a transfer transaction
 * 2.Estimating fees with STATE mode (considers current network state)
 * 3.Estimating fees with INTRINSIC mode (only transaction properties)
 * 4.Displaying detailed fee breakdowns
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
     * Network can be:  localhost, testnet, previewnet or mainnet.
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

        Client client = createAndConfigureClient();
        AccountId recipientId = AccountId.fromString("0.0.3");

        TransferTransaction tx = createTransferTransaction(client, recipientId);
        FeeEstimateResponse stateEstimate = estimateWithStateMode(client, tx);
        FeeEstimateResponse intrinsicEstimate = estimateWithIntrinsicMode(client, tx);
        compareEstimates(stateEstimate, intrinsicEstimate);
        demonstrateTokenCreationEstimate(client);

        client.close();
        System.out.println("\nExample complete!");
    }

    private static Client createAndConfigureClient() throws Exception {
        Client client = ClientHelper.forName(HEDERA_NETWORK);

        if ("localhost".equals(HEDERA_NETWORK)) {
            client.setMirrorNetwork(List.of("127.0.0.1:8084"));
        }

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));
        return client;
    }

    private static TransferTransaction createTransferTransaction(Client client, AccountId recipientId) {
        System.out.println("\n=== Creating Transfer Transaction ===");
        Hbar transferAmount = Hbar.from(1);

        TransferTransaction tx = new TransferTransaction()
            .addHbarTransfer(OPERATOR_ID, transferAmount.negated())
            .addHbarTransfer(recipientId, transferAmount)
            .setTransactionMemo("Fee estimate example")
            .freezeWith(client);

        tx.signWithOperator(client);

        System.out.println(
            "Transaction created: Transfer " + transferAmount + " from " + OPERATOR_ID + " to " + recipientId);
        return tx;
    }

    private static FeeEstimateResponse estimateWithStateMode(Client client, TransferTransaction tx) throws Exception {
        System.out.println("\n=== Estimating Fees with STATE Mode ===");

        FeeEstimateResponse stateEstimate =
            new FeeEstimateQuery().setMode(FeeEstimateMode.STATE).setTransaction(tx).execute(client);

        System.out.println("Mode: " + stateEstimate.getMode());
        printNetworkFee(stateEstimate);
        printNodeFee(stateEstimate);
        printServiceFee(stateEstimate);
        printTotalFee(stateEstimate);
        printNotes(stateEstimate);

        return stateEstimate;
    }

    private static void printNetworkFee(FeeEstimateResponse estimate) {
        System.out.println("\nNetwork Fee:");
        System.out.println("  Multiplier: " + estimate.getNetworkFee().getMultiplier());
        System.out.println("  Subtotal: " + estimate.getNetworkFee().getSubtotal() + " tinycents");
    }

    private static void printNodeFee(FeeEstimateResponse estimate) {
        System.out.println("\nNode Fee:");
        System.out.println("  Base: " + estimate.getNodeFee().getBase() + " tinycents");
        long nodeTotal = estimate.getNodeFee().getBase();
        for (FeeExtra extra : estimate.getNodeFee().getExtras()) {
            System.out.println("  Extra - " + extra.getName() + ": " + extra.getSubtotal() + " tinycents");
            nodeTotal += extra.getSubtotal();
        }
        System.out.println("  Node Total: " + nodeTotal + " tinycents");
    }

    private static void printServiceFee(FeeEstimateResponse estimate) {
        System.out.println("\nService Fee:");
        System.out.println("  Base: " + estimate.getServiceFee().getBase() + " tinycents");
        long serviceTotal = estimate.getServiceFee().getBase();
        for (FeeExtra extra : estimate.getServiceFee().getExtras()) {
            System.out.println("  Extra - " + extra.getName() + ": " + extra.getSubtotal() + " tinycents");
            serviceTotal += extra.getSubtotal();
        }
        System.out.println("  Service Total: " + serviceTotal + " tinycents");
    }

    private static void printTotalFee(FeeEstimateResponse estimate) {
        System.out.println("\nTotal Estimated Fee: " + estimate.getTotal() + " tinycents");
        System.out.println("Total Estimated Fee: " + Hbar.fromTinybars(estimate.getTotal() / 100));
    }

    private static void printNotes(FeeEstimateResponse estimate) {
        if (!estimate.getNotes().isEmpty()) {
            System.out.println("\nNotes:");
            for (String note : estimate.getNotes()) {
                System.out.println("  - " + note);
            }
        }
    }

    private static FeeEstimateResponse estimateWithIntrinsicMode(Client client, TransferTransaction tx)
        throws Exception {
        System.out.println("\n=== Estimating Fees with INTRINSIC Mode ===");

        FeeEstimateResponse intrinsicEstimate = new FeeEstimateQuery()
            .setMode(FeeEstimateMode.INTRINSIC)
            .setTransaction(tx)
            .execute(client);

        System.out.println("Mode: " + intrinsicEstimate.getMode());
        System.out.println(
            "Network Fee Subtotal: " + intrinsicEstimate.getNetworkFee().getSubtotal() + " tinycents");
        System.out.println(
            "Node Fee Base: " + intrinsicEstimate.getNodeFee().getBase() + " tinycents");
        System.out.println(
            "Service Fee Base: " + intrinsicEstimate.getServiceFee().getBase() + " tinycents");
        System.out.println("Total Estimated Fee: " + intrinsicEstimate.getTotal() + " tinycents");
        System.out.println("Total Estimated Fee: " + Hbar.fromTinybars(intrinsicEstimate.getTotal() / 100));

        return intrinsicEstimate;
    }

    private static void compareEstimates(FeeEstimateResponse stateEstimate, FeeEstimateResponse intrinsicEstimate) {
        System.out.println("\n=== Comparison ===");
        System.out.println("STATE mode total:  " + stateEstimate.getTotal() + " tinycents");
        System.out.println("INTRINSIC mode total: " + intrinsicEstimate.getTotal() + " tinycents");
        long difference = Math.abs(stateEstimate.getTotal() - intrinsicEstimate.getTotal());
        System.out.println("Difference: " + difference + " tinycents");
    }

    private static void demonstrateTokenCreationEstimate(Client client) throws Exception {
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

        System.out.println("Token Creation Estimated Fee:  " + tokenEstimate.getTotal() + " tinycents");
        System.out.println("Token Creation Estimated Fee: " + Hbar.fromTinybars(tokenEstimate.getTotal() / 100));
    }
}
