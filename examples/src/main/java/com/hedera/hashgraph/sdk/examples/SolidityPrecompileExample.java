// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/*
This example just instantiates the solidity contract
defined in resources/org/hiero/sdk/java/examples/contracts/precompile/PrecompileExample.sol, which has been
compiled into resources/org/hiero/sdk/java/examples/contracts/precompile/PrecompileExample.json.

You should go look at that PrecompileExample.sol file, because that's where the meat of this example is.

This example uses the ContractHelper class (defined in ./ContractHelper.java) to declutter things.

When this example spits out a raw response code,
you can look it up here: https://github.com/hashgraph/hedera-protobufs/blob/main/services/response_code.proto
 */

public class SolidityPrecompileExample {

    // see `.env.sample` in the repository root for how to specify these values
    // or set environment variables with the same names
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));
    // HEDERA_NETWORK defaults to testnet if not specified in dotenv
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    private SolidityPrecompileExample() {}

    public static void main(String[] args) throws Exception {
        Client client = ClientHelper.forName(HEDERA_NETWORK);

        // Defaults the operator account ID and key such that all generated transactions will be paid for
        // by this account and be signed by this key
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        // Create a new account to use as the operator for subsequent transactions
        PrivateKey newOperatorPrivateKey = PrivateKey.generateED25519();
        PublicKey newOperatorPublicKey = newOperatorPrivateKey.getPublicKey();
        AccountId newOperatorAccountId = Objects.requireNonNull(new AccountCreateTransaction()
                .setKeyWithoutAlias(newOperatorPublicKey)
                .setInitialBalance(Hbar.fromTinybars(1000000))
                .execute(client)
                .getReceipt(client)
                .accountId);

        // Set the new account as the operator
        client.setOperator(newOperatorAccountId, newOperatorPrivateKey);

        PrivateKey alicePrivateKey = PrivateKey.generateED25519();
        PublicKey alicePublicKey = alicePrivateKey.getPublicKey();
        AccountId aliceAccountId = Objects.requireNonNull(new AccountCreateTransaction()
                .setKeyWithoutAlias(alicePublicKey)
                .setInitialBalance(Hbar.fromTinybars(1000))
                .execute(client)
                .getReceipt(client)
                .accountId);

        // Instantiate ContractHelper
        ContractHelper contractHelper = new ContractHelper(
                "contracts/precompile/PrecompileExample.json",
                new ContractFunctionParameters()
                        .addAddress(OPERATOR_ID.toEvmAddress())
                        .addAddress(aliceAccountId.toEvmAddress()),
                client);

        // Update the signer to have contractId KeyList (this is by security requirement)
        new AccountUpdateTransaction()
                .setAccountId(OPERATOR_ID)
                .setKey(KeyList.of(OPERATOR_KEY.getPublicKey(), contractHelper.contractId)
                        .setThreshold(1))
                .execute(client)
                .getReceipt(client);

        // Update the Alice account to have contractId KeyList (this is by security requirement)
        new AccountUpdateTransaction()
                .setAccountId(aliceAccountId)
                .setKey(KeyList.of(alicePublicKey, contractHelper.contractId).setThreshold(1))
                .freezeWith(client)
                .sign(alicePrivateKey)
                .execute(client)
                .getReceipt(client);

        Consumer<String> additionalLogic = tokenAddress -> {
            try {
                var tokenUpdateTransactionReceipt = new TokenUpdateTransaction()
                        .setTokenId(TokenId.fromEvmAddress(0,0,tokenAddress))
                        .setAdminKey(KeyList.of(OPERATOR_KEY.getPublicKey(), contractHelper.contractId)
                                .setThreshold(1))
                        .setSupplyKey(KeyList.of(OPERATOR_KEY.getPublicKey(), contractHelper.contractId)
                                .setThreshold(1))
                        .freezeWith(client)
                        .sign(alicePrivateKey)
                        .execute(client)
                        .getReceipt(client);

                System.out.println("Status of Token Update Transaction: " + tokenUpdateTransactionReceipt.status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // Configure steps in ContractHelper
        contractHelper
                .setResultValidatorForStep(0, contractFunctionResult -> {
                    System.out.println(
                            "getPseudoRandomSeed() returned " + Arrays.toString(contractFunctionResult.getBytes32(0)));
                    return true;
                })
                .setPayableAmountForStep(1, Hbar.from(20))
                // step 3 associates Alice with the token, which requires Alice's signature
                .addSignerForStep(3, alicePrivateKey)
                .addSignerForStep(5, alicePrivateKey)
                .setParameterSupplierForStep(11, () -> {
                    return new ContractFunctionParameters()
                            // when contracts work with a public key, they handle the raw bytes of the public key
                            .addBytes(alicePublicKey.toBytesRaw());
                })
                .setPayableAmountForStep(11, Hbar.from(40))
                // Because we're setting the adminKey for the created NFT token to Alice's key,
                // Alice must sign the ContractExecuteTransaction.
                .addSignerForStep(11, alicePrivateKey)
                .setStepLogic(11, additionalLogic)
                // and Alice must sign for minting because her key is the supply key.
                .addSignerForStep(12, alicePrivateKey)
                .setParameterSupplierForStep(12, () -> {
                    return new ContractFunctionParameters()
                            // add three metadatas
                            .addBytesArray(new byte[][] {new byte[] {0x01b}, new byte[] {0x02b}, new byte[] {0x03b}});
                }) // and alice must sign to become associated with the token.
                .addSignerForStep(13, alicePrivateKey)
                // Alice must sign to burn the token because her key is the supply key
                .addSignerForStep(16, alicePrivateKey);

        // step 0 tests pseudo random number generator (PRNG)
        // step 1 creates a fungible token
        // step 2 mints it
        // step 3 associates Alice with it
        // step 4 transfers it to Alice.
        // step 5 approves an allowance of the fungible token with operator as the owner and Alice as the spender [NOT
        // WORKING]
        // steps 6 - 10 test misc functions on the fungible token (see PrecompileExample.sol for details).
        // step 11 creates an NFT token with a custom fee, and with the admin and supply set to Alice's key
        // step 12 mints some NFTs
        // step 13 associates Alice with the NFT token
        // step 14 transfers some NFTs to Alice
        // step 15 approves an NFT allowance with operator as the owner and Alice as the spender [NOT WORKING]
        // step 16 burn some NFTs

        contractHelper.executeSteps(/* from step */ 0, /* to step */ 16, client);

        System.out.println("All steps completed with valid results.");
    }
}
