// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to transfer Hbar between accounts.
 */
class TransferCryptoExample {

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
        System.out.println("Transfer Crypto Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        AccountId recipientId = AccountId.fromString("0.0.3");

        /*
         * Step 1:
         * Check Hbar balance of sender and recipient.
         */
        Hbar senderBalanceBefore =
                new AccountBalanceQuery().setAccountId(OPERATOR_ID).execute(client).hbars;

        Hbar recipientBalanceBefore =
                new AccountBalanceQuery().setAccountId(recipientId).execute(client).hbars;

        System.out.println("Sender (" + OPERATOR_ID + ") balance before transfer: " + senderBalanceBefore);
        System.out.println("Recipient (" + recipientId + ") balance before transfer: " + recipientBalanceBefore);

        /*
         * Step 2:
         * Execute the transfer transaction to send Hbars from operator to the recipient.
         */
        System.out.println("Executing the transfer transaction...");
        Hbar transferAmount = Hbar.from(1);
        TransactionResponse transferTxResponse = new TransferTransaction()
                // addSender and addRecipient can be called as many times as you want as long as the total sum from
                // both sides is equivalent.
                .addHbarTransfer(OPERATOR_ID, transferAmount.negated())
                .addHbarTransfer(recipientId, transferAmount)
                .setTransactionMemo("Transfer example")
                .execute(client);

        System.out.println("Transaction info: " + transferTxResponse);
        TransactionRecord record = transferTxResponse.getRecord(client);
        System.out.println("Transferred " + transferAmount);
        System.out.println("Transfer memo: " + record.transactionMemo);

        /*
         * Step 6:
         * Check Hbar balance of the sender and recipient after transfer transaction was executed.
         */
        Hbar senderBalanceAfter =
                new AccountBalanceQuery().setAccountId(OPERATOR_ID).execute(client).hbars;

        Hbar receiptBalanceAfter =
                new AccountBalanceQuery().setAccountId(recipientId).execute(client).hbars;

        System.out.println("Sender (" + OPERATOR_ID + ") balance after transfer: " + senderBalanceAfter);
        System.out.println("Recipient (" + recipientId + ") balance after transfer: " + receiptBalanceAfter);

        /*
         * Clean up:
         */
        client.close();
        System.out.println("Example complete!");
    }
}

