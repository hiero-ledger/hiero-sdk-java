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
 * Mock integration tests for LambdaSStoreTransaction.
 * <p>
 * These tests use a mock gRPC server to simulate the Hedera network
 * and test the LambdaSStore functionality without requiring a real network.
 */
public class LambdaSStoreTransactionMockIntegrationTest {

    private TestServer testServer;
    private Client client;

    @BeforeEach
    void setUp() throws IOException {
        // Create a mock crypto service that handles LambdaSStore transactions
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

        testServer = new TestServer("LambdaSStoreTransactionMockIntegrationTest", mockCryptoService);
        client = testServer.client;
    }

    @AfterEach
    void tearDown() throws TimeoutException, InterruptedException {
        if (testServer != null) {
            testServer.close();
        }
    }

    @Test
    @DisplayName("LambdaSStoreTransaction with storage slot updates")
    void lambdaSStoreWithStorageSlotUpdates() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A hook ID
        AccountId accountId = new AccountId(0, 0, 1001);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Given: A LambdaSStoreTransaction with storage slot updates
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageSlot(new byte[]{0x01, 0x02}, new byte[]{0x03, 0x04})
                .addStorageSlot(new byte[]{0x05, 0x06}, new byte[]{0x07, 0x08});

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("LambdaSStoreTransaction with mapping entries updates")
    void lambdaSStoreWithMappingEntriesUpdates() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A hook ID
        AccountId accountId = new AccountId(0, 0, 1001);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Given: Mapping entries
        LambdaMappingEntry entry1 = LambdaMappingEntry.ofKey(
                new byte[]{0x01}, new byte[]{0x02});
        LambdaMappingEntry entry2 = LambdaMappingEntry.withPreimage(
                new byte[]{0x03, 0x04}, new byte[]{0x05});
        
        // Given: A LambdaSStoreTransaction with mapping entries updates
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addMappingEntries(new byte[]{0x10}, List.of(entry1, entry2));

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("LambdaSStoreTransaction with mixed storage updates")
    void lambdaSStoreWithMixedStorageUpdates() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A hook ID
        AccountId accountId = new AccountId(0, 0, 1001);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Given: Mixed storage updates
        LambdaStorageUpdate storageSlot = new LambdaStorageUpdate.LambdaStorageSlot(
                new byte[]{0x01}, new byte[]{0x02});
        
        LambdaMappingEntry mappingEntry = LambdaMappingEntry.ofKey(
                new byte[]{0x03}, new byte[]{0x04});
        LambdaStorageUpdate mappingEntries = new LambdaStorageUpdate.LambdaMappingEntries(
                new byte[]{0x05}, List.of(mappingEntry));
        
        // Given: A LambdaSStoreTransaction with mixed storage updates
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageUpdate(storageSlot)
                .addStorageUpdate(mappingEntries);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("LambdaSStoreTransaction with utility methods")
    void lambdaSStoreWithUtilityMethods() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A hook ID
        AccountId accountId = new AccountId(0, 0, 1001);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Given: Storage updates created using utility methods
        LambdaStorageUpdate storageSlot = StorageUtils.createDirectStorageUpdate(
                new byte[]{0x01}, new byte[]{0x02});
        
        LambdaStorageUpdate numberUpdate = StorageUtils.createNumberStorageUpdate(
                new byte[]{0x03}, 12345L);
        
        LambdaStorageUpdate stringUpdate = StorageUtils.createStringStorageUpdate(
                new byte[]{0x04}, "hello");
        
        // Given: A LambdaSStoreTransaction with utility-created storage updates
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageUpdate(storageSlot)
                .addStorageUpdate(numberUpdate)
                .addStorageUpdate(stringUpdate);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("LambdaSStoreTransaction with hex string utilities")
    void lambdaSStoreWithHexStringUtilities() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A hook ID
        AccountId accountId = new AccountId(0, 0, 1001);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Given: Storage updates created using hex string utilities
        LambdaStorageUpdate storageSlot = HookUtils.createStorageSlotFromHex("0102", "0304");
        
        LambdaStorageUpdate mappingEntry = HookUtils.createMappingEntryFromHex("05", "0607", "0809");
        
        // Given: A LambdaSStoreTransaction with hex-created storage updates
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageUpdate(storageSlot)
                .addStorageUpdate(mappingEntry);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("LambdaSStoreTransaction validation - missing hook ID")
    void lambdaSStoreValidationMissingHookId() throws TimeoutException {
        // Given: A LambdaSStoreTransaction without a hook ID
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .addStorageSlot(new byte[]{0x01}, new byte[]{0x02});

        // When/Then: The transaction fails with validation error
        assertThatThrownBy(() -> transaction.execute(client))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hookId must be set");
    }

    @Test
    @DisplayName("LambdaSStoreTransaction validation - empty storage updates")
    void lambdaSStoreValidationEmptyStorageUpdates() throws TimeoutException {
        // Given: A hook ID
        AccountId accountId = new AccountId(0, 0, 1001);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Given: A LambdaSStoreTransaction with empty storage updates
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId);

        // When/Then: The transaction fails with validation error
        assertThatThrownBy(() -> transaction.execute(client))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one storage update must be specified");
    }

    @Test
    @DisplayName("LambdaSStoreTransaction with clear storage operations")
    void lambdaSStoreWithClearStorageOperations() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A hook ID
        AccountId accountId = new AccountId(0, 0, 1001);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Given: Clear storage operations
        LambdaStorageUpdate clearSlot = StorageUtils.createClearStorageUpdate(new byte[]{0x01});
        
        LambdaStorageUpdate clearMapping = StorageUtils.createClearMappingUpdate(
                new byte[]{0x02}, new byte[]{0x03});
        
        // Given: A LambdaSStoreTransaction with clear operations
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageUpdate(clearSlot)
                .addStorageUpdate(clearMapping);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }

    @Test
    @DisplayName("LambdaSStoreTransaction with complex mapping operations")
    void lambdaSStoreWithComplexMappingOperations() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A hook ID
        AccountId accountId = new AccountId(0, 0, 1001);
        HookEntityId entityId = HookEntityId.ofAccount(accountId);
        HookId hookId = new HookId(entityId, 1L);
        
        // Given: Complex mapping operations
        LambdaStorageUpdate multipleMappings = StorageUtils.createMappingStorageUpdates(
                new byte[]{0x10},
                new byte[]{0x01}, new byte[]{0x02},
                new byte[]{0x03}, new byte[]{0x04},
                new byte[]{0x05}, new byte[]{0x06}
        );
        
        // Given: A LambdaSStoreTransaction with complex mapping operations
        LambdaSStoreTransaction transaction = new LambdaSStoreTransaction()
                .setHookId(hookId)
                .addStorageUpdate(multipleMappings);

        // When: The transaction is executed
        com.hedera.hashgraph.sdk.TransactionResponse response = transaction.execute(client);
        
        // Then: The transaction is submitted successfully
        assertThat(response).isNotNull();
        assertThat(response.transactionId).isNotNull();
    }
}
