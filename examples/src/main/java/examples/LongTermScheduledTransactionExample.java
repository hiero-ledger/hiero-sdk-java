// SPDX-License-Identifier: Apache-2.0
package examples;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountUpdateTransaction;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.KeyList;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ScheduleInfo;
import com.hedera.hashgraph.sdk.ScheduleInfoQuery;
import com.hedera.hashgraph.sdk.ScheduleSignTransaction;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.time.Instant;
import java.util.Objects;

/**
 * How to long term schedule transactions (HIP-423).
 */
class LongTermScheduledTransactionExample {

    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * Operator's account ID. Used to sign and pay for operations on Hedera.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file. Network can be: localhost, testnet,
     * previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file. Log levels can be: TRACE, DEBUG, INFO, WARN,
     * ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL, for example via VM
     * options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    public static void main(String[] args) throws Exception {
        System.out.println("Long Term Scheduled Transaction Example Start!");

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
         * Create key pairs
         */
        var privateKey1 = PrivateKey.generateED25519();
        var publicKey1 = privateKey1.getPublicKey();
        var privateKey2 = PrivateKey.generateED25519();

        System.out.println(
                "Creating a Key List..."
                        + "(with threshold, it will require 2 of 2 keys we generated to sign on anything modifying this account).");
        KeyList thresholdKey = KeyList.withThreshold(2);
        thresholdKey.add(privateKey1);
        thresholdKey.add(privateKey2);
        System.out.println("Created a Key List: " + thresholdKey);

        /*
         * Step 2:
         * Create the account
         */
        System.out.println("Creating new account...(with the above Key List as an account key).");
        var alice = new AccountCreateTransaction()
                .setKeyWithoutAlias(thresholdKey)
                .setInitialBalance(new Hbar(2))
                .execute(client)
                .getReceipt(client)
                .accountId;
        System.out.println("Created new account with ID: " + alice);

        /*
         * Step 3:
         * Schedule a transfer transaction of 1 Hbar from the created account to the
         * operator account with an expirationTime of
         * 24 hours in the future and waitForExpiry=false
         */
        System.out.println("Creating new scheduled transaction with 1 day expiry");
        TransferTransaction transfer = new TransferTransaction()
                .addHbarTransfer(alice, new Hbar(1).negated())
                .addHbarTransfer(client.getOperatorAccountId(), new Hbar(1));

        int oneDayInSecs = 86400;
        var scheduleId = transfer.schedule()
                .setWaitForExpiry(false)
                .setExpirationTime(Instant.now().plusSeconds(oneDayInSecs))
                .execute(client)
                .getReceipt(client)
                .scheduleId;

        /*
         * Step 4:
         * Sign the transaction with one key and verify the transaction is not executed
         */
        System.out.println("Signing the new scheduled transaction with 1 key");
        new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(privateKey1)
                .execute(client)
                .getReceipt(client);

        ScheduleInfo info = new ScheduleInfoQuery().setScheduleId(scheduleId).execute(client);
        System.out.println("Scheduled transaction is not yet executed. Executed at: " + info.executedAt);

        /*
         * Step 5:
         * Sign the transaction with the other key and verify the transaction executes successfully
         */
        var accountBalance = new AccountBalanceQuery().setAccountId(alice).execute(client);
        System.out.println("Alice's account balance before schedule transfer: " + accountBalance.hbars);

        System.out.println("Signing the new scheduled transaction with the 2nd key");
        new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(privateKey2)
                .execute(client)
                .getReceipt(client);

        accountBalance = new AccountBalanceQuery().setAccountId(alice).execute(client);
        System.out.println("Alice's account balance after schedule transfer: " + accountBalance.hbars);

        info = new ScheduleInfoQuery().setScheduleId(scheduleId).execute(client);
        System.out.println("Scheduled transaction is executed. Executed at: " + info.executedAt);

        /*
         * Step 6:
         * Schedule another transfer transaction of 1 Hbar from the account to the operator account
         * with an expirationTime of 10 seconds in the future and waitForExpiry=true .
         */
        System.out.println("Creating new scheduled transaction with 10 seconds expiry");
        transfer = new TransferTransaction()
                .addHbarTransfer(alice, new Hbar(1).negated())
                .addHbarTransfer(client.getOperatorAccountId(), new Hbar(1));

        var scheduleId2 = transfer.schedule()
                .setWaitForExpiry(true)
                .setExpirationTime(Instant.now().plusSeconds(10))
                .execute(client)
                .getReceipt(client)
                .scheduleId;
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;

        /*
         * Step 7:
         * Sign the transaction with one key and verify the transaction is not executed
         */
        System.out.println("Signing the new scheduled transaction with 1 key");
        new ScheduleSignTransaction()
                .setScheduleId(scheduleId2)
                .freezeWith(client)
                .sign(privateKey1)
                .execute(client)
                .getReceipt(client);

        info = new ScheduleInfoQuery().setScheduleId(scheduleId2).execute(client);
        System.out.println("Scheduled transaction is not yet executed. Executed at: " + info.executedAt);

        /*
         * Step 8:
         * Update the account’s key to be only the one key
         * that has already signed the scheduled transfer.
         */
        System.out.println("Updating Alice's key to be the 1st key");
        new AccountUpdateTransaction()
                .setAccountId(alice)
                .setKey(publicKey1)
                .freezeWith(client)
                .sign(privateKey1)
                .sign(privateKey2)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 9:
         * Verify that the transfer successfully executes roughly at the time of its expiration.
         */
        accountBalance = new AccountBalanceQuery().setAccountId(alice).execute(client);

        System.out.println("Alice's account balance before schedule transfer: " + accountBalance.hbars);
        while (elapsedTime < 10 * 1000) {
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.printf("Elapsed time: %.1f seconds\r", elapsedTime / 1000.0);
            Thread.sleep(100); // Pause briefly to reduce CPU usage
        }
        accountBalance = new AccountBalanceQuery().setAccountId(alice).execute(client);
        System.out.println("Alice's account balance after schedule transfer: " + accountBalance.hbars);

        System.out.println("Long Term Scheduled Transaction Example Complete!");
    }
}
