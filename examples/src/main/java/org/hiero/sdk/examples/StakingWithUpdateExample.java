// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to stake to some account and then unstake.
 */
class StakingWithUpdateExample {

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
        System.out.println("Staking With Update Example Start!");

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
         * Generate an ED25519 key pair.
         */
        System.out.println("Generating ED25519 key pair...");
        PrivateKey privateKey = PrivateKey.generateED25519();
        PublicKey publicKey = privateKey.getPublicKey();

        /*
         * Step 2:
         * Create an account and stake to an account ID.
         *
         * In this case we're staking to account ID 3 which happens to be
         * the account ID of node 0, we're only doing this as an example.
         * If you really want to stake to node 0, you should use setStakedNodeId() instead.
         */
        System.out.println("Creating new account with staked account ID...");
        AccountId stakedAccountId = AccountId.fromString("0.0.3");
        AccountId newAccountId = new AccountCreateTransaction()
                .setKeyWithoutAlias(publicKey)
                .setInitialBalance(Hbar.from(1))
                .setStakedAccountId(stakedAccountId)
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(newAccountId);
        System.out.println("Created new account with ID: " + newAccountId);

        // Show the required key used to sign the account update transaction to
        // stake the accounts Hbar i.e. the fee payer key and key to authorize
        // changes to the account should be different.
        System.out.println("Key required to update staking information: " + publicKey);
        System.out.println("Fee payer or operator key: " + client.getOperatorPublicKey());

        /*
         * Step 3:
         * Query the account info, it should show the staked account ID to be 0.0.3.
         */
        AccountInfo accountInfo =
                new AccountInfoQuery().setAccountId(newAccountId).execute(client);

        if (accountInfo.stakingInfo.stakedAccountId.equals(stakedAccountId)) {
            System.out.println("New account staking info: " + accountInfo.stakingInfo);
        } else {
            throw new Exception("Staked account ID was not set correctly! (Fail)");
        }

        /*
         * Step 4:
         * Use the AccountUpdateTransaction to unstake the account's Hbar.
         *
         * If this succeeds then we should no longer have a staked account ID.
         */
        System.out.println("Updating the new account...(unstake Hbar).");
        new AccountUpdateTransaction()
                .setAccountId(newAccountId)
                .clearStakedAccountId()
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 5:
         * Query the account info, it should show the staked account ID to be null.
         */
        accountInfo = new AccountInfoQuery().setAccountId(newAccountId).execute(client);

        if (accountInfo.stakingInfo.stakedAccountId == null) {
            System.out.println("New account staking info: " + accountInfo.stakingInfo);
        } else {
            throw new Exception("Staked account ID was not set correctly! (Fail)");
        }

        /*
         * Clean up:
         * Delete created account.
         */
        new AccountDeleteTransaction()
                .setAccountId(newAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Staking With Update Example Complete!");
    }
}

