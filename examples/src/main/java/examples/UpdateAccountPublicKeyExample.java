// SPDX-License-Identifier: Apache-2.0
package examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to update account's key.
 */
class UpdateAccountPublicKeyExample {

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
        System.out.println("Update Account Public Key Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        client.setDefaultMaxTransactionFee(Hbar.from(10));

        /*
         * Step 1:
         * Generate ED25519 key pairs.
         */
        System.out.println("Generating ED25519 key pairs...");
        PrivateKey privateKey1 = PrivateKey.generateED25519();
        PublicKey publicKey1 = privateKey1.getPublicKey();
        PrivateKey privateKey2 = PrivateKey.generateED25519();
        PublicKey publicKey2 = privateKey2.getPublicKey();

        /*
         * Step 2:
         * Create a new account.
         */
        System.out.println("Creating new account...");
        TransactionResponse accountCreateTxResponse = new AccountCreateTransaction()
                .setKeyWithoutAlias(publicKey1)
                .setInitialBalance(Hbar.from(1))
                .execute(client);

        AccountId accountId = Objects.requireNonNull(accountCreateTxResponse.getReceipt(client).accountId);
        Objects.requireNonNull(accountId);
        System.out.println("Created new account with ID: " + accountId + " and public key: " + publicKey1);

        /*
         * Step 2:
         * Update account's key.
         */
        System.out.println("Updating public key of new account...(Setting key: " + publicKey2 + ").");
        TransactionResponse accountUpdateTxResponse = new AccountUpdateTransaction()
                .setAccountId(accountId)
                .setKey(publicKey2)
                .freezeWith(client)
                // Sign with the previous key and the new key.
                .sign(privateKey1)
                .sign(privateKey2)
                // Execute will implicitly sign with the operator.
                .execute(client);

        // (Important!) Wait for the transaction to complete by querying the receipt.
        accountUpdateTxResponse.getReceipt(client);

        /*
         * Step 3:
         * Get account info to confirm the key was changed.
         */
        AccountInfo accountInfo = new AccountInfoQuery().setAccountId(accountId).execute(client);

        System.out.println("New account public key: " + accountInfo.key);

        /*
         * Clean up:
         * Delete created account.
         */
        new AccountDeleteTransaction()
                .setAccountId(accountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(privateKey2)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Update Account Public Key Example Complete!");
    }
}
