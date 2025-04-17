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

        System.out.println("Showcasing manual batch transaction preparation");
        executeBatchWithManualInnerTransactionFreeze(client);

        System.out.println("Showcasing automatic batch transaction preparation using batchify");
        executeBatchWithBatchify(client);

        client.close();
        System.out.println("Batch Transaction Example Complete!");
    }

    private static void executeBatchWithManualInnerTransactionFreeze(Client client) throws Exception {
        /*
         * Step 1:
         * Create batch keys
         */
        var batchKey1 = PrivateKey.generateECDSA();
        var batchKey2 = PrivateKey.generateECDSA();
        var batchKey3 = PrivateKey.generateECDSA();

        /*
         * Step 2:
         * Create 3 accounts and prepare transfers for batching
         */
        System.out.println("Creating three accounts and preparing batched transfers...");
        var aliceKey = PrivateKey.generateECDSA();
        var alice = new AccountCreateTransaction()
                .setKeyWithoutAlias(aliceKey)
                .setInitialBalance(new Hbar(2))
                .execute(client)
                .getReceipt(client)
                .accountId;
        var aliceBatchedTransfer = new TransferTransaction()
                .addHbarTransfer(client.getOperatorAccountId(), Hbar.from(1))
                .addHbarTransfer(alice, Hbar.from(1).negated())
                .setTransactionId(TransactionId.generate(alice))
                .setBatchKey(batchKey1)
                .freezeWith(client)
                .sign(aliceKey);
        System.out.println("Created first account (Alice): " + alice);

        var bobKey = PrivateKey.generateECDSA();
        var bob = new AccountCreateTransaction()
                .setKeyWithoutAlias(bobKey)
                .setInitialBalance(new Hbar(2))
                .execute(client)
                .getReceipt(client)
                .accountId;
        var bobBatchedTransfer = new TransferTransaction()
                .addHbarTransfer(client.getOperatorAccountId(), Hbar.from(1))
                .addHbarTransfer(bob, Hbar.from(1).negated())
                .setTransactionId(TransactionId.generate(bob))
                .setBatchKey(batchKey2)
                .freezeWith(client)
                .sign(bobKey);
        System.out.println("Created second account (Bob): " + bob);

        var carolKey = PrivateKey.generateECDSA();
        var carol = new AccountCreateTransaction()
                .setKeyWithoutAlias(carolKey)
                .setInitialBalance(new Hbar(2))
                .execute(client)
                .getReceipt(client)
                .accountId;
        var carolBatchedTransfer = new TransferTransaction()
                .addHbarTransfer(client.getOperatorAccountId(), Hbar.from(1))
                .addHbarTransfer(carol, Hbar.from(1).negated())
                .setTransactionId(TransactionId.generate(carol))
                .setBatchKey(batchKey3)
                .freezeWith(client)
                .sign(carolKey);
        System.out.println("Created third account (Carol): " + carol);

        /*
         * Step 3:
         * Get the balances in order to compare after the batch execution
         */
        var aliceBalanceBefore = new AccountBalanceQuery().setAccountId(alice).execute(client);
        var bobBalanceBefore = new AccountBalanceQuery().setAccountId(bob).execute(client);
        var carolBalanceBefore = new AccountBalanceQuery().setAccountId(carol).execute(client);
        var operatorBalanceBefore = new AccountBalanceQuery()
                .setAccountId(client.getOperatorAccountId())
                .execute(client);

        /*
         * Step 4:
         * Execute the batch
         */
        System.out.println("Executing batch transaction...");
        var receipt = new BatchTransaction()
                .addInnerTransaction(aliceBatchedTransfer)
                .addInnerTransaction(bobBatchedTransfer)
                .addInnerTransaction(carolBatchedTransfer)
                .freezeWith(client)
                .sign(batchKey1)
                .sign(batchKey2)
                .sign(batchKey3)
                .execute(client)
                .getReceipt(client);
        System.out.println("Batch transaction executed with status: " + receipt.status);

        /*
         * Step 5:
         * Verify the new balances
         */
        System.out.println("Verifying the balances after batch execution...");
        var aliceBalanceAfter = new AccountBalanceQuery().setAccountId(alice).execute(client);
        var bobBalanceAfter = new AccountBalanceQuery().setAccountId(bob).execute(client);
        var carolBalanceAfter = new AccountBalanceQuery().setAccountId(carol).execute(client);
        var operatorBalanceAfter = new AccountBalanceQuery()
                .setAccountId(client.getOperatorAccountId())
                .execute(client);

        System.out.println(
                "Alice's initial balance: " + aliceBalanceBefore.hbars + ", after: " + aliceBalanceAfter.hbars);
        System.out.println("Bob's initial balance: " + bobBalanceBefore.hbars + ", after: " + bobBalanceAfter.hbars);
        System.out.println(
                "Carol's initial balance: " + carolBalanceBefore.hbars + ", after: " + carolBalanceAfter.hbars);
        System.out.println("Operator's initial balance: " + operatorBalanceBefore.hbars + ", after: "
                + operatorBalanceAfter.hbars);
    }

    private static void executeBatchWithBatchify(Client client) throws Exception {

        /*
         * Step 1:
         * Create batch key
         */
        var batchKey = PrivateKey.generateECDSA();

        /*
         * Step 2:
         * Create acccount - alice
         */
        System.out.println("Creating three accounts and preparing batched transfers...");
        var aliceKey = PrivateKey.generateECDSA();
        var alice = new AccountCreateTransaction()
                .setKeyWithoutAlias(aliceKey)
                .setInitialBalance(new Hbar(2))
                .execute(client)
                .getReceipt(client)
                .accountId;

        System.out.println("Created Alice: " + alice);

        /*
         * Step 3:
         * Create client for alice
         */
        var aliceClient = ClientHelper.forName(HEDERA_NETWORK);
        aliceClient.setOperator(alice, aliceKey);

        /*
         * Step 4:
         * Batchify a transfer transaction
         */
        var aliceBatchedTransfer = new TransferTransaction()
                .addHbarTransfer(client.getOperatorAccountId(), Hbar.from(1))
                .addHbarTransfer(alice, Hbar.from(1).negated())
                .batchify(aliceClient, batchKey);

        /*
         * Step 5:
         * Get the balances in order to compare after the batch execution
         */
        var aliceBalanceBefore = new AccountBalanceQuery().setAccountId(alice).execute(client);
        var operatorBalanceBefore = new AccountBalanceQuery()
                .setAccountId(client.getOperatorAccountId())
                .execute(client);

        /*
         * Step 6:
         * Execute the batch
         */
        System.out.println("Executing batch transaction...");
        var receipt = new BatchTransaction()
                .addInnerTransaction(aliceBatchedTransfer)
                .freezeWith(client)
                .sign(batchKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("Batch transaction executed with status: " + receipt.status);

        /*
         * Step 7:
         * Verify the new balances
         */
        System.out.println("Verifying the balances after batch execution...");
        var aliceBalanceAfter = new AccountBalanceQuery().setAccountId(alice).execute(client);
        var operatorBalanceAfter = new AccountBalanceQuery()
                .setAccountId(client.getOperatorAccountId())
                .execute(client);

        System.out.println(
                "Alice's initial balance: " + aliceBalanceBefore.hbars + ", after: " + aliceBalanceAfter.hbars);
        System.out.println("Operator's initial balance: " + operatorBalanceBefore.hbars + ", after: "
                + operatorBalanceAfter.hbars);
    }
}
