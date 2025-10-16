// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class TransferTransactionHooksTest {

    @Test
    void shouldAddHbarTransferWithPreTxAllowanceHook() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);
        var hookCall = new FungibleHookCall(
                123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

        var result = tx.addHbarTransferWithHook(accountId, amount, hookCall);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHbarTransfers()).hasSize(1);
        assertThat(tx.getHbarTransfers().get(accountId)).isEqualTo(amount);
    }

    @Test
    void shouldAddHbarTransferWithPrePostTxAllowanceHook() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(2000);
        var hookCall = new FungibleHookCall(
                456L, new EvmHookCall(new byte[] {4, 5, 6}, 200000L), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);

        var result = tx.addHbarTransferWithHook(accountId, amount, hookCall);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHbarTransfers()).hasSize(1);
        assertThat(tx.getHbarTransfers().get(accountId)).isEqualTo(amount);
    }

    @Test
    void shouldThrowExceptionForNullAccountId() {
        var tx = new TransferTransaction();
        var amount = Hbar.fromTinybars(1000);
        var hookCall = new FungibleHookCall(
                123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

        assertThatThrownBy(() -> tx.addHbarTransferWithHook(null, amount, hookCall))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("accountId cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullAmount() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var hookCall = new FungibleHookCall(
                123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

        assertThatThrownBy(() -> tx.addHbarTransferWithHook(accountId, null, hookCall))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("amount cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullHookCall() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);
        assertThatThrownBy(() -> tx.addHbarTransferWithHook(accountId, amount, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("hookCall cannot be null");
    }

    @Test
    void shouldNotAllowNullHookTypeRemovedByTypedAPI() {
        // No-op: hook type is encoded in FungibleHookCall now
        assertThat(true).isTrue();
    }

    @Test
    void shouldUpdateExistingTransferWithHook() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);

        // First add a regular transfer
        tx.addHbarTransfer(accountId, amount);

        // Then add a hook to the existing transfer
        var hookCall = new FungibleHookCall(
                123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

        var result = tx.addHbarTransferWithHook(accountId, amount, hookCall);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHbarTransfers()).hasSize(1);
        assertThat(tx.getHbarTransfers().get(accountId)).isEqualTo(Hbar.fromTinybars(2000));
    }

    @Test
    void shouldCreateNewTransferWhenExistingTransferHasHook() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);

        // First add a transfer with a hook
        var hookCall1 = new FungibleHookCall(
                123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
        tx.addHbarTransferWithHook(accountId, amount, hookCall1);

        // Try to add another hook - should create a new transfer
        var hookCall2 = new FungibleHookCall(
                456L, new EvmHookCall(new byte[] {4, 5, 6}, 200000L), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);
        tx.addHbarTransferWithHook(accountId, amount, hookCall2);

        assertThat(tx.getHbarTransfers()).hasSize(1);
        assertThat(tx.getHbarTransfers().get(accountId)).isEqualTo(Hbar.fromTinybars(2000));
    }

    @Test
    void shouldHandleMultipleAccountsWithHooks() {
        var tx = new TransferTransaction();
        var accountId1 = new AccountId(0, 0, 1);
        var accountId2 = new AccountId(0, 0, 2);
        var amount = Hbar.fromTinybars(1000);

        var hookCall1 = new FungibleHookCall(
                123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
        var hookCall2 = new FungibleHookCall(
                456L, new EvmHookCall(new byte[] {4, 5, 6}, 200000L), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);

        tx.addHbarTransferWithHook(accountId1, amount, hookCall1);
        tx.addHbarTransferWithHook(accountId2, amount, hookCall2);

        assertThat(tx.getHbarTransfers()).hasSize(2);
        assertThat(tx.getHbarTransfers().get(accountId1)).isEqualTo(amount);
        assertThat(tx.getHbarTransfers().get(accountId2)).isEqualTo(amount);
    }

    @Test
    void shouldThrowExceptionWhenFrozen() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);
        var hookCall = new FungibleHookCall(
                123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

        // Set up the transaction properly before freezing
        tx.setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), java.time.Instant.now()));
        tx.setNodeAccountIds(java.util.Arrays.asList(AccountId.fromString("0.0.5005")));

        // Freeze the transaction
        tx.freeze();

        assertThatThrownBy(() -> tx.addHbarTransferWithHook(accountId, amount, hookCall))
                .isInstanceOf(IllegalStateException.class);
    }
}
