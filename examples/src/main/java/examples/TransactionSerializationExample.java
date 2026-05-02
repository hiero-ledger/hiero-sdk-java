// SPDX-License-Identifier: Apache-2.0
package examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to serialize incomplete transaction, deserialize it, complete and execute (HIP-745).
 */
class TransactionSerializationExample {

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
        System.out.println("Transaction Serialization (HIP-745) Example Start!");

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
         * Check Hbar balance of sender and recipient.
         */
        AccountId recipientId = AccountId.fromString("0.0.3");
        Hbar senderBalanceBefore =
                new AccountBalanceQuery().setAccountId(OPERATOR_ID).execute(client).hbars;

        Hbar recipientBalanceBefore =
                new AccountBalanceQuery().setAccountId(recipientId).execute(client).hbars;

        System.out.println("Sender (" + OPERATOR_ID + ") balance before transfer: " + senderBalanceBefore);
        System.out.println("Recipient (" + recipientId + ") balance before transfer: " + recipientBalanceBefore);

        /*
         * Step 2:
         * Create the transfer transaction with adding only Hbar transfer which credits the operator.
         */
        System.out.println("Creating the transfer transaction...");
        Hbar transferAmount = Hbar.from(1);
        var transferTx = new TransferTransaction()
                // addSender and addRecipient can be called as many times as you want as long as the total sum from
                // both sides is equivalent.
                .addHbarTransfer(OPERATOR_ID, transferAmount.negated());

        /*
         * Step 3:
         * Serialize the transfer transaction.
         */
        System.out.println("Serializing the transfer transaction...");
        var transactionBytes = transferTx.toBytes();

        /*
         * Step 4:
         * Deserialize the transfer transaction.
         */
        System.out.println("Deserializing the transfer transaction...");
        TransferTransaction transferTxDeserialized = (TransferTransaction) Transaction.fromBytes(transactionBytes);

        /*
         * Step 5:
         * Complete the transfer transaction-- add Hbar transfer which debits Hbar to the recipient.
         * And execute the transfer transaction.
         */
        System.out.println("Completing and executing the transfer transaction...");
        var transferTxResponse = transferTxDeserialized
                .addHbarTransfer(recipientId, transferAmount)
                .setTransactionMemo("HIP-745 example")
                .execute(client);

        System.out.println("Transaction info: " + transferTxResponse);
        TransactionRecord transferTxRecord = transferTxResponse.getRecord(client);
        System.out.println("Transferred " + transferAmount);
        System.out.println("Transfer memo: " + transferTxRecord.transactionMemo);

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
