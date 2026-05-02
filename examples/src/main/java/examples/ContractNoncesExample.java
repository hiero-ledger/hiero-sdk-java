// SPDX-License-Identifier: Apache-2.0
package examples;

import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.Objects;

/**
 * How to check contract nonces and validate HIP-729 behaviour.
 * <p>
 * HIP-729: Contract Accounts Nonce Externalization.
 * A deployed contract A should have a nonce value that reflects the number
 * of other contracts that were created since A’s creation.
 * <p>
 * To validate this behaviour, we deploy contract, which deploys another contract in its constructor.
 */
class ContractNoncesExample {

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
        System.out.println("Contract Nonces (HIP-729) Example Start!");

        /*
         * Step 0:
         * Create and configure the SDK Client.
         */
        Client client = ClientHelper.forName(HEDERA_NETWORK);
        // All generated transactions will be paid by this account and signed by this key.
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
        // Attach logger to the SDK Client.
        client.setLogger(new Logger(LogLevel.valueOf(SDK_LOG_LEVEL)));

        PublicKey operatorPublicKey = OPERATOR_KEY.getPublicKey();

        System.out.println("Creating new contract...");

        /*
         * Step 1:
         * Create a file with smart contract bytecode.
         */
        String contractBytecodeHex =
                ContractHelper.getBytecodeHex("contracts/parent_deploys_child/parent_deploys_child.json");
        TransactionResponse bytecodeFileCreateTxResponse = new FileCreateTransaction()
                .setKeys(operatorPublicKey)
                .setContents(contractBytecodeHex)
                .setMaxTransactionFee(Hbar.from(2))
                .execute(client);

        TransactionReceipt bytecodeFileCreateTxReceipt = bytecodeFileCreateTxResponse.getReceipt(client);
        FileId bytecodeFileId = bytecodeFileCreateTxReceipt.fileId;
        Objects.requireNonNull(bytecodeFileId);

        /*
         * Step 2:
         * Create a smart contract.
         */
        TransactionResponse contractCreateTxResponse = new ContractCreateTransaction()
                .setAdminKey(operatorPublicKey)
                .setGas(100_000)
                .setBytecodeFileId(bytecodeFileId)
                .setContractMemo("HIP-729 Contract")
                .execute(client);

        TransactionReceipt contractCreateTxReceipt = contractCreateTxResponse.getReceipt(client);
        ContractId contractId = contractCreateTxReceipt.contractId;
        Objects.requireNonNull(contractId);

        System.out.println("Created new contract with ID: " + contractId);

        /*
         * Step 3:
         * Get a record from a contract create transaction to check contracts nonces.
         * We expect to see `nonce=2` as we deploy a contract that creates another contract in its constructor.
         */
        List<ContractNonceInfo> contractNonces =
                contractCreateTxResponse.getRecord(client).contractFunctionResult.contractNonces;

        System.out.println("Contract nonces: " + contractNonces);

        /*
         * Clean up:
         * Delete created contract.
         */
        new ContractDeleteTransaction()
                .setContractId(contractId)
                .setTransferAccountId(contractCreateTxReceipt.transactionId.accountId)
                .setMaxTransactionFee(Hbar.from(1))
                .execute(client)
                .getReceipt(client);

        client.close();

        System.out.println("Contract Nonces (HIP-729) Example Complete!");
    }
}
