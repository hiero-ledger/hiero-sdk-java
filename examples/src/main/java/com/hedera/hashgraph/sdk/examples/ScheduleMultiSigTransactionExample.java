// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * How to schedule a transaction with a multi-sig account.
 */
class ScheduleMultiSigTransactionExample {

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
        System.out.println("Scheduled Transaction Multi-Sig Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        PublicKey operatorPublicKey = OPERATOR_KEY.getPublicKey();

        /*
         * Step 1:
         * Generate three ED25519 private keys.
         */
        System.out.println("Generating ED25519 private keys...");
        PrivateKey privateKey1 = PrivateKey.generateED25519();
        PublicKey publicKey1 = privateKey1.getPublicKey();
        PrivateKey privateKey2 = PrivateKey.generateED25519();
        PublicKey publicKey2 = privateKey2.getPublicKey();
        PrivateKey privateKey3 = PrivateKey.generateED25519();
        PublicKey publicKey3 = privateKey3.getPublicKey();

        /*
         * Step 2:
         * Create a Key List from keys generated in previous step.
         *
         * This key will be used as the new account's key.
         * The reason we want to use a `KeyList` is to simulate a multi-party system where
         * multiple keys are required to sign.
         */
        System.out.println("Creating a Key List...");
        KeyList keyList = new KeyList();
        keyList.add(publicKey1);
        keyList.add(publicKey2);
        keyList.add(publicKey3);
        System.out.println("Created a Key List: " + keyList);

        /*
         * Step 3:
         * Create a new account with a Key List created in a previous step.
         */
        System.out.println("Creating new account...");
        TransactionResponse accountCreateTxResponse = new AccountCreateTransaction()
                .setNodeAccountIds(Collections.singletonList(new AccountId(0, 0, 3)))
                // The only required property here is key.
                .setKeyWithoutAlias(keyList)
                .setInitialBalance(Hbar.from(2))
                .execute(client);

        // This will wait for the receipt to become available.
        TransactionReceipt accountCreateTxReceipt = accountCreateTxResponse.getReceipt(client);
        AccountId accountId = Objects.requireNonNull(accountCreateTxReceipt.accountId);
        System.out.println("Created new account with ID: " + accountId);

        /*
         * Step 4:
         * Create a new scheduled transaction for transferring Hbars.
         */
        // Generate a TransactionId. This id is used to query the inner scheduled transaction
        // after we expect it to have been executed.
        TransactionId transactionId = TransactionId.generate(OPERATOR_ID);

        System.out.println("Generated `TransactionId` for a scheduled transaction: " + transactionId);

        // Create a transfer transaction with 2/3 signatures.
        System.out.println("Creating a token transfer transaction...");
        TransferTransaction transferTx = new TransferTransaction()
                .addHbarTransfer(accountId, Hbar.from(1).negated())
                .addHbarTransfer(OPERATOR_ID, Hbar.from(1));

        // Schedule the transaction.
        System.out.println("Scheduling the token transfer transaction...");
        ScheduleCreateTransaction scheduled = transferTx
                .schedule()
                .setPayerAccountId(OPERATOR_ID)
                .setAdminKey(operatorPublicKey)
                .freezeWith(client)
                .sign(privateKey2);

        accountCreateTxReceipt = scheduled.execute(client).getReceipt(client);
        // Get the schedule ID from the receipt.
        ScheduleId scheduleId = Objects.requireNonNull(accountCreateTxReceipt.scheduleId);
        System.out.println("Schedule ID: " + scheduleId);

        /*
         * Step 5:
         * Get the schedule info to see if signatories is populated with 2/3 signatures.
         */
        ScheduleInfo scheduleInfo_BeforeLastSignature = new ScheduleInfoQuery()
                .setNodeAccountIds(Collections.singletonList(accountCreateTxResponse.nodeId))
                .setScheduleId(scheduleId)
                .execute(client);

        System.out.println("Schedule info: " + scheduleInfo_BeforeLastSignature);

        transferTx = (TransferTransaction) scheduleInfo_BeforeLastSignature.getScheduledTransaction();
        Map<AccountId, Hbar> transfers = transferTx.getHbarTransfers();

        // Make sure the transfer transaction is what we expect.
        if (transfers.size() != 2) {
            throw new Exception("More transfers than expected! (Fail)");
        }

        if (!transfers.get(accountId).equals(Hbar.from(1).negated())) {
            throw new Exception("Transfer for " + accountId + " is not what is expected " + transfers.get(accountId));
        }

        if (!transfers.get(OPERATOR_ID).equals(Hbar.from(1))) {
            throw new Exception(
                    "Transfer for " + OPERATOR_ID + " is not what is expected " + transfers.get(OPERATOR_ID));
        }

        System.out.println("Sending schedule sign transaction...");

        /*
         * Step 6:
         * Send this last signature to Hedera.
         *
         * This last signature should mean the transaction executes since all 3 signatures have been provided.
         */
        System.out.println(
                "Appending private key #3 signature to a schedule transaction..."
                        + "(This last signature should mean the transaction executes since all 3 signatures have been provided)");
        TransactionReceipt scheduleSignTxReceipt = new ScheduleSignTransaction()
                .setNodeAccountIds(Collections.singletonList(accountCreateTxResponse.nodeId))
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(privateKey3)
                .execute(client)
                .getReceipt(client);

        System.out.println("A transaction that appends signature to a schedule transaction (private key #3) "
                + "was complete with status: " + scheduleSignTxReceipt.status);

        /*
         * Step 7:
         * Query the schedule info again.
         */
        ScheduleInfo scheduleInfo_AfterAllSigned = new ScheduleInfoQuery()
                .setNodeAccountIds(Collections.singletonList(accountCreateTxResponse.nodeId))
                .setScheduleId(scheduleId)
                .execute(client);

        System.out.println("Schedule info: " + scheduleInfo_AfterAllSigned);

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
                .sign(privateKey3)
                .execute(client);

        client.close();

        System.out.println("Scheduled Transaction Multi-Sig Example Complete!");
    }
}
