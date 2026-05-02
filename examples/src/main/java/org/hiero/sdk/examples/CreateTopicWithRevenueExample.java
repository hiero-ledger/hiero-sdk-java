// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.examples;

import org.hiero.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Demonstrates the creation of a revenue-generating topic with HBAR and token-based custom fees,
 * account creation, and fee exemptions using the Hedera SDK.
 */
public class CreateTopicWithRevenueExample {

    /**
     * Operator's account ID used to sign and pay for transactions on Hedera.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key for signing transactions.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * Hedera network (localhost, testnet, previewnet, or mainnet).
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Hedera Custom Fees Example...");

        // Step 0: Initialize client and set the operator.

        try (Client client = ClientHelper.forName(HEDERA_NETWORK).setOperator(OPERATOR_ID, OPERATOR_KEY)) {
            /**
             * Step 1: Create an account for Alice with an initial balance of 5 HBAR.
             */
            System.out.println("Creating Alice's account...");
            PrivateKey aliceKey = PrivateKey.generateECDSA();

            var aliceAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(aliceKey)
                    .setMaxAutomaticTokenAssociations(1)
                    .setInitialBalance(Hbar.from(2))
                    .execute(client)
                    .getReceipt(client)
                    .accountId;
            Objects.requireNonNull(aliceAccountId);

            System.out.println("Alice's Account ID: " + aliceAccountId);

            /**
             * Step 2: Create a topic with an HBAR custom fee.
             */
            System.out.println("Creating a topic with HBAR custom fee...");

            var customFee =
                    new CustomFixedFee().setAmount(new Hbar(1).toTinybars()).setFeeCollectorAccountId(OPERATOR_ID);

            var topicId = new TopicCreateTransaction()
                    .setAdminKey(OPERATOR_KEY)
                    .setFeeScheduleKey(OPERATOR_KEY)
                    .setCustomFees(Collections.singletonList(customFee))
                    .execute(client)
                    .getReceipt(client)
                    .topicId;

            System.out.println("Created Topic ID: " + topicId);

            /**
             * Step 3: Submit a message to the topic, paid by Alice, with a custom fee limit.
             */
            System.out.println("Submitting a message as Alice to the topic...");

            var aliceBalanceBefore =
                    new AccountBalanceQuery().setAccountId(aliceAccountId).execute(client).hbars;

            var feeCollectorBalanceBefore =
                    new AccountBalanceQuery().setAccountId(OPERATOR_ID).execute(client).hbars;

            var customFeeLimit = new CustomFeeLimit()
                    .setPayerId(aliceAccountId)
                    .setCustomFees(
                            List.of(new CustomFixedFee().setAmount(Hbar.from(2).toTinybars())));

            client.setOperator(aliceAccountId, aliceKey);

            new TopicMessageSubmitTransaction()
                    .setCustomFeeLimits(List.of(customFeeLimit))
                    .setTopicId(topicId)
                    .setMessage("Hello, Hedera™ hashgraph!")
                    .execute(client)
                    .getReceipt(client);

            System.out.println("Message submitted successfully.");

            /**
             * Step 4: Verify Alice's and fee collector's balance after the transaction.
             */
            client.setOperator(OPERATOR_ID, OPERATOR_KEY);

            var aliceBalanceAfter =
                    new AccountBalanceQuery().setAccountId(aliceAccountId).execute(client).hbars;

            var feeCollectorBalanceAfter =
                    new AccountBalanceQuery().setAccountId(OPERATOR_ID).execute(client).hbars;

            System.out.println("Alice's balance before: " + aliceBalanceBefore + ", after: " + aliceBalanceAfter);
            System.out.println("Fee collector's balance before: " + feeCollectorBalanceBefore + ", after: "
                    + feeCollectorBalanceAfter);

            /**
             * Step 5: Create a fungible token and transfer it to Alice.
             */
            System.out.println("Creating a token and transferring it to Alice...");

            var tokenId = new TokenCreateTransaction()
                    .setTokenName("revenue-generating token")
                    .setTokenSymbol("RGT")
                    .setTreasuryAccountId(client.getOperatorAccountId())
                    .setDecimals(8)
                    .setInitialSupply(100)
                    .execute(client)
                    .getReceipt(client)
                    .tokenId;

            new TransferTransaction()
                    .addTokenTransfer(tokenId, client.getOperatorAccountId(), -1)
                    .addTokenTransfer(tokenId, aliceAccountId, 1)
                    .execute(client)
                    .getReceipt(client);

            /**
             * Step 6: Update the topic to charge a token-based fee.
             */
            System.out.println("Updating the topic to charge a token-based fee...");

            var customFeeToken = new CustomFixedFee()
                    .setAmount(1)
                    .setFeeCollectorAccountId(OPERATOR_ID)
                    .setDenominatingTokenId(tokenId);

            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setCustomFees(List.of(customFeeToken))
                    .execute(client)
                    .getReceipt(client);

            /**
             * Step 7: Submit another message without specifying a custom fee limit.
             */
            System.out.println("Submitting another message without custom fee limit...");

            client.setOperator(aliceAccountId, aliceKey);

            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Another message!")
                    .execute(client)
                    .getReceipt(client);

            client.setOperator(OPERATOR_ID, OPERATOR_KEY);

            /**
             * Step 8: Verify Alice's token balance and the fee collector's token balance after the transaction.
             */
            var aliceTokenBalanceAfter = new AccountBalanceQuery()
                    .setAccountId(aliceAccountId)
                    .execute(client)
                    .tokens
                    .get(tokenId);

            var feeCollectorTokenBalanceAfter = new AccountBalanceQuery()
                    .setAccountId(OPERATOR_ID)
                    .execute(client)
                    .tokens
                    .get(tokenId);

            System.out.println("Alice's token balance: " + aliceTokenBalanceAfter);
            System.out.println("Fee collector's token balance: " + feeCollectorTokenBalanceAfter);

            /**
             * Step 9: Create Bob's account with 10 HBAR.
             */
            System.out.println("Creating Bob's account...");
            Hbar initialBalance = new Hbar(10);
            PrivateKey bobKey = PrivateKey.generateECDSA();
            var bobAccountId = new AccountCreateTransaction()
                    .setKey(bobKey)
                    .setInitialBalance(initialBalance)
                    .setMaxAutomaticTokenAssociations(100)
                    .execute(client)
                    .getReceipt(client)
                    .accountId;

            System.out.println("Bob's Account ID: " + bobAccountId);

            /**
             * Step 10: Exempt Bob from paying topic fees.
             */
            System.out.println("Updating topic to add Bob as a fee-exempt key...");

            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .addFeeExemptKey(bobKey)
                    .execute(client)
                    .getReceipt(client);

            /**
             * Step 11: Bob submits a message to the topic without paying the fee.
             */
            client.setOperator(bobAccountId, bobKey);

            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("Hello from Bob!")
                    .execute(client)
                    .getReceipt(client);

            System.out.println("Message submitted successfully by Bob without being charged.");

            /**
             * Step 12: Verify Bob's balance should be almost the same as the initial
             */
            var bobBalanceAfter =
                    new AccountBalanceQuery().setAccountId(bobAccountId).execute(client).hbars;
            System.out.println("Bob's initial balance: " + initialBalance + ", after: " + bobBalanceAfter);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Example execution completed.");
        }
    }
}

