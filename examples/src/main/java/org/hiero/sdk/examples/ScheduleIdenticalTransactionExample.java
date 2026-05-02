// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Collections;
import java.util.Objects;

/**
 * How to schedule identical transactions.
 */
class ScheduleIdenticalTransactionExample {

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
        System.out.println("Schedule Identical Transaction Example Start!");

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
         * Create key pairs, clients and accounts.
         */
        PrivateKey[] privateKeys = new PrivateKey[3];
        PublicKey[] publicKeys = new PublicKey[3];
        Client[] clients = new Client[3];
        AccountId[] accounts = new AccountId[3];

        ScheduleId scheduleId = null;

        for (int i = 0; i < 3; i++) {
            System.out.println("Generating ED25519 key pair...");

            PrivateKey newPrivateKey = PrivateKey.generateED25519();
            PublicKey newPublicKey = newPrivateKey.getPublicKey();

            privateKeys[i] = newPrivateKey;
            publicKeys[i] = newPublicKey;

            System.out.println("Key pair #" + (i + 1) + " | Private key: " + privateKeys[i]);
            System.out.println("Key pair #" + (i + 1) + " | Public key: " + publicKeys[i]);

            System.out.println("Creating new account...");
            TransactionResponse accountCreateTxResponse = new AccountCreateTransaction()
                    .setKeyWithoutAlias(newPublicKey)
                    .setInitialBalance(Hbar.from(1))
                    .execute(client);

            // Make sure the transaction succeeded.
            TransactionReceipt accountCreateTxReceipt = accountCreateTxResponse.getReceipt(client);

            Client newClient = ClientHelper.forName(HEDERA_NETWORK);
            newClient.setOperator(Objects.requireNonNull(accountCreateTxReceipt.accountId), newPrivateKey);
            clients[i] = newClient;
            accounts[i] = accountCreateTxReceipt.accountId;

            System.out.println("Created new account with ID: " + accounts[i]);
            System.out.println("---");
        }

        /*
         * Step 2:
         * Create a threshold key with a threshold of 2 and length of 3 requires
         * (at least 2 of 3 keys to sign anything modifying the account).
         */
        System.out.println(
                "Creating a Key List..."
                        + "(with threshold, it will require 2 of 3 keys we generated to sign on anything modifying this account).");
        KeyList thresholdKey = KeyList.withThreshold(2);
        Collections.addAll(thresholdKey, publicKeys);
        System.out.println("Created a Key List: " + thresholdKey);

        /*
         * Step 3:
         * Create a new account with the Key List from previous step.
         */
        // We are using all of these keys, so the scheduled transaction doesn't automatically go through.
        // It works perfectly fine with just one key.
        System.out.println("Creating new account...(with the above Key List as an account key).");
        TransactionResponse accountCreateTxResponse = new AccountCreateTransaction()
                // The key that must sign each transfer out of the account. If receiverSigRequired is true, then
                // it must also sign any transfer into the account.
                .setKeyWithoutAlias(thresholdKey)
                .setInitialBalance(Hbar.from(10))
                .execute(client);

        // Make sure the transaction succeeded.
        TransactionReceipt accountCreateTxReceipt = accountCreateTxResponse.getReceipt(client);

        AccountId thresholdAccount = accountCreateTxReceipt.accountId;
        Objects.requireNonNull(thresholdAccount);
        System.out.println("Created new account with ID: " + thresholdAccount);

        System.out.println("\n---\n");

        /*
         * Step 4:
         * Each loopClient creates an identical transaction, sending 1 Hbar to each of the created accounts,
         * sent from the threshold Account.
         */
        for (Client loopClient : clients) {
            AccountId operatorId = loopClient.getOperatorAccountId();

            System.out.println("Creating transfer transaction...");
            TransferTransaction transferTx = new TransferTransaction();
            for (AccountId account : accounts) {
                transferTx.addHbarTransfer(account, Hbar.from(1));
            }
            transferTx.addHbarTransfer(
                    Objects.requireNonNull(thresholdAccount), Hbar.from(3).negated());

            System.out.println("Scheduling created transfer transaction...");
            ScheduleCreateTransaction scheduledTx = new ScheduleCreateTransaction().setScheduledTransaction(transferTx);

            scheduledTx.setPayerAccountId(thresholdAccount);

            TransactionResponse scheduledTxResponse = scheduledTx.execute(loopClient);

            System.out.println("Executing scheduled transaction...");
            TransactionReceipt loopReceipt = new TransactionReceiptQuery()
                    .setTransactionId(scheduledTxResponse.transactionId)
                    .setNodeAccountIds(Collections.singletonList(scheduledTxResponse.nodeId))
                    .execute(loopClient);

            System.out.println("Operator (ID: " + operatorId + ") | Schedule ID: " + loopReceipt.scheduleId);

            // Save the schedule ID, so that it can be asserted for each loopClient submission.
            if (scheduleId == null) {
                scheduleId = loopReceipt.scheduleId;
            }

            if (!scheduleId.equals(Objects.requireNonNull(loopReceipt.scheduleId))) {
                throw new Exception(
                        "Invalid generated schedule ID! Expected " + scheduleId + ", got " + loopReceipt.scheduleId);
            }

            // If the status return by the receipt is related to already created, execute a schedule sign transaction.
            if (loopReceipt.status == Status.IDENTICAL_SCHEDULE_ALREADY_CREATED) {
                System.out.println("Appending signature to a schedule transaction...");
                TransactionResponse scheduleSignTxResponse = new ScheduleSignTransaction()
                        .setScheduleId(scheduleId)
                        .setNodeAccountIds(Collections.singletonList(accountCreateTxResponse.nodeId))
                        .setScheduleId(loopReceipt.scheduleId)
                        .execute(loopClient);

                TransactionReceipt scheduleSignTxReceipt = new TransactionReceiptQuery()
                        .setTransactionId(scheduleSignTxResponse.transactionId)
                        .execute(client);

                System.out.println("A transaction that appends signature to a schedule transaction "
                        + "was complete with status: " + scheduleSignTxReceipt.status);

                if (scheduleSignTxReceipt.status != Status.SUCCESS
                        && scheduleSignTxReceipt.status != Status.SCHEDULE_ALREADY_EXECUTED) {
                    throw new Exception("Bad status while getting receipt of schedule sign with operator " + operatorId
                            + ": " + scheduleSignTxReceipt.status);
                }
            }
            System.out.println("---");
        }

        System.out.println("\n---\n");

        /*
         * Step 5:
         * Query the state of a schedule transaction.
         */
        ScheduleInfo scheduleInfo =
                new ScheduleInfoQuery().setScheduleId(scheduleId).execute(client);
        System.out.println("Scheduled transaction info: " + scheduleInfo);

        /*
         * Clean up:
         * Delete created accounts and close created clients.
         */
        AccountDeleteTransaction accountDeleteTx = new AccountDeleteTransaction()
                .setAccountId(thresholdAccount)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client);

        for (int i = 0; i < 3; i++) {
            accountDeleteTx.sign(privateKeys[i]);
            new AccountDeleteTransaction()
                    .setAccountId(accounts[i])
                    .setTransferAccountId(OPERATOR_ID)
                    .freezeWith(client)
                    .sign(privateKeys[i])
                    .execute(client)
                    .getReceipt(client);
        }

        accountDeleteTx.execute(client).getReceipt(client);

        client.close();

        for (Client loopClient : clients) {
            loopClient.close();
        }

        System.out.println("Schedule Identical Transaction Example Complete!");
    }
}

