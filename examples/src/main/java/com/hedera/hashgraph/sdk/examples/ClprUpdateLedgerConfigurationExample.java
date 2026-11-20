// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ClprGetLedgerConfigurationQuery;
import com.hedera.hashgraph.sdk.ClprThrottles;
import com.hedera.hashgraph.sdk.ClprUpdateLedgerConfigurationTransaction;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to change one CLPR throttle while keeping the other six (HIP-1535).
 * <p>
 * Requires a network with {@code clpr.enabled=true} and the treasury or system admin account in
 * GENESIS_OPERATOR_ID/GENESIS_OPERATOR_KEY. Set CLPR_MAX_MESSAGES_PER_BUNDLE to the new positive limit.
 * The update must send all seven limits, so read the current ones first.
 */
class ClprUpdateLedgerConfigurationExample {

    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file.
     * Network can be: localhost, testnet, previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file.
     * Log levels can be: TRACE, DEBUG, INFO, WARN, ERROR, SILENT.
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    /**
     * Treasury or system admin account ID, which is authorized to update the CLPR configuration.
     */
    private static final AccountId GENESIS_OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("GENESIS_OPERATOR_ID")));

    /**
     * Treasury or system admin private key.
     */
    private static final PrivateKey GENESIS_OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("GENESIS_OPERATOR_KEY")));

    /**
     * The new maximum number of messages per bundle.
     */
    private static final long CLPR_MAX_MESSAGES_PER_BUNDLE =
            Long.parseLong(Objects.requireNonNull(Dotenv.load().get("CLPR_MAX_MESSAGES_PER_BUNDLE")));

    public static void main(String[] args) throws Exception {
        System.out.println("CLPR Update Ledger Configuration Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client with the admin account.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        client.setOperator(GENESIS_OPERATOR_ID, GENESIS_OPERATOR_KEY);
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Read the current throttles.
         */
        var current = new ClprGetLedgerConfigurationQuery().execute(client).getThrottles();

        /*
         * Step 2:
         * Change one limit and keep the other six. ClprThrottles.of validates all seven.
         */
        var throttles = ClprThrottles.of(
                CLPR_MAX_MESSAGES_PER_BUNDLE,
                current.getMaxMessagePayloadBytes(),
                current.getMaxGasPerMessage(),
                current.getMaxQueueDepth(),
                current.getMaxSyncBytes(),
                current.getMaxLocalEndpoints(),
                current.getMaxPeerEndpoints());

        /*
         * Step 3:
         * Submit the update.
         */
        var receipt = new ClprUpdateLedgerConfigurationTransaction()
                .setThrottles(throttles)
                .execute(client)
                .getReceipt(client);
        System.out.println("Update throttles: " + receipt.status);
        System.out.println("maxMessagesPerBundle: " + current.getMaxMessagesPerBundle() + " -> "
                + throttles.getMaxMessagesPerBundle());

        /*
         * Clean up:
         */
        client.close();
        System.out.println("CLPR Update Ledger Configuration Example Complete!");
    }
}
