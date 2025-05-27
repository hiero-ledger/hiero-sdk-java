// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.ContractDeleteTransaction;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.ContractNonceInfo;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileDeleteTransaction;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ContractNonceInfoIntegrationTest {
    private static final String SMART_CONTRACT_BYTECODE =
            "6080604052348015600f57600080fd5b50604051601a90603b565b604051809103906000f0801580156035573d6000803e3d6000fd5b50506047565b605c8061009483390190565b603f806100556000396000f3fe6080604052600080fdfea2646970667358221220a20122cbad3457fedcc0600363d6e895f17048f5caa4afdab9e655123737567d64736f6c634300081200336080604052348015600f57600080fd5b50603f80601d6000396000f3fe6080604052600080fdfea264697066735822122053dfd8835e3dc6fedfb8b4806460b9b7163f8a7248bac510c6d6808d9da9d6d364736f6c63430008120033";

    @Test
    @DisplayName("Contract Create of A nonce, which deploys contract B in CONSTRUCTOR")
    void canIncrementNonceThroughContractConstructor() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(SMART_CONTRACT_BYTECODE)
                    .execute(testEnv.client);

            var fileId = Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);

            response = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setGas(300000)
                    .setBytecodeFileId(fileId)
                    .setContractMemo("[e2e::ContractADeploysContractBInConstructor]")
                    .execute(testEnv.client);

            var contractFunctionResult = response.getRecord(testEnv.client).contractFunctionResult;

            ContractId contractA = contractFunctionResult.contractId;
            ContractId contractB = contractFunctionResult.contractNonces.stream()
                    .filter(contractNonce -> !contractNonce.contractId.equals(contractA))
                    .findFirst()
                    .get()
                    .contractId;

            ContractNonceInfo contractANonceInfo = contractFunctionResult.contractNonces.stream()
                    .filter(contractNonce -> contractNonce.contractId.equals(contractA))
                    .findFirst()
                    .get();
            ContractNonceInfo contractBNonceInfo = contractFunctionResult.contractNonces.stream()
                    .filter(contractNonce -> contractNonce.contractId.equals(contractB))
                    .findFirst()
                    .get();

            // A.nonce = 2
            assertThat(contractANonceInfo.nonce).isEqualTo(2);
            // B.nonce = 1
            assertThat(contractBNonceInfo.nonce).isEqualTo(1);
            // validate HIP-844 case - signer nonce should be set only for Ethereum transactions
            assertThat(contractFunctionResult.signerNonce).isEqualTo(0);

            var contractId = Objects.requireNonNull(response.getReceipt(testEnv.client).contractId);

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
}
