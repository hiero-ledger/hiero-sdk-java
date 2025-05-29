// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.ContractCallQuery;
import com.hedera.hashgraph.sdk.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.ContractDeleteTransaction;
import com.hedera.hashgraph.sdk.ContractExecuteTransaction;
import com.hedera.hashgraph.sdk.ContractFunctionParameters;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileDeleteTransaction;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.MirrorNodeContractCallQuery;
import com.hedera.hashgraph.sdk.MirrorNodeContractEstimateGasQuery;
import com.hedera.hashgraph.sdk.PrivateKey;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MirrorNodeContractQueryIntegrationTest {
    private static final String SMART_CONTRACT_BYTECODE =
            "6080604052348015600e575f80fd5b50335f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506104a38061005b5f395ff3fe608060405260043610610033575f3560e01c8063607a4427146100375780637065cb4814610053578063893d20e81461007b575b5f80fd5b610051600480360381019061004c919061033c565b6100a5565b005b34801561005e575f80fd5b50610079600480360381019061007491906103a2565b610215565b005b348015610086575f80fd5b5061008f6102b7565b60405161009c91906103dc565b60405180910390f35b3373ffffffffffffffffffffffffffffffffffffffff165f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16146100fb575f80fd5b805f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550600181908060018154018082558091505060019003905f5260205f20015f9091909190916101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505f8173ffffffffffffffffffffffffffffffffffffffff166108fc3490811502906040515f60405180830381858888f19350505050905080610211576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016102089061044f565b60405180910390fd5b5050565b805f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550600181908060018154018082558091505060019003905f5260205f20015f9091909190916101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f61030b826102e2565b9050919050565b61031b81610301565b8114610325575f80fd5b50565b5f8135905061033681610312565b92915050565b5f60208284031215610351576103506102de565b5b5f61035e84828501610328565b91505092915050565b5f610371826102e2565b9050919050565b61038181610367565b811461038b575f80fd5b50565b5f8135905061039c81610378565b92915050565b5f602082840312156103b7576103b66102de565b5b5f6103c48482850161038e565b91505092915050565b6103d681610367565b82525050565b5f6020820190506103ef5f8301846103cd565b92915050565b5f82825260208201905092915050565b7f5472616e73666572206661696c656400000000000000000000000000000000005f82015250565b5f610439600f836103f5565b915061044482610405565b602082019050919050565b5f6020820190508181035f8301526104668161042d565b905091905056fea26469706673582212206c46ddb2acdbcc4290e15be83eb90cd0b2ce5bd82b9bfe58a0709c5aec96305564736f6c634300081a0033";
    private static final String ADDRESS = "0x5B38Da6a701c568545dCfcB03FcB875f56beddC4";

    @Test
    @DisplayName("Can estimate and simulate transaction")
    void canSimulateTransaction() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(SMART_CONTRACT_BYTECODE)
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            response = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setGas(400000)
                    .setBytecodeFileId(fileId)
                    .execute(testEnv.client);

            var contractId = Objects.requireNonNull(response.getReceipt(testEnv.client).contractId);

            // Wait for mirror node to import data
            Thread.sleep(5000);

            var gas = new MirrorNodeContractEstimateGasQuery()
                    .setContractId(contractId)
                    .setFunction("getOwner")
                    .execute(testEnv.client);

            var result = new ContractCallQuery()
                    .setContractId(contractId)
                    .setGas(gas)
                    .setFunction("getOwner")
                    .setQueryPayment(new Hbar(1))
                    .execute(testEnv.client);

            var simulationResult = new MirrorNodeContractCallQuery()
                    .setContractId(contractId)
                    .setFunction("getOwner")
                    .execute(testEnv.client);

            assertThat(result.getAddress(0)).isEqualTo(simulationResult.substring(26));

            gas = new MirrorNodeContractEstimateGasQuery()
                    .setContractId(contractId)
                    .setFunction("addOwner", new ContractFunctionParameters().addAddress(ADDRESS))
                    .execute(testEnv.client);

            new ContractExecuteTransaction()
                    .setContractId(contractId)
                    .setGas(gas)
                    .setFunction("addOwner", new ContractFunctionParameters().addAddress(ADDRESS))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new MirrorNodeContractCallQuery()
                    .setContractId(contractId)
                    .setFunction("addOwner", new ContractFunctionParameters().addAddress(ADDRESS))
                    .execute(testEnv.client);

            new ContractDeleteTransaction()
                    .setTransferAccountId(testEnv.operatorId)
                    .setContractId(contractId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new FileDeleteTransaction()
                    .setFileId(fileId)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);
        }
    }

    @Test
    @DisplayName("Returns default values when contract is not deployed")
    void returnsDefaultValuesWhenContractIsNotDeployed() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var defaultGas = 22892;
            var contractId = new ContractId(0, 0, 1231456);

            var gas = new MirrorNodeContractEstimateGasQuery()
                    .setContractId(contractId)
                    .setFunction("getOwner")
                    .execute(testEnv.client);
            assertThat(gas).isEqualTo(defaultGas);

            var result = new MirrorNodeContractCallQuery()
                    .setContractId(contractId)
                    .setFunction("getOwner")
                    .execute(testEnv.client);

            assertThat(result).isEqualTo("0x");
        }
    }

    @Test
    @DisplayName("Fails when gas limit is low")
    void failsWhenGasLimitIsLow() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(SMART_CONTRACT_BYTECODE)
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            response = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setGas(400000)
                    .setBytecodeFileId(fileId)
                    .execute(testEnv.client);

            var contractId = Objects.requireNonNull(response.getReceipt(testEnv.client).contractId);

            // Wait for mirror node to import data
            Thread.sleep(5000);

            assertThatExceptionOfType(ExecutionException.class)
                    .isThrownBy(() -> {
                        new MirrorNodeContractEstimateGasQuery()
                                .setContractId(contractId)
                                .setGasLimit(100)
                                .setFunction(
                                        "addOwnerAndTransfer", new ContractFunctionParameters().addAddress(ADDRESS))
                                .execute(testEnv.client);
                    })
                    .withMessageContaining("Received non-200 response from Mirror Node");

            assertThatExceptionOfType(ExecutionException.class)
                    .isThrownBy(() -> {
                        new MirrorNodeContractCallQuery()
                                .setContractId(contractId)
                                .setGasLimit(100)
                                .setFunction(
                                        "addOwnerAndTransfer", new ContractFunctionParameters().addAddress(ADDRESS))
                                .execute(testEnv.client);
                    })
                    .withMessageContaining("Received non-200 response from Mirror Node");
        }
    }

    @Test
    @DisplayName("Fails when sender is not set")
    void failsWhenSenderIsNotSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(SMART_CONTRACT_BYTECODE)
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            response = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setGas(400000)
                    .setBytecodeFileId(fileId)
                    .execute(testEnv.client);

            var contractId = Objects.requireNonNull(response.getReceipt(testEnv.client).contractId);

            // Wait for mirror node to import data
            Thread.sleep(5000);

            assertThatExceptionOfType(ExecutionException.class)
                    .isThrownBy(() -> {
                        new MirrorNodeContractEstimateGasQuery()
                                .setContractId(contractId)
                                .setFunction(
                                        "addOwnerAndTransfer", new ContractFunctionParameters().addAddress(ADDRESS))
                                .execute(testEnv.client);
                    })
                    .withMessageContaining("Received non-200 response from Mirror Node");

            assertThatExceptionOfType(ExecutionException.class)
                    .isThrownBy(() -> {
                        new MirrorNodeContractCallQuery()
                                .setContractId(contractId)
                                .setFunction(
                                        "addOwnerAndTransfer", new ContractFunctionParameters().addAddress(ADDRESS))
                                .execute(testEnv.client);
                    })
                    .withMessageContaining("Received non-200 response from Mirror Node");
        }
    }

    @Test
    @DisplayName("Can simulate with sender set")
    void canSimulateWithSenderSet() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(SMART_CONTRACT_BYTECODE)
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            response = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setGas(400000)
                    .setBytecodeFileId(fileId)
                    .execute(testEnv.client);

            var contractId = Objects.requireNonNull(response.getReceipt(testEnv.client).contractId);

            var receiverAccountId = new AccountCreateTransaction()
                    .setKeyWithoutAlias(PrivateKey.generateED25519())
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .accountId;

            // Wait for mirror node to import data
            Thread.sleep(5000);

            var receiverEvmAddress = receiverAccountId.toSolidityAddress();

            var owner = new MirrorNodeContractCallQuery()
                    .setContractId(contractId)
                    .setFunction("getOwner")
                    .execute(testEnv.client)
                    .substring(26);

            var gas = new MirrorNodeContractEstimateGasQuery()
                    .setContractId(contractId)
                    .setGasLimit(1_000_000)
                    .setFunction("addOwnerAndTransfer", new ContractFunctionParameters().addAddress(receiverEvmAddress))
                    .setSenderEvmAddress(owner)
                    .setValue(123)
                    .execute(testEnv.client);

            new ContractExecuteTransaction()
                    .setContractId(contractId)
                    .setGas(gas)
                    .setPayableAmount(new Hbar(1))
                    .setFunction("addOwnerAndTransfer", new ContractFunctionParameters().addAddress(receiverEvmAddress))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            new MirrorNodeContractCallQuery()
                    .setContractId(contractId)
                    .setGasLimit(1_000_000)
                    .setFunction("addOwnerAndTransfer", new ContractFunctionParameters().addAddress(receiverEvmAddress))
                    .setSenderEvmAddress(owner)
                    .setValue(123)
                    .execute(testEnv.client);
        }
    }
}
