// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import org.hiero.sdk.logger.LogLevel;
import org.hiero.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to use auto account creation (HIP-32).
 * <p>
 * You can "create" an account by generating a private key, and then deriving the public key,
 * without any need to interact with the Hedera network. The public key more or less acts as the user's
 * account ID. This public key is an account's aliasKey: a public key that aliases (or will eventually alias)
 * to a Hedera account.
 * <p>
 * An AccountId takes one of two forms: a normal AccountId with a null aliasKey member takes the form 0.0.123,
 * while an account ID with a non-null aliasKey member takes the form
 * 0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777
 * Note the prefix of "0.0." indicating the shard and realm. Also note that the aliasKey is stringified
 * as a hex-encoded ASN1 DER representation of the key.
 * <p>
 * An AccountId with an aliasKey can be used just like a normal AccountId for the purposes of queries and
 * transactions, however most queries and transactions involving such an AccountId won't work until Hbar has
 * been transferred to the aliasKey account.
 * <p>
 * There is no record in the Hedera network of an account associated with a given aliasKey
 * until an amount of Hbar is transferred to the account. The moment that Hbar is transferred to that aliasKey
 * AccountId is the moment that that account actually begins to exist in the Hedera ledger.
 */
class AccountAliasExample {

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
        System.out.println("Account Alias Example (HIP-32) Start!");

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
         * Generate ED25519 key pair.
         */
        System.out.println("Generating ED25519 key pair...");
        PrivateKey privateKey = PrivateKey.generateED25519();
        PublicKey publicKey = privateKey.getPublicKey();

        /*
         * Step 2:
         * Create a couple of example Account Ids.
         *
         * Note that no queries or transactions have taken place yet.
         * This account "creation" process is entirely local.
         *
         * AccountId.fromString() can construct an AccountId with an aliasKey.
         * It expects a string of the form 0.0.123 in the case of a normal AccountId, or of the form
         * 0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777
         * in the case of an AccountId with aliasKey. Note the prefix of '0.0.' to indicate the shard and realm.
         *
         * If the shard and realm are known, you may use PublicKey.fromString().toAccountId() to construct the
         * aliasKey AccountId.
         */
        System.out.println("\"Creating\" new account...");

        // Assuming that the target shard and realm are known.
        // For now, they are virtually always 0 and 0.
        AccountId aliasAccountId = publicKey.toAccountId(0, 0);

        System.out.println("New account ID: " + aliasAccountId);
        System.out.println("Just the aliasKey: " + aliasAccountId.aliasKey);

        AccountId fromStringExample = AccountId.fromString(
                "0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777");

        AccountId fromKeyStringExample = PublicKey.fromString(
                        "302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777")
                .toAccountId(0, 0);

        /*
         * Step 3:
         * Transfer Hbar to the new account.
         *
         * Transfer will actually create an actual Hedera account,
         * deducting the creation fee from the amount transferred.
         */
        System.out.println("Transferring Hbar to the new account...");
        new TransferTransaction()
                .addHbarTransfer(OPERATOR_ID, Hbar.from(1).negated())
                .addHbarTransfer(aliasAccountId, Hbar.from(1))
                .execute(client)
                .getReceipt(client);

        /*
         * Step 4:
         * Query and output info about the new account.
         *
         * Note that once an account exists in the ledger, it is assigned a normal AccountId, which can be retrieved
         * via an AccountInfoQuery.
         *
         * Users may continue to refer to the account by its aliasKey AccountId, but they may also
         * now refer to it by its normal AccountId
         */
        AccountBalance newAccountBalance =
                new AccountBalanceQuery().setAccountId(aliasAccountId).execute(client);

        System.out.println("Balances of the new account: " + newAccountBalance);

        AccountInfo newAccountInfo =
                new AccountInfoQuery().setAccountId(aliasAccountId).execute(client);

        Objects.requireNonNull(newAccountInfo.accountId);

        System.out.println("Info about the new account: " + newAccountInfo);
        System.out.println("The normal account ID: " + newAccountInfo.accountId);
        System.out.println("The alias key: " + newAccountInfo.aliasKey);

        /*
         * Clean up:
         * Delete created account and close the client.
         */
        new AccountDeleteTransaction()
                .setAccountId(newAccountInfo.accountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(privateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Account Alias Example (HIP-32) Complete!");
    }
}

