// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.hedera.hashgraph.sdk.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.ContractUpdateTransaction;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HookCreationDetails;
import com.hedera.hashgraph.sdk.HookExtensionPoint;
import com.hedera.hashgraph.sdk.LambdaEvmHook;
import com.hedera.hashgraph.sdk.LambdaStorageUpdate;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.Status;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContractUpdateTransactionHooksIntegrationTest {

    private static final String SMART_CONTRACT_BYTECODE =
            "6080604052348015600e575f80fd5b50335f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506104a38061005b5f395ff3fe608060405260043610610033575f3560e01c8063607a4427146100375780637065cb4814610053578063893d20e81461007b575b5f80fd5b610051600480360381019061004c919061033c565b6100a5565b005b34801561005e575f80fd5b50610079600480360381019061007491906103a2565b610215565b005b348015610086575f80fd5b5061008f6102b7565b60405161009c91906103dc565b60405180910390f35b3373ffffffffffffffffffffffffffffffffffffffff165f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16146100fb575f80fd5b805f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550600181908060018154018082558091505060019003905f5260205f20015f9091909190916101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505f8173ffffffffffffffffffffffffffffffffffffffff166108fc3490811502906040515f60405180830381858888f19350505050905080610211576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016102089061044f565b60405180910390fd5b5050565b805f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550600181908060018154018082558091505060019003905f5260205f20015f9091909190916101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f61030b826102e2565b9050919050565b61031b81610301565b8114610325575f80fd5b50565b5f8135905061033681610312565b92915050565b5f60208284031215610351576103506102de565b5b5f61035e84828501610328565b91505092915050565b5f610371826102e2565b9050919050565b61038181610367565b811461038b575f80fd5b50565b5f8135905061039c81610378565b92915050565b5f602082840312156103b7576103b66102de565b5b5f6103c48482850161038e565b91505092915050565b6103d681610367565b82525050565b5f6020820190506103ef5f8301846103cd565b92915050565b5f82825260208201905092915050565b7f5472616e73666572206661696c656400000000000000000000000000000000005f82015250565b5f610439600f836103f5565b915061044482610405565b602082019050919050565b5f6020820190508181035f8301526104668161042d565b905091905056fea26469706673582212206c46ddb2acdbcc4290e15be83eb90cd0b2ce5bd82b9bfe58a0709c5aec96305564736f6c634300081a0033";

    @Test
    @DisplayName(
            "Given a contract exists without hooks, when a ContractUpdateTransaction adds a basic lambda EVM hook with valid signatures, then the hook is successfully attached to the contract")
    void contractUpdateWithBasicLambdaHookSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            ContractId targetHookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var fileId = createBytecodeFile(testEnv);
            var createdContractId = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setBytecodeFileId(fileId)
                    .setGas(400000)
                    .setInitialBalance(Hbar.fromTinybars(0))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .contractId;

            var lambdaHook = new LambdaEvmHook(targetHookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            var response = new ContractUpdateTransaction()
                    .setContractId(createdContractId)
                    .setMaxTransactionFee(Hbar.from(20))
                    .addHookToCreate(hookDetails)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given a ContractUpdateTransaction is configured with duplicate hook IDs in the same creation details, when the transaction is executed, then the transaction fails with a HOOK_ID_REPEATED_IN_CREATION_DETAILS error during precheck")
    void contractUpdateWithDuplicateHookIdsInSameTransactionFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var fileId = createBytecodeFile(testEnv);
            var createdContractId = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setBytecodeFileId(fileId)
                    .setGas(400000)
                    .setInitialBalance(Hbar.fromTinybars(0))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .contractId;

            ContractId targetHookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);

            var lambdaHook = new LambdaEvmHook(targetHookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            assertThatExceptionOfType(PrecheckStatusException.class)
                    .isThrownBy(() -> new ContractUpdateTransaction()
                            .setContractId(createdContractId)
                            .setMaxTransactionFee(Hbar.from(20))
                            .setHooksToCreate(java.util.List.of(hookDetails, hookDetails))
                            .execute(testEnv.client))
                    .withMessageContaining(Status.HOOK_ID_REPEATED_IN_CREATION_DETAILS.toString());
        }
    }

    @Test
    @DisplayName(
            "Given a contract exists with a hook, when a ContractUpdateTransaction attempts to add a hook with the same ID that already exists on the contract, then the transaction fails with a HOOK_ID_IN_USE error")
    void contractUpdateWithExistingHookIdFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var fileId = createBytecodeFile(testEnv);
            var createdContractId = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setBytecodeFileId(fileId)
                    .setGas(400000)
                    .setInitialBalance(Hbar.fromTinybars(0))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .contractId;

            ContractId targetHookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var lambdaHook = new LambdaEvmHook(targetHookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            new ContractUpdateTransaction()
                    .setContractId(createdContractId)
                    .addHookToCreate(hookDetails)
                    .setMaxTransactionFee(Hbar.from(20))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        var response = new ContractUpdateTransaction()
                                .setContractId(createdContractId)
                                .addHookToCreate(hookDetails)
                                .setMaxTransactionFee(Hbar.from(20))
                                .execute(testEnv.client);
                        response.getReceipt(testEnv.client);
                    })
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_ID_IN_USE));
        }
    }

    @Test
    @DisplayName(
            "Given a contract exists without hooks, when a ContractUpdateTransaction adds a lambda EVM hook with initial storage updates, then the hook is attached and storage is initialized correctly")
    void contractUpdateWithLambdaHookAndStorageUpdatesSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var fileId = createBytecodeFile(testEnv);
            var createdContractId = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setBytecodeFileId(fileId)
                    .setGas(400000)
                    .setInitialBalance(Hbar.fromTinybars(0))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .contractId;

            ContractId targetHookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var storageSlot = new LambdaStorageUpdate.LambdaStorageSlot(new byte[] {0x01}, new byte[] {0x02});
            var lambdaHook = new LambdaEvmHook(targetHookContractId, java.util.List.of(storageSlot));
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            var response = new ContractUpdateTransaction()
                    .setContractId(createdContractId)
                    .setMaxTransactionFee(Hbar.from(20))
                    .addHookToCreate(hookDetails)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given a contract exists with an existing hook, when a ContractUpdateTransaction attempts to add another hook with the same ID that is already in use, then the transaction fails with a HOOK_ID_IN_USE error")
    void contractUpdateWithHookIdAlreadyInUseFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var fileId = createBytecodeFile(testEnv);
            var createdContractId = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setBytecodeFileId(fileId)
                    .setGas(400000)
                    .setInitialBalance(Hbar.fromTinybars(0))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .contractId;

            ContractId targetHookContractId1 = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var lambdaHook1 = new LambdaEvmHook(targetHookContractId1);
            var hookDetails1 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook1);

            new ContractUpdateTransaction()
                    .setMaxTransactionFee(Hbar.from(20))
                    .setContractId(createdContractId)
                    .addHookToCreate(hookDetails1)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            ContractId targetHookContractId2 = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var lambdaHook2 = new LambdaEvmHook(targetHookContractId2);
            var hookDetails2 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook2);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> new ContractUpdateTransaction()
                            .setContractId(createdContractId)
                            .addHookToCreate(hookDetails2)
                            .setMaxTransactionFee(Hbar.from(20))
                            .execute(testEnv.client)
                            .getReceipt(testEnv.client))
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_ID_IN_USE));
        }
    }

    @Test
    @DisplayName(
            "Given a contract exists with a hook, when a ContractUpdateTransaction deletes the hook by ID with valid signatures, then the hook is successfully removed from the contract")
    void contractUpdateWithHookDeletionSucceeds() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var fileId = createBytecodeFile(testEnv);
            var createdContractId = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setBytecodeFileId(fileId)
                    .setGas(400000)
                    .setInitialBalance(Hbar.fromTinybars(0))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .contractId;

            ContractId targetHookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var lambdaHook = new LambdaEvmHook(targetHookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            new ContractUpdateTransaction()
                    .setMaxTransactionFee(Hbar.from(20))
                    .setContractId(createdContractId)
                    .addHookToCreate(hookDetails)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            var response = new ContractUpdateTransaction()
                    .setContractId(createdContractId)
                    .setMaxTransactionFee(Hbar.from(20))
                    .addHookToDelete(1L)
                    .execute(testEnv.client);

            var receipt = response.getReceipt(testEnv.client);
            assertThat(receipt.status).isEqualTo(Status.SUCCESS);
        }
    }

    @Test
    @DisplayName(
            "Given a contract exists with hooks, when a ContractUpdateTransaction attempts to delete a hook ID that doesn't exist on the contract, then the transaction fails with a HOOK_NOT_FOUND error")
    void contractUpdateWithNonExistentHookIdDeletionFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var fileId = createBytecodeFile(testEnv);
            var createdContractId = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setBytecodeFileId(fileId)
                    .setGas(400000)
                    .setInitialBalance(Hbar.fromTinybars(0))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .contractId;

            ContractId targetHookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var lambdaHook = new LambdaEvmHook(targetHookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            new ContractUpdateTransaction()
                    .setMaxTransactionFee(Hbar.from(20))
                    .setContractId(createdContractId)
                    .addHookToCreate(hookDetails)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        var response = new ContractUpdateTransaction()
                                .setContractId(createdContractId)
                                .setMaxTransactionFee(Hbar.from(20))
                                .addHookToDelete(123L)
                                .execute(testEnv.client);
                        response.getReceipt(testEnv.client);
                    })
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_NOT_FOUND));
        }
    }

    @Test
    @DisplayName(
            "Given a ContractUpdateTransaction attempts to add and delete hooks with the same ID in the same transaction, when the transaction is executed, then the transaction fails with a HOOK_NOT_FOUND error")
    void contractUpdateWithAddAndDeleteSameHookIdFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var fileId = createBytecodeFile(testEnv);
            var createdContractId = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setBytecodeFileId(fileId)
                    .setGas(400000)
                    .setInitialBalance(Hbar.fromTinybars(0))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .contractId;

            ContractId targetHookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var lambdaHook = new LambdaEvmHook(targetHookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        var response = new ContractUpdateTransaction()
                                .setContractId(createdContractId)
                                .setMaxTransactionFee(Hbar.from(20))
                                .setHooksToCreate(java.util.List.of(hookDetails))
                                .addHookToDelete(1L)
                                .execute(testEnv.client);
                        response.getReceipt(testEnv.client);
                    })
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_NOT_FOUND));
        }
    }

    @Test
    @DisplayName(
            "Given a contract exists with a hook that has been previously deleted, when a ContractUpdateTransaction attempts to delete the same hook again, then the transaction fails with a HOOK_NOT_FOUND error")
    void contractUpdateWithAlreadyDeletedHookFails() throws Exception {
        try (var testEnv = new IntegrationTestEnv(1)) {
            var fileId = createBytecodeFile(testEnv);
            var createdContractId = new ContractCreateTransaction()
                    .setAdminKey(testEnv.operatorKey)
                    .setBytecodeFileId(fileId)
                    .setGas(400000)
                    .setInitialBalance(Hbar.fromTinybars(0))
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client)
                    .contractId;

            ContractId targetHookContractId = EntityHelper.createContract(testEnv, testEnv.operatorKey);
            var lambdaHook = new LambdaEvmHook(targetHookContractId);
            var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

            // Add the hook
            new ContractUpdateTransaction()
                    .setMaxTransactionFee(Hbar.from(20))
                    .setContractId(createdContractId)
                    .addHookToCreate(hookDetails)
                    .execute(testEnv.client)
                    .getReceipt(testEnv.client);

            // First deletion - should succeed
            var firstDeleteResponse = new ContractUpdateTransaction()
                    .setContractId(createdContractId)
                    .setMaxTransactionFee(Hbar.from(20))
                    .addHookToDelete(1L)
                    .execute(testEnv.client);
            var firstDeleteReceipt = firstDeleteResponse.getReceipt(testEnv.client);
            assertThat(firstDeleteReceipt.status).isEqualTo(Status.SUCCESS);

            // Second deletion - should fail with HOOK_DELETED
            assertThatExceptionOfType(ReceiptStatusException.class)
                    .isThrownBy(() -> {
                        var response = new ContractUpdateTransaction()
                                .setMaxTransactionFee(Hbar.from(20))
                                .setContractId(createdContractId)
                                .addHookToDelete(1L)
                                .execute(testEnv.client);
                        response.getReceipt(testEnv.client);
                    })
                    .satisfies(e -> assertThat(e.receipt.status).isEqualTo(Status.HOOK_NOT_FOUND));
        }
    }

    private FileId createBytecodeFile(final IntegrationTestEnv testEnv) throws Exception {
        var response = new FileCreateTransaction()
                .setKeys(testEnv.operatorKey)
                .setContents(SMART_CONTRACT_BYTECODE)
                .execute(testEnv.client);
        return Objects.requireNonNull(response.getReceipt(testEnv.client).fileId);
    }
}
