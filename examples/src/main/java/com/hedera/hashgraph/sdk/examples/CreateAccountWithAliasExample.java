// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;

/**
 * Hedera Account Creation Example.
 * <p>
 * Demonstrates different methods of creating Hedera accounts with various key configurations.
 * Shows how to create accounts with and without aliases using different key types.
 * <p>
 * Reference: HIP-583 Expand alias support in CryptoCreate and CryptoTransfer Transactions
 * https://hips.hedera.com/hip/hip-583
 */
public class CreateAccountWithAliasExample {
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
     * This demonstrates creating an account where both the account key and alias
     * are derived from the same ECDSA key.
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
        System.out.println("\n--- Creating account with ECDSA key and derived alias ---");
        System.out.println("ECDSA private key: " + privateKey);

        /*
         * Step 2:
         * Extract the ECDSA public key and generate EVM address.
         */
        PublicKey publicKey = privateKey.getPublicKey();
        EvmAddress evmAddress = publicKey.toEvmAddress();
        System.out.println("ECDSA public key: " + publicKey);
        System.out.println("EVM address: " + evmAddress);

        /*
         * Step 3:
         * Create an account creation transaction with the key as an alias.
         * Extract accountId from Transaction's receipt
         */
        AccountId accountId = new AccountCreateTransaction()
                .setKeyWithAlias(privateKey)
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;

        /*
         * Step 4:
         * Query the account information to verify details.
         */
        AccountInfo info = new AccountInfoQuery().setAccountId(accountId).execute(client);
        System.out.println("Created account ID: " + accountId);
        System.out.println("Account key: " + info.key);
        System.out.println("Initial EVM address: " + evmAddress + " is the same as " + info.contractAccountId);
    }

    /**
     * Creates a Hedera account with both ED25519 and ECDSA keys.
     * This demonstrates setting a separate account key (ED25519) and
     * ECDSA key for alias derivation.
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
        System.out.println("\n--- Creating account with ED25519 account key and ECDSA alias key ---");
        System.out.println("ED25519 key: " + ed25519Key);
        System.out.println("ECDSA key: " + ecdsaKey);

        /*
         * Step 2:
         * Derive the EVM address from the ECDSA public key.
         */
        EvmAddress evmAddress = ecdsaKey.getPublicKey().toEvmAddress();
        System.out.println("EVM address: " + evmAddress);

        /*
         * Step 3:
         * Create an account creation transaction with both keys.
         * It is required that transaction is signed with both keys
         * Extract accountId from Transaction's receipt
         */
        AccountId accountId = new AccountCreateTransaction()
                .setKeyWithAlias(ed25519Key, ecdsaKey)
                .freezeWith(client)
                .sign(ed25519Key)
                .sign(ecdsaKey)
                .execute(client)
                .getReceipt(client)
                .accountId;

        /*
         * Step 4:
         * Query the account information to verify details.
         */
        AccountInfo info = new AccountInfoQuery().setAccountId(accountId).execute(client);
        System.out.println("Created account ID: " + accountId);
        System.out.println("Account's key: " + info.key + " is the same as " + ed25519Key.getPublicKey());
        System.out.println("Initial EVM address: " + evmAddress + " is the same as " + info.contractAccountId);
    }

    /**
     * Creates a Hedera account without an alias.
     * This demonstrates creating an account with a key but no EVM address alias.
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
        System.out.println("\n--- Creating account without alias ---");
        System.out.println("ECDSA key: " + privateKey);

        /*
         * Step 2:
         * Create an account creation transaction without an alias.
         * Extract accountId from Transaction's receipt
         */
        AccountId accountId = new AccountCreateTransaction()
                .setKeyWithoutAlias(privateKey)
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;

        /*
         * Step 3:
         * Query the account information to verify details.
         */
        AccountInfo info = new AccountInfoQuery().setAccountId(accountId).execute(client);
        System.out.println("Created account ID: " + accountId);
        System.out.println("Account's key: " + info.key + " is the same as " + privateKey.getPublicKey());
        System.out.println("Account has no alias: " + isZeroAddress(Hex.decode(info.contractAccountId)));
    }

    /**
     * Creates a Hedera account with an alias derived from ECDSA public key.
     * This demonstrates creating an account with the public key directly
     * rather than the private key.
     *
     * @param client The Hedera network client used to execute the transaction
     * @throws Exception If there's an error during account creation
     */
    public static void createAccountWithPublicKeyAlias(Client client) throws Exception {
        /*
         * Step 1:
         * Create an ECDSA private key and derive the public key.
         */
        PrivateKey privateKey = PrivateKey.generateECDSA();
        PublicKey publicKey = privateKey.getPublicKey();
        System.out.println("\n--- Creating account with public ECDSA key alias ---");
        System.out.println("ECDSA private key: " + privateKey);
        System.out.println("ECDSA public key: " + publicKey);

        /*
         * Step 2:
         * Generate the EVM address from the public key.
         */
        EvmAddress evmAddress = publicKey.toEvmAddress();
        System.out.println("EVM address: " + evmAddress);

        /*
         * Step 3:
         * Create an account with the public key as an alias.
         * The transaction must be signed with the corresponding private key.
         */
        AccountId accountId = new AccountCreateTransaction()
                .setKeyWithAlias(publicKey)
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;

        /*
         * Step 4:
         * Query the account information to verify details.
         */
        AccountInfo info = new AccountInfoQuery().setAccountId(accountId).execute(client);
        System.out.println("Created account ID: " + accountId);
        System.out.println("Account key: " + info.key);
        System.out.println("Initial EVM address: " + evmAddress + " is the same as " + info.contractAccountId);
    }

    /**
     * Creates a Hedera account with ED25519 account key and ECDSA public key for alias.
     * This demonstrates using public key for alias rather than private key.
     *
     * @param client The Hedera network client used to execute the transaction
     * @throws Exception If there's an error during account creation
     */
    public static void createAccountWithSeparatePublicKeyAlias(Client client) throws Exception {
        /*
         * Step 1:
         * Generate ED25519 account key and ECDSA key pair for alias.
         */
        PrivateKey accountKey = PrivateKey.generateED25519();
        PrivateKey aliasPrivateKey = PrivateKey.generateECDSA();
        PublicKey aliasPublicKey = aliasPrivateKey.getPublicKey();
        System.out.println("\n--- Creating account with ED25519 key and separate ECDSA public key alias ---");
        System.out.println("Account key (ED25519): " + accountKey);
        System.out.println("Alias private key (ECDSA): " + aliasPrivateKey);
        System.out.println("Alias public key (ECDSA): " + aliasPublicKey);

        /*
         * Step 2:
         * Derive the EVM address from the ECDSA public key.
         */
        EvmAddress evmAddress = aliasPublicKey.toEvmAddress();
        System.out.println("EVM address: " + evmAddress);

        /*
         * Step 3:
         * Create an account with separate keys.
         * The transaction must be signed with both the account key and the alias key.
         */
        AccountId accountId = new AccountCreateTransaction()
                .setKeyWithAlias(accountKey, aliasPublicKey)
                .freezeWith(client)
                .sign(accountKey)
                .sign(aliasPrivateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;

        /*
         * Step 4:
         * Query the account information to verify details.
         */
        AccountInfo info = new AccountInfoQuery().setAccountId(accountId).execute(client);
        System.out.println("Created account ID: " + accountId);
        System.out.println("Account's key: " + info.key + " is the same as " + accountKey.getPublicKey());
        System.out.println("Initial EVM address: " + evmAddress + " is the same as " + info.contractAccountId);
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
        createAccountWithPublicKeyAlias(client);
        createAccountWithSeparatePublicKeyAlias(client);

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
