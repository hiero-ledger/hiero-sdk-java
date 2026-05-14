// SPDX-License-Identifier: Apache-2.0
package examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Collections;
import java.util.Objects;

/**
 * How to schedule a transaction with a multi-sig account with a threshold.
 */
class ScheduledTransactionMultiSigThresholdExample {

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
        System.out.println("Scheduled Transaction Multi-Sig With Threshold Example Start!");

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
         * Generate four ED25519 key pairs.
         */
        System.out.println("Generating ED25519 key pairs...");
        PrivateKey[] privateKeys = new PrivateKey[4];
        PublicKey[] publicKeys = new PublicKey[4];
        for (int i = 0; i < 4; i++) {
            PrivateKey key = PrivateKey.generateED25519();
            privateKeys[i] = key;
            publicKeys[i] = key.getPublicKey();
            System.out.println("Key pair #" + (i + 1) + " | Private key: " + privateKeys[i]);
            System.out.println("Key pair #" + (i + 1) + " | Public key: " + publicKeys[i]);
        }

        /*
         * Step 2:
         * Create a Key List with threshold
         * (require 3 of 4 keys we generated to sign on anything modifying this account).
         */
        System.out.println(
                "Creating a Key List..."
                        + "(with threshold, it will require 3 of 4 keys we generated to sign on anything modifying this account).");
        KeyList thresholdKey = KeyList.withThreshold(3);
        Collections.addAll(thresholdKey, publicKeys);
        System.out.println("Created a Key List: " + thresholdKey);

        /*
         * Step 3:
         * Create a new account with a Key List from previous step.
         */
        System.out.println("Creating new account...(with the above Key List as an account key).");
        TransactionResponse accountCreateTxResponse = new AccountCreateTransaction()
                .setKeyWithoutAlias(thresholdKey)
                .setInitialBalance(Hbar.from(1))
                .setAccountMemo("3-of-4 multi-sig account")
                .execute(client);

        // This will wait for the receipt to become available.
        TransactionReceipt accountCreateTxReceipt = accountCreateTxResponse.getReceipt(client);
        AccountId multiSigAccountId = Objects.requireNonNull(accountCreateTxReceipt.accountId);
        Objects.requireNonNull(multiSigAccountId);
        System.out.println("Created new account with ID: " + multiSigAccountId);

        /*
         * Step 4:
         * Check the balance of the newly created account.
         */
        AccountBalance accountBalance =
                new AccountBalanceQuery().setAccountId(multiSigAccountId).execute(client);

        System.out.println("Balance of a newly created account with ID " + multiSigAccountId + ": "
                + accountBalance.hbars.toTinybars() + " tinybar.");

        /*
         * Step 5:
         * Schedule crypto transfer from multi-sig account to operator account.
         */
        System.out.println("Scheduling crypto transfer from multi-sig account to operator account...");
        TransactionResponse transferTxScheduled = new TransferTransaction()
                .addHbarTransfer(multiSigAccountId, Hbar.from(1).negated())
                .addHbarTransfer(Objects.requireNonNull(client.getOperatorAccountId()), Hbar.from(1))
                .schedule()
                .freezeWith(client)
                // Add first signature.
                .sign(privateKeys[0])
                .execute(client);

        TransactionReceipt transferTxScheduledReceipt = transferTxScheduled.getReceipt(client);
        System.out.println("Schedule status: " + transferTxScheduledReceipt.status);
        ScheduleId scheduleId = Objects.requireNonNull(transferTxScheduledReceipt.scheduleId);
        System.out.println("Schedule ID: " + scheduleId);
        TransactionId scheduledTxId = Objects.requireNonNull(transferTxScheduledReceipt.scheduledTransactionId);
        System.out.println("Scheduled transaction ID: " + scheduledTxId);

        // Add second signature.
        TransactionResponse scheduleSignTxResponseSecondSignature = new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(privateKeys[1])
                .execute(client);

        TransactionReceipt scheduleSignTxReceiptSecondSignature =
                scheduleSignTxResponseSecondSignature.getReceipt(client);
        System.out.println("A transaction that appends signature to a schedule transaction (private key #2) "
                + "was complete with status: " + scheduleSignTxReceiptSecondSignature.status);

        // Add third signature.
        TransactionResponse scheduleSignTxResponseThirdSignature = new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(privateKeys[2])
                .execute(client);

        TransactionReceipt scheduleSignTxReceiptThirdSignature =
                scheduleSignTxResponseThirdSignature.getReceipt(client);
        System.out.println("A transaction that appends signature to a schedule transaction (private key #3) "
                + "was complete with status: " + scheduleSignTxReceiptThirdSignature.status);

        /*
         * Step 6:
         * Query schedule.
         */
        ScheduleInfo scheduleInfo =
                new ScheduleInfoQuery().setScheduleId(scheduleId).execute(client);
        System.out.println("Schedule info: " + scheduleInfo);

        /*
         * Step 7:
         * Query triggered scheduled transaction.
         */
        TransactionRecord recordScheduledTx =
                new TransactionRecordQuery().setTransactionId(scheduledTxId).execute(client);
        System.out.println("Triggered scheduled transaction info: " + recordScheduledTx);

        /*
         * Clean up:
         * Delete created account.
         */
        new AccountDeleteTransaction()
                .setAccountId(multiSigAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(privateKeys[0])
                .sign(privateKeys[1])
                .sign(privateKeys[2])
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Scheduled Transaction Multi-Sig With Threshold Example Complete!");
    }
}
