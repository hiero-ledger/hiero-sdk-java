// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * How to remove signatures from a transaction with a multi-sig account.
 * <p>
 * Demonstrates {@link Transaction#removeSignature(PublicKey)} and {@link Transaction#removeAllSignatures()},
 * which are useful when a signer revokes their signature or when signatures must be dropped to keep a transaction
 * within size limits.
 */
class RemoveSignatureExample {

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
        System.out.println("Remove Signature Example Start!");

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
         * Generate ED25519 key pairs for Alice and Bob.
         */
        System.out.println("Generating ED25519 private and public keys for accounts...");

        PrivateKey alicePrivateKey = PrivateKey.generateED25519();
        PublicKey alicePublicKey = alicePrivateKey.getPublicKey();
        System.out.println("Alice's ED25519 Public Key: " + alicePublicKey);

        PrivateKey bobPrivateKey = PrivateKey.generateED25519();
        PublicKey bobPublicKey = bobPrivateKey.getPublicKey();
        System.out.println("Bob's ED25519 Public Key: " + bobPublicKey);

        /*
         * Step 2:
         * Create a multi-sig account owned by Alice and Bob.
         */
        System.out.println("Creating new Key List...");
        KeyList keylist = new KeyList();
        keylist.add(alicePublicKey);
        keylist.add(bobPublicKey);

        System.out.println("Creating a new account...");
        TransactionResponse createAccountTxResponse = new AccountCreateTransaction()
                .setInitialBalance(Hbar.from(2))
                .setKeyWithoutAlias(keylist)
                .execute(client);

        var newAccountId = Objects.requireNonNull(createAccountTxResponse.getReceipt(client).accountId);
        System.out.println("Created new account with ID: " + newAccountId);

        /*
         * Step 3:
         * Create a transfer from the new account to the account with ID '0.0.3', freeze it and collect signatures.
         */
        System.out.println("Transferring 1 Hbar from new account to the account with ID `0.0.3`...");
        TransferTransaction transferTx = new TransferTransaction()
                .setNodeAccountIds(Collections.singletonList(new AccountId(0, 0, 3)))
                .addHbarTransfer(newAccountId, Hbar.from(1).negated())
                .addHbarTransfer(new AccountId(0, 0, 3), Hbar.from(1))
                .freezeWith(client);

        byte[] transactionBytes = transferTx.toBytes();
        Transaction<?> transactionToExecute = Transaction.fromBytes(transactionBytes);

        System.out.println("Collecting Alice's and Bob's signatures offline...");
        byte[] alicesSignature = alicePrivateKey.signTransaction(Transaction.fromBytes(transactionBytes));
        byte[] bobsSignature = bobPrivateKey.signTransaction(Transaction.fromBytes(transactionBytes));

        transactionToExecute.signWithOperator(client);
        transactionToExecute.addSignature(alicePublicKey, alicesSignature);
        transactionToExecute.addSignature(bobPublicKey, bobsSignature);

        /*
         * Step 4:
         * Remove Alice's signature (for example, if she revokes it), then add it back before executing.
         */
        System.out.println("Removing Alice's signature from the transaction...");
        List<byte[]> removedAlicesSignatures = transactionToExecute.removeSignature(alicePublicKey);
        System.out.println("Removed " + removedAlicesSignatures.size() + " signature(s) for Alice.");

        System.out.println("Adding Alice's signature back to the transaction...");
        transactionToExecute.addSignature(alicePublicKey, alicesSignature);

        /*
         * Step 5:
         * Execute the transaction with all required signatures present.
         */
        System.out.println("Executing transfer transaction...");
        TransactionReceipt transferTxReceipt =
                transactionToExecute.execute(client).getReceipt(client);
        System.out.println("Transfer transaction was complete with status: " + transferTxReceipt.status);

        /*
         * Step 6:
         * Demonstrate removeAllSignatures() on a fresh copy of the same transfer, then re-sign and execute.
         */
        System.out.println("Demonstrating removeAllSignatures()...");
        Transaction<?> secondTransfer = Transaction.fromBytes(transactionBytes);
        secondTransfer.signWithOperator(client);
        secondTransfer.addSignature(alicePublicKey, alicesSignature);
        secondTransfer.addSignature(bobPublicKey, bobsSignature);

        var removedSignatures = secondTransfer.removeAllSignatures();
        System.out.println(
                "Removed all signatures for " + removedSignatures.keySet().size() + " public key(s).");

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

        System.out.println("Remove Signature Example Complete!");
    }
}
