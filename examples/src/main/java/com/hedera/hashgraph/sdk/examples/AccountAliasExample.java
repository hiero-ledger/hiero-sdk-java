// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountDeleteTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ClientHelper;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
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
// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountDeleteTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ClientHelper;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
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
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
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
         */
        System.out.println("\"Creating\" new account...");

        AccountId aliasAccountId = publicKey.toAccountId(0, 0);

        System.out.println("New account ID: " + aliasAccountId);
        System.out.println("Just the aliasKey: " + aliasAccountId.aliasKey);

        System.out.println(AccountId.fromString(
                "0.0.302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777"));

        System.out.println(PublicKey.fromString(
                "302a300506032b6570032100114e6abc371b82dab5c15ea149f02d34a012087b163516dd70f44acafabf7777")
                .toAccountId(0, 0));

        /*
         * Step 3:
         * Transfer Hbar to the new account.
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
