// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to schedule a transfer transaction.
 * <p>
 * A scheduled transaction is a transaction that has been proposed by an account,
 * but which requires more signatures before it will actually execute on the Hedera network.
 * <p>
 * For example, if Alice wants to transfer an amount of Hbar to Bob, and Bob has
 * `receiverSignatureRequired` set to true, then that transaction must be signed by
 * both Alice and Bob before the transaction will be executed.
 * <p>
 * To solve this problem, Alice can propose the transaction by creating a scheduled
 * transaction on the Hedera network which, if executed, would transfer Hbar from
 * Alice to Bob. That scheduled transaction will have a `ScheduleId` by which we can
 * refer to that scheduled transaction. Alice can communicate the `ScheduleId` to Bob, and
 * then Bob can use a `ScheduleSignTransaction` to sign that scheduled transaction.
 * <p>
 * Bob has a 30-minute window in which to sign the scheduled transaction, starting at the
 * moment that Alice creates the scheduled transaction.  If a scheduled transaction
 * is not signed by all the necessary signatories within the 30-minute window,
 * that scheduled transaction will expire, and will not be executed.
 * <p>
 * Once a scheduled transaction has all the signatures necessary to execute, it will
 * be executed on the Hedera network automatically. If you create a scheduled transaction
 * on the Hedera network, but that transaction only requires your signature in order to
 * execute and no one else's, that scheduled transaction will be automatically
 * executed immediately.
 */
class ScheduledTransferExample {

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
        System.out.println("Scheduled Transfer Transaction Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        System.out.println("In this example Alice's account ID would be equal to the Operator's account ID: "
                + client.getOperatorAccountId());

        /*
         * Step 1:
         * Generate ED25519 key pair.
         */
        System.out.println("Generating ED25519 key pair for Bob's account...");
        PrivateKey bobPrivateKey = PrivateKey.generateED25519();
        PublicKey bobPublicKey = bobPrivateKey.getPublicKey();

        /*
         * Step 2:
         * Create Bob's account with receiver signature property enabled.
         */
        System.out.println("Create Bob's account...(with receiver signature property enabled).");
        AccountId bobAccountId = new AccountCreateTransaction()
                .setReceiverSignatureRequired(true)
                .setKeyWithoutAlias(bobPublicKey)
                .setInitialBalance(Hbar.from(1))
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(bobAccountId);
        System.out.println("Created Bob's account with ID: " + bobAccountId);

        /*
         * Step 3:
         * Check Bob's initial balance.
         */
        AccountBalance bobsInitialBalance =
                new AccountBalanceQuery().setAccountId(bobAccountId).execute(client);
        System.out.println("Bob's initial account balance: " + bobsInitialBalance);

        /*
         * Step 4:
         * Create a transfer transaction which we will schedule.
         */
        TransferTransaction transferTx = new TransferTransaction()
                .addHbarTransfer(client.getOperatorAccountId(), Hbar.from(1).negated())
                .addHbarTransfer(bobAccountId, Hbar.from(1));
        System.out.println("Scheduling token transfer: " + transferTx);

        /*
         * Step 5:
         * Create a scheduled transaction from a transfer transaction.
         *
         * The payerAccountId is the account that will be charged the fee
         * for executing the scheduled transaction if/when it is executed.
         * That fee is separate from the fee that we will pay to execute the
         * ScheduleCreateTransaction itself.
         *
         * To clarify: Alice pays a fee to execute the ScheduleCreateTransaction,
         * which creates the scheduled transaction on the Hedera network.
         * She specifies when creating the scheduled transaction that Bob will pay
         * the fee for the scheduled transaction when it is executed.
         *
         * If payerAccountId is not specified, the account who creates the scheduled transaction
         * will be charged for executing the scheduled transaction.
         */
        ScheduleId scheduleId = new ScheduleCreateTransaction()
                .setScheduledTransaction(transferTx)
                .setPayerAccountId(bobAccountId)
                .execute(client)
                .getReceipt(client)
                .scheduleId;
        Objects.requireNonNull(scheduleId);
        System.out.println("Schedule ID for the transaction above: " + scheduleId);

        /*
         * Step 6:
         * Check Bob's balance -- it should be unchanged, because the transfer has been scheduled,
         * but it hasn't been executed yet as it requires Bob's signature.
         */
        AccountBalance bobsBalanceAfterSchedule =
                new AccountBalanceQuery().setAccountId(bobAccountId).execute(client);
        System.out.println(
                "Bob's balance after scheduling the transfer (should be unchanged): " + bobsBalanceAfterSchedule);

        /*
         * Step 7:
         * Query the state of a schedule transaction.
         *
         * Once Alice has communicated the scheduleId to Bob, Bob can query for information about the
         * scheduled transaction.
         */
        ScheduleInfo scheduledTransactionInfo =
                new ScheduleInfoQuery().setScheduleId(scheduleId).execute(client);
        System.out.println("Scheduled transaction info: " + scheduledTransactionInfo);

        // getScheduledTransaction() will return an SDK Transaction object identical to the transaction
        // that was scheduled, which Bob can then inspect like a normal transaction.
        Transaction<?> scheduledTransaction = scheduledTransactionInfo.getScheduledTransaction();

        // We happen to know that this transaction is (or certainly ought to be) a TransferTransaction.
        if (scheduledTransaction instanceof TransferTransaction) {
            TransferTransaction scheduledTransfer = (TransferTransaction) scheduledTransaction;
            System.out.println("The scheduled transfer transaction from Bob's POV: " + scheduledTransfer);
        } else {
            throw new Exception("The scheduled transaction was not a transfer transaction! (Fail)");
        }

        /*
         * Step 8:
         * Appends Bob's signature to a schedule transaction, i.e. Bob signs the scheduled transaction.
         */
        System.out.println("Appending Bob's signature to a schedule transaction...");
        var scheduleSignTxReceipt = new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("A transaction that appends Bob's signature to a schedule transfer transaction "
                + "was complete with status: " + scheduleSignTxReceipt.status);

        /*
         * Step 9:
         * Check Bob's account balance after signing the scheduled transaction.
         */
        AccountBalance balanceAfterSigning =
                new AccountBalanceQuery().setAccountId(bobAccountId).execute(client);
        System.out.println("Bob's balance after signing the scheduled transaction: " + balanceAfterSigning);

        /*
         * Step 10:
         * Query the state of a schedule transaction.
         */
        ScheduleInfo postTransactionInfo =
                new ScheduleInfoQuery().setScheduleId(scheduleId).execute(client);
        System.out.println(
                "Scheduled transaction info (`executedAt` should no longer be `null`): " + postTransactionInfo);

        /*
         * Clean up:
         * Delete created account.
         */
        new AccountDeleteTransaction()
                .setTransferAccountId(client.getOperatorAccountId())
                .setAccountId(bobAccountId)
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Scheduled Transfer Transaction Example Complete!");
    }
}

