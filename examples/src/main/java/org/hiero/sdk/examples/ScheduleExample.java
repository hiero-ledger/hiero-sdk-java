// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.time.Instant;
import java.util.Objects;

/**
 * How to schedule a transaction.
 */
class ScheduleExample {

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
        System.out.println("Schedule Transaction Example Start!");

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
         * Generate ED25519 key pairs for accounts.
         */
        System.out.println("Generating ED25519 key pairs for accounts...");
        PrivateKey privateKey1 = PrivateKey.generateED25519();
        PublicKey publicKey1 = privateKey1.getPublicKey();
        PrivateKey privateKey2 = PrivateKey.generateED25519();
        PublicKey publicKey2 = privateKey2.getPublicKey();

        /*
         * Step 1:
         * Create new account.
         */
        System.out.println("Creating new account...");
        AccountId accountId = new AccountCreateTransaction()
                .setKeyWithoutAlias(KeyList.of(publicKey1, publicKey2))
                .setInitialBalance(Hbar.from(1))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(accountId);
        System.out.println("Created new account with ID: " + accountId);

        /*
         * Step 2:
         * Schedule a transfer transaction.
         */
        System.out.println("Scheduling token transfer...");
        TransactionResponse transferTxResponse = new TransferTransaction()
                .addHbarTransfer(accountId, Hbar.from(1).negated())
                .addHbarTransfer(client.getOperatorAccountId(), Hbar.from(1))
                .schedule()
                // Set expiration time to be now + 24 hours
                .setExpirationTime(Instant.now().plusSeconds(24 * 60 * 60))
                // Set wait for expiry to true
                .setWaitForExpiry(true)
                .execute(client);

        System.out.println("Scheduled transaction ID: " + transferTxResponse.transactionId);

        ScheduleId scheduleId = Objects.requireNonNull(transferTxResponse.getReceipt(client).scheduleId);
        System.out.println("Schedule ID for the transaction above: " + scheduleId);

        TransactionRecord record = transferTxResponse.getRecord(client);
        System.out.println("Scheduled transaction record: " + record);

        /*
         * Step 3:
         * Sign the schedule transaction with the first key.
         */
        System.out.println("Appending private key #1 signature to a schedule transaction...");
        var scheduleSignTxReceiptFirstSignature = new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(privateKey1)
                .execute(client)
                .getReceipt(client);

        System.out.println("A transaction that appends signature to a schedule transaction (private key #1) "
                + "was complete with status: " + scheduleSignTxReceiptFirstSignature.status);

        /*
         * Step 4:
         * Query the state of a schedule transaction.
         */
        ScheduleInfo scheduleInfo =
                new ScheduleInfoQuery().setScheduleId(scheduleId).execute(client);

        System.out.println("Schedule info: " + scheduleInfo);

        /*
         * Step 5:
         * Sign the schedule transaction with the second key.
         */
        System.out.println("Appending private key #2 signature to a schedule transaction...");
        var scheduleSignTxReceiptSecondSignature = new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(privateKey2)
                .execute(client)
                .getReceipt(client);

        System.out.println("A transaction that appends signature to a schedule transaction (private key #2) "
                + "was complete with status: " + scheduleSignTxReceiptSecondSignature.status);

        TransactionId transactionId = transferTxResponse.transactionId;
        String validMirrorTransactionId = transactionId.accountId.toString() + "-"
                + transactionId.validStart.getEpochSecond() + "-" + transactionId.validStart.getNano();
        String mirrorNodeUrl =
                "https://" + HEDERA_NETWORK + ".mirrornode.hedera.com/api/v1/transactions/" + validMirrorTransactionId;
        System.out.println(
                "The following link should query the mirror node for the scheduled transaction: " + mirrorNodeUrl);

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
                .execute(client);

        client.close();

        System.out.println("Schedule Transaction Example Complete!");
    }
}

