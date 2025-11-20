// SPDX-License-Identifier: Apache-2.0
package com.hedera.hashgraph.sdk;

import java.util.Objects;

/**
 * A typed hook call for NFT transfers.
 */
public class NftHookCall extends HookCall {
    private final NftHookType type;

    public NftHookCall(long hookId, EvmHookCall evmHookCall, NftHookType type) {
        super(hookId, evmHookCall);
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    public NftHookType getType() {
        return type;
    }
}
