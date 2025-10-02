package com.hedera.hashgraph.sdk.test.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.hedera.hashgraph.sdk.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Assumptions;

/**
 * Integration tests for AccountCreateTransaction with hooks.
 * <p>
 * These tests require a running Hedera test network and will be skipped
 * if the OPERATOR_ID system property is not set.
 */
@EnabledIfSystemProperty(named = "OPERATOR_ID", matches = ".*")
public class AccountCreateTransactionHooksIntegrationTest {

    private Client client;
    private PrivateKey operatorKey;
    private AccountId operatorId;

    @BeforeEach
    void setUp() throws TimeoutException {
        // Initialize client for test network
        String network = System.getProperty("HEDERA_NETWORK", "testnet");
        switch (network.toLowerCase()) {
            case "mainnet":
                client = Client.forMainnet();
                break;
            case "previewnet":
                client = Client.forPreviewnet();
                break;
            case "localhost":
                // For localhost, you might need to configure specific endpoints
                client = Client.forTestnet(); // Fallback to testnet for now
                break;
            default:
                client = Client.forTestnet();
                break;
        }

        // Get operator credentials from system properties
        operatorKey = PrivateKey.fromString(System.getProperty("OPERATOR_KEY",
            "302e020100300506032b657004220420db484b828e64b2d8f12ce3c0a0e93a0b8cce7af1bb8f39c97732394482538e10"));
        operatorId = AccountId.fromString(System.getProperty("OPERATOR_ID", "0.0.2"));

        client.setOperator(operatorId, operatorKey);
    }

    @AfterEach
    void tearDown() throws TimeoutException {
        if (client != null) {
            client.close();
        }
    }

    @Test
    @DisplayName("AccountCreateTransaction with basic lambda EVM hook (without storage updates)")
    void accountCreateWithBasicLambdaEvmHook() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = resolveHookContractOrSkip();

        // Given: An AccountCreateTransaction configured with a basic lambda EVM hook
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
            .setKey(accountKey.getPublicKey())
            .setInitialBalance(Hbar.from(10))
            .addAccountAllowanceHook(1L, hookContract);

        // When: The transaction is executed
        TransactionResponse response = transaction.execute(client);
        TransactionReceipt receipt = response.getReceipt(client);
        AccountId accountId = receipt.accountId;

        // Then: The account is created with the lambda hook successfully
        assertThat(accountId).isNotNull();
        assertThat(receipt.status).isEqualTo(Status.SUCCESS);

        // Verify the account was created
        AccountInfo accountInfo = new AccountInfoQuery()
            .setAccountId(accountId)
            .execute(client);
        assertThat(accountInfo.accountId).isEqualTo(accountId);
    }

    @Test
    @DisplayName("AccountCreateTransaction with lambda EVM hook with storage updates")
    void accountCreateWithLambdaEvmHookWithStorage() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = resolveHookContractOrSkip();

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
        TransactionResponse response = transaction.execute(client);
        TransactionReceipt receipt = response.getReceipt(client);
        AccountId accountId = receipt.accountId;

        // Then: The account is created with the lambda hook successfully
        assertThat(accountId).isNotNull();
        assertThat(receipt.status).isEqualTo(Status.SUCCESS);

        // Verify the account was created
        AccountInfo accountInfo = new AccountInfoQuery()
            .setAccountId(accountId)
            .execute(client);
        assertThat(accountInfo.accountId).isEqualTo(accountId);
    }

    @Test
    @DisplayName("AccountCreateTransaction with lambda EVM hook that has no contract ID specified")
    void accountCreateWithInvalidHookSpec() throws TimeoutException {
        // Given: An AccountCreateTransaction configured with a lambda EVM hook that has no contract ID
        PrivateKey accountKey = PrivateKey.generateED25519();

        // Creating an invalid spec without contract id throws immediately
        assertThatThrownBy(() -> new EvmHookSpec(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("AccountCreateTransaction with duplicate hook IDs in the same creation details")
    void accountCreateWithDuplicateHookIds() throws TimeoutException {
        // Given: A contract that implements the hook
        ContractId hookContract = resolveHookContractOrSkip();

        // Given: An AccountCreateTransaction configured with duplicate hook IDs
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
            .setKey(accountKey.getPublicKey())
            .setInitialBalance(Hbar.from(10))
            .addAccountAllowanceHook(1L, hookContract)
            .addAccountAllowanceHook(1L, hookContract); // Duplicate hook ID

        // When/Then: Client-side validation throws before submission
        assertThatThrownBy(() -> new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addAccountAllowanceHook(1L, hookContract)
                .addAccountAllowanceHook(1L, hookContract))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate hook ID");
    }

    @Test
    @DisplayName("AccountCreateTransaction with lambda EVM hook that has an admin key specified")
    void accountCreateWithLambdaEvmHookWithAdminKey() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = resolveHookContractOrSkip();

        // Given: An admin key for the hook
        PrivateKey adminKey = PrivateKey.generateED25519();

        // Given: An AccountCreateTransaction configured with a lambda EVM hook that has an admin key
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
            .setKey(accountKey.getPublicKey())
            .setInitialBalance(Hbar.from(10))
            .addAccountAllowanceHook(1L, hookContract, adminKey.getPublicKey(), null);

        // When: The transaction is executed with the admin key signature
        TransactionResponse response = transaction.execute(client);
        TransactionReceipt receipt = response.getReceipt(client);
        AccountId accountId = receipt.accountId;

        // Then: The account is created with the lambda hook and admin key successfully
        assertThat(accountId).isNotNull();
        assertThat(receipt.status).isEqualTo(Status.SUCCESS);

        // Verify the account was created
        AccountInfo accountInfo = new AccountInfoQuery()
            .setAccountId(accountId)
            .execute(client);
        assertThat(accountInfo.accountId).isEqualTo(accountId);
    }

    @Test
    @DisplayName("AccountCreateTransaction with multiple hooks")
    void accountCreateWithMultipleHooks() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        // Given: A contract that implements the hook
        ContractId hookContract = resolveHookContractOrSkip();

        // Given: An AccountCreateTransaction configured with multiple hooks
        PrivateKey accountKey = PrivateKey.generateED25519();
        AccountCreateTransaction transaction = new AccountCreateTransaction()
            .setKey(accountKey.getPublicKey())
            .setInitialBalance(Hbar.from(10))
            .addAccountAllowanceHook(1L, hookContract)
            .addAccountAllowanceHook(2L, hookContract);

        // When: The transaction is executed
        TransactionResponse response = transaction.execute(client);
        TransactionReceipt receipt = response.getReceipt(client);
        AccountId accountId = receipt.accountId;

        // Then: The account is created with multiple hooks successfully
        assertThat(accountId).isNotNull();
        assertThat(receipt.status).isEqualTo(Status.SUCCESS);

        // Verify the account was created
        AccountInfo accountInfo = new AccountInfoQuery()
            .setAccountId(accountId)
            .execute(client);
        assertThat(accountInfo.accountId).isEqualTo(accountId);
    }

//    @Test
//    @DisplayName("AccountCreateTransaction with hook using HookBuilder")
//    void accountCreateWithHookBuilder() throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
//        // Given: A contract that implements the hook
//        ContractId hookContract = createTestContract();
//
//        // Given: A hook created using HookBuilder
//        HookCreationDetails hookDetails = HookBuilder.accountAllowanceHook()
//            .setHookId(1L)
//            .setContract(hookContract)
//            .addStorageSlot(new byte[]{0x01}, new byte[]{0x02})
//            .build();
//
//        // Given: An AccountCreateTransaction configured with the hook
//        PrivateKey accountKey = PrivateKey.generateED25519();
//        AccountCreateTransaction transaction = new AccountCreateTransaction()
//            .setKey(accountKey.getPublicKey())
//            .setInitialBalance(Hbar.from(10))
//            .addHookCreationDetails(hookDetails);
//
//        // When: The transaction is executed
//        TransactionResponse response = transaction.execute(client);
//        TransactionReceipt receipt = response.getReceipt(client);
//        AccountId accountId = receipt.accountId;
//
//        // Then: The account is created with the hook successfully
//        assertThat(accountId).isNotNull();
//        assertThat(receipt.status).isEqualTo(com.hedera.hashgraph.sdk.proto.ResponseCodeEnum.SUCCESS);
//
//        // Verify the account was created
//        AccountInfo accountInfo = new AccountInfoQuery()
//            .setAccountId(accountId)
//            .execute(client);
//        assertThat(accountInfo.accountId).isEqualTo(accountId);
//    }

    @Test
    @DisplayName("AccountCreateTransaction with hook validation")
    void accountCreateWithHookValidation() throws TimeoutException {
        // Given: A contract that implements the hook
        ContractId hookContract = resolveHookContractOrSkip();

        // Given: An AccountCreateTransaction with invalid hook configuration
        PrivateKey accountKey = PrivateKey.generateED25519();

        // Test validation: negative hook ID
        assertThatThrownBy(() -> {
            new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addAccountAllowanceHook(-1L, hookContract);
        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Hook ID must be non-negative");

        // Test validation: null contract ID
        assertThatThrownBy(() -> {
            new AccountCreateTransaction()
                .setKey(accountKey.getPublicKey())
                .setInitialBalance(Hbar.from(10))
                .addAccountAllowanceHook(1L, null);
        }).isInstanceOf(NullPointerException.class);
    }

    /**
     * Helper method to create a test contract.
     * In a real integration test, this would deploy an actual contract.
     * For now, we'll use a mock contract ID.
     */
    private ContractId resolveHookContractOrSkip() {
        String contract = System.getProperty("HOOK_CONTRACT_ID");
        Assumptions.assumeTrue(contract != null && !contract.isBlank(), "Skipping: HOOK_CONTRACT_ID not provided");
        return ContractId.fromString(contract);
    }
}
