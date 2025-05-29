// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.hashgraph.sdk.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.ContractFunctionParameters;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.ContractInfoQuery;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContractIdPopulationIntegrationTest {
    @Test
    @DisplayName("Can populate ContractId num from mirror node (using sync method)")
    void canPopulateContractIdNumSync() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var testContractByteCode =
                    "608060405234801561001057600080fd5b50336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506101cb806100606000396000f3fe608060405260043610610046576000357c03000000000000000000000000000000000000000000000000000000009004806341c0e1b51461004b578063cfae321714610062575b600080fd5b34801561005757600080fd5b506100606100f2565b005b34801561006e57600080fd5b50610077610162565b6040518080602001828103825283818151815260200191508051906020019080838360005b838110156100b757808201518184015260208101905061009c565b50505050905090810190601f1680156100e45780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161415610160573373ffffffffffffffffffffffffffffffffffffffff16ff5b565b60606040805190810160405280600d81526020017f48656c6c6f2c20776f726c64230000000000000000000000000000000000000081525090509056fea165627a7a72305820ae96fb3af7cde9c0abfe365272441894ab717f816f07f41f07b1cbede54e256e0029";

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(testContractByteCode)
                    .execute(testEnv.client);

            var receipt = response.setValidateStatus(true).getReceipt(testEnv.client);
            var fileId = Objects.requireNonNull(receipt.fileId);

            response = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setGas(300000)
                    .setConstructorParameters(new ContractFunctionParameters().addString("Hello from Hedera."))
                    .setBytecodeFileId(fileId)
                    .setContractMemo("[e2e::canPopulateContractIdNum]")
                    .execute(testEnv.client);

            receipt = response.setValidateStatus(true).getReceipt(testEnv.client);

            var contractId = Objects.requireNonNull(receipt.contractId);

            var info = new ContractInfoQuery().setContractId(contractId).execute(testEnv.client);

            var idMirror = ContractId.fromEvmAddress(0, 0, info.contractAccountId);
            Thread.sleep(5000);

            var newContractId = idMirror.populateContractNum(testEnv.client);

            assertThat(contractId.num).isEqualTo(newContractId.num);
        }
    }

    @Test
    @DisplayName("Can populate ContractId num from mirror node (using async method)")
    void canPopulateContractIdNumAsync() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {

            var testContractByteCode =
                    "608060405234801561001057600080fd5b50336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506101cb806100606000396000f3fe608060405260043610610046576000357c03000000000000000000000000000000000000000000000000000000009004806341c0e1b51461004b578063cfae321714610062575b600080fd5b34801561005757600080fd5b506100606100f2565b005b34801561006e57600080fd5b50610077610162565b6040518080602001828103825283818151815260200191508051906020019080838360005b838110156100b757808201518184015260208101905061009c565b50505050905090810190601f1680156100e45780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161415610160573373ffffffffffffffffffffffffffffffffffffffff16ff5b565b60606040805190810160405280600d81526020017f48656c6c6f2c20776f726c64230000000000000000000000000000000000000081525090509056fea165627a7a72305820ae96fb3af7cde9c0abfe365272441894ab717f816f07f41f07b1cbede54e256e0029";

            var response = new FileCreateTransaction()
                    .setKeys(testEnv.operatorKey)
                    .setContents(testContractByteCode)
                    .execute(testEnv.client);

            var receipt = response.setValidateStatus(true).getReceipt(testEnv.client);
            var fileId = Objects.requireNonNull(receipt.fileId);

            response = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setGas(300000)
                    .setConstructorParameters(new ContractFunctionParameters().addString("Hello from Hedera."))
                    .setBytecodeFileId(fileId)
                    .setContractMemo("[e2e::canPopulateContractIdNum]")
                    .execute(testEnv.client);

            receipt = response.setValidateStatus(true).getReceipt(testEnv.client);

            var contractId = Objects.requireNonNull(receipt.contractId);

            var info = new ContractInfoQuery().setContractId(contractId).execute(testEnv.client);

            var idMirror = ContractId.fromEvmAddress(0, 0, info.contractAccountId);
            Thread.sleep(5000);

            var newContractId =
                    idMirror.populateContractNumAsync(testEnv.client).get();

            assertThat(contractId.num).isEqualTo(newContractId.num);
        }
    }
}
