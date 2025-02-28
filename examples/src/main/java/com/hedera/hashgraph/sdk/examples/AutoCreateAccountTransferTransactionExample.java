// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to use auto account creation (HIP-583).
 * <p>
 * This HIP allows any new account on Hedera to have an ECDSA derived alias, compatible with Ethereum addresses,
 * and permits the use of this alias for all operations supported by alias today.
 * This HIP expands support in the auto-create flow for ECDSA derived addresses to be used as the alias.
 * It also adds support to CryptoCreate transactions for creation with an ECDSA derived alias.
 */
public class AutoCreateAccountTransferTransactionExample {

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
        System.out.println("Auto Create Account Via Transfer Transaction (HIP-583) Example Start!");

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
         * Generate ECSDA private key.
         */
        PrivateKey privateKey = PrivateKey.generateECDSA();

        /*
         * Step 2:
         * Extract ECDSA public key.
         */
        PublicKey publicKey = privateKey.getPublicKey();

        /*
         * Step 3:
         * Extract Ethereum public address.
         */
        EvmAddress evmAddress = publicKey.toEvmAddress();
        System.out.println("EVM address of the new account: " + evmAddress);

        /*
         * Step 4:
         * Use the TransferTransaction.
         * - populate the FromAddress with the sender Hedera account ID;
         * - populate the ToAddress with Ethereum public address.
         *
         * Note: Can transfer from public address to public address in the TransferTransaction for complete accounts.
         * Transfers from hollow accounts will not work because the hollow account does not have a public key
         * assigned to authorize transfers out of the account.
         */
        TransferTransaction transferTx = new TransferTransaction()
                .addHbarTransfer(OPERATOR_ID, Hbar.from(1).negated())
                .addHbarTransfer(AccountId.fromEvmAddress(evmAddress, 0, 0), Hbar.from(1))
                .freezeWith(client);

        /*
         * Step 5:
         * Sign and execute the TransferTransaction transaction using existing Hedera account
         * and key paying for the transaction fee.
         */
        System.out.println("Transferring Hbar to the the new account...");
        TransactionResponse transferTxResponse = transferTx.execute(client);

        /*
         * Step 6:
         * Get the new account ID ask for the child receipts or child records for the parent transaction ID of the TransferTransaction
         * (the AccountCreateTransaction is executed as a child transaction triggered by the TransferTransaction).
         */
        TransactionReceipt transferTxReceipt = new TransactionReceiptQuery()
                .setTransactionId(transferTxResponse.transactionId)
                .setIncludeChildren(true)
                .execute(client);

        AccountId aliceAccountId = transferTxReceipt.children.get(0).accountId;
        Objects.requireNonNull(aliceAccountId);
        System.out.println("The \"normal\" account ID of the given alias: " + aliceAccountId);

        /*
         * Step 7:
         * Get the AccountInfo and verify the account is a hollow account with the supplied public address (may need to verify with mirror node API).
         *
         * The Hedera Account that was created has a public address the user specified in the TransferTransaction ToAddress:
         *  - will not have a public key at this stage;
         *  - cannot do anything besides receive Hbar or tokens;
         *  - the alias property of the account does not have the public address;
         *  - referred to as a hollow account.
         */
        AccountInfo aliceAccountInfo_BeforeEnhancing =
                new AccountInfoQuery().setAccountId(aliceAccountId).execute(client);

        if (((KeyList) aliceAccountInfo_BeforeEnhancing.key).isEmpty()) {
            System.out.println("The newly created account is a hollow account! (Success)");
        } else {
            throw new Exception("The newly created account is not a hollow account! (Fail)");
        }

        /*
         * Step 8:
         * Create a HAPI transaction and assign the new hollow account as the transaction fee payer.
         *
         * Sign with the private key that corresponds to the public key on the hollow account
         * (to enhance the hollow account to have a public key the hollow account needs to be specified as a transaction fee payer in a HAPI transaction).
         */
        System.out.println("Creating new topic...");
        TransactionReceipt topicCreateTxReceipt = new TopicCreateTransaction()
                .setAdminKey(publicKey)
                .setTransactionId(TransactionId.generate(aliceAccountId))
                .setTopicMemo("Memo")
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("Created new topic with ID: " + topicCreateTxReceipt.topicId);

        /*
         * Step 9:
         * Get the account info and return public key to show its complete account.
         */
        AccountInfo aliceAccountInfo_AfterEnhancing =
                new AccountInfoQuery().setAccountId(aliceAccountId).execute(client);

        System.out.println(
                "The public key of the newly created and now complete account: " + aliceAccountInfo_AfterEnhancing.key);

        /*
         * Clean up:
         * Delete created account and topic.
         */
        new AccountDeleteTransaction()
                .setTransferAccountId(OPERATOR_ID)
                .setAccountId(aliceAccountId)
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client);

        new TopicDeleteTransaction()
                .setTopicId(topicCreateTxReceipt.topicId)
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Auto Create Account Via Transfer Transaction (HIP-583) Example Complete!");
    }
}
