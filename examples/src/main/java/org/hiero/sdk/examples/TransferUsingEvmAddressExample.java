// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to transfer Hbar or tokens to a Hedera account using their public-address (HIP-583).
 */
class TransferUsingEvmAddressExample {

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
        System.out.println("Transfer Using Evm Address Example Start!");

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
         * Create an ECSDA private key.
         */
        PrivateKey alicePrivateKey = PrivateKey.generateECDSA();

        /*
         * Step 2:
         * Extract the ECDSA public key.
         */
        PublicKey alicePublicKey = alicePrivateKey.getPublicKey();

        /*
         * Step 3:
         * Extract the Ethereum public address.
         */
        EvmAddress aliceEvmAddress = alicePublicKey.toEvmAddress();
        System.out.println("EVM address of Alice's account: " + aliceEvmAddress);

        /*
         * Step 4:
         * Transfer tokens using the TransferTransaction to the Ethereum Account Address.
         * - the from field should be a complete account that has a public address;
         * - the to field should be to a public address (to create a new account).
         */
        System.out.println("Transferring Hbar to Alice's account...");
        TransferTransaction transferTx = new TransferTransaction()
                .addHbarTransfer(OPERATOR_ID, Hbar.from(1).negated())
                .addHbarTransfer(aliceEvmAddress, Hbar.from(1))
                .freezeWith(client);

        TransferTransaction transferTxSigned = transferTx.sign(OPERATOR_KEY);
        TransactionResponse transferTxResponse = transferTxSigned.execute(client);

        /*
         * Step 5:
         * Get the child receipt or child record to return the Hedera Account ID for the new account that was created.
         */
        TransactionReceipt transferTxReceipt = new TransactionReceiptQuery()
                .setTransactionId(transferTxResponse.transactionId)
                .setIncludeChildren(true)
                .execute(client);

        AccountId aliceAccountId = transferTxReceipt.children.get(0).accountId;
        Objects.requireNonNull(aliceAccountId);
        System.out.println("The \"normal\" account ID of the given alias: " + aliceAccountId);

        /*
         * Step 6:
         * Get the AccountInfo on the new account and show it is a hollow account by not having a public key.
         */
        AccountInfo aliceAccountInfo_BeforeEnhancing =
                new AccountInfoQuery().setAccountId(aliceAccountId).execute(client);

        System.out.println("Alice's account info: " + aliceAccountInfo_BeforeEnhancing);

        /*
         * Step 7:
         * Use the hollow account as a transaction fee payer in a HAPI transaction.
         */
        System.out.println("Setting new account as client's operator...");
        client.setOperator(aliceAccountId, alicePrivateKey);
        PrivateKey bobPrivateKey = PrivateKey.generateED25519();
        PublicKey bobPublicKey = bobPrivateKey.getPublicKey();

        System.out.println("Creating Bob's account...");
        AccountCreateTransaction accountCreateTx =
                new AccountCreateTransaction().setKeyWithoutAlias(bobPublicKey).freezeWith(client);

        /*
         * Step 8:
         * Sign the transaction with ECDSA private key.
         */
        AccountCreateTransaction accountCreateTxSigned = accountCreateTx.sign(alicePrivateKey);
        TransactionResponse accountCreateTxResponse = accountCreateTxSigned.execute(client);
        TransactionReceipt accountCreateTxReceipt = accountCreateTxResponse.getReceipt(client);
        var bobAccountId = accountCreateTxReceipt.accountId;
        Objects.requireNonNull(bobAccountId);
        System.out.println("Created Bob's account with ID: " + bobAccountId);

        /*
         * Step 9:
         * Get the AccountInfo of the account and show the account is now a complete account
         * by returning the public key on the account.
         */
        AccountInfo aliceAccountInfo_AfterEnhancing =
                new AccountInfoQuery().setAccountId(aliceAccountId).execute(client);

        System.out.println("The public key of the newly created (and now complete) account: "
                + aliceAccountInfo_AfterEnhancing.key);

        /*
         * Clean up:
         * Delete created accounts.
         */
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        new AccountDeleteTransaction()
                .setAccountId(aliceAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(bobAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Transfer Using Evm Address Example Complete!");
    }
}

