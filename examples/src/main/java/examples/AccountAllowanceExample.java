// SPDX-License-Identifier: Apache-2.0
package examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * How to approve and delete allowance.
 * <p>
 * Approve allowance is a transaction that allows a token owner to delegate a token spender to spend
 * a specified token amount on behalf of the token owner. This can be done for HBAR, non-fungible,
 * and fungible tokens. The owner grants the token allowance to the spender, who can then transfer tokens
 * from the owner's account to another recipient, paying for the transaction fees themselves.
 * <p>
 * On the other hand, delete allowance is a transaction that removes one or more non-fungible
 * approved allowances from an owner's account. This operation removes the allowances granted to
 * specific non-fungible token serial numbers. HBAR and fungible token allowances can be removed by
 * setting the amount to zero in CryptoApproveAllowance.
 */
class AccountAllowanceExample {

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
        System.out.println("Account Allowance Example Start!");

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
        System.out.println("Generating ED25519 key pairs...");
        PrivateKey alicePrivateKey = PrivateKey.generateED25519();
        PublicKey alicePublicKey = alicePrivateKey.getPublicKey();

        PrivateKey bobPrivateKey = PrivateKey.generateED25519();
        PublicKey bobPublicKey = bobPrivateKey.getPublicKey();

        PrivateKey charliePrivateKey = PrivateKey.generateED25519();
        PublicKey charliePublicKey = charliePrivateKey.getPublicKey();

        /*
         * Step 2:
         * Create accounts for this example.
         */
        System.out.println("Creating Alice's, Bob's and Charlie's accounts...");

        AccountId aliceId = new AccountCreateTransaction()
                .setKeyWithoutAlias(alicePublicKey)
                .setInitialBalance(Hbar.from(5))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(aliceId);

        AccountId bobId = new AccountCreateTransaction()
                .setKeyWithoutAlias(bobPublicKey)
                .setInitialBalance(Hbar.from(5))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(bobId);

        AccountId charlieId = new AccountCreateTransaction()
                .setKeyWithoutAlias(charliePublicKey)
                .setInitialBalance(Hbar.from(5))
                .execute(client)
                .getReceipt(client)
                .accountId;
        Objects.requireNonNull(charlieId);

        System.out.println("Alice's account ID: " + aliceId);
        System.out.println("Bob's account ID: " + bobId);
        System.out.println("Charlie's account ID: " + charlieId);

        System.out.println("Alice's balance: "
                + new AccountBalanceQuery().setAccountId(aliceId).execute(client).hbars);
        System.out.println("Bob's balance: "
                + new AccountBalanceQuery().setAccountId(bobId).execute(client).hbars);
        System.out.println("Charlie's balance: "
                + new AccountBalanceQuery().setAccountId(charlieId).execute(client).hbars);

        /*
         * Step 3:
         * Approve an allowance of 2 Hbar with owner Alice and spender Bob.
         */
        System.out.println("Approving an allowance of 2 Hbar with owner Alice and spender Bob...");

        new AccountAllowanceApproveTransaction()
                .approveHbarAllowance(aliceId, bobId, Hbar.from(2))
                .freezeWith(client)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("Alice's balance: "
                + new AccountBalanceQuery().setAccountId(aliceId).execute(client).hbars);
        System.out.println("Bob's balance: "
                + new AccountBalanceQuery().setAccountId(bobId).execute(client).hbars);
        System.out.println("Charlie's balance: "
                + new AccountBalanceQuery().setAccountId(charlieId).execute(client).hbars);

        /*
         * Step 4:
         * Demonstrate allowance -- transfer 1 Hbar from Alice to Charlie, but the transaction is signed only by Bob
         * (Bob is dipping into his allowance from Alice).
         */
        System.out.println("Transferring 1 Hbar from Alice to Charlie, "
                + "but the transaction is signed only by Bob (Bob is dipping into his allowance from Alice)...");

        new TransferTransaction()
                // "addApproved*Transfer()" means that the transfer has been approved by an allowance
                .addApprovedHbarTransfer(aliceId, Hbar.from(1).negated())
                .addHbarTransfer(charlieId, Hbar.from(1))
                // The allowance spender must be pay the fee for the transaction.
                // use setTransactionId() to set the account ID that will pay the fee for the transaction.
                .setTransactionId(TransactionId.generate(bobId))
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("Transfer succeeded. Bob should now have 1 Hbar left in his allowance.");

        System.out.println("Alice's balance: "
                + new AccountBalanceQuery().setAccountId(aliceId).execute(client).hbars);
        System.out.println("Bob's balance: "
                + new AccountBalanceQuery().setAccountId(bobId).execute(client).hbars);
        System.out.println("Charlie's balance: "
                + new AccountBalanceQuery().setAccountId(charlieId).execute(client).hbars);

        /*
         * Step 5:
         * Demonstrate the absence of an allowance -- attempt to transfer 2 Hbar from Alice to Charlie using Bob's allowance.
         *
         * This should fail, because there is only 1 Hbar left in Bob's allowance.
         */
        try {
            System.out.println("Attempting to transfer 2 Hbar from Alice to Charlie using Bob's allowance... "
                    + "(this should fail, because there is only 1 Hbar left in Bob's allowance).");

            new TransferTransaction()
                    .addApprovedHbarTransfer(aliceId, Hbar.from(2).negated())
                    .addHbarTransfer(charlieId, Hbar.from(2))
                    .setTransactionId(TransactionId.generate(bobId))
                    .freezeWith(client)
                    .sign(bobPrivateKey)
                    .execute(client)
                    .getReceipt(client);

            throw new Exception("This transfer shouldn't have succeeded!");
        } catch (Throwable error) {
            System.out.println("This transfer failed as expected: " + error.getMessage());
        }

        /*
         * Step 6:
         * Demonstrate update of an allowance -- adjust Bob's allowance to 3 Hbar.
         */
        System.out.println("Adjusting Bob's allowance to 3 Hbar...");

        new AccountAllowanceApproveTransaction()
                .approveHbarAllowance(aliceId, bobId, Hbar.from(3))
                .freezeWith(client)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        /*
         * Step 7:
         * Demonstrate allowance -- transfer 2 Hbar from Alice to Charlie using Bob's allowance again.
         */
        System.out.println("Attempting to transfer 2 Hbar from Alice to Charlie using Bob's allowance again... "
                + "(this time it should succeed).");

        new TransferTransaction()
                .addApprovedHbarTransfer(aliceId, Hbar.from(2).negated())
                .addHbarTransfer(charlieId, Hbar.from(2))
                .setTransactionId(TransactionId.generate(bobId))
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("Transfer succeeded.");

        System.out.println("Alice's balance: "
                + new AccountBalanceQuery().setAccountId(aliceId).execute(client).hbars);
        System.out.println("Bob's balance: "
                + new AccountBalanceQuery().setAccountId(bobId).execute(client).hbars);
        System.out.println("Charlie's balance: "
                + new AccountBalanceQuery().setAccountId(charlieId).execute(client).hbars);

        /*
         * Clean up:
         * Delete allowance and created accounts.
         */
        new AccountAllowanceApproveTransaction()
                .approveHbarAllowance(aliceId, bobId, Hbar.ZERO)
                .freezeWith(client)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(aliceId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(bobId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(bobPrivateKey)
                .execute(client)
                .getReceipt(client);

        new AccountDeleteTransaction()
                .setAccountId(charlieId)
                .setTransferAccountId(OPERATOR_ID)
                .freezeWith(client)
                .sign(charliePrivateKey)
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Account Allowance Example Complete!");
    }
}
