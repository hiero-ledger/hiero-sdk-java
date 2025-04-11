// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

class BatchTransactionExample {

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
        System.out.println("Batch Transaction Example Start!");

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
         * BatchKey is the public key of the client that executes a batch transaction
         */
        var batchKey = client.getOperatorPublicKey();
        System.out.println("Batch key: " + batchKey);

        /*
         * Step 2:
         * Generate three key pairs for the new accounts.
         */
        PrivateKey privateKey1 = PrivateKey.generateECDSA();
        PublicKey publicKey1 = privateKey1.getPublicKey();

        PrivateKey privateKey2 = PrivateKey.generateECDSA();
        PublicKey publicKey2 = privateKey2.getPublicKey();

        PrivateKey privateKey3 = PrivateKey.generateECDSA();
        PublicKey publicKey3 = privateKey3.getPublicKey();

        /*
         * Step 3:
         * Create three account create transactions with batch keys, but do not execute them.
         */
        System.out.println("Creating three account create transactions...");

        var accountCreateTx1 = new AccountCreateTransaction()
                .setKeyWithoutAlias(publicKey1)
                .setInitialBalance(Hbar.from(1))
                .batchify(client, batchKey);

        var accountCreateTx2 = new AccountCreateTransaction()
                .setKeyWithoutAlias(publicKey2)
                .setInitialBalance(Hbar.from(1))
                .batchify(client, batchKey);

        var accountCreateTx3 = new AccountCreateTransaction()
                .setKeyWithoutAlias(publicKey3)
                .setInitialBalance(Hbar.from(1))
                .batchify(client, batchKey);

        /*
         * Step 4:
         * Create a batch transaction that contains the three account create transactions.
         */
        System.out.println("Creating batch transaction...");
        BatchTransaction batchTransaction = new BatchTransaction();
        batchTransaction.addInnerTransaction(accountCreateTx1);
        batchTransaction.addInnerTransaction(accountCreateTx2);
        batchTransaction.addInnerTransaction(accountCreateTx3);

        /*
         * Step 5:
         * Execute the batch transaction
         */
        System.out.println("Executing batch transaction...");
        batchTransaction.execute(client).getReceipt(client);

        /*
         * Step 6:
         * Verify the three account IDs of the newly created accounts using innerTransactionIds.
         */
        System.out.println("Verifying the newly created accounts...");
        var innerTransactionIds = batchTransaction.getInnerTransactionIds();
        System.out.println("Inner transaction ids: " + innerTransactionIds.toString());

        // First inner transaction

        var accountId1 = new AccountInfoQuery()
                .setAccountId(Objects.requireNonNull(innerTransactionIds.get(0).getReceipt(client).accountId))
                .execute(client)
                .accountId;
        System.out.println("Created account 1 with ID: " + accountId1);

        // Second inner transaction
        var accountInfo2 = new AccountInfoQuery()
                .setAccountId(Objects.requireNonNull(innerTransactionIds.get(1).getReceipt(client).accountId))
                .execute(client);
        System.out.println("Created account 2 with ID: " + accountInfo2.accountId);

        // Third inner transaction
        var accountInfo3 = new AccountInfoQuery()
                .setAccountId(Objects.requireNonNull(innerTransactionIds.get(2).getReceipt(client).accountId))
                .execute(client);
        System.out.println("Created account 3 with ID: " + accountInfo3.accountId);

        client.close();

        System.out.println("Batch Transaction Example Complete!");
    }
}
