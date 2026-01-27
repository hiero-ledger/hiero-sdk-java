// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hedera.hashgraph.sdk.proto.TransactionBody;
import org.junit.jupiter.api.Test;

class NftHookCallTest {
    @Test
    void constructorWithNumericIdAndType() {
        var evm = new EvmHookCall(new byte[] {}, 25_000L);
        var call = new NftHookCall(2L, evm, NftHookType.PRE_HOOK_SENDER);
        assertThat(call.getType()).isEqualTo(NftHookType.PRE_HOOK_SENDER);
    }

    @Test
    void nullTypeThrows() {
        var evm = new EvmHookCall(new byte[] {}, 1L);
        assertThatThrownBy(() -> new NftHookCall(1L, evm, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type cannot be null");
    }

    @Test
    void nftTransferSerializesSenderAndReceiverHooksByType() {
        var tx = new TransferTransaction();
        var token = new TokenId(0, 0, 7777);
        var nftId = new NftId(token, 1L);
        var sender = new AccountId(0, 0, 8001);
        var receiver = new AccountId(0, 0, 8002);

        var senderHook = new NftHookCall(2L, new EvmHookCall(new byte[] {}, 10L), NftHookType.PRE_HOOK_SENDER);
        var receiverHook = new NftHookCall(3L, new EvmHookCall(new byte[] {}, 10L), NftHookType.PRE_POST_HOOK_RECEIVER);

        tx.addNftTransferWithHook(nftId, sender, receiver, senderHook, receiverHook);

        var body = tx.build();
        var hasSenderPre = body.getTokenTransfersList().stream()
                .flatMap(tl -> tl.getNftTransfersList().stream())
                .anyMatch(t -> t.hasPreTxSenderAllowanceHook());
        var hasReceiverPrePost = body.getTokenTransfersList().stream()
                .flatMap(tl -> tl.getNftTransfersList().stream())
                .anyMatch(t -> t.hasPrePostTxReceiverAllowanceHook());
        assertThat(hasSenderPre).isTrue();
        assertThat(hasReceiverPrePost).isTrue();

        // Round-trip parse back
        var rebuilt = new TransferTransaction(
                TransactionBody.newBuilder().setCryptoTransfer(body).build());
        var rebuiltNfts = rebuilt.getTokenNftTransfers();
        assertThat(rebuiltNfts.get(token)).hasSize(1);
    }
}
