// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Collections;
import java.util.Objects;

/**
 * How to sign a transaction with a multi-sig account.
 */
class SignTransactionExample {

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
        System.out.println("Sign Transaction Example Start!");

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
         * Generate ED25519 key pairs.
         */
        System.out.println("Generating ED25519 key pairs...");
        PrivateKey privateKey1 = PrivateKey.generateED25519();
        PublicKey publicKey1 = privateKey1.getPublicKey();
        PrivateKey privateKey2 = PrivateKey.generateED25519();
        PublicKey publicKey2 = privateKey2.getPublicKey();

        /*
         * Step 2:
         * Create a Key List from keys generated in previous step.
         */
        System.out.println("Creating a Key List...");
        KeyList keylist = new KeyList();
        keylist.add(publicKey1);
        keylist.add(publicKey2);
        System.out.println("Created a Key List: " + keylist);

        /*
         * Step 3:
         * Create a new account with a Key List created in a previous step.
         */
        System.out.println("Creating new account...");
        TransactionResponse createAccountTxResponse = new AccountCreateTransaction()
                .setInitialBalance(Hbar.from(2))
                .setKeyWithoutAlias(keylist)
                .execute(client);

        TransactionReceipt createAccountTxReceipt = createAccountTxResponse.getReceipt(client);
        var accountId = createAccountTxReceipt.accountId;
        Objects.requireNonNull(accountId);
        System.out.println("Created new account with ID: " + accountId);

        /*
         * Step 4:
         * Create a transfer transaction and freeze it with a client.
         */
        System.out.println("Creating a transfer transaction...");
        TransferTransaction transferTx = new TransferTransaction()
                .setNodeAccountIds(Collections.singletonList(new AccountId(0, 0, 3)))
                .addHbarTransfer(
                        Objects.requireNonNull(createAccountTxReceipt.accountId),
                        Hbar.from(1).negated())
                .addHbarTransfer(new AccountId(0, 0, 3), Hbar.from(1))
                .freezeWith(client);

        /*
         * Step 5:
         * Sign the transfer transaction with all respective keys (from a Key List).
         */
        System.out.println("Signing the transfer transaction...");
        transferTx.signWithOperator(client);
        privateKey1.signTransaction(transferTx);
        privateKey2.signTransaction(transferTx);

        /*
         * Step 6:
         * Execute the transfer transaction and output its status.
         */
        System.out.println("Executing the transfer transaction...");
        TransactionResponse transferTxResponse = transferTx.execute(client);
        TransactionReceipt transferTxReceipt = transferTxResponse.getReceipt(client);
        System.out.println("The transfer transaction was complete with status: " + transferTxReceipt.status);

        /*
         * Clean up:
         * Delete created account.
         */
        new AccountDeleteTransaction()
                .setAccountId(accountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(privateKey1)
                .sign(privateKey2)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Sign Transaction Example Complete!");
    }
}
