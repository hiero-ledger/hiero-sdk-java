// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.Objects;

public class InitializeClientWithMirrorNetworkExample {
    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * Operator's account ID. Used to sign and pay for operations on Hedera.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file. Log levels can be: TRACE, DEBUG, INFO, WARN,
     * ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL, for example via VM
     * options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = Client.forMirrorNetwork(List.of("testnet.mirrornode.hedera.com:443"), 0, 0);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Generate ED25519 key pair.
         */
        System.out.println("Generating ED25519 key pair...");
        PrivateKey privateKey = PrivateKey.generateED25519();

        /*
         * Step 2:
         * Create account
         */
        AccountId aliceId = new AccountCreateTransaction()
                .setKeyWithoutAlias(privateKey)
                .setInitialBalance(Hbar.from(5))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(aliceId);
        System.out.println("Alice's account ID: " + aliceId);
    }
}
