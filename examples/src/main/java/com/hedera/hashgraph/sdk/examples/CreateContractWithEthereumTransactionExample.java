// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ContractCallQuery;
import com.hedera.hashgraph.sdk.ContractFunctionResult;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.EthereumTransaction;
import com.hedera.hashgraph.sdk.EthereumTransactionDataEip2930;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.math.BigInteger;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;

/**
 * How to deploy a smart contract with an Ethereum transaction (EIP-2930).
 * <p>
 * An Ethereum transaction with an empty {@code to} field is a contract creation: the call data is the
 * contract's creation bytecode. The transaction is built and signed with an ECDSA (secp256k1) key, so
 * the contract is deployed by the EVM account that owns that key - not by the Hedera operator.
 * <p>
 */
class CreateContractWithEthereumTransactionExample {

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
        System.out.println("Create Contract With Ethereum Transaction Example Start!");

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
         * Create and fund the account that will send the Ethereum transaction.
         *
         * The Ethereum transaction is signed with an ECDSA (secp256k1) key, and the network resolves the
         * sender from that signature. Transferring Hbar to the key's alias account ID auto-creates the
         * account with the matching ECDSA key and EVM address.
         */
        PrivateKey senderPrivateKey = PrivateKey.generateECDSA();
        AccountId senderAliasAccountId = senderPrivateKey.toAccountId(0, 0);

        System.out.println("Funding the Ethereum transaction sender account...");
        new TransferTransaction()
                .addHbarTransfer(OPERATOR_ID, Hbar.from(1).negated())
                .addHbarTransfer(senderAliasAccountId, Hbar.from(1))
                .execute(client)
                .getReceipt(client);

        System.out.println(
                "Sender's EVM address: 0x" + senderPrivateKey.getPublicKey().toEvmAddress());

        /*
         * Step 2:
         * Read the contract's creation bytecode - this is the call data of the Ethereum transaction.
         */
        String contractBytecodeHex = ContractHelper.getBytecodeHex("contracts/hello_world/hello_world.json");
        // The HelloWorld constructor takes no arguments. Had it taken any, their ABI encoding
        // would have to be appended to the creation bytecode here.
        byte[] callData = Hex.decode(contractBytecodeHex);

        /*
         * Step 3:
         * Build and sign the EIP-2930 (type 1) Ethereum transaction data.
         */
        System.out.println("Building and signing the Ethereum transaction data...");
        EthereumTransactionDataEip2930 ethereumTransactionData = new EthereumTransactionDataEip2930()
                .setChainId(chainIdFor(HEDERA_NETWORK))
                // This is the sender account's first transaction.
                .setNonce(0)
                // A zero gas price means the sender authorizes nothing towards the fee, so the whole
                // fee is charged to the operator, up to the max gas allowance set in the next step.
                .setGasPrice(0)
                .setGasLimit(1_000_000)
                // An empty "to" makes this a contract creation instead of a contract call.
                .setTo(new byte[] {})
                .setValue(BigInteger.ZERO)
                .setCallData(callData);

        // Signing populates the r, s and recovery id fields of the transaction data.
        ethereumTransactionData.sign(senderPrivateKey);

        /*
         * Step 4:
         * Submit the Ethereum transaction to deploy the contract.
         */
        System.out.println("Deploying the contract with an Ethereum transaction...");
        TransactionRecord ethereumTxRecord = new EthereumTransaction()
                .setEthereumDataFromBody(ethereumTransactionData)
                // The operator is willing to pay up to this much of the transaction's gas cost.
                .setMaxGasAllowanceHbar(Hbar.from(5))
                .execute(client)
                .getRecord(client);

        ContractFunctionResult contractCreateResult = Objects.requireNonNull(ethereumTxRecord.contractFunctionResult);
        ContractId newContractId = contractCreateResult.contractId;
        System.out.println("Created new contract with ID: " + newContractId);
        if (contractCreateResult.evmAddress != null) {
            System.out.println(
                    "Created new contract with EVM address: 0x" + contractCreateResult.evmAddress.toEvmAddress());
        }
        System.out.println("Gas used: " + contractCreateResult.gasUsed);
        // The sender's nonce is incremented by the Ethereum transaction (HIP-844).
        System.out.println("Sender's nonce after the deployment: " + contractCreateResult.signerNonce);

        /*
         * Step 5:
         * Call a function of the deployed contract to confirm it works.
         */
        System.out.println("Calling contract function \"greet\"...");
        ContractFunctionResult contractCallResult = new ContractCallQuery()
                .setGas(300_000)
                .setContractId(newContractId)
                .setFunction("greet")
                .setMaxQueryPayment(Hbar.from(1))
                .execute(client);

        if (contractCallResult.errorMessage != null) {
            throw new Exception("Error calling contract function \"greet\": " + contractCallResult.errorMessage);
        }

        System.out.println("Contract call result (\"greet\" function returned): " + contractCallResult.getString(0));

        client.close();

        System.out.println("Create Contract With Ethereum Transaction Example Complete!");
    }

    /**
     * Returns the EVM chain ID of the given Hedera network. Signing with the wrong chain ID
     * fails with a {@code WRONG_CHAIN_ID} status.
     *
     * @param network the network name
     * @return the chain ID of the network
     */
    private static long chainIdFor(String network) {
        return switch (network) {
            case "mainnet" -> 295;
            case "testnet" -> 296;
            case "previewnet" -> 297;
            case ClientHelper.LOCAL_NETWORK_NAME -> 298;
            default -> throw new IllegalArgumentException("Unknown network: " + network);
        };
    }
}
