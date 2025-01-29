// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;
import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;

/**
 * Hedera Account Creation Example.
 * <p>
 * Demonstrates different methods of creating Hedera accounts with various key configurations.
 * Shows how to create accounts with and without aliases using different key types.
 */
public class AccountCreateWithAndWithoutAliasExample {

    // UTIL VARIABLES BELOW
    private static final int TOTAL_ACCOUNT_CREATION_METHODS = 3;

    // CONFIG VARIABLES BELOW

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

    /**
     * Creates a Hedera account with an alias using an ECDSA key.
     *
     * @param client The Hedera network client used to execute the transaction
     * @throws Exception If there's an error during account creation
     */
    public static void createAccountWithAlias(Client client) throws Exception {
        /*
         * Step 1:
         * Create an ECDSA private key.
         */
        PrivateKey privateKey = PrivateKey.generateECDSA();

        /*
         * Step 2:
         * Extract the ECDSA public key and generate EVM address.
         */
        PublicKey publicKey = privateKey.getPublicKey();
        EvmAddress evmAddress = publicKey.toEvmAddress();

        /*
         * Step 3:
         * Create an account creation transaction with the key as an alias.
         */
        AccountCreateTransaction transaction =
                new AccountCreateTransaction().setKeyWithAlias(privateKey).freezeWith(client);

        /*
         * Step 4:
         * Sign the transaction with the private key.
         */
        transaction.sign(privateKey);

        /*
         * Step 5:
         * Execute the transaction and get the response.
         */
        TransactionResponse response = transaction.execute(client);

        /*
         * Step 6:
         * Retrieve the newly created account ID.
         */
        AccountId accountId = response.getReceipt(client).accountId;

        /*
         * Step 7:
         * Query the account information to verify details.
         */
        AccountInfo info = new AccountInfoQuery().setAccountId(accountId).execute(client);

        /*
         * Step 8:
         * Print the EVM address to confirm it matches the contract account ID.
         */
        System.out.println("Initial EVM address: " + evmAddress + " is the same as " + info.contractAccountId);
    }

    /**
     * Creates a Hedera account with both ED25519 and ECDSA keys.
     *
     * @param client The Hedera network client used to execute the transaction
     * @throws Exception If there's an error during account creation
     */
    public static void createAccountWithBothKeys(Client client) throws Exception {
        /*
         * Step 1:
         * Generate separate ED25519 and ECDSA private keys.
         */
        PrivateKey ed25519Key = PrivateKey.generateED25519();
        PrivateKey ecdsaKey = PrivateKey.generateECDSA();

        /*
         * Step 2:
         * Derive the EVM address from the ECDSA public key.
         */
        EvmAddress evmAddress = ecdsaKey.getPublicKey().toEvmAddress();

        /*
         * Step 3:
         * Create an account creation transaction with both keys.
         */
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKeyWithAlias(ed25519Key, ecdsaKey)
                .freezeWith(client);

        /*
         * Step 4:
         * Sign the transaction with both keys.
         */
        transaction.sign(ed25519Key);
        transaction.sign(ecdsaKey);

        /*
         * Step 5:
         * Execute the transaction and get the response.
         */
        TransactionResponse response = transaction.execute(client);

        /*
         * Step 6:
         * Retrieve the newly created account ID.
         */
        AccountId accountId = response.getReceipt(client).accountId;

        /*
         * Step 7:
         * Query the account information to verify details.
         */
        AccountInfo info = new AccountInfoQuery().setAccountId(accountId).execute(client);

        /*
         * Step 8:
         * Print key and EVM address details for verification.
         */
        System.out.println("Account's key: " + info.key + " is the same as " + ed25519Key.getPublicKey());
        System.out.println("Initial EVM address: " + evmAddress + " is the same as " + info.contractAccountId);
    }

    /**
     * Creates a Hedera account without an alias.
     *
     * @param client The Hedera network client used to execute the transaction
     * @throws Exception If there's an error during account creation
     */
    public static void createAccountWithoutAlias(Client client) throws Exception {
        /*
         * Step 1:
         * Create a new ECDSA private key.
         */
        PrivateKey privateKey = PrivateKey.generateECDSA();

        /*
         * Step 2:
         * Create an account creation transaction without an alias.
         */
        AccountCreateTransaction transaction =
                new AccountCreateTransaction().setKeyWithoutAlias(privateKey).freezeWith(client);

        /*
         * Step 3:
         * Sign the transaction with the private key.
         */
        transaction.sign(privateKey);

        /*
         * Step 4:
         * Execute the transaction and get the response.
         */
        TransactionResponse response = transaction.execute(client);

        /*
         * Step 5:
         * Retrieve the newly created account ID.
         */
        AccountId accountId = response.getReceipt(client).accountId;

        /*
         * Step 6:
         * Query the account information to verify details.
         */
        AccountInfo info = new AccountInfoQuery().setAccountId(accountId).execute(client);

        /*
         * Step 7:
         * Print key and alias details for verification.
         */
        System.out.println("Account's key: " + info.key + " is the same as " + privateKey.getPublicKey());
        System.out.println("Account has no alias: " + isZeroAddress(Hex.decode(info.contractAccountId)));
    }

    /**
     * Checks if an address is a zero address (all first 12 bytes are zero).
     *
     * @param address The byte array representing the address to check
     * @return true if the address is a zero address, false otherwise
     */
    private static boolean isZeroAddress(byte[] address) {
        // Check if the first 12 bytes of the address are all zero
        for (int i = 0; i < 12; i++) {
            if (address[i] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Main method to demonstrate different account creation methods.
     *
     * @param args Command-line arguments (not used in this example)
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Example Start!");

        /*
         * Step 0:
         * Create and configure SDK Client.
         */
        Client client = createClient();

        /*
         * Step 1:
         * Demonstrate different account creation methods.
         */
        createAccountWithAlias(client);
        createAccountWithBothKeys(client);
        createAccountWithoutAlias(client);

        /*
         * Clean up:
         */
        client.close();

        System.out.println("Example Complete!");
    }

    /**
     * Creates a Hedera network client using configuration from class-level constants.
     *
     * @return Configured Hedera network client
     * @throws InterruptedException If there's an interruption during client creation
     */
    public static Client createClient() throws InterruptedException {
        /*
         * Step 1:
         * Create a client for the specified network.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);

        /*
         * Step 2:
         * Set the operator (account paying for transactions).
         */
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        /*
         * Step 3:
         * Configure logging for the client.
         */
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        return client;
    }
}
