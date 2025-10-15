// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HookCallTest {
    @Test
    void toFromProtoUsesFullIdWhenProvided() {
        var full = new HookId(new HookEntityId(new AccountId(0, 0, 42)), 9L);
        var evm = new EvmHookCall(new byte[] {1, 2}, 123L);
        var call = new FungibleHookCall(full, evm, FungibleHookType.PRE_TX_ALLOWANCE_HOOK);

        var proto = call.toProtobuf();
        assertThat(proto.hasFullHookId()).isTrue();
        assertThat(proto.hasHookId()).isFalse();

        var parsedBase = HookCall.fromProtobuf(proto);
        assertThat(parsedBase.hasFullHookId()).isTrue();
    }

    @Test
    void toFromProtoUsesNumericIdWhenNoFullId() {
        var evm = new EvmHookCall(new byte[] {}, 1L);
        var call = new NftHookCall(7L, evm, NftHookType.PRE_HOOK_SENDER);

        var proto = call.toProtobuf();
        assertThat(proto.hasFullHookId()).isFalse();
        assertThat(proto.hasHookId()).isTrue();

        var parsedBase = HookCall.fromProtobuf(proto);
        assertThat(parsedBase.hasFullHookId()).isFalse();
    }
}
