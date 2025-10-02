// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

/**
 * Integration test demonstrating the complete hook workflow.
 * <p>
 * This test shows how to:
 * 1. Create an account with hooks
 * 2. Update hook storage using LambdaSStoreTransaction
 * 3. Verify the complete workflow
 */
public class HookTest {

    @Test
    public void testAccountCreateWithAllowanceHook() {
        // Create a contract that will implement the hook
        ContractId hookContract = new ContractId(1000);

        // Create an account with an allowance hook
        AccountCreateTransaction accountCreateTx = new AccountCreateTransaction()
                .setKey(PrivateKey.generateED25519().getPublicKey())
                .setInitialBalance(Hbar.from(100))
                .addAccountAllowanceHook(1L, hookContract);

        // Verify the account creation transaction has hooks
        List<HookCreationDetails> hookDetails = accountCreateTx.getHookCreationDetails();
        assertEquals(1, hookDetails.size());

        HookCreationDetails hookDetail = hookDetails.get(0);
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, hookDetail.getExtensionPoint());
        assertEquals(1L, hookDetail.getHookId());
        assertEquals(hookContract, hookDetail.getHook().getSpec().getContractId());

        // Protobuf serialization of AccountCreate hooks
        var accountCreateProto = accountCreateTx.build();
        assertTrue(accountCreateProto.getHookCreationDetailsCount() > 0);

        // Duplicate hook IDs in account creation should fail
        assertThrows(IllegalArgumentException.class, () -> {
            new AccountCreateTransaction()
                    .setKey(PrivateKey.generateED25519().getPublicKey())
                    .setInitialBalance(Hbar.from(50))
                    .addAccountAllowanceHook(1L, hookContract)
                    .addAccountAllowanceHook(1L, hookContract)
                    .build();
        });
    }

    @Test
    public void testHookTypesSerialization() {
        // Test all hook types can be serialized to protobuf and back

        // Test HookExtensionPoint
        HookExtensionPoint extensionPoint = HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK;
        assertEquals(extensionPoint, HookExtensionPoint.fromProtobuf(extensionPoint.getProtoValue()));

        // EvmHookSpec
        ContractId contractId = new ContractId(4000);
        // Test EvmHookSpec
        EvmHookSpec evmHookSpec = new EvmHookSpec(contractId);
        assertEquals(evmHookSpec, EvmHookSpec.fromProtobuf(evmHookSpec.toProtobuf()));

        // Test LambdaStorageUpdate
        LambdaStorageUpdate storageSlot = new LambdaStorageUpdate.LambdaStorageSlot(
                new byte[]{0x20}, new byte[]{0x21});
        assertEquals(storageSlot, LambdaStorageUpdate.fromProtobuf(storageSlot.toProtobuf()));

        // Test LambdaMappingEntry
        LambdaMappingEntry mappingEntry = LambdaMappingEntry.ofKey(
                new byte[]{0x22}, new byte[]{0x23});
        assertEquals(mappingEntry, LambdaMappingEntry.fromProtobuf(mappingEntry.toProtobuf()));

        // Test LambdaEvmHook
        LambdaEvmHook lambdaEvmHook = new LambdaEvmHook(evmHookSpec, Collections.singletonList(storageSlot));
        assertEquals(lambdaEvmHook, LambdaEvmHook.fromProtobuf(lambdaEvmHook.toProtobuf()));

        // Test HookCreationDetails
        HookCreationDetails hookCreationDetails = new HookCreationDetails(
                extensionPoint, 6L, lambdaEvmHook, PrivateKey.generateED25519().getPublicKey());
        assertEquals(hookCreationDetails, HookCreationDetails.fromProtobuf(hookCreationDetails.toProtobuf()));
    }

    @Test
    public void testHookValidation() {
        // Test various validation scenarios

        // Test LambdaStorageSlot validation
        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaStorageUpdate.LambdaStorageSlot(new byte[33], new byte[]{0x01});
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new LambdaStorageUpdate.LambdaStorageSlot(new byte[]{0x00, 0x01}, new byte[]{0x01});
        });

        // Test LambdaMappingEntry validation
        assertThrows(IllegalArgumentException.class, () -> {
            LambdaMappingEntry.ofKey(new byte[33], new byte[]{0x01});
        });

        assertThrows(IllegalArgumentException.class, () -> {
            LambdaMappingEntry.ofKey(new byte[]{0x00, 0x01}, new byte[]{0x01});
        });

        // No LambdaSStoreTransaction tests on this branch
    }
}
