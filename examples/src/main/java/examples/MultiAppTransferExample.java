// SPDX-License-Identifier: Apache-2.0
package examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to transfer Hbar to an account with the receiver signature enabled.
 */
class MultiAppTransferExample {

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
        System.out.println("MultiApp Transfer Example Start!");

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
        // The exchange should possess this key, we're only generating it for demonstration purposes.
        PrivateKey exchangePrivateKey = PrivateKey.generateED25519();
        PublicKey exchangePublicKey = exchangePrivateKey.getPublicKey();

        // This is the only key we should actually possess.
        PrivateKey userPrivateKey = PrivateKey.generateED25519();
        PublicKey userPublicKey = userPrivateKey.getPublicKey();

        /*
         * Step 2:
         * Create exchange and receiver accounts.
         */
        System.out.println("Creating exchange and receiver accounts...");
        // The exchange creates an account for the user to transfer funds to.
        AccountId exchangeAccountId = new AccountCreateTransaction()
                // The exchange only accepts transfers that it validates through a side channel (e.g. REST API).
                .setReceiverSignatureRequired(true)
                .setKeyWithoutAlias(exchangePublicKey)
                // The owner key has to sign this transaction when setReceiverSignatureRequired is true.
                .freezeWith(client)
                .sign(exchangePrivateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(exchangeAccountId);

        // For the purpose of this example we create an account for the user with a balance of 5 Hbar.
        AccountId userAccountId = new AccountCreateTransaction()
                .setInitialBalance(Hbar.from(2))
                .setKeyWithoutAlias(userPublicKey)
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(userAccountId);

        Hbar senderBalanceBefore =
                new AccountBalanceQuery().setAccountId(userAccountId).execute(client).hbars;

        Hbar exchangeBalanceBefore =
                new AccountBalanceQuery().setAccountId(exchangeAccountId).execute(client).hbars;

        System.out.println("User account (" + userAccountId + ") balance: " + senderBalanceBefore);
        System.out.println("Exchange account (" + exchangeAccountId + ") balance: " + exchangeBalanceBefore);

        /*
         * Step 3:
         * Make a transfer from the user account to the exchange account, this requires signing by both parties.
         */
        TransferTransaction transferTx = new TransferTransaction()
                .addHbarTransfer(userAccountId, Hbar.from(1).negated())
                .addHbarTransfer(exchangeAccountId, Hbar.from(1))
                // The exchange-provided memo required to validate the transaction.
                .setTransactionMemo("https://some-exchange.com/user1/account1")
                // NOTE: to manually sign, you must freeze the Transaction first
                .freezeWith(client)
                .sign(userPrivateKey);

        // The exchange must sign the transaction in order for it to be accepted by the network
        // (assume this is some REST call to the exchange API server).
        byte[] signedTransferTxBytes = Transaction.fromBytes(transferTx.toBytes())
                .sign(exchangePrivateKey)
                .toBytes();

        // Parse the transaction bytes returned from the exchange.
        Transaction<?> signedTransferTx = Transaction.fromBytes(signedTransferTxBytes);

        // Get the amount we are about to transfer (we built this with +2, -2).
        Hbar transferAmount = ((TransferTransaction) signedTransferTx)
                .getHbarTransfers()
                .values()
                .toArray(new Hbar[0])[0];

        System.out.println("Transferring " + transferAmount + " from the user account to the exchange account...");

        // We now execute the signed transaction and wait for it to be accepted.
        TransactionResponse transactionTxResponse = signedTransferTx.execute(client);

        // (Important!) Wait for consensus by querying for the receipt.
        transactionTxResponse.getReceipt(client);

        /*
         * Step 4:
         * Query user and exchange account balance to validate the transfer was successfully complete.
         */
        Hbar senderBalanceAfter =
                new AccountBalanceQuery().setAccountId(userAccountId).execute(client).hbars;

        Hbar exchangeBalanceAfter =
                new AccountBalanceQuery().setAccountId(exchangeAccountId).execute(client).hbars;

        System.out.println("User account (" + userAccountId + ") balance: " + senderBalanceAfter);
        System.out.println("Exchange account (" + exchangeAccountId + ") balance: " + exchangeBalanceAfter);

        /*
         * Clean up:
         * Delete created accounts.
         */
        new AccountDeleteTransaction()
                .setAccountId(exchangeAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(exchangePrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(userAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(userPrivateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("MultiApp Transfer Example Complete!");
    }
}
