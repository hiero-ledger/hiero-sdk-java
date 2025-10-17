// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * How to add hooks to an existing account.
 * <p>
 * This example demonstrates how to add multiple hooks to an existing account using
 * AccountUpdateTransaction. It shows different types of hook configurations including
 * basic lambda hooks and hooks with storage updates.
 */
class AddHooksToAccountExample {

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

    // this bytecode comes from a sample contract that can be found at /contracts/HieroHookContract.sol
    private static final String SMART_CONTRACT_BYTECODE =
            "6080604052348015600e575f5ffd5b506107d18061001c5f395ff3fe608060405260043610610033575f3560e01c8063124d8b301461003757806394112e2f14610067578063bd0dd0b614610097575b5f5ffd5b610051600480360381019061004c91906106f2565b6100c7565b60405161005e9190610782565b60405180910390f35b610081600480360381019061007c91906106f2565b6100d2565b60405161008e9190610782565b60405180910390f35b6100b160048036038101906100ac91906106f2565b6100dd565b6040516100be9190610782565b60405180910390f35b5f6001905092915050565b5f6001905092915050565b5f6001905092915050565b5f604051905090565b5f5ffd5b5f5ffd5b5f5ffd5b5f60a08284031215610112576101116100f9565b5b81905092915050565b5f5ffd5b5f601f19601f8301169050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52604160045260245ffd5b6101658261011f565b810181811067ffffffffffffffff821117156101845761018361012f565b5b80604052505050565b5f6101966100e8565b90506101a2828261015c565b919050565b5f5ffd5b5f5ffd5b5f67ffffffffffffffff8211156101c9576101c861012f565b5b602082029050602081019050919050565b5f5ffd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610207826101de565b9050919050565b610217816101fd565b8114610221575f5ffd5b50565b5f813590506102328161020e565b92915050565b5f8160070b9050919050565b61024d81610238565b8114610257575f5ffd5b50565b5f8135905061026881610244565b92915050565b5f604082840312156102835761028261011b565b5b61028d604061018d565b90505f61029c84828501610224565b5f8301525060206102af8482850161025a565b60208301525092915050565b5f6102cd6102c8846101af565b61018d565b905080838252602082019050604084028301858111156102f0576102ef6101da565b5b835b818110156103195780610305888261026e565b8452602084019350506040810190506102f2565b5050509392505050565b5f82601f830112610337576103366101ab565b5b81356103478482602086016102bb565b91505092915050565b5f67ffffffffffffffff82111561036a5761036961012f565b5b602082029050602081019050919050565b5f67ffffffffffffffff8211156103955761039461012f565b5b602082029050602081019050919050565b5f606082840312156103bb576103ba61011b565b5b6103c5606061018d565b90505f6103d484828501610224565b5f8301525060206103e784828501610224565b60208301525060406103fb8482850161025a565b60408301525092915050565b5f6104196104148461037b565b61018d565b9050808382526020820190506060840283018581111561043c5761043b6101da565b5b835b81811015610465578061045188826103a6565b84526020840193505060608101905061043e565b5050509392505050565b5f82601f830112610483576104826101ab565b5b8135610493848260208601610407565b91505092915050565b5f606082840312156104b1576104b061011b565b5b6104bb606061018d565b90505f6104ca84828501610224565b5f83015250602082013567ffffffffffffffff8111156104ed576104ec6101a7565b5b6104f984828501610323565b602083015250604082013567ffffffffffffffff81111561051d5761051c6101a7565b5b6105298482850161046f565b60408301525092915050565b5f61054761054284610350565b61018d565b9050808382526020820190506020840283018581111561056a576105696101da565b5b835b818110156105b157803567ffffffffffffffff81111561058f5761058e6101ab565b5b80860161059c898261049c565b8552602085019450505060208101905061056c565b5050509392505050565b5f82601f8301126105cf576105ce6101ab565b5b81356105df848260208601610535565b91505092915050565b5f604082840312156105fd576105fc61011b565b5b610607604061018d565b90505f82013567ffffffffffffffff811115610626576106256101a7565b5b61063284828501610323565b5f83015250602082013567ffffffffffffffff811115610655576106546101a7565b5b610661848285016105bb565b60208301525092915050565b5f604082840312156106825761068161011b565b5b61068c604061018d565b90505f82013567ffffffffffffffff8111156106ab576106aa6101a7565b5b6106b7848285016105e8565b5f83015250602082013567ffffffffffffffff8111156106da576106d96101a7565b5b6106e6848285016105e8565b60208301525092915050565b5f5f60408385031215610708576107076100f1565b5b5f83013567ffffffffffffffff811115610725576107246100f5565b5b610731858286016100fd565b925050602083013567ffffffffffffffff811115610752576107516100f5565b5b61075e8582860161066d565b9150509250929050565b5f8115159050919050565b61077c81610768565b82525050565b5f6020820190506107955f830184610773565b9291505056fea26469706673582212207dfe7723f6d6869419b1cb0619758b439da0cf4ffd9520997c40a3946299d4dc64736f6c634300081e0033";

    public static void main(String[] args) throws Exception {
        System.out.println("Add Hooks to Account Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        ContractId contractId = createContractId(client);
        System.out.println("hookContractId: " + contractId);

        // Declare variables outside try block for cleanup access
        PrivateKey accountKey;
        AccountId accountId;

        /*
         * Step 1:
         * Create an account to add hooks to.
         */
        System.out.println("Creating account to add hooks to...");
        accountKey = PrivateKey.generateED25519();
        PublicKey accountPublicKey = accountKey.getPublicKey();

        TransactionResponse accountCreateResponse = new AccountCreateTransaction()
                .setKeyWithoutAlias(accountPublicKey)
                .setInitialBalance(Hbar.from(1))
                .execute(client);

        TransactionReceipt accountCreateReceipt = accountCreateResponse.getReceipt(client);
        accountId = accountCreateReceipt.accountId;
        Objects.requireNonNull(accountId);
        System.out.println("Created account with ID: " + accountId);

        /*
         * Step 2:
         * Parse the hook contract ID and validate it exists.
         */
        System.out.println("Using hook contract ID: " + contractId);

        // Verify the contract exists by querying its info
        try {
            ContractInfo contractInfo =
                    new ContractInfoQuery().setContractId(contractId).execute(client);
            System.out.println("Hook contract verified: " + contractInfo.contractId);
        } catch (Exception e) {
            System.err.println("Warning: Could not verify hook contract exists: " + e.getMessage());
        }

        /*
         * Step 3:
         * Create hook configurations with different storage setups.
         */
        // Admin key for the hooks (using operator key for simplicity)
        Key adminKey = OPERATOR_KEY.getPublicKey();

        // Hook 1: Basic lambda hook with no storage updates
        LambdaEvmHook basicHook = new LambdaEvmHook(contractId);
        HookCreationDetails hook1 =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, basicHook, adminKey);

        // Hook 2: Lambda hook with storage slot updates
        byte[] storageKey = new byte[32];
        Arrays.fill(storageKey, (byte) 0x01); // Fill with 0x01

        byte[] storageValue = new byte[32];
        Arrays.fill(storageValue, (byte) 0x64); // Fill with 0x64 (decimal 100)

        List<LambdaStorageUpdate> storageUpdates =
                List.of(new LambdaStorageUpdate.LambdaStorageSlot(storageKey, storageValue));

        LambdaEvmHook storageHook = new LambdaEvmHook(contractId, storageUpdates);
        HookCreationDetails hook2 =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, storageHook, adminKey);

        // Hook 3: Another basic hook to demonstrate multiple hooks
        LambdaEvmHook basicHook2 = new LambdaEvmHook(contractId);
        HookCreationDetails hook3 =
                new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 3L, basicHook2, adminKey);

        /*
         * Step 4:
         * Add hooks to the account using AccountUpdateTransaction.
         */
        try {
            TransactionResponse accountUpdateResponse = new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .addHookToCreate(hook1)
                    .addHookToCreate(hook2)
                    .addHookToCreate(hook3)
                    .freezeWith(client)
                    .sign(accountKey)
                    .execute(client);

            TransactionReceipt accountUpdateReceipt = accountUpdateResponse.getReceipt(client);

            if (accountUpdateReceipt.status == Status.SUCCESS) {
                System.out.println("Successfully added hooks to account!");
            } else {
                System.err.println("Failed to add hooks to account. Status: " + accountUpdateReceipt.status);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute hook transaction: " + e.getMessage());
        }

        /*
         * Step 5:
         * Verify the hooks were added by querying account info.
         */
        AccountInfo accountInfo = new AccountInfoQuery().setAccountId(accountId).execute(client);

        System.out.println("Account ID: " + accountInfo.accountId);
        System.out.println("Account balance: " + accountInfo.balance);

        /*
         * Step 6:
         * Demonstrate hook deletion (optional cleanup).
         */
        try {
            TransactionResponse deleteHookResponse = new AccountUpdateTransaction()
                    .setAccountId(accountId)
                    .addHookToDelete(3L) // Delete hook with ID 3
                    .freezeWith(client)
                    .sign(accountKey)
                    .execute(client);

            TransactionReceipt deleteHookReceipt = deleteHookResponse.getReceipt(client);

            if (deleteHookReceipt.status == Status.SUCCESS) {
                System.out.println("Successfully deleted hook with ID: 3");
            } else {
                System.err.println("Failed to delete hook. Status: " + deleteHookReceipt.status);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute hook deletion: " + e.getMessage());
        }

        client.close();
        System.out.println("Add Hooks to Account Example Complete!");
    }

    private static FileId createBytecodeFile(Client client) throws Exception {
        var response = new FileCreateTransaction()
                .setKeys(OPERATOR_KEY)
                .setContents(SMART_CONTRACT_BYTECODE)
                .execute(client);
        return Objects.requireNonNull(response.getReceipt(client).fileId);
    }

    private static ContractId createContractId(Client client) throws Exception {
        var fileId = createBytecodeFile(client);

        var response = new ContractCreateTransaction()
                .setAdminKey(OPERATOR_KEY)
                .setGas(1_000_000)
                .setBytecodeFileId(fileId)
                .execute(client);

        var receipt = response.getReceipt(client);
        return receipt.contractId;
    }
}
