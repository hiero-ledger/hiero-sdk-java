// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import com.google.protobuf.ByteString;
import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to get exchange rates info from the Hedera network.
 */
class GetExchangeRatesExample {

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
        System.out.println("Get Exchange Rates Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Get contents of the file '0.0.112'. It is a system file, where exchange rate is stored.
         */
        System.out.println("Getting contents of the file `0.0.112`...");
        ByteString fileContentsByteString =
                new FileContentsQuery().setFileId(FileId.fromString("0.0.112")).execute(client);
        Objects.requireNonNull(fileContentsByteString);

        /*
         * Step 2:
         * Parse file contents to an ExchangeRates object.
         */
        byte[] fileContents = fileContentsByteString.toByteArray();
        ExchangeRates exchangeRateSet = ExchangeRates.fromBytes(fileContents);

        /*
         * Step 3:
         * Print the info.
         */
        System.out.println("Current numerator: " + exchangeRateSet.currentRate.cents);
        System.out.println("Current denominator: " + exchangeRateSet.currentRate.hbars);
        System.out.println("Current expiration time: " + exchangeRateSet.currentRate.expirationTime.toString());
        System.out.println("Current Exchange Rate: " + exchangeRateSet.currentRate.exchangeRateInCents);
        System.out.println("Next numerator: " + exchangeRateSet.nextRate.cents);
        System.out.println("Next denominator: " + exchangeRateSet.nextRate.hbars);
        System.out.println("Next expiration time: " + exchangeRateSet.nextRate.expirationTime.toString());
        System.out.println("Next Exchange Rate: " + exchangeRateSet.nextRate.exchangeRateInCents);

        /*
         * Clean up:
         */
        client.close();
        System.out.println("Get Exchange Rates Example Complete!");
    }
}

