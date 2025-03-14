// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

/**
 * How to sign a transaction with multi-sig account.
 */
class MultiSigOfflineExample {

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
        System.out.println("Multi Sig Offline Example Start!");

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
        System.out.println("Generating ED25519 private and public keys for accounts...");

        PrivateKey alicePrivateKey = PrivateKey.generateED25519();
        System.out.println("Alice's ED25519 Private Key: " + alicePrivateKey);

        PublicKey alicePublicKey = alicePrivateKey.getPublicKey();
        System.out.println("Alice's ED25519 Public Key: " + alicePublicKey);

        PrivateKey bobPrivateKey = PrivateKey.generateED25519();
        System.out.println("Bob's ED25519 Private Key: " + bobPrivateKey);

        PublicKey bobPublicKey = bobPrivateKey.getPublicKey();
        System.out.println("Bob's ED25519 Public Key: " + bobPublicKey);

        /*
         * Step 2:
         * Create a Multi-sig account.
         */
        System.out.println("Creating new Key List..");
        KeyList keylist = new KeyList();
        keylist.add(alicePublicKey);
        keylist.add(bobPublicKey);
        System.out.println("Created Key List: " + keylist);

        System.out.println("Creating a new account...");
        TransactionResponse createAccountTxResponse = new AccountCreateTransaction()
                .setInitialBalance(Hbar.from(2))
                .setKeyWithoutAlias(keylist)
                .execute(client);

        TransactionReceipt createAccountTxReceipt = createAccountTxResponse.getReceipt(client);
        var newAccountId = createAccountTxReceipt.accountId;
        Objects.requireNonNull(newAccountId);
        System.out.println("Created new account with ID: " + newAccountId);

        /*
         * Step 2:
         * Create a transfer from new account to the account with ID '0.0.3'.
         */
        System.out.println("Transferring 1 Hbar from new account to the account with ID `0.0.3`...");
        TransferTransaction transferTx = new TransferTransaction()
                .setNodeAccountIds(Collections.singletonList(new AccountId(0, 0, 3)))
                .addHbarTransfer(
                        Objects.requireNonNull(createAccountTxReceipt.accountId),
                        Hbar.from(1).negated())
                .addHbarTransfer(new AccountId(0, 0, 3), Hbar.from(1))
                .freezeWith(client);

        /*
         * Step 3:
         * Convert transaction to bytes to send to signatories.
         */
        System.out.println("Converting transaction to bytes to send to signatories...");
        byte[] transactionBytes = transferTx.toBytes();
        Transaction<?> transactionToExecute = Transaction.fromBytes(transactionBytes);

        /*
         * Step 4:
         * Ask users to sign and return signature.
         */
        byte[] alicesSignature = alicePrivateKey.signTransaction(Transaction.fromBytes(transactionBytes));
        System.out.println("Alice signed the transaction. Signature: " + Arrays.toString(alicesSignature));
        byte[] bobsSignature = bobPrivateKey.signTransaction(Transaction.fromBytes(transactionBytes));
        System.out.println("Bob signed the transaction. Signature: " + Arrays.toString(bobsSignature));

        /*
         * Step 5:
         * Recreate the transaction from bytes.
         */
        System.out.println("Adding users' signatures to the transaction...");
        transactionToExecute.signWithOperator(client);
        transactionToExecute.addSignature(alicePrivateKey.getPublicKey(), alicesSignature);
        transactionToExecute.addSignature(bobPrivateKey.getPublicKey(), bobsSignature);

        /*
         * Step 6:
         * Execute recreated transaction.
         */
        System.out.println("Executing transfer transaction...");
        TransactionResponse transferTxResponse = transactionToExecute.execute(client);
        createAccountTxReceipt = transferTxResponse.getReceipt(client);
        System.out.println("Transfer transaction was complete with status: " + createAccountTxReceipt.status);

        /*
         * Clean up:
         * Delete created account.
         */
        new AccountDeleteTransaction()
                .setAccountId(newAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(alicePrivateKey)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Multi Sig Offline Example Complete!");
    }
}
