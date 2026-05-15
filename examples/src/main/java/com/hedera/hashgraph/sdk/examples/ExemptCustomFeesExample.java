// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountDeleteTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.CustomFractionalFee;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenDeleteTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenType;
import com.hedera.hashgraph.sdk.TokenWipeTransaction;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * How to exempt token creators all of their token’s fee collectors from a custom fee (HIP-573).
 */
class ExemptCustomFeesExample {

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
        System.out.println("Exempt Custom Fees Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        var operatorPublicKey = OPERATOR_KEY.getPublicKey();

        /*
         * Step 1:
         * Create three accounts: Alice, Bob, and Charlie.
         */
        System.out.println("Creating new accounts...");
        Hbar initialBalance = Hbar.from(1);
        PrivateKey alicePrivateKey = PrivateKey.generateED25519();
        PublicKey alicePublicKey = alicePrivateKey.getPublicKey();
        AccountId aliceAccountId = new AccountCreateTransaction()
                .setInitialBalance(initialBalance)
                .setKeyWithoutAlias(alicePublicKey)
                .freezeWith(client)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(aliceAccountId);

        PrivateKey bobPrivateKey = PrivateKey.generateED25519();
        PublicKey bobPublicKey = bobPrivateKey.getPublicKey();
        AccountId bobAccountId = new AccountCreateTransaction()
                .setInitialBalance(initialBalance)
                .setKeyWithoutAlias(bobPublicKey)
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(bobAccountId);

        PrivateKey charilePrivateKey = PrivateKey.generateED25519();
        PublicKey charilePublicKey = charilePrivateKey.getPublicKey();
        AccountId charlieAccountId = new AccountCreateTransaction()
                .setInitialBalance(initialBalance)
                .setKeyWithoutAlias(charilePublicKey)
                .freezeWith(client)
                .sign(charilePrivateKey)
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(charlieAccountId);

        /*
         * Step 2:
         * Create a fungible token that has three fractional fees:
         * - aliceFee sends 1/100 of the transferred value to Alice's account;
         * - bobFee sends 2/100 of the transferred value to Bob's account;
         * - charlieFee sends 3/100 of the transferred value to Charlie's account.
         */
        CustomFractionalFee aliceFee = new CustomFractionalFee()
                .setFeeCollectorAccountId(aliceAccountId)
                .setNumerator(1)
                .setDenominator(100)
                .setAllCollectorsAreExempt(true);

        CustomFractionalFee bobFee = new CustomFractionalFee()
                .setFeeCollectorAccountId(bobAccountId)
                .setNumerator(2)
                .setDenominator(100)
                .setAllCollectorsAreExempt(true);

        CustomFractionalFee charlieFee = new CustomFractionalFee()
                .setFeeCollectorAccountId(charlieAccountId)
                .setNumerator(3)
                .setDenominator(100)
                .setAllCollectorsAreExempt(true);

        System.out.println("Creating new Fungible Token using the Hedera Token Service...");
        TokenId fungibleTokenId = new TokenCreateTransaction()
                .setTokenName("HIP-573 Fungible Token")
                .setTokenSymbol("HIP573FT")
                .setTokenType(TokenType.FUNGIBLE_COMMON)
                .setTreasuryAccountId(OPERATOR_ID)
                .setAutoRenewAccountId(OPERATOR_ID)
                .setAdminKey(operatorPublicKey)
                .setFreezeKey(operatorPublicKey)
                .setWipeKey(operatorPublicKey)
                .setInitialSupply(100_000_000)
                .setDecimals(2)
                .setCustomFees(List.of(aliceFee, bobFee, charlieFee))
                .freezeWith(client)
                .sign(alicePrivateKey)
                .sign(bobPrivateKey)
                .sign(charilePrivateKey)
                .execute(client)
                .getReceipt(client)
                .tokenId;
        Objects.requireNonNull(fungibleTokenId);
        System.out.println("Created new fungible token with ID: " + fungibleTokenId);

        /*
         * Step 3:
         * Transfer tokens:
         * - 10_000 units of the Fungible Token from the operator's to Bob's account;
         * - 10_000 units of the Fungible Token from Bob's to Alice's account.
         */
        System.out.println("Transferring 10_000 units of the Fungible Token from the operator's to Bob's account...");
        new TransferTransaction()
                .addTokenTransfer(fungibleTokenId, OPERATOR_ID, -10_000)
                .addTokenTransfer(fungibleTokenId, bobAccountId, 10_000)
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .execute(client);

        System.out.println("Transferring 10_000 units of the Fungible Token from Bob's to Alice's account...");
        TransactionResponse transferTxResponse = new TransferTransaction()
                .addTokenTransfer(fungibleTokenId, bobAccountId, -10_000)
                .addTokenTransfer(fungibleTokenId, aliceAccountId, 10_000)
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client);

        /*
         * Step 4:
         * Get the transaction fee for that transfer transaction.
         */
        Hbar transactionFee = transferTxResponse.getRecord(client).transactionFee;

        System.out.println("Transaction fee for the transfer above: " + transactionFee);

        /*
         * Step 5:
         * Show that the fee collector accounts in the custom fee list
         * of the token that was created was not charged a custom fee in the transfer.
         */
        Long aliceAccountBalanceAfter = new AccountBalanceQuery()
                .setAccountId(aliceAccountId)
                .execute(client)
                .tokens
                .get(fungibleTokenId);

        Long bobAccountBalanceAfter = new AccountBalanceQuery()
                .setAccountId(bobAccountId)
                .execute(client)
                .tokens
                .get(fungibleTokenId);

        Long charlieAccountBalanceAfter = new AccountBalanceQuery()
                .setAccountId(charlieAccountId)
                .execute(client)
                .tokens
                .get(fungibleTokenId);

        System.out.println("Alice's balance after transferring the fungible token: " + aliceAccountBalanceAfter);
        System.out.println("Bob's account balance after transferring the fungible token: " + bobAccountBalanceAfter);
        System.out.println(
                "Charlie's account balance after transferring the fungible token: " + charlieAccountBalanceAfter);

        /*
         * Clean up:
         * Delete created accounts and token.
         */
        Map<TokenId, Long> alicesTokens =
                new AccountBalanceQuery().setAccountId(aliceAccountId).execute(client).tokens;

        new TokenWipeTransaction()
                .setTokenId(fungibleTokenId)
                .setAmount(alicesTokens.get(fungibleTokenId))
                .setAccountId(aliceAccountId)
                .freezeWith(client)
                .sign(OPERATOR_KEY)
                .execute(client)
                .getReceipt(client);

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

        new AccountDeleteTransaction()
                .setAccountId(charlieAccountId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(charilePrivateKey)
                .execute(client)
                .getReceipt(client);

        new TokenDeleteTransaction().setTokenId(fungibleTokenId).execute(client).getReceipt(client);

        client.close();

        System.out.println("Exempt Custom Fees Example Complete!");
    }
}
