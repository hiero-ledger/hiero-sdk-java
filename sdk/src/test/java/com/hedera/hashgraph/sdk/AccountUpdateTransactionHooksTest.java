// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

public class AccountUpdateTransactionHooksTest {

    @Test
    void shouldAddHook() {
        var tx = new AccountUpdateTransaction();
        var contractId = new ContractId(0, 0, 1);
        var evmHookSpec = new EvmHookSpec(contractId);
        var lambdaHook = new LambdaEvmHook(evmHookSpec);
        var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

        var result = tx.addHook(hookDetails);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHooksToCreate()).hasSize(1);
        assertThat(tx.getHooksToCreate().get(0)).isEqualTo(hookDetails);
    }

    @Test
    void shouldSetHooks() {
        var tx = new AccountUpdateTransaction();
        var contractId = new ContractId(0, 0, 1);
        var evmHookSpec = new EvmHookSpec(contractId);
        var lambdaHook = new LambdaEvmHook(evmHookSpec);
        var hookDetails1 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);
        var hookDetails2 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, lambdaHook);
        var hooks = List.of(hookDetails1, hookDetails2);

        var result = tx.setHooks(hooks);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHooksToCreate()).hasSize(2);
        assertThat(tx.getHooksToCreate()).containsExactlyInAnyOrder(hookDetails1, hookDetails2);
    }

    @Test
    void shouldDeleteHook() {
        var tx = new AccountUpdateTransaction();
        var hookId = 123L;

        var result = tx.deleteHook(hookId);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHooksToDelete()).hasSize(1);
        assertThat(tx.getHooksToDelete()).contains(hookId);
    }

    @Test
    void shouldDeleteHooks() {
        var tx = new AccountUpdateTransaction();
        var hookIds = List.of(123L, 456L, 789L);

        var result = tx.deleteHooks(hookIds);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHooksToDelete()).hasSize(3);
        assertThat(tx.getHooksToDelete()).containsExactlyInAnyOrder(123L, 456L, 789L);
    }

    @Test
    void shouldGetHooksToCreate() {
        var tx = new AccountUpdateTransaction();
        var contractId = new ContractId(0, 0, 1);
        var evmHookSpec = new EvmHookSpec(contractId);
        var lambdaHook = new LambdaEvmHook(evmHookSpec);
        var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);
        tx.addHook(hookDetails);

        var result = tx.getHooksToCreate();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(hookDetails);
        // Verify it returns a copy
        result.clear();
        assertThat(tx.getHooksToCreate()).hasSize(1);
    }

    @Test
    void shouldGetHooksToDelete() {
        var tx = new AccountUpdateTransaction();
        tx.deleteHook(123L);

        var result = tx.getHooksToDelete();

        assertThat(result).hasSize(1);
        assertThat(result).contains(123L);
        // Verify it returns a copy
        result.clear();
        assertThat(tx.getHooksToDelete()).hasSize(1);
    }

    @Test
    void shouldThrowWhenAddingHookAfterFreeze() {
        var tx = new AccountUpdateTransaction();
        tx.setNodeAccountIds(java.util.Arrays.asList(AccountId.fromString("0.0.5005")));
        tx.setTransactionId(TransactionId.withValidStart(
                AccountId.fromString("0.0.5006"), java.time.Instant.ofEpochSecond(1554158542)));
        tx.freeze();
        var contractId = new ContractId(0, 0, 1);
        var evmHookSpec = new EvmHookSpec(contractId);
        var lambdaHook = new LambdaEvmHook(evmHookSpec);
        var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

        assertThatThrownBy(() -> tx.addHook(hookDetails))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transaction is immutable");
    }

    @Test
    void shouldThrowWhenSettingHooksAfterFreeze() {
        var tx = new AccountUpdateTransaction();
        tx.setNodeAccountIds(java.util.Arrays.asList(AccountId.fromString("0.0.5005")));
        tx.setTransactionId(TransactionId.withValidStart(
                AccountId.fromString("0.0.5006"), java.time.Instant.ofEpochSecond(1554158542)));
        tx.freeze();
        var contractId = new ContractId(0, 0, 1);
        var evmHookSpec = new EvmHookSpec(contractId);
        var lambdaHook = new LambdaEvmHook(evmHookSpec);
        var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);

        assertThatThrownBy(() -> tx.setHooks(List.of(hookDetails)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transaction is immutable");
    }

    @Test
    void shouldThrowWhenDeletingHookAfterFreeze() {
        var tx = new AccountUpdateTransaction();
        tx.setNodeAccountIds(java.util.Arrays.asList(AccountId.fromString("0.0.5005")));
        tx.setTransactionId(TransactionId.withValidStart(
                AccountId.fromString("0.0.5006"), java.time.Instant.ofEpochSecond(1554158542)));
        tx.freeze();

        assertThatThrownBy(() -> tx.deleteHook(123L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transaction is immutable");
    }

    @Test
    void shouldThrowWhenDeletingHooksAfterFreeze() {
        var tx = new AccountUpdateTransaction();
        tx.setNodeAccountIds(java.util.Arrays.asList(AccountId.fromString("0.0.5005")));
        tx.setTransactionId(TransactionId.withValidStart(
                AccountId.fromString("0.0.5006"), java.time.Instant.ofEpochSecond(1554158542)));
        tx.freeze();

        assertThatThrownBy(() -> tx.deleteHooks(List.of(123L, 456L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transaction is immutable");
    }

    @Test
    void shouldThrowWhenAddingNullHook() {
        var tx = new AccountUpdateTransaction();

        assertThatThrownBy(() -> tx.addHook(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("hookDetails cannot be null");
    }

    @Test
    void shouldThrowWhenSettingNullHooks() {
        var tx = new AccountUpdateTransaction();

        assertThatThrownBy(() -> tx.setHooks(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("hookDetails cannot be null");
    }

    @Test
    void shouldThrowWhenDeletingNullHook() {
        var tx = new AccountUpdateTransaction();

        assertThatThrownBy(() -> tx.deleteHook(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("hookId cannot be null");
    }

    @Test
    void shouldThrowWhenDeletingNullHooks() {
        var tx = new AccountUpdateTransaction();

        assertThatThrownBy(() -> tx.deleteHooks(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("hookIds cannot be null");
    }

    @Test
    void shouldSerializeHooksInBuild() {
        var tx = new AccountUpdateTransaction();
        var contractId = new ContractId(0, 0, 1);
        var evmHookSpec = new EvmHookSpec(contractId);
        var lambdaHook = new LambdaEvmHook(evmHookSpec);
        var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);
        tx.addHook(hookDetails);
        tx.deleteHook(123L);

        var builder = tx.build();

        assertThat(builder.getHookCreationDetailsList()).hasSize(1);
        assertThat(builder.getHookIdsToDeleteList()).hasSize(1);
        assertThat(builder.getHookIdsToDeleteList()).contains(123L);
    }

    @Test
    void shouldDeserializeHooksFromTransactionBody() throws Exception {
        var tx = new AccountUpdateTransaction();
        var contractId = new ContractId(0, 0, 1);
        var evmHookSpec = new EvmHookSpec(contractId);
        var lambdaHook = new LambdaEvmHook(evmHookSpec);
        var hookDetails = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);
        tx.addHook(hookDetails);
        tx.deleteHook(123L);

        var bytes = tx.toBytes();
        var deserializedTx = (AccountUpdateTransaction) Transaction.fromBytes(bytes);

        assertThat(deserializedTx.getHooksToCreate()).hasSize(1);
        assertThat(deserializedTx.getHooksToDelete()).hasSize(1);
        assertThat(deserializedTx.getHooksToDelete()).contains(123L);
    }

    @Test
    void shouldHandleEmptyHooks() {
        var tx = new AccountUpdateTransaction();

        assertThat(tx.getHooksToCreate()).isEmpty();
        assertThat(tx.getHooksToDelete()).isEmpty();

        var builder = tx.build();
        assertThat(builder.getHookCreationDetailsList()).isEmpty();
        assertThat(builder.getHookIdsToDeleteList()).isEmpty();
    }

    @Test
    void shouldSupportMultipleHooks() {
        var tx = new AccountUpdateTransaction();
        var contractId = new ContractId(0, 0, 1);
        var evmHookSpec = new EvmHookSpec(contractId);
        var lambdaHook = new LambdaEvmHook(evmHookSpec);

        var hookDetails1 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 1L, lambdaHook);
        var hookDetails2 = new HookCreationDetails(HookExtensionPoint.ACCOUNT_ALLOWANCE_HOOK, 2L, lambdaHook);

        tx.addHook(hookDetails1);
        tx.addHook(hookDetails2);
        tx.deleteHook(100L);
        tx.deleteHook(200L);

        assertThat(tx.getHooksToCreate()).hasSize(2);
        assertThat(tx.getHooksToDelete()).hasSize(2);
        assertThat(tx.getHooksToDelete()).containsExactlyInAnyOrder(100L, 200L);
    }
}
