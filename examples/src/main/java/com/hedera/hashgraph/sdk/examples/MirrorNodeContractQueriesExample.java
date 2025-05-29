// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.examples;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ContractCallQuery;
import com.hedera.hashgraph.sdk.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.MirrorNodeContractCallQuery;
import com.hedera.hashgraph.sdk.MirrorNodeContractEstimateGasQuery;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.logger.LogLevel;
import com.hedera.hashgraph.sdk.logger.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bouncycastle.util.encoders.Hex;

public class MirrorNodeContractQueriesExample {
    /*
     * See .env.sample in the examples folder root for how to specify values below
     * or set environment variables with the same names.
     */

    /**
     * Operator's account ID. Used to sign and pay for operations on Hedera.
     */
    private static final AccountId OPERATOR_ID =
            AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));

    /**
     * Operator's private key.
     */
    private static final PrivateKey OPERATOR_KEY =
            PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    /**
     * HEDERA_NETWORK defaults to testnet if not specified in dotenv file. Network can be: localhost, testnet,
     * previewnet or mainnet.
     */
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");

    /**
     * SDK_LOG_LEVEL defaults to SILENT if not specified in dotenv file. Log levels can be: TRACE, DEBUG, INFO, WARN,
     * ERROR, SILENT.
     * <p>
     * Important pre-requisite: set simple logger log level to same level as the SDK_LOG_LEVEL, for example via VM
     * options: -Dorg.slf4j.simpleLogger.log.org.hiero=trace
     */
    private static final String SDK_LOG_LEVEL = Dotenv.load().get("SDK_LOG_LEVEL", "SILENT");

    private static final String SMART_CONTRACT_BYTECODE =
            "60806040526040518060400160405280600581526020017f68656c6c6f0000000000000000000000000000000000000000000000000000008152505f90816100479190610293565b50348015610053575f80fd5b50610362565b5f81519050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52604160045260245ffd5b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f60028204905060018216806100d457607f821691505b6020821081036100e7576100e6610090565b5b50919050565b5f819050815f5260205f209050919050565b5f6020601f8301049050919050565b5f82821b905092915050565b5f600883026101497fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8261010e565b610153868361010e565b95508019841693508086168417925050509392505050565b5f819050919050565b5f819050919050565b5f61019761019261018d8461016b565b610174565b61016b565b9050919050565b5f819050919050565b6101b08361017d565b6101c46101bc8261019e565b84845461011a565b825550505050565b5f90565b6101d86101cc565b6101e38184846101a7565b505050565b5b81811015610206576101fb5f826101d0565b6001810190506101e9565b5050565b601f82111561024b5761021c816100ed565b610225846100ff565b81016020851015610234578190505b610248610240856100ff565b8301826101e8565b50505b505050565b5f82821c905092915050565b5f61026b5f1984600802610250565b1980831691505092915050565b5f610283838361025c565b9150826002028217905092915050565b61029c82610059565b67ffffffffffffffff8111156102b5576102b4610063565b5b6102bf82546100bd565b6102ca82828561020a565b5f60209050601f8311600181146102fb575f84156102e9578287015190505b6102f38582610278565b86555061035a565b601f198416610309866100ed565b5f5b828110156103305784890151825560018201915060208501945060208101905061030b565b8683101561034d5784890151610349601f89168261025c565b8355505b6001600288020188555050505b505050505050565b6102178061036f5f395ff3fe608060405234801561000f575f80fd5b5060043610610029575f3560e01c8063ce6d41de1461002d575b5f80fd5b61003561004b565b6040516100429190610164565b60405180910390f35b60605f8054610059906101b1565b80601f0160208091040260200160405190810160405280929190818152602001828054610085906101b1565b80156100d05780601f106100a7576101008083540402835291602001916100d0565b820191905f5260205f20905b8154815290600101906020018083116100b357829003601f168201915b5050505050905090565b5f81519050919050565b5f82825260208201905092915050565b5f5b838110156101115780820151818401526020810190506100f6565b5f8484015250505050565b5f601f19601f8301169050919050565b5f610136826100da565b61014081856100e4565b93506101508185602086016100f4565b6101598161011c565b840191505092915050565b5f6020820190508181035f83015261017c818461012c565b905092915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f60028204905060018216806101c857607f821691505b6020821081036101db576101da610184565b5b5091905056fea26469706673582212202a86c27939bfab6d4a2c61ebbf096d8424e17e22dfdd42320f6e2654863581e964736f6c634300081a0033";

    public static void main(String[] args) throws Exception {
        System.out.println("Mirror Node contract queries Example Start!");
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
         * Create the contract
         */
        var response = new ContractCreateTransaction()
                .setGas(300_000)
                .setBytecode(Hex.decode(SMART_CONTRACT_BYTECODE))
                .setContractMemo("Simple contract with string field")
                .execute(client);

        var contractId = Objects.requireNonNull(response.getReceipt(client).contractId);
        System.out.println("Created new contract with ID: " + contractId);

        /*
         * Step 3:
         * Wait for mirror node to import data
         */
        Thread.sleep(5000);

        /*
         * Step 4:
         * Estimate the gas needed
         */
        var gas = new MirrorNodeContractEstimateGasQuery()
                .setContractId(contractId)
                .setSender(client.getOperatorAccountId())
                .setGasLimit(30_000)
                .setGasPrice(1234)
                .setFunction("getMessage")
                .execute(client);

        System.out.println("Gas needed for this query: " + gas);

        /*
         * Step 5:
         * Do the query against the consensus node using the estimated gas
         */
        var callQuery = new ContractCallQuery()
                .setContractId(contractId)
                .setGas(gas)
                .setFunction("getMessage")
                .setQueryPayment(new Hbar(1));

        var result = callQuery.execute(client);

        /*
         * Step 6:
         * Simulate the transaction for free, using the mirror node
         */
        var simulationResult = new MirrorNodeContractCallQuery()
                .setContractId(contractId)
                .setSender(client.getOperatorAccountId())
                .setGasLimit(30_000)
                .setBlockNumber(10000)
                .setGasPrice(1234)
                .setFunction("getMessage")
                .execute(client);

        // Decode the result since it's coming in ABI Hex format from the Mirror Node
        var decodedResult = decodeABIHexString(simulationResult);
        System.out.println("Simulation result: " + decodedResult);
        System.out.println("Contract call result: " + result.getString(0));
    }

    /**
     * Decodes a hex-encoded ABI (Application Binary Interface) string into a UTF-8 string.
     * <p>
     * The function assumes the input follows the ABI encoding standard for dynamic data. Specifically, it parses the
     * length of the dynamic data and extracts the corresponding substring.
     * <p>
     * The structure of the input hex string is as follows: - The first 64 characters represent metadata, such as
     * offsets and other header information. - Characters from index 64 to 128 encode the length of the dynamic data in
     * bytes. - Characters from index 128 onward represent the actual dynamic data.
     * <p>
     * This method removes the `0x` prefix if present, parses the length, and decodes the dynamic data into UTF-8.
     *
     * @param hex the hex string to decode, which follows the ABI encoding standard
     * @return the decoded UTF-8 string
     */
    private static String decodeABIHexString(String hex) {
        // Trim 0x at the beginning
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }

        // Extract the length of the data by parsing the substring from position 64 to 128 as a hexadecimal integer
        // This section represents the length of the dynamic data, specifically the number of bytes in the string or
        // array
        int length = Integer.parseInt(hex.substring(64, 128), 16);

        // Using the extracted length, the code calculates the substring containing the actual data starting from
        // position 128.
        String hexStringData = hex.substring(128, 128 + length * 2);

        byte[] bytes = new byte[length];
        // Iterate through the extracted hex data, two characters at a time, converting each pair to a byte and storing
        // it in a byte array.
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) Integer.parseInt(hexStringData.substring(i * 2, i * 2 + 2), 16);
        }

        // Convert to UTF 8
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
