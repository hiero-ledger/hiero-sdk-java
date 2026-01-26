// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hedera.hashgraph.sdk.proto.TransactionBody;
import org.junit.jupiter.api.Test;

class FungibleHookCallTest {
    @Test
    void constructorWithNumericIdAndType() {
        var evm = new EvmHookCall(new byte[] {}, 25_000L);
        var call = new FungibleHookCall(2L, evm, FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
        assertThat(call.getType()).isEqualTo(FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
    }

    @Test
    void nullTypeThrows() {
        var evm = new EvmHookCall(new byte[] {}, 1L);
        assertThatThrownBy(() -> new FungibleHookCall(1L, evm, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type cannot be null");
    }

    @Test
    void hbarTransferSerializesHookByType() {
        var tx = new TransferTransaction();
        var accountId = new AccountId(0, 0, 123);
        var hookPre =
                new FungibleHookCall(2L, new EvmHookCall(new byte[] {}, 10L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
        var hookPrePost = new FungibleHookCall(
                3L, new EvmHookCall(new byte[] {}, 10L), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);

        tx.addHbarTransferWithHook(accountId, Hbar.fromTinybars(1), hookPre);
        tx.addHbarTransferWithHook(new AccountId(0, 0, 124), Hbar.fromTinybars(2), hookPrePost);

        var body = tx.build();
        var list = body.getTransfers().getAccountAmountsList();
        assertThat(list.stream().anyMatch(a -> a.hasPreTxAllowanceHook())).isTrue();
        assertThat(list.stream().anyMatch(a -> a.hasPrePostTxAllowanceHook())).isTrue();

        // Round-trip
        var rebuilt = new TransferTransaction(
                TransactionBody.newBuilder().setCryptoTransfer(body).build());
        assertThat(rebuilt.getHbarTransfers().get(accountId)).isEqualTo(Hbar.fromTinybars(1));
    }

    @Test
    void tokenTransferSerializesHookByType() {
        var tx = new TransferTransaction();
        var token = new TokenId(0, 0, 3333);
        var sender = new AccountId(0, 0, 5001);
        var hookPre =
                new FungibleHookCall(2L, new EvmHookCall(new byte[] {}, 10L), FungibleHookType.PRE_TX_ALLOWANCE_HOOK);
        var hookPrePost = new FungibleHookCall(
                3L, new EvmHookCall(new byte[] {}, 10L), FungibleHookType.PRE_POST_TX_ALLOWANCE_HOOK);

        tx.addTokenTransferWithHook(token, sender, -100, hookPre);
        tx.addTokenTransfer(token, new AccountId(0, 0, 5002), 100);
        tx.addTokenTransferWithHook(token, new AccountId(0, 0, 5003), -200, hookPrePost);
        tx.addTokenTransfer(token, new AccountId(0, 0, 5004), 200);

        var body = tx.build();
        var anyPre = body.getTokenTransfersList().stream()
                .flatMap(tl -> tl.getTransfersList().stream())
                .anyMatch(a -> a.hasPreTxAllowanceHook());
        var anyPrePost = body.getTokenTransfersList().stream()
                .flatMap(tl -> tl.getTransfersList().stream())
                .anyMatch(a -> a.hasPrePostTxAllowanceHook());
        assertThat(anyPre).isTrue();
        assertThat(anyPrePost).isTrue();

        // Round-trip parse back
        var rebuilt = new TransferTransaction(
                TransactionBody.newBuilder().setCryptoTransfer(body).build());
        var tokenTransfers = rebuilt.getTokenTransfers();
        assertThat(tokenTransfers.get(token).get(sender)).isEqualTo(-100L);
    }
}
