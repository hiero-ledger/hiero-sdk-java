// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.AddressBookQuery;
import org.hiero.sdk.Client;
import org.hiero.sdk.FileId;
import org.hiero.sdk.NodeAddressBook;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.ByteArrayInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Objects;

/**
 * How to get the network address book and then inspect node public keys, etc.
 */
class GetAddressBookExample {

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
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL,
     * for example via VM options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        System.out.println("Get Address Book Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Fetch the address book.
         * Note: from Feb 25 2022 you can now fetch the address book for free from a mirror node with AddressBookQuery.
         */
        System.out.println("Getting address book for " + HEDERA_NETWORK + "...");

        NodeAddressBook addressBook =
                new AddressBookQuery().setFileId(FileId.ADDRESS_BOOK).execute(client);

        Objects.requireNonNull(addressBook);
        System.out.println("Address book for " + HEDERA_NETWORK + ": " + addressBook);

        /*
         * Clean up:
         */
        Files.deleteIfExists(FileSystems.getDefault().getPath("address-book.proto.bin"));
        client.close();

        Files.copy(
                new ByteArrayInputStream(addressBook.toBytes().toByteArray()),
                FileSystems.getDefault().getPath("address-book.proto.bin"));

        System.out.println("Get Address Book Example Complete!");
    }
}

