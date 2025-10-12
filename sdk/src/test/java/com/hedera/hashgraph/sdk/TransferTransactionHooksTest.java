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
        var hookCall = new HookCall(123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L));
        var hookType = HookType.PRE_TX_ALLOWANCE_HOOK;

        var result = tx.addHbarTransferWithHook(accountId, amount, hookCall, hookType);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHbarTransfers()).hasSize(1);
        assertThat(tx.getHbarTransfers().get(accountId)).isEqualTo(amount);
    }

    @Test
    void shouldAddHbarTransferWithPrePostTxAllowanceHook() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(2000);
        var hookCall = new HookCall(456L, new EvmHookCall(new byte[] {4, 5, 6}, 200000L));
        var hookType = HookType.PRE_POST_TX_ALLOWANCE_HOOK;

        var result = tx.addHbarTransferWithHook(accountId, amount, hookCall, hookType);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHbarTransfers()).hasSize(1);
        assertThat(tx.getHbarTransfers().get(accountId)).isEqualTo(amount);
    }

    @Test
    void shouldThrowExceptionForNullAccountId() {
        var tx = new TransferTransaction();
        var amount = Hbar.fromTinybars(1000);
        var hookCall = new HookCall(123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L));
        var hookType = HookType.PRE_TX_ALLOWANCE_HOOK;

        assertThatThrownBy(() -> tx.addHbarTransferWithHook(null, amount, hookCall, hookType))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("accountId cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullAmount() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var hookCall = new HookCall(123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L));
        var hookType = HookType.PRE_TX_ALLOWANCE_HOOK;

        assertThatThrownBy(() -> tx.addHbarTransferWithHook(accountId, null, hookCall, hookType))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("amount cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullHookCall() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);
        var hookType = HookType.PRE_TX_ALLOWANCE_HOOK;

        assertThatThrownBy(() -> tx.addHbarTransferWithHook(accountId, amount, null, hookType))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("hookCall cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullHookType() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);
        var hookCall = new HookCall(123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L));

        assertThatThrownBy(() -> tx.addHbarTransferWithHook(accountId, amount, hookCall, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("hookType cannot be null");
    }

    @Test
    void shouldUpdateExistingTransferWithHook() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);

        // First add a regular transfer
        tx.addHbarTransfer(accountId, amount);

        // Then add a hook to the existing transfer
        var hookCall = new HookCall(123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L));
        var hookType = HookType.PRE_TX_ALLOWANCE_HOOK;

        var result = tx.addHbarTransferWithHook(accountId, amount, hookCall, hookType);

        assertThat(result).isSameAs(tx);
        assertThat(tx.getHbarTransfers()).hasSize(1);
        assertThat(tx.getHbarTransfers().get(accountId)).isEqualTo(amount);
    }

    @Test
    void shouldCreateNewTransferWhenExistingTransferHasHook() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);

        // First add a transfer with a hook
        var hookCall1 = new HookCall(123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L));
        var hookType1 = HookType.PRE_TX_ALLOWANCE_HOOK;
        tx.addHbarTransferWithHook(accountId, amount, hookCall1, hookType1);

        // Try to add another hook - should create a new transfer
        var hookCall2 = new HookCall(456L, new EvmHookCall(new byte[] {4, 5, 6}, 200000L));
        var hookType2 = HookType.PRE_POST_TX_ALLOWANCE_HOOK;
        tx.addHbarTransferWithHook(accountId, amount, hookCall2, hookType2);

        // getHbarTransfers() only shows the last transfer for each account (Map.put behavior)
        // So we should see only the second transfer's amount
        assertThat(tx.getHbarTransfers()).hasSize(1);
        assertThat(tx.getHbarTransfers().get(accountId)).isEqualTo(amount); // Only the last transfer's amount

        // Note: Internally there are 2 HbarTransfer objects, but getHbarTransfers()
        // uses Map.put() so only the last one's amount is visible
    }

    @Test
    void shouldHandleMultipleAccountsWithHooks() {
        var tx = new TransferTransaction();
        var accountId1 = new AccountId(0, 0, 1);
        var accountId2 = new AccountId(0, 0, 2);
        var amount = Hbar.fromTinybars(1000);

        var hookCall1 = new HookCall(123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L));
        var hookCall2 = new HookCall(456L, new EvmHookCall(new byte[] {4, 5, 6}, 200000L));

        tx.addHbarTransferWithHook(accountId1, amount, hookCall1, HookType.PRE_TX_ALLOWANCE_HOOK);
        tx.addHbarTransferWithHook(accountId2, amount, hookCall2, HookType.PRE_POST_TX_ALLOWANCE_HOOK);

        assertThat(tx.getHbarTransfers()).hasSize(2);
        assertThat(tx.getHbarTransfers().get(accountId1)).isEqualTo(amount);
        assertThat(tx.getHbarTransfers().get(accountId2)).isEqualTo(amount);
    }

    @Test
    void shouldThrowExceptionWhenFrozen() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 1);
        var amount = Hbar.fromTinybars(1000);
        var hookCall = new HookCall(123L, new EvmHookCall(new byte[] {1, 2, 3}, 100000L));
        var hookType = HookType.PRE_TX_ALLOWANCE_HOOK;

        // Set up the transaction properly before freezing
        tx.setTransactionId(TransactionId.withValidStart(AccountId.fromString("0.0.5006"), java.time.Instant.now()));
        tx.setNodeAccountIds(java.util.Arrays.asList(AccountId.fromString("0.0.5005")));

        // Freeze the transaction
        tx.freeze();

        assertThatThrownBy(() -> tx.addHbarTransferWithHook(accountId, amount, hookCall, hookType))
                .isInstanceOf(IllegalStateException.class);
    }
}
