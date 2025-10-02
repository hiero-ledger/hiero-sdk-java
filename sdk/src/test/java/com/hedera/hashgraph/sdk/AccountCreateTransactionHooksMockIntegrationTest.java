// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hedera.hashgraph.sdk.proto.CryptoServiceGrpc;
import com.hedera.hashgraph.sdk.proto.Query;
import com.hedera.hashgraph.sdk.proto.Response;
import com.hedera.hashgraph.sdk.proto.Transaction;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Mock integration tests for AccountCreateTransaction with hooks.
 * <p>
 * These tests use a mock gRPC server to simulate the Hedera network
 * and test the hook functionality without requiring a real network.
 */
public class AccountCreateTransactionHooksMockIntegrationTest {

    private TestServer testServer;
    private Client client;

    @BeforeEach
    void setUp() throws IOException {
        // Create a mock crypto service that handles account creation with hooks
        CryptoServiceGrpc.CryptoServiceImplBase mockCryptoService = new CryptoServiceGrpc.CryptoServiceImplBase() {
            @Override
            public void createAccount(Transaction request, io.grpc.stub.StreamObserver<TransactionResponse> responseObserver) {
                // Simulate successful account creation
                TransactionResponse response = TransactionResponse.newBuilder()
                        .setNodeTransactionPrecheckCode(com.hedera.hashgraph.sdk.proto.ResponseCodeEnum.OK)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };

        testServer = new TestServer("AccountCreateTransactionHooksMockIntegrationTest", mockCryptoService);
        client = testServer.client;
    }

    @AfterEach
    void tearDown() throws TimeoutException, InterruptedException {
        if (testServer != null) {
            testServer.close();
        }
    }

    @Test
    @DisplayName("AccountCreateTransaction with basic lambda EVM hook (without storage updates)")
    void accountCreateWithBasicLambdaEvmHook() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = new ContractId(0, 0, 1001);
        
        // Given: An AccountCreateTransaction configured with a basic lambda EVM hook
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addAccountAllowanceHook(1L, hookContract);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("AccountCreateTransaction with lambda EVM hook with storage updates")
    void accountCreateWithLambdaEvmHookWithStorage() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = new ContractId(0, 0, 1001);
        
        // Given: Storage updates for the hook
        LambdaStorageUpdate storageUpdate = new LambdaStorageUpdate.LambdaStorageSlot(
                new byte[]{0x01, 0x02}, 
                new byte[]{0x03, 0x04}
        );
        
        // Given: An AccountCreateTransaction configured with a lambda EVM hook with storage updates
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addAccountAllowanceHook(1L, hookContract, null, List.of(storageUpdate));

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("AccountCreateTransaction with lambda EVM hook that has no contract ID specified")
    void accountCreateWithInvalidHookSpec() throws TimeoutException {
        // Given: An AccountCreateTransaction configured with a lambda EVM hook that has no contract ID
        PrivateKey accountKey = PrivateKey.generateED25519();
        
        // Create a hook with invalid spec (no contract ID)
        EvmHookSpec invalidSpec = new EvmHookSpec(null); // This should fail validation
        LambdaEvmHook invalidHook = new LambdaEvmHook(invalidSpec);
        HookCreationDetails invalidHookDetails = new HookCreationDetails(
                HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, invalidHook);
        
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addHookCreationDetails(invalidHookDetails);

        // When/Then: The transaction fails with validation error
        assertThatThrownBy(() -> transaction.execute(client))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AccountCreateTransaction with duplicate hook IDs in the same creation details")
    void accountCreateWithDuplicateHookIds() throws TimeoutException {
        // Given: A contract that implements the hook
        ContractId hookContract = new ContractId(0, 0, 1001);
        
        // Given: An AccountCreateTransaction configured with duplicate hook IDs
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addAccountAllowanceHook(1L, hookContract)
                .addAccountAllowanceHook(1L, hookContract); // Duplicate hook ID

        // When/Then: The transaction fails with validation error
        assertThatThrownBy(() -> transaction.execute(client))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate hook ID");
    }

    @Test
    @DisplayName("AccountCreateTransaction with lambda EVM hook that has an admin key specified")
    void accountCreateWithLambdaEvmHookWithAdminKey() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = new ContractId(0, 0, 1001);
        
        // Given: An admin key for the hook
        PrivateKey adminKey = PrivateKey.generateED25519();
        
        // Given: An AccountCreateTransaction configured with a lambda EVM hook that has an admin key
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addAccountAllowanceHook(1L, hookContract, adminKey.getPublicKey(), null);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("AccountCreateTransaction with multiple hooks")
    void accountCreateWithMultipleHooks() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = new ContractId(0, 0, 1001);
        
        // Given: An AccountCreateTransaction configured with multiple hooks
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addAccountAllowanceHook(1L, hookContract)
                .addAccountAllowanceHook(2L, hookContract);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("AccountCreateTransaction with hook using HookBuilder")
    void accountCreateWithHookBuilder() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = new ContractId(0, 0, 1001);
        
        // Given: A hook created using HookBuilder
        HookCreationDetails hookDetails = HookBuilder.accountAllowanceHook()
                .setHookId(1L)
                .setContract(hookContract)
                .addStorageSlot(new byte[]{0x01}, new byte[]{0x02})
                .build();
        
        // Given: An AccountCreateTransaction configured with the hook
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addHookCreationDetails(hookDetails);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("AccountCreateTransaction with hook validation")
    void accountCreateWithHookValidation() throws TimeoutException {
        // Given: A contract that implements the hook
        ContractId hookContract = new ContractId(0, 0, 1001);
        
        // Given: An AccountCreateTransaction with invalid hook configuration
        PrivateKey accountKey = PrivateKey.generateED25519();
        
        // Test validation: negative hook ID
        assertThatThrownBy(() -> {
            new AccountCreateTransaction()
                    .setKey(accountKey.getPublicKey())
                    .setInitialBalance(Hbar.from(10))
                    .addAccountAllowanceHook(-1L, hookContract, null, null);
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Hook ID must be non-negative");
        
        // Test validation: null contract ID
        assertThatThrownBy(() -> {
            new AccountCreateTransaction()
                    .setKey(accountKey.getPublicKey())
                    .setInitialBalance(Hbar.from(10))
                    .addAccountAllowanceHook(1L, null, null, null);
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("AccountCreateTransaction with hook using utility methods")
    void accountCreateWithHookUtilities() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = new ContractId(0, 0, 1001);
        
        // Given: A hook created using utility methods
        HookCreationDetails hookDetails = HookUtils.createSimpleAccountAllowanceHook(1L, hookContract);
        
        // Given: An AccountCreateTransaction configured with the hook
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addHookCreationDetails(hookDetails);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("AccountCreateTransaction with complex hook configuration")
    void accountCreateWithComplexHookConfiguration() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = new ContractId(0, 0, 1001);
        
        // Given: A complex hook configuration with multiple storage updates
        LambdaStorageUpdate storageSlot = new LambdaStorageUpdate.LambdaStorageSlot(
                new byte[]{0x01}, new byte[]{0x02});
        
        LambdaMappingEntry mappingEntry = LambdaMappingEntry.ofKey(
                new byte[]{0x03}, new byte[]{0x04});
        LambdaStorageUpdate mappingEntries = new LambdaStorageUpdate.LambdaMappingEntries(
                new byte[]{0x05}, List.of(mappingEntry));
        
        HookCreationDetails hookDetails = HookBuilder.accountAllowanceHook()
                .setHookId(1L)
                .setContract(hookContract)
                .addStorageUpdate(storageSlot)
                .addStorageUpdate(mappingEntries)
                .setAdminKey(PrivateKey.generateED25519().getPublicKey())
                .build();
        
        // Given: An AccountCreateTransaction configured with the complex hook
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addHookCreationDetails(hookDetails);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }
}
