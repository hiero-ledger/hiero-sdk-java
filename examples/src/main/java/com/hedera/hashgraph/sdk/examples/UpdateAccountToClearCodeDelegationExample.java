// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * HIP-1340: EOA Code Delegation.
 *
 * <p>How to clear an existing delegation address on an account by submitting
 * an AccountUpdateTransaction with the zero EVM address.
 */
class UpdateAccountToClearCodeDelegationExample {

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

    /** Operator's private key. */
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
        System.out.println("Update Account To Clear Code Delegation Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        /*
         * Step 1:
         * Create an account with a delegation address already set, so the
         * subsequent clear has something meaningful to remove.
         */
        PrivateKey accountKey = PrivateKey.generateED25519();
        PublicKey accountPublicKey = accountKey.getPublicKey();

        var createResponse = new AccountCreateTransaction()
                .setKeyWithoutAlias(accountPublicKey)
                .setDelegationAddress("0x1111111111111111111111111111111111111111")
                .execute(client);
        var accountId = Objects.requireNonNull(createResponse.getReceipt(client).accountId);
        System.out.println("Created account with ID: " + accountId);

        var infoBeforeClear = new AccountInfoQuery().setAccountId(accountId).execute(client);
        System.out.println("AccountInfo.delegationAddress before clear: " + infoBeforeClear.delegationAddress);

        /*
         * Step 2:
         * Clear the delegation address by submitting an AccountUpdateTransaction
         * with the zero EVM address (0x00…00). The SDK detects all-zero bytes
         * and sends an empty value, which the network treats as a request to
         * remove the existing delegation.
         *
         * AccountUpdateTransaction must be signed by the account's key.
         */
        var transaction = new AccountUpdateTransaction()
                .setAccountId(accountId)
                .setDelegationAddress("0x0000000000000000000000000000000000000000")
                .freezeWith(client);

        // Access delegation address via getter — null because the zero address was normalized to "clear"
        System.out.println("Delegation address on transaction: " + transaction.getDelegationAddress());

        transaction.sign(accountKey).execute(client).getReceipt(client);

        var infoAfterClear = new AccountInfoQuery().setAccountId(accountId).execute(client);
        System.out.println("AccountInfo.delegationAddress after clear: " + infoAfterClear.delegationAddress);

        /*
         * Clean up:
         * Delete created account.
         */
        new AccountDeleteTransaction()
                .setTransferAccountId(OPERATOR_ID)
                .setAccountId(accountId)
                .freezeWith(client)
                .sign(accountKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Update Account To Clear Code Delegation Example Complete!");
    }
}
