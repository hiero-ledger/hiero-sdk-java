// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ContractCreateTransactionHooksTest {

    @Test
    public void testContractCreateTransactionWithHooks() {
        // Create a test contract ID that the hook will reference (it can be any number for unit tests)
        ContractId targetContractId = new ContractId(100);

        // Create a test admin key
        PrivateKey adminKey = PrivateKey.generateED25519();

        // Create storage updates
        byte[] storageKey = {0x01, 0x02};
        byte[] storageValue = {0x03, 0x04};
        LambdaStorageUpdate storageUpdate = new LambdaStorageUpdate.LambdaStorageSlot(storageKey, storageValue);
        List<LambdaStorageUpdate> storageUpdates = Collections.singletonList(storageUpdate);

        // Build two hooks, one with admin key and storage, one simple
        ContractCreateTransaction tx = new ContractCreateTransaction()
                .setGas(1_000_000)
                .setInitialBalance(Hbar.from(10))
                .addHook(new HookCreationDetails(
                        HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                        1L,
                        new LambdaEvmHook(targetContractId, storageUpdates),
                        adminKey.getPublicKey()))
                .addHook(new HookCreationDetails(
                        HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, new LambdaEvmHook(targetContractId)));

        var hooks = tx.getHooks();
        assertEquals(2, hooks.size());

        var first = hooks.get(0);
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, first.getExtensionPoint());
        assertEquals(1L, first.getHookId());
        assertEquals(adminKey.getPublicKey(), first.getAdminKey());
        assertNotNull(first.getHook());
        assertEquals(1, first.getHook().getStorageUpdates().size());

        var second = hooks.get(1);
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, second.getExtensionPoint());
        assertEquals(2L, second.getHookId());
        assertNull(second.getAdminKey());
        assertNotNull(second.getHook());
        assertTrue(second.getHook().getStorageUpdates().isEmpty());
    }

    @Test
    public void testContractCreateTransactionSetHooks() {
        ContractId targetContractId = new ContractId(200);

        var lambdaHook = new LambdaEvmHook(targetContractId);
        var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

        var tx = new ContractCreateTransaction()
                .setGas(500_000)
                .setInitialBalance(Hbar.from(5))
                .setHooks(Collections.singletonList(hookDetails));

        var retrieved = tx.getHooks();
        assertEquals(1, retrieved.size());
        assertEquals(hookDetails, retrieved.get(0));
    }

    @Test
    public void testContractCreateTransactionHookValidationDuplicateIdsNotBlockedClientSide() {
        ContractId targetContractId = new ContractId(300);

        var tx = new ContractCreateTransaction()
                .setGas(250_000)
                .setInitialBalance(Hbar.from(3))
                .addHook(new HookCreationDetails(
                        HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, new LambdaEvmHook(targetContractId)))
                .addHook(new HookCreationDetails(
                        HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, new LambdaEvmHook(targetContractId)));

        var proto = tx.build();
        assertEquals(2, proto.getHookCreationDetailsCount());
        assertEquals(1L, proto.getHookCreationDetails(0).getHookId());
        assertEquals(1L, proto.getHookCreationDetails(1).getHookId());
    }

    @Test
    public void testContractCreateTransactionProtobufSerialization() {
        ContractId targetContractId = new ContractId(400);

        var tx = new ContractCreateTransaction()
                .setGas(750_000)
                .setInitialBalance(Hbar.from(7))
                .addHook(new HookCreationDetails(
                        HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, new LambdaEvmHook(targetContractId)));

        var protoBody = tx.build();
        assertEquals(1, protoBody.getHookCreationDetailsCount());

        var protoHook = protoBody.getHookCreationDetails(0);
        assertEquals(
                com.hedera.hashgraph.sdk.proto.HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK,
                protoHook.getExtensionPoint());
        assertEquals(1L, protoHook.getHookId());
        assertTrue(protoHook.hasLambdaEvmHook());
    }

    @Test
    public void testContractCreateTransactionEmptyHooks() {
        var tx = new ContractCreateTransaction().setGas(123_456).setInitialBalance(Hbar.from(1));

        var hooks = tx.getHooks();
        assertTrue(hooks.isEmpty());

        var proto = tx.build();
        assertEquals(0, proto.getHookCreationDetailsCount());
    }

    @Test
    public void testContractCreateTransactionHooksPersistThroughBytesRoundTrip()
            throws com.google.protobuf.InvalidProtocolBufferException {
        ContractId targetContractId = new ContractId(500);
        var lambdaHook = new LambdaEvmHook(targetContractId);
        var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 3L, lambdaHook);

        var original = new ContractCreateTransaction()
                .setGas(999_999)
                .setInitialBalance(Hbar.from(9))
                .setHooks(Collections.singletonList(hookDetails));

        byte[] bytes = original.toBytes();
        Transaction<?> parsed = Transaction.fromBytes(bytes);
        assertTrue(parsed instanceof ContractCreateTransaction);
        var parsedTx = (ContractCreateTransaction) parsed;

        var parsedHooks = parsedTx.getHooks();
        assertEquals(1, parsedHooks.size());
        var parsedHook = parsedHooks.get(0);
        assertEquals(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, parsedHook.getExtensionPoint());
        assertEquals(3L, parsedHook.getHookId());
        assertNotNull(parsedHook.getHook());
        assertTrue(parsedHook.getHook().getStorageUpdates().isEmpty());
    }
}
